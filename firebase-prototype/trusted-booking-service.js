import { FieldValue } from 'firebase-admin/firestore';

class BookingError extends Error {
  constructor(code) {
    super(code);
    this.code = code;
  }
}

const validId = value => typeof value === 'string' && /^[A-Za-z0-9_-]{1,64}$/.test(value);
const validRoom = value => typeof value === 'string' && /^[1-9][0-9]{0,3}$/.test(value);
const maxNights = 31;

function normalize(command) {
  const { property, groupId, requestId, version, changes, dates } = command;
  if (![property, groupId, requestId].every(validId) || !Number.isSafeInteger(version) || version < 0 ||
      !changes || typeof changes !== 'object' || Array.isArray(changes)) {
    throw new BookingError('invalid-request');
  }
  const entries = Object.entries(changes).sort(([a], [b]) => a.localeCompare(b));
  if (!entries.length || entries.some(([room, count]) =>
    !validRoom(room) || (count !== null && (!Number.isSafeInteger(count) || count < 1)))) {
    throw new BookingError('invalid-request');
  }
  if (version === 0) {
    if (!Number.isInteger(dates?.startDay) || !Number.isInteger(dates?.endDay) ||
        dates.startDay < 0 || dates.endDay > 100000 || dates.endDay <= dates.startDay ||
        dates.endDay - dates.startDay > maxNights || entries.some(([, count]) => count === null)) {
      throw new BookingError('invalid-request');
    }
  } else if (dates !== undefined) {
    throw new BookingError('invalid-request');
  }
  return { property, groupId, requestId, version, entries,
    dates: dates ? { startDay: dates.startDay, endDay: dates.endDay } : null };
}

export function trustedBookingService(auth, db) {
  return async function changeBooking(idToken, input) {
    let uid;
    try {
      ({ uid } = await auth.verifyIdToken(idToken));
    } catch {
      throw new BookingError('unauthenticated');
    }
    const command = normalize(input ?? {});
    const { property, groupId, requestId, version, entries, dates } = command;
    const scope = db.collection('properties').doc(property);
    const groupRef = scope.collection('bookingGroups').doc(groupId);
    const requestRef = scope.collection('bookingRequests').doc(requestId);
    const fingerprint = JSON.stringify(command);

    return db.runTransaction(async tx => {
      const member = await tx.get(scope.collection('members').doc(uid));
      if (!['Owner', 'Manager'].includes(member.data()?.role)) throw new BookingError('access-denied');

      const [previous, groupSnapshot] = await tx.getAll(requestRef, groupRef);
      if (previous.exists) {
        const saved = previous.data();
        if (saved.uid !== uid || saved.fingerprint !== fingerprint) throw new BookingError('request-id-reused');
        return saved.result;
      }

      const group = groupSnapshot.data();
      if ((group?.version ?? 0) !== version) throw new BookingError('stale-version');
      if (group?.status === 'cancelled') throw new BookingError('booking-cancelled');
      const { startDay, endDay } = group ?? dates;
      const rooms = { ...group?.rooms };
      const roomRefs = entries.map(([room]) => scope.collection('rooms').doc(room));
      const inventory = await tx.getAll(...roomRefs);

      for (const [index, [room, count]] of entries.entries()) {
        const existing = Object.hasOwn(rooms, room);
        const inventoryRoom = inventory[index].data();
        if (!inventoryRoom || (count === null && !existing) ||
            (count !== null && ((!existing && !inventoryRoom.active) || count > inventoryRoom.capacity))) {
          throw new BookingError('invalid-room-change');
        }
      }

      const additions = entries.flatMap(([room], index) =>
        Object.hasOwn(rooms, room) ? [] : [roomRefs[index]]);
      // Any reservation that could overlap started less than maxNights before check-in.
      const overlaps = await Promise.all(additions.map(room =>
        tx.get(room.collection('reservations')
          .where('startDay', '>', startDay - maxNights).where('startDay', '<', endDay))));
      if (overlaps.some(snapshot => snapshot.docs.some(doc => doc.get('endDay') > startDay))) {
        throw new BookingError('room-unavailable');
      }

      for (const [index, [room, count]] of entries.entries()) {
        const roomRef = roomRefs[index];
        const reservation = roomRef.collection('reservations').doc(groupId);
        const existed = Object.hasOwn(rooms, room);
        if (count === null) {
          delete rooms[room];
          tx.delete(reservation);
        } else {
          rooms[room] = count;
          tx.set(reservation, { startDay, endDay, guestCount: count });
        }
        if (count === null || !existed) {
          // Every occupancy change writes the same room document it read before checking overlap.
          tx.update(roomRef, { revision: FieldValue.increment(1) });
        }
      }

      const result = { groupId, version: version + 1, status: Object.keys(rooms).length ? 'active' : 'cancelled' };
      tx.set(groupRef, { ...result, startDay, endDay, rooms, updatedBy: uid, updatedAt: FieldValue.serverTimestamp() });
      tx.create(requestRef, { uid, fingerprint, result, at: FieldValue.serverTimestamp() });
      return result;
    });
  };
}
