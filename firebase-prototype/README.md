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

## Trusted backend prototype

The second suite uses `demo-guesthouse-trusted` and `trusted-firestore.rules`. Run it alone with `npm run test:trusted`, or run both suites with `npm test`. The projects and rule sets are separate.

The service verifies a Firebase ID token, then checks property membership inside the write transaction. Firestore denies direct client booking, reservation, and inventory writes. In production the Admin SDK would run on a trusted host. The test harness uses local emulators without service-account credentials.

A group has fixed check-in and exclusive checkout dates, a version, and per-room guest counts. The prototype interprets a month as up to 31 nights. A create or edit either applies in full or leaves the group unchanged. Each room has its own date-range reservation, so cancelling selected rooms releases only those rooms. A command may add and remove rooms and adjust guest counts together. Cancelling the final room closes the group. Request IDs let retries return the saved result, and an expected group version guards against stale edits.

The transaction reads and updates a revision on each room whose occupancy changes. This makes concurrent writers contend even when both start with an empty calendar. The overlap query assumes reservations obey the same 31-night cap. Any migration must preserve that invariant or adjust the query before importing longer stays.

The suite uses the current 42-room inventory layout as a fixture and exercises a 31-night whole-property booking, rollback on one conflict, partial cancellation, capacity checks, a concurrent room addition, stale edits, retries, and client bypass attempts. Production contention, hosting and costs, API key restrictions, full booking fields, date edits, and Room migration remain pending.

## Android smoke test

Run `npm run android:demo` from this directory to start Auth and Firestore emulators, seed an Owner and Manager, and serve `POST /booking-changes` on host loopback port 8787. Keep that terminal open. The API accepts a Firebase ID token as a bearer token. Success returns a committed booking version. Conflicts return HTTP 409. A connection failure or HTTP 503 can leave the result uncertain, so a client should retry the exact same command and request ID.

Boot two disposable Android emulators, then run these commands from the repository root in PowerShell.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\firebase-prototype\android-smoke.ps1 -OwnerDevice emulator-5554 -ManagerDevice emulator-5556
```

The instrumentation test uses the emulator host alias `10.0.2.2` and a named Firebase app with the demo project ID. The regular app still opens Room. The Owner creates a two-room booking, the Manager sees a conflict and cancels one room, and the Owner reads the change and rebooks the released room. The final step sends a request to an unused port and verifies that there is no confirmed booking. Restart the demo for a clean fixture before repeating the script, or use a nonoverlapping `-StartDay`.
