import { createServer } from 'node:http';

const statuses = new Map([
  ['unauthenticated', 401], ['access-denied', 403],
  ['invalid-request', 400], ['invalid-room-change', 400],
  ['room-unavailable', 409], ['stale-version', 409],
  ['request-id-reused', 409], ['booking-cancelled', 409]
]);

export function bookingHttpServer(changeBooking) {
  return createServer(async (req, res) => {
    function reply(status, body) {
      res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(body));
    }

    if (req.url !== '/booking-changes') return reply(404, { error: 'not-found' });
    if (req.method !== 'POST') {
      res.setHeader('Allow', 'POST');
      return reply(405, { error: 'method-not-allowed' });
    }
    const token = /^Bearer (\S+)$/i.exec(req.headers.authorization ?? '')?.[1];
    if (!token) return reply(401, { error: 'unauthenticated' });
    if (req.headers['content-type']?.split(';')[0].trim().toLowerCase() !== 'application/json') {
      return reply(415, { error: 'unsupported-content-type' });
    }
    try {
      let size = 0;
      const chunks = [];
      for await (const chunk of req.iterator({ destroyOnReturn: false })) {
        size += chunk.length;
        if (size > 32 * 1024) {
          res.setHeader('Connection', 'close');
          return reply(413, { error: 'request-too-large' });
        }
        chunks.push(chunk);
      }
      let command;
      try {
        command = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      } catch {
        return reply(400, { error: 'invalid-request' });
      }
      const result = await changeBooking(token, command);
      return reply(200, result);
    } catch (error) {
      const status = statuses.get(error.code);
      if (!status) return reply(503, { error: 'result-unknown' });
      return reply(status, { error: error.code });
    }
  });
}
