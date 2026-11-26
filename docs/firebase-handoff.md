# Firebase shared bookings handoff

## Completed

- The registered Android app points to Firebase project `guesthouse-booking-mynks-5d54f`. The Firebase Auth and Firestore SDKs are pinned through BoM `33.16.0` for Kotlin `2.0.21`.
- The Android app still uses `RoomBookingRepository` through `BookingRepository`. This work made no changes to the Android database or production Firebase configuration.
- The [local prototype](../firebase-prototype/README.md) runs Auth and Firestore against `demo-guesthouse-booking`. Tests sign in with separate email/password accounts. The booking client uses a signed-in UID independently of the authentication provider.
- Firestore rules scope permissions by property and UID. Owners manage Manager and Viewer membership. Owners and Managers create and cancel bookings. Viewers can read bookings and room inventory.
- Booking writes claim every room-night atomically. A stable request ID supports retries. Cancellation frees the whole group and records the actor and server timestamp.

## Verified in the emulators

All nine tests passed. They cover role enforcement, staff revocation, cross-property denials, concurrent overlapping requests, direct writes that bypass the booking client, multi-room rollback, adjacent stays, retries, cancellation, and a real connection interruption. Cached bookings remained readable during the interruption. The offline write was never confirmed.

The tested limits are one room for five nights, two rooms for two nights each, or three rooms for one night each. All rooms in a group share dates. The maximum create uses at most 17 rules document lookups across the commit. See the prototype README for test commands and the lookup calculation.

## Pending before production

1. Review existing booking durations and room counts against the prototype caps. Decide how to support longer stays, full booking fields, editing, and individual-room cancellation before adopting the data model.
2. Implement Android sign-in and a Firestore-backed `BookingRepository` in an isolated test build. Scope cached data by Firebase UID and property, mark cached availability as advisory, and confirm writes only after a successful backend response. Verify sign-out, account switching, and timeout reconciliation.
3. Test two real phones and validate contention and rules limits in an isolated Firestore test environment. Review the reported API allowlist and test a separate Android-restricted key with the debug and release signing fingerprints. Verify sign-in, token refresh, password reset, and authenticated access. Confirm the live database location and closed rules in the console before rollout.
4. Plan and rehearse the Room import. Back up each source database, preserve booking UUIDs, reconcile records entered on multiple phones, validate dates and occupancy, and compare imported records before switching the app. Add explicit Room migrations for supported older schemas before any database version change.

Production Firestore access stays closed during this work. Keep the rollout within Spark quotas and keep administrative credentials out of the Android app.
