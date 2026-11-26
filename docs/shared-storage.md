# Shared booking storage and migration plan

The [Firebase Emulator prototype](../firebase-prototype/README.md) explores
Firebase Authentication and Firestore for the shared backend. The Sheets and
Postgres sections below describe the earlier proposal. The Android app still
uses Room.

## Status

The Android app now consumes BookingRepository. RoomBookingRepository is the
only implemented strategy; it remains local to this phone. No Google Sheets,
login, roles, network API or cross-device synchronization is implemented yet.

MainActivity -> BookingViewModelFactory -> BookingRepositoryProvider selects
the strategy. BookingViewModel and Compose screens must not reference Room
DAOs, Sheets row numbers, SQL queries or vendor SDKs. Existing Room data and
schema are retained. The unused in-memory repository was replaced.

BookingRepository exposes observed bookings and rooms plus atomic create and
delete operations. Writes return provider-independent results. Availability
shown from observed data is advisory; only a successful write confirms a booking.
Room checks availability and inserts all selected bookings in one transaction.
Concurrent remote bookings require server enforcement; the local transaction
does not protect other phones.

Current Booking and GuestRoom models retain their Room annotations for a small,
non-destructive refactor. Remote DTOs must be mapped in the data layer rather
than serializing these entities as the public API contract.

## Target architecture

Android screens -> ViewModel -> BookingRepository
                                    |
                         CachedRemoteBookingRepository
                              /               \
                      Room cache          BookingApi
                                              |
                             authenticated, versioned backend
                                              |
                                  BookingStore interface
                                    /               \
                          SheetsBookingStore   PostgresBookingStore

The future Android repository coordinates API commands and the local cache.
BookingApi hides transport details (Apps Script may initially use action-based
POST requests). The server BookingStore interface hides actual persistence.
Keep authentication, membership checks, validation and audit logic above the
server storage adapter.

Switching only the server adapter behind the same API should require no Android
UI changes. Moving away from Apps Script may also require a new API base URL,
authentication configuration and deployment. This is reduced rework, not a
zero-work migration.

## Backend contract to implement before connecting Sheets

Use logical, versioned operations rather than exposing spreadsheet CRUD:

- listRooms(propertyId)
- listBookings(propertyId, dateWindow, cursor)
- createBookings(propertyId, requestId, bookings)
- updateBooking(propertyId, bookingId, expectedVersion, changes)
- cancelBooking(propertyId, bookingId, expectedVersion)
- listChanges(propertyId, cursor), including cancellations/deletions

Initial Android functionality supports create and delete only. Replace physical
deletion with audited cancellation when shared storage is introduced.

Every request is authenticated. Derive user identity from a verified credential,
never a caller-provided email, user ID or role. Membership and role are checked
server-side for every operation. A URL or embedded shared secret is not login.
Never put service-account private keys or administrator credentials in the APK.
Google sign-in integration with an Apps Script endpoint needs a verified
authentication design before deployment; do not expose an anonymous booking API.

Roles: Owner manages property and memberships; Manager manages bookings;
Viewer reads only. Public sign-up does not grant property membership.
Do not give staff direct sheet editing access: direct edits bypass validation,
locking, authorization and audit history.

Provider-independent response codes should include SUCCESS, UNAVAILABLE,
INVALID_REQUEST, ACCESS_DENIED, CONNECTION_REQUIRED, STALE_VERSION and
STORAGE_ERROR. Android's current write result covers current create/delete
needs; add version/update results with the corresponding workflow.

## Portable records

- propertyId is mandatory for all remote records and permissions.
- bookingId is a stable UUID, never a sheet row number. Preserve existing IDs.
- roomId is stable per property; display roomNumber separately. Introduce this
  mapping when moving beyond the current single-property Room schema.
- Check-in and check-out are ISO local dates (yyyy-MM-dd) in the property's
  configured time zone, using [checkIn, checkOut). Check-out day is available.
  Existing Android Date fields need explicit mapping at the API boundary.
- Audit times are server-generated UTC instants; do not trust phone clocks.
- Store money as integer minor units plus currency, with explicit rounding when
  mapping the current Double amounts.
- Include record version, status and audit actor/timestamps.
- Separate reservation status from payment status in the remote model.
- Persist requestId with the write result for safe retries. A timeout does not
  prove a write failed; retry the same request ID and booking IDs.
- Keep tombstones/change events until all supported clients can process them.
  Use server cursors rather than phone timestamps for incremental sync.

## Concurrency and confirmation

Sheets: one script-wide lock must cover checking current availability,
validating the complete batch, writing reservations and recording idempotency.
All writer code paths must use the same lock. Locks do not provide a database
transaction or undo partial writes. A Sheets implementation must include batch
write/recovery design, timeout handling and reconciliation before claiming the
atomic contract is met. Keep transaction/audit/idempotency records recoverable.

Postgres: enforce room/date overlap constraints in the database and use a
transaction for booking batches, changes and audit records. Do not rely on a
check followed by an unprotected insert.

For both: reject the entire multi-room request if any room conflicts. Different
rooms can overlap; adjacent stays in the same room can touch at check-out.
Exclude cancelled reservations from occupancy.

First shared release: offline viewing only; online confirmation for changes.
Cache only server-confirmed reservations. Refresh after writes, on screen entry
and on manual refresh; later add polling/change streams. Display offline/stale
state. Preserve drafts on failure. If a request times out, reconcile by its
request ID before offering a fresh submission.

Scope Room cache by account/property before enabling login; clear or isolate it
on sign-out/account changes. Never replay pending writes under a different user.
Revocation prevents future server access but cannot erase an offline phone.
Keep a sync status separate from reservation confirmation and payment status.

## Migration from Sheets to a database

1. Implement the same backend contract and authorization checks for the new store.
2. Export rooms, bookings, memberships, versions, audit data and retry records.
3. Validate IDs, property links, date ranges, money conversions and occupancy.
4. Take a backup and pause old writes for a consistent cutover.
5. Import and compare counts plus record contents; resolve conflicts explicitly.
6. Switch the configured server adapter/endpoint. Keep only one writable source.
7. Refresh client caches with a new sync epoch so old cursors cannot skip data.
8. Verify both roles and simultaneous booking attempts before enabling writes.
9. Retain a rollback backup; reverting after new writes requires reconciliation.

Do not implement dual writes without a separate consistency/recovery design.
Do not use destructive Room migration to introduce account/property caching.

## Acceptance checks

Run the same behavioral suite for every strategy/backend:

- Conflicting active booking is rejected; no partial multi-room save.
- Two simultaneous same-room requests yield exactly one confirmation.
- Adjacent dates and different rooms are accepted.
- Cancelled reservations free their room.
- Duplicate request returns its prior result without duplicate reservations.
- Viewer writes, non-member access and cross-property access are rejected.
- Stale update is rejected without overwriting another user's change.
- Offline/timeout state is never presented as confirmation.
- Account changes cannot expose another account's cached guest information.
- Migration preserves IDs, occupancy and retry behavior.

Local Room instrumented tests cover transaction behavior, date boundaries,
cancellations, concurrent requests and rollback. Server permission, idempotency,
network and migration checks remain for the remote implementation.
