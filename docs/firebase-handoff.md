# Firebase shared bookings handoff

## Revised booking design

The group booking model now has a separate [trusted-backend emulator prototype](../firebase-prototype/README.md#trusted-backend-prototype). One booking reference covers any selection of active rooms, up to the whole property, for up to 31 nights. Staff can add rooms, cancel selected rooms, and adjust guest counts within room capacity. A failed change leaves the existing booking intact. Android still uses Room. No production rules or data have been deployed.

Five emulator tests cover the revised model, including all 42 rooms for 31 nights, whole-group rollback when one room conflicts, partial cancellation, competing room additions, and direct client write denial.

The [Android smoke test](../firebase-prototype/README.md#android-smoke-test) exercises the local HTTP API with two emulator accounts. It uses an opt-in instrumentation entry point while the regular app still opens Room. The trusted Node suite now includes HTTP request validation, token checks, conflict handling, and partial cancellation.

Both debug APKs built successfully. All six trusted Node tests and all three Android smoke stages passed. The Owner created a booking on one Android emulator. A second emulator read it, verified the conflict response, and cancelled one room. The follow-up confirmed that the released room could be booked again and an unreachable API gave no confirmed result. The two Android devices were run sequentially with the same local backend.

## Completed

- The registered Android app points to Firebase project `guesthouse-booking-mynks-5d54f`. The Firebase Auth and Firestore SDKs are pinned through BoM `33.16.0` for Kotlin `2.0.21`.
- The Android app still uses `RoomBookingRepository` through `BookingRepository`. This work made no changes to the Android database or production Firebase configuration.
- The [local prototype](../firebase-prototype/README.md) runs Auth and Firestore against `demo-guesthouse-booking`. Tests sign in with separate email/password accounts. The booking client uses a signed-in UID independently of the authentication provider.
- Firestore rules scope permissions by property and UID. Owners manage Manager and Viewer membership. Owners and Managers create and cancel bookings. Viewers can read bookings and room inventory.
- Booking writes claim every room-night atomically. A stable request ID supports retries. Cancellation frees the whole group and records the actor and server timestamp.

## Original rules-only prototype

All nine tests passed. They cover role enforcement, staff revocation, cross-property denials, concurrent overlapping requests, direct writes that bypass the booking client, multi-room rollback, adjacent stays, retries, cancellation, and a real connection interruption. Cached bookings remained readable during the interruption. The offline write was never confirmed.

The tested limits are one room for five nights, two rooms for two nights each, or three rooms for one night each. All rooms in a group share dates. The maximum create uses at most 17 rules document lookups across the commit. See the prototype README for test commands and the lookup calculation.

## Pending before production

1. Choose where to host the trusted booking backend and verify production quotas and contention. Map the full Android booking fields and confirm whether existing bookings need date changes or stays longer than the 31-night prototype limit.
2. Implement Android sign-in and a Firestore-backed `BookingRepository` in an isolated test build. Scope cached data by Firebase UID and property, mark cached availability as advisory, and confirm writes only after a successful backend response. Verify sign-out, account switching, and timeout reconciliation.
3. Test two real phones and validate contention and rules limits in an isolated Firestore test environment. Review the reported API allowlist and test a separate Android-restricted key with the debug and release signing fingerprints. Verify sign-in, token refresh, password reset, and authenticated access. Confirm the live database location and closed rules in the console before rollout.
4. Plan and rehearse the Room import. Back up each source database, preserve booking UUIDs, reconcile records entered on multiple phones, validate dates and occupancy, and compare imported records before switching the app. Add explicit Room migrations for supported older schemas before any database version change.

Production Firestore access stays closed during this work. Keep the rollout within Spark quotas and keep administrative credentials out of the Android app.
