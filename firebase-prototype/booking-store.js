import { collection, doc, runTransaction, serverTimestamp } from 'firebase/firestore';

export class BookingError extends Error {
  constructor(code) {
    super(code);
    this.code = code;
  }
}

function validate(booking) {
  const { rooms, startDay, endDay, guestName } = booking;
  if (!Array.isArray(rooms) || rooms.length < 1 || rooms.length > 3 ||
      new Set(rooms).size !== rooms.length ||
      rooms.some(room => !Number.isInteger(room) || room < 1 || room > 9999) ||
      !Number.isInteger(startDay) || !Number.isInteger(endDay) ||
      startDay < 0 || endDay > 100000 || endDay <= startDay ||
      rooms.length * (endDay - startDay) > 5 ||
      typeof guestName !== 'string' || !guestName.trim() || guestName.length > 200) {
    throw new BookingError('invalid-booking');
  }
}

export function bookingStore(db, property, uid) {
  const bookings = collection(db, 'properties', property, 'bookings');
  const occupancy = collection(db, 'properties', property, 'occupancy');
  const membership = doc(db, 'properties', property, 'members', uid);

  async function requireWriter(tx) {
    const role = (await tx.get(membership)).data()?.role;
    if (role !== 'Owner' && role !== 'Manager') throw new BookingError('access-denied');
  }

  function bookingRef(id) {
    if (typeof id !== 'string' || !/^[A-Za-z0-9_-]{16,64}$/.test(id)) {
      throw new BookingError('invalid-id');
    }
    return doc(bookings, id);
  }

  function roomNights({ rooms, startDay, endDay }) {
    return rooms.flatMap(room => Array.from({ length: endDay - startDay }, (_, offset) => {
      const day = startDay + offset;
      return { room, day, ref: doc(occupancy, `${room}_${day}`) };
    }));
  }

  return {
    async create(id, input) {
      validate(input);
      const ref = bookingRef(id);
      const { startDay, endDay, guestName } = input;
      const rooms = [...input.rooms].sort((a, b) => a - b);
      const data = { rooms, startDay, endDay, guestName };
      const slots = roomNights(data);
      return runTransaction(db, async tx => {
        await requireWriter(tx);
        const prior = await tx.get(ref);
        if (prior.exists()) {
          const saved = prior.data();
          if (saved.createdBy !== uid || saved.startDay !== startDay || saved.endDay !== endDay ||
              saved.guestName !== guestName || saved.rooms.length !== rooms.length ||
              rooms.some(room => !saved.rooms.includes(room))) {
            throw new BookingError('request-id-reused');
          }
          return { id, status: saved.status };
        }

        const occupied = await Promise.all(slots.map(({ ref }) => tx.get(ref)));
        if (occupied.some(snapshot => snapshot.exists())) {
          throw new BookingError('room-unavailable');
        }

        tx.set(ref, { ...data, status: 'active', createdBy: uid, createdAt: serverTimestamp() });
        for (const { ref, room, day } of slots) {
          tx.set(ref, { bookingId: id, room, day });
        }
        return { id, status: 'active' };
      });
    },

    async cancel(id) {
      const ref = bookingRef(id);
      return runTransaction(db, async tx => {
        await requireWriter(tx);
        const snapshot = await tx.get(ref);
        if (!snapshot.exists()) throw new BookingError('booking-not-found');
        const saved = snapshot.data();
        if (saved.status === 'active') {
          tx.update(ref, { status: 'cancelled', cancelledBy: uid, cancelledAt: serverTimestamp() });
          for (const slot of roomNights(saved)) tx.delete(slot.ref);
        }
        return { id, status: 'cancelled' };
      });
    }
  };
}
