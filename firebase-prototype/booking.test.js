import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { once } from 'node:events';
import { connect, createServer } from 'node:net';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteApp, initializeApp } from 'firebase/app';
import {
  connectAuthEmulator, createUserWithEmailAndPassword, getAuth, signInWithEmailAndPassword
} from 'firebase/auth';
import {
  collection, connectFirestoreEmulator, deleteDoc, disableNetwork, doc,
  getDocFromCache, getDocFromServer, getDocsFromServer, getFirestore, serverTimestamp,
  setDoc, updateDoc, writeBatch
} from 'firebase/firestore';
import { bookingStore } from './booking-store.js';

const projectId = 'demo-guesthouse-booking';
const property = 'guesthouse';
const otherProperty = 'other';
const users = {};
const apps = [];
let env;

function emulatorHost(variable) {
  const address = process.env[variable];
  assert.match(address ?? '', /^127\.0\.0\.1:\d+$/, `${variable} must point to a local emulator`);
  return address;
}

function newClient(name, firestoreAddress = emulatorHost('FIRESTORE_EMULATOR_HOST')) {
  const app = initializeApp({ projectId, apiKey: 'demo-emulator-only' }, name);
  apps.push(app);
  const auth = getAuth(app);
  connectAuthEmulator(auth, `http://${emulatorHost('FIREBASE_AUTH_EMULATOR_HOST')}`, { disableWarnings: true });
  const db = getFirestore(app);
  const [host, port] = firestoreAddress.split(':');
  connectFirestoreEmulator(db, host, Number(port));
  return { auth, db };
}

function ref(db, group, id, site = property) {
  return doc(db, 'properties', site, group, id);
}

function member(db, user, site = property) {
  return ref(db, 'members', user.uid, site);
}

function records(db, group, site = property) {
  return getDocsFromServer(collection(db, 'properties', site, group));
}

function stay(rooms = [101], startDay = 21000, endDay = startDay + 1) {
  return { rooms, startDay, endDay, guestName: 'Test Guest' };
}

function bookingData(user, data) {
  return { ...data, status: 'active', createdBy: user.uid, createdAt: serverTimestamp() };
}

function directCreate(user, id, data) {
  const batch = writeBatch(user.db);
  batch.set(ref(user.db, 'bookings', id), bookingData(user, data));
  for (const room of data.rooms) {
    for (let day = data.startDay; day < data.endDay; day++) {
      batch.set(ref(user.db, 'occupancy', `${room}_${day}`), { bookingId: id, room, day });
    }
  }
  return batch;
}

before(async () => {
  emulatorHost('FIREBASE_AUTH_EMULATOR_HOST');
  emulatorHost('FIRESTORE_EMULATOR_HOST');
  env = await initializeTestEnvironment({ projectId });
  for (const name of ['owner', 'manager', 'viewer', 'outsider']) {
    const { auth, db } = newClient(name);
    const email = `${name}@example.test`;
    const password = 'demo-personal-password';
    await createUserWithEmailAndPassword(auth, email, password);
    await auth.signOut();
    const { user } = await signInWithEmailAndPassword(auth, email, password);
    users[name] = { uid: user.uid, db, store: bookingStore(db, property, user.uid) };
  }
  users.anonymous = newClient('anonymous');
});

beforeEach(async () => {
  await env.clearFirestore();
  const { owner, manager, viewer, outsider } = users;
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    const batch = writeBatch(db);
    batch.set(member(db, owner), { role: 'Owner' });
    batch.set(member(db, manager), { role: 'Manager' });
    batch.set(member(db, viewer), { role: 'Viewer' });
    batch.set(member(db, outsider, otherProperty), { role: 'Owner' });
    for (const room of ['101', '102', '103', '104']) {
      batch.set(ref(db, 'rooms', room), { active: true });
    }
    batch.set(ref(db, 'rooms', '105'), { active: false });
    await batch.commit();
  });
});

after(async () => {
  await Promise.all(apps.map(deleteApp));
  await env?.cleanup();
});

test('email/password sessions receive distinct UIDs and permissions follow property membership', async () => {
  const { owner, manager, viewer, outsider, anonymous } = users;
  assert.equal(new Set([owner.uid, manager.uid, viewer.uid, outsider.uid]).size, 4);
  const id = randomUUID();
  await manager.store.create(id, stay());
  assert.equal((await records(owner.db, 'bookings')).size, 1);
  assert.equal((await records(viewer.db, 'bookings')).size, 1);
  assert.equal((await records(viewer.db, 'rooms')).size, 5);
  await assertFails(records(anonymous.db, 'bookings'));
  await assertFails(records(outsider.db, 'bookings'));
  await assertFails(records(owner.db, 'bookings', otherProperty));
  await assertFails(getDocFromServer(ref(outsider.db, 'bookings', id)));
  await assertFails(directCreate(viewer, randomUUID(), stay([102])).commit());
  await assertFails(directCreate(outsider, randomUUID(), stay([102])).commit());
  const cancel = writeBatch(viewer.db);
  cancel.update(ref(viewer.db, 'bookings', id), {
    status: 'cancelled', cancelledBy: viewer.uid, cancelledAt: serverTimestamp()
  });
  cancel.delete(ref(viewer.db, 'occupancy', '101_21000'));
  await assertFails(cancel.commit());
  await assertFails(deleteDoc(ref(owner.db, 'bookings', id)));
  await owner.store.cancel(id);
});

test('Owner provisions, changes and revokes staff; staff cannot grant or elevate roles', async () => {
  const { owner, manager, viewer, outsider } = users;
  await assertFails(setDoc(member(manager.db, manager), { role: 'Owner' }));
  await assertFails(setDoc(member(viewer.db, outsider), { role: 'Manager' }));
  await assertFails(setDoc(member(outsider.db, outsider), { role: 'Manager' }));
  await assertFails(setDoc(member(owner.db, outsider), { role: 'Owner' }));
  await assertFails(deleteDoc(member(owner.db, owner)));
  await assertFails(records(manager.db, 'members'));
  assert.equal((await records(owner.db, 'members')).size, 3);
  await setDoc(member(owner.db, outsider), { role: 'Viewer' });
  assert.equal((await records(outsider.db, 'bookings')).size, 0);
  await setDoc(member(owner.db, outsider), { role: 'Manager' });
  await outsider.store.create(randomUUID(), stay());
  await deleteDoc(member(owner.db, outsider));
  await assertFails(records(outsider.db, 'bookings'));
  await assertFails(directCreate(outsider, randomUUID(), stay([102])).commit());
});

test('two authenticated clients racing for the same room confirm exactly one transaction', async () => {
  const { owner, manager } = users;
  const outcomes = await Promise.allSettled([
    owner.store.create(randomUUID(), stay([101, 102], 21000, 21002)),
    manager.store.create(randomUUID(), stay([101, 103], 21001, 21003))
  ]);
  assert.equal(outcomes.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(outcomes.find(result => result.status === 'rejected').reason.code, 'room-unavailable');
  const saved = await records(owner.db, 'bookings');
  const slots = await records(owner.db, 'occupancy');
  assert.equal(saved.size, 1);
  assert.equal(slots.size, 4);
  assert.ok(slots.docs.every(slot => slot.data().bookingId === saved.docs[0].id));
});

test('direct atomic writes cannot both claim the same slot, even without transaction reads', async () => {
  const { owner, manager } = users;
  const outcomes = await Promise.allSettled([
    directCreate(owner, randomUUID(), stay()).commit(),
    directCreate(manager, randomUUID(), stay()).commit()
  ]);
  assert.equal(outcomes.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(outcomes.find(result => result.status === 'rejected').reason.code, 'permission-denied');
  assert.equal((await records(owner.db, 'bookings')).size, 1);
  assert.equal((await records(owner.db, 'occupancy')).size, 1);
});

test('conflict rejects the full group, while adjacent stays and different rooms are accepted', async () => {
  const { owner, manager } = users;
  await owner.store.create(randomUUID(), stay([101], 21000, 21002));
  const rejected = randomUUID();
  await assert.rejects(manager.store.create(rejected, stay([101, 102])), { code: 'room-unavailable' });
  assert.equal((await getDocFromServer(ref(owner.db, 'bookings', rejected))).exists(), false);
  assert.equal((await getDocFromServer(ref(owner.db, 'occupancy', '102_21000'))).exists(), false);
  await manager.store.create(randomUUID(), stay([101], 21002));
  await manager.store.create(randomUUID(), stay([102]));
  assert.equal((await records(owner.db, 'bookings')).size, 3);
});

test('create and cancellation retries are idempotent and cancellation frees the whole group', async () => {
  const { owner, manager } = users;
  const id = randomUUID();
  const data = stay([101, 102], 21000, 21002);
  assert.deepEqual(await manager.store.create(id, data), { id, status: 'active' });
  assert.deepEqual(await manager.store.create(id, { ...data, rooms: [102, 101] }), { id, status: 'active' });
  await assert.rejects(manager.store.create(id, { ...data, guestName: 'Different Guest' }), { code: 'request-id-reused' });
  await assert.rejects(owner.store.create(id, data), { code: 'request-id-reused' });
  assert.equal((await records(owner.db, 'bookings')).size, 1);
  assert.deepEqual(await owner.store.cancel(id), { id, status: 'cancelled' });
  const cancelledAt = (await getDocFromServer(ref(owner.db, 'bookings', id))).data().cancelledAt;
  assert.deepEqual(await manager.store.cancel(id), { id, status: 'cancelled' });
  assert.deepEqual(await manager.store.create(id, data), { id, status: 'cancelled' });
  assert.ok(cancelledAt.isEqual((await getDocFromServer(ref(owner.db, 'bookings', id))).data().cancelledAt));
  assert.equal((await records(owner.db, 'occupancy')).size, 0);
  await manager.store.create(randomUUID(), data);
});

test('raw writes cannot skip, add, alias, delete or repoint occupancy', async () => {
  const { manager, owner } = users;
  const id = randomUUID();
  const booking = ref(manager.db, 'bookings', id);
  await assertFails(setDoc(booking, bookingData(manager, stay([101, 102]))));
  const incomplete = writeBatch(manager.db);
  incomplete.set(booking, bookingData(manager, stay([101], 21000, 21002)));
  incomplete.set(ref(manager.db, 'occupancy', '101_21000'), { bookingId: id, room: 101, day: 21000 });
  await assertFails(incomplete.commit());
  const extra = directCreate(manager, id, stay());
  extra.set(ref(manager.db, 'occupancy', '101_21001'), { bookingId: id, room: 101, day: 21001 });
  await assertFails(extra.commit());
  const alias = directCreate(manager, id, stay());
  alias.set(ref(manager.db, 'occupancy', '0101_21000'), { bookingId: id, room: 101, day: 21000 });
  await assertFails(alias.commit());
  assert.equal((await records(owner.db, 'bookings')).size, 0);
  assert.equal((await records(owner.db, 'occupancy')).size, 0);

  await manager.store.create(id, stay([101, 102]));
  const slot = ref(manager.db, 'occupancy', '101_21000');
  await assertFails(updateDoc(slot, { bookingId: randomUUID() }));
  await assertFails(deleteDoc(slot));
  await assertFails(updateDoc(booking, { endDay: 21002 }));
  const incompleteCancel = writeBatch(manager.db);
  incompleteCancel.update(booking, { status: 'cancelled', cancelledBy: manager.uid, cancelledAt: serverTimestamp() });
  incompleteCancel.delete(slot);
  await assertFails(incompleteCancel.commit());
  const replacementId = randomUUID();
  const replacement = writeBatch(manager.db);
  replacement.update(booking, { status: 'cancelled', cancelledBy: manager.uid, cancelledAt: serverTimestamp() });
  replacement.delete(slot);
  replacement.delete(ref(manager.db, 'occupancy', '102_21000'));
  replacement.set(ref(manager.db, 'bookings', replacementId), bookingData(manager, stay()));
  replacement.set(slot, { bookingId: replacementId, room: 101, day: 21000 });
  await assertFails(replacement.commit());
  assert.equal((await getDocFromServer(booking)).data().status, 'active');
  assert.equal((await getDocFromServer(ref(manager.db, 'bookings', replacementId))).exists(), false);
  assert.equal((await records(owner.db, 'occupancy')).size, 2);
  await manager.store.cancel(id);
  await assertFails(setDoc(slot, { bookingId: id, room: 101, day: 21000 }));
  await assertFails(updateDoc(booking, { status: 'active' }));
});

test('booking and cancellation work at supported bounds; raw oversized and malformed writes fail', async () => {
  const { manager, owner } = users;
  for (const data of [stay([101], 21000, 21005), stay([101, 102], 21000, 21002), stay([101, 102, 103])]) {
    const id = randomUUID();
    await manager.store.create(id, data);
    assert.equal((await records(owner.db, 'occupancy')).size, data.rooms.length * (data.endDay - data.startDay));
    await owner.store.cancel(id);
    assert.equal((await records(owner.db, 'occupancy')).size, 0);
  }
  for (const data of [stay([101], 21000, 21006), stay([101, 102], 21000, 21003),
    stay([101, 102, 103, 104]), stay([101, 101]), stay([105]), stay([9999]), stay([101], 21000, 21000)]) {
    await assertFails(directCreate(manager, randomUUID(), data).commit());
  }
  const id = randomUUID();
  const spoof = directCreate(manager, id, stay());
  spoof.set(ref(manager.db, 'bookings', id), { ...bookingData(manager, stay()), createdBy: owner.uid });
  await assertFails(spoof.commit());
  assert.equal((await records(owner.db, 'occupancy')).size, 0);
  assert.equal((await records(owner.db, 'bookings')).size, 3);
});

test('a cached booking remains readable offline and an offline transaction never confirms', async () => {
  const { manager, owner } = users;
  const id = randomUUID();
  await manager.store.create(id, stay());

  // The Node SDK's disableNetwork alone does not block direct transaction RPCs.
  const [host, port] = emulatorHost('FIRESTORE_EMULATOR_HOST').split(':');
  const sockets = new Set();
  const proxy = createServer(inbound => {
    const upstream = connect({ host, port: Number(port) });
    for (const socket of [inbound, upstream]) {
      sockets.add(socket);
      socket.on('close', () => sockets.delete(socket));
      socket.on('error', () => socket.destroy());
    }
    inbound.pipe(upstream).pipe(inbound);
  });
  proxy.listen(0, '127.0.0.1');
  await once(proxy, 'listening');
  const offlineId = randomUUID();
  try {
    const { auth, db } = newClient('offline', `127.0.0.1:${proxy.address().port}`);
    const { user } = await signInWithEmailAndPassword(auth, 'manager@example.test', 'demo-personal-password');
    const booking = ref(db, 'bookings', id);
    await getDocFromServer(booking);
    await disableNetwork(db);
    proxy.close();
    for (const socket of sockets) socket.destroy();
    assert.equal((await getDocFromCache(booking)).data().status, 'active');
    await assert.rejects(bookingStore(db, property, user.uid).create(offlineId, stay([102])), { code: 'unavailable' });
  } finally {
    if (proxy.listening) proxy.close();
    for (const socket of sockets) socket.destroy();
  }
  assert.equal((await getDocFromServer(ref(owner.db, 'bookings', offlineId))).exists(), false);
});
