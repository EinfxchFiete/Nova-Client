'use strict';
// Tests the Nova server against a fake Mojang session server.
const http = require('http');
const assert = require('assert');
const { spawn } = require('child_process');
const path = require('path');

const joined = new Set(); // "name|serverId" that "joined" at the fake Mojang
const UUIDS = { Flipp1009: '0c68d1f1ba5c4356833b5f1cb4e4530c', EinfxchFiete: '38f96e1e970c4c9fbb7da15c4b07bfcb' };

const mojang = http.createServer((req, res) => {
  const u = new URL(req.url, 'http://x');
  if (u.pathname === '/session/minecraft/hasJoined') {
    const name = u.searchParams.get('username');
    if (joined.has(`${name}|${u.searchParams.get('serverId')}`)) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ id: UUIDS[name], name }));
    }
    res.writeHead(204);
    return res.end();
  }
  res.writeHead(404);
  res.end();
});

async function post(port, p, body, token) {
  const res = await fetch(`http://127.0.0.1:${port}${p}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body),
  });
  return { status: res.status, json: await res.json() };
}

async function login(port, name, joinIt = true) {
  const s = await post(port, '/v1/auth/start', { name });
  assert.strictEqual(s.status, 200);
  if (joinIt) joined.add(`${name}|${s.json.serverId}`);
  return post(port, '/v1/auth/finish', { name, serverId: s.json.serverId });
}

(async () => {
  await new Promise((r) => mojang.listen(0, '127.0.0.1', r));
  const port = 18000 + Math.floor(Math.random() * 1000);
  const proc = spawn(process.execPath, [path.join(__dirname, '..', 'server.js')], {
    env: { ...process.env, PORT: String(port), NOVA_SESSION_SERVER: `http://127.0.0.1:${mojang.address().port}`, NOVA_DEV: '' },
    stdio: ['ignore', 'pipe', 'inherit'],
  });
  await new Promise((r) => proc.stdout.once('data', r));
  try {
    // login that Mojang did not confirm is rejected
    const bad = await login(port, 'Flipp1009', false);
    assert.strictEqual(bad.status, 403, 'unconfirmed login must fail');

    const a = await login(port, 'Flipp1009');
    assert.strictEqual(a.status, 200);
    assert.strictEqual(a.json.uuid, '0c68d1f1-ba5c-4356-833b-5f1cb4e4530c');
    const b = await login(port, 'EinfxchFiete');
    assert.strictEqual(b.status, 200);

    // a status with valid and invalid values
    const longTitle = 'x'.repeat(200);
    let r = await post(port, '/v1/sync', {
      status: {
        cosmetics: { cape: 'anim:galaxy', head: 'crown', headColor: 'Rot', wings: 'hacker', wingsColor: '§cevil' },
        song: { title: `§4${longTitle}\n`, artist: 'Nova' },
        emote: { id: 'wave', age: 100 },
      },
      players: [],
    }, a.json.token);
    assert.strictEqual(r.status, 200);

    r = await post(port, '/v1/sync', { status: {}, players: [a.json.uuid, 'not-a-uuid'] }, b.json.token);
    assert.strictEqual(r.status, 200);
    const seen = r.json.players[a.json.uuid];
    assert.ok(seen, 'B should see A');
    assert.strictEqual(seen.cosmetics.cape, 'anim:galaxy');
    assert.strictEqual(seen.cosmetics.head, 'crown');
    assert.strictEqual(seen.cosmetics.wings, '', 'unknown wings must be dropped');
    assert.strictEqual(seen.cosmetics.wingsColor, '', 'invalid colour must be dropped');
    assert.strictEqual(seen.song.title.length, 80, 'title must be cut to 80 chars');
    assert.ok(!seen.song.title.includes('§') && !seen.song.title.includes('\n'), 'formatting/control chars must be removed');
    assert.strictEqual(seen.emote.id, 'wave');
    assert.ok(seen.emote.age >= 100 && seen.emote.age < 5000);

    // wrong token, dev endpoint disabled
    r = await post(port, '/v1/sync', { status: {} }, 'f'.repeat(64));
    assert.strictEqual(r.status, 401);
    r = await post(port, '/v1/auth/dev', { uuid: UUIDS.Flipp1009, name: 'Flipp1009' });
    assert.strictEqual(r.status, 404, 'dev login must be off by default');

    // rate limit
    let limited = false;
    for (let i = 0; i < 15; i++) {
      const x = await post(port, '/v1/sync', { status: {} }, b.json.token);
      if (x.status === 429) limited = true;
    }
    assert.ok(limited, 'sync must be rate limited');

    console.log('SERVER TEST PASSED');
  } finally {
    proc.kill();
    mojang.close();
  }
})().catch((e) => {
  console.error('SERVER TEST FAILED:', e.message);
  process.exit(1);
});
