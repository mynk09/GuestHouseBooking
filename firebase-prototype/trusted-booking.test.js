import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { deleteApp as deleteAdmin, initializeApp as initAdmin } from 'firebase-admin/app';
import { getAuth as getAdminAuth } from 'firebase-admin/auth';
import { getFirestore as getAdminDb } from 'firebase-admin/firestore';
import { initializeApp, deleteApp } from 'firebase/app';
import { connectAuthEmulator, createUserWithEmailAndPassword, getAuth } from 'firebase/auth';
import { connectFirestoreEmulator, doc, getDoc, getFirestore, setDoc } from 'firebase/firestore';
import { bookingHttpServer } from './booking-http.js';
import { trustedBookingService } from './trusted-booking-service.js';

const projectId = 'demo-guesthouse-trusted';
const property = 'guesthouse';
const apps = [];
const users = {};
const allRooms = [
  ...[[101, 108], [201, 210], [301, 310], [401, 410]]
    .flatMap(([first, last]) => Array.from({ length: last - first + 1 }, (_, i) => String(first + i))),
  '211', '311', '411', '511'
];
let admin, db, change;

function command(groupId, changes, startDay = 21000, nights = 31) {
  return { property, groupId, requestId: groupId, version: 0, changes,
    dates: { startDay, endDay: startDay + nights } };
}

function edit(groupId, requestId, version, changes) {
  return { property, groupId, requestId, version, changes };
}

const groupPath = id => `properties/${property}/bookingGroups/${id}`;
const reservationPath = (room, id) => `properties/${property}/rooms/${room}/reservations/${id}`;

before(async () => {
  for (const variable of ['FIREBASE_AUTH_EMULATOR_HOST', 'FIRESTORE_EMULATOR_HOST']) {
    assert.match(process.env[variable] ?? '', /^127\.0\.0\.1:\d+$/);
  }
  admin = initAdmin({ projectId });
  db = getAdminDb(admin);
  change = trustedBookingService(getAdminAuth(admin), db);
  const batch = db.batch();
  for (const role of ['Owner', 'Manager', 'Viewer', 'Outsider']) {
    const app = initializeApp({ projectId, apiKey: 'demo-only' }, role);
    apps.push(app);
    const auth = getAuth(app);
    connectAuthEmulator(auth, `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}`, { disableWarnings: true });
    const clientDb = getFirestore(app);
    const [host, port] = process.env.FIRESTORE_EMULATOR_HOST.split(':');
    connectFirestoreEmulator(clientDb, host, Number(port));
    const { user } = await createUserWithEmailAndPassword(auth, `${role}@example.test`, 'demo-password');
    users[role] = { token: await user.getIdToken(), db: clientDb, uid: user.uid };
    const site = role === 'Outsider' ? 'other' : property;
    batch.set(db.doc(`properties/${site}/members/${user.uid}`), { role: role === 'Outsider' ? 'Owner' : role });
  }
  for (const room of allRooms) {
    batch.set(db.doc(`properties/${property}/rooms/${room}`), { active: true, capacity: 2, revision: 0 });
  }
  await batch.commit();
});

after(async () => {
  await Promise.all(apps.map(deleteApp));
  if (admin) await deleteAdmin(admin);
});

test('all 42 rooms can be booked for 31 nights and selected rooms can be released', async () => {
  assert.equal(allRooms.length, 42);
  const create = command('full-house', Object.fromEntries(allRooms.map(room => [room, 1])));
  const initial = await change(users.Owner.token, create);
  assert.deepEqual(initial, { groupId: 'full-house', version: 1, status: 'active' });
  const reservations = await db.getAll(...allRooms.map(room => db.doc(reservationPath(room, 'full-house'))));
  assert.ok(reservations.every(room => room.get('endDay') - room.get('startDay') === 31));
  const partial = edit('full-house', 'partial', 1, { 101: null, 102: null, 103: 2 });
  const updated = await change(users.Manager.token, partial);
  assert.equal(updated.version, 2);
  assert.deepEqual(await change(users.Manager.token, partial), updated);
  await assert.rejects(change(users.Manager.token, { ...partial, changes: { 104: null } }), { code: 'request-id-reused' });
  const group = (await db.doc(groupPath('full-house')).get()).data();
  assert.equal(Object.keys(group.rooms).length, 40);
  assert.equal(group.rooms['103'], 2);
  assert.equal(group.rooms['104'], 1);
  assert.equal((await db.doc(reservationPath('101', 'full-house')).get()).exists, false);
  assert.equal((await db.doc(reservationPath('102', 'full-house')).get()).exists, false);
  assert.equal((await db.doc(reservationPath('103', 'full-house')).get()).get('guestCount'), 2);
  assert.deepEqual(await change(users.Owner.token, create), initial);
  await change(users.Manager.token, command('reuse', { 101: 1, 102: 2 }));
  await assert.rejects(change(users.Manager.token, command('remains-held', { 104: 1 })), { code: 'room-unavailable' });
});

test('one room conflict rejects a whole-property request and adjacent stays are accepted', async () => {
  await change(users.Owner.token, command('one-room', { 511: 1 }, 21100));
  const full = command('blocked-house', Object.fromEntries(allRooms.map(room => [room, 1])), 21100);
  await assert.rejects(change(users.Manager.token, full), { code: 'room-unavailable' });
  const refs = [db.doc(groupPath('blocked-house')), ...allRooms.map(room => db.doc(reservationPath(room, 'blocked-house')))];
  assert.ok((await db.getAll(...refs)).every(snapshot => !snapshot.exists));
  await change(users.Manager.token, command('adjacent', { 511: 1 }, 21131));
  await assert.rejects(change(users.Manager.token, command('too-long', { 101: 1 }, 21200, 32)), { code: 'invalid-request' });
});

test('adding a room races a new booking and leaves existing rooms intact', async () => {
  await change(users.Owner.token, command('editable', { 101: 1 }, 21200));
  const results = await Promise.allSettled([
    change(users.Owner.token, edit('editable', 'add-room', 1, { 102: 1 })),
    change(users.Manager.token, command('competing', { 102: 1 }, 21200))
  ]);
  assert.equal(results.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(results.find(result => result.status === 'rejected').reason.code, 'room-unavailable');
  const original = await db.doc(reservationPath('101', 'editable')).get();
  assert.equal(original.get('guestCount'), 1);
  const competing = await db.getAll(...['editable', 'competing'].map(id => db.doc(reservationPath('102', id))));
  assert.equal(competing.filter(snapshot => snapshot.exists).length, 1);
});

test('stale edits, capacity violations and conflicts do not change existing rooms', async () => {
  await change(users.Owner.token, command('original', { 101: 1, 102: 2 }, 21300));
  await change(users.Manager.token, command('occupied', { 103: 1 }, 21300));
  await assert.rejects(change(users.Owner.token, edit('original', 'over-capacity', 1, { 101: 3, 102: null })),
    { code: 'invalid-room-change' });
  await assert.rejects(change(users.Owner.token, edit('original', 'mixed-conflict', 1, { 101: null, 103: 1 })),
    { code: 'room-unavailable' });
  const saved = await change(users.Owner.token, edit('original', 'add-and-reduce-guests', 1, { 102: 1, 104: 1 }));
  await assert.rejects(change(users.Manager.token, edit('original', 'stale-cancel', 1, { 101: null })),
    { code: 'stale-version' });
  assert.deepEqual((await db.doc(groupPath('original')).get()).get('rooms'), { 101: 1, 102: 1, 104: 1 });
  assert.equal((await db.doc(reservationPath('104', 'original')).get()).get('guestCount'), 1);
  await change(users.Owner.token, edit('original', 'last-rooms', saved.version, { 101: null, 102: null, 104: null }));
  assert.equal((await db.doc(groupPath('original')).get()).get('status'), 'cancelled');
});

test('the backend verifies identity and clients cannot bypass booking rules', async () => {
  const data = command('protected', { 101: 1 }, 21400);
  await assert.rejects(change('invalid-token', data), { code: 'unauthenticated' });
  await assert.rejects(change(users.Viewer.token, data), { code: 'access-denied' });
  await assert.rejects(change(users.Outsider.token, data), { code: 'access-denied' });
  await change(users.Manager.token, data);
  assert.equal((await getDoc(doc(users.Viewer.db, groupPath('protected')))).exists(), true);
  await assert.rejects(getDoc(doc(users.Outsider.db, groupPath('protected'))), { code: 'permission-denied' });
  for (const path of [groupPath('protected'), reservationPath('101', 'protected'),
    `properties/${property}/rooms/101`, `properties/${property}/bookingRequests/protected`,
    `properties/${property}/bookings/legacy`]) {
    await assert.rejects(setDoc(doc(users.Owner.db, path), { forged: true }), { code: 'permission-denied' });
  }
  await db.doc(`properties/${property}/members/${users.Manager.uid}`).delete();
  await assert.rejects(change(users.Manager.token, data), { code: 'access-denied' });
});

test('the HTTP API accepts a Firebase bearer token and exposes conflict and transport errors', async () => {
  const server = bookingHttpServer(change);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const data = command('http-owner', { 101: 1, 102: 1 }, 21500);
  const send = (body, token = users.Owner.token) => fetch(`${base}/booking-changes`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body
  });
  try {
    assert.equal((await fetch(`${base}/booking-changes`)).status, 405);
    assert.equal((await send(JSON.stringify(data), 'invalid-token')).status, 401);
    assert.equal((await send('{')).status, 400);
    assert.equal((await send(' '.repeat(33 * 1024))).status, 413);
    const created = await send(JSON.stringify(data));
    assert.equal(created.status, 200);
    assert.equal((await created.json()).version, 1);
    const conflict = await send(JSON.stringify(command('http-conflict', { 101: 1 }, 21500)));
    assert.equal(conflict.status, 409);
    assert.deepEqual(await conflict.json(), { error: 'room-unavailable' });
    const partial = await send(JSON.stringify(edit('http-owner', 'http-edit', 1, { 101: null })));
    assert.equal(partial.status, 200);
    assert.deepEqual((await db.doc(groupPath('http-owner')).get()).get('rooms'), { 102: 1 });
  } finally {
    await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  }
});
