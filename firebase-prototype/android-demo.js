import assert from 'node:assert/strict';
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import { bookingHttpServer } from './booking-http.js';
import { trustedBookingService } from './trusted-booking-service.js';

for (const name of ['FIREBASE_AUTH_EMULATOR_HOST', 'FIRESTORE_EMULATOR_HOST']) {
  assert.match(process.env[name] ?? '', /^127\.0\.0\.1:\d+$/);
}
const app = initializeApp({ projectId: 'demo-guesthouse-trusted' });
const auth = getAuth(app);
const db = getFirestore(app);
const batch = db.batch();
const property = db.collection('properties').doc('android-demo');

for (const role of ['Owner', 'Manager']) {
  const { uid } = await auth.createUser({
    email: `${role.toLowerCase()}@android.example.test`,
    password: 'local-emulator-password'
  });
  batch.set(property.collection('members').doc(uid), { role });
}
for (const room of ['101', '102']) {
  batch.set(property.collection('rooms').doc(room), { active: true, capacity: 2, revision: 0 });
}
await batch.commit();
bookingHttpServer(trustedBookingService(auth, db)).listen(8787, '127.0.0.1', () => {
  console.log('Android booking demo listening on 127.0.0.1:8787');
});
