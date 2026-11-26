# Firebase shared bookings prototype

Run from this directory with Node.js 24 and Java 21:

```sh
npm ci
npm test
```

The command runs Firebase Authentication and Firestore on localhost using the fixed `demo-guesthouse-booking` project ID. The test harness signs in with disposable email/password accounts and seeds the initial Owner and rooms through the emulator's privileged test context. There is no production project alias or deploy step. `booking-store.js` takes a Firestore client and a signed-in UID. It has no dependency on a sign-in provider. The Android app continues to use its Room repository.

## Rules and limits

- Firestore resolves roles from `properties/{property}/members/{uid}`. Owners can grant, change, and revoke Manager and Viewer membership. Initial Owners and room inventory are seeded by a trusted operator. Owners and Managers can create and cancel bookings, and Viewers can read bookings and room inventory.
- One create request has a stable ID, a shared date range for all rooms, and one occupancy document for each room and night. Dates are integer calendar days relative to 1970-01-01, with an exclusive checkout day. Each valid occupancy document uses the canonical `<room>_<day>` path. Firestore forbids updates to those documents. A new booking must own every required slot in the final commit, and each slot creation must refer to a newly created booking for that exact room and day. Cancellation keeps the booking record, releases all its slots atomically, and records the cancelling actor and server time.
- One request accepts up to three rooms and five room-nights. One room can have five nights, two rooms can have two nights each, and three rooms can have one night each. A create of one room for five nights uses at most 17 rule document lookups across the commit: one membership lookup, one room lookup, five slot lookups, and two booking lookups for each slot create. The booking write itself uses seven. The corresponding cancellation uses at most 16 across the commit and six on the booking write. These counts assume no rule lookup caching. Firestore documents ceilings of 20 across the atomic write and 10 per operation. [Firestore limits](https://firebase.google.com/docs/firestore/quotas#security_rules)
- A repeat create with the same ID, actor and payload reads the existing booking and returns its current status. A different request under the same ID fails. Retrying cancellation preserves its original audit fields. A new confirmation comes from a completed Firestore transaction. A cached record represents the last observed state.

The prototype cancels a whole booking group, and its payload is deliberately limited to guest name, rooms and dates plus status and audit fields. Editing, individual-room cancellation, Room import, Android cache isolation, and production key restriction testing are outside this prototype. Validate the size caps against existing stays before choosing this model for migration. The Firestore emulator and the production service can differ in contention and limits. [Emulator differences](https://firebase.google.com/docs/emulator-suite/connect_firestore#how_the_cloud_firestore_emulator_differs_from_production)
