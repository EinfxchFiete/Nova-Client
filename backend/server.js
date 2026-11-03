'use strict';
// Nova server: lets Nova Client players see each other's cosmetics, songs and emotes.
// No dependencies, nothing is stored on disk – everything lives in memory and expires automatically.
//
// Login works like joining a Minecraft server:
//   1. POST /v1/auth/start  {name}            -> {serverId}     (random nonce)
//   2. the client calls Mojang's joinServer(uuid, accessToken, serverId)   (token goes only to Mojang)
//   3. POST /v1/auth/finish {name, serverId}  -> {token, uuid}  (we ask Mojang's hasJoined)
//   4. POST /v1/sync with "Authorization: Bearer <token>" every few seconds.

const http = require('http');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 8080);
const SESSION_SERVER = (process.env.NOVA_SESSION_SERVER || 'https://sessionserver.mojang.com').replace(/\/+$/, '');
const DEV = process.env.NOVA_DEV === '1';

const STATUS_TTL = 30 * 1000;
const TOKEN_TTL = 24 * 60 * 60 * 1000;
const PENDING_TTL = 2 * 60 * 1000;
const MAX_BODY = 32 * 1024;
const MAX_PLAYERS_PER_SYNC = 200;

const HEADS = new Set(['bandana', 'crown', 'tophat', 'horns', 'catears', 'halo',
  'partyhat', 'cowboyhat', 'bunnyears', 'headphones', 'sunglasses', 'viking', 'flowercrown', 'beanie']);
const EXTRAS = new Set(['backpack', 'cattail', 'foxtail', 'deviltail', 'scarf']);
const WINGS = new Set(['angel', 'dragon', 'butterfly', 'bat', 'fairy', 'phoenix']);
const COLORS = new Set(['Normal', 'Rot', 'Orange', 'Gelb', 'Grün', 'Türkis', 'Blau', 'Lila', 'Pink', 'Schwarz', 'Regenbogen']);
const CAPES = new Set(['nova', 'galaxy', 'fire', 'ocean', 'forest', 'gold', 'ice', 'rainbow', 'cherry', 'checker', 'lava', 'night', 'sunset', 'matrix',
  'anim:nova', 'anim:galaxy', 'anim:fire', 'anim:ocean', 'anim:rainbow', 'anim:matrix',
  'anim:aurora', 'anim:lava', 'anim:storm', 'anim:sakura', 'anim:ender', 'anim:snow', 'anim:neon',
  'anim:hearts', 'anim:glitch', 'anim:portal']);
const MINIMEE_MODES = new Set(['neck', 'pocket', 'head']);
const EMOTES = new Map([['wave', 3000], ['cheer', 3000], ['dance', 6000], ['clap', 3000], ['facepalm', 2500], ['point', 2500],
  ['bow', 2500], ['salute', 2500], ['zombie', 4000], ['shrug', 2000], ['think', 3000]]);

const pending = new Map();  // serverId -> { name, created }
const sessions = new Map(); // token -> { uuid, name, expires, lastSync }
const statuses = new Map(); // uuid -> { name, cosmetics, song, emote:{id,start}, updated }
const limits = new Map();   // key -> { count, reset }

function log(...args) {
  console.log(new Date().toISOString(), ...args);
}

function send(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY) {
        reject(Object.assign(new Error('zu groß'), { status: 413 }));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => {
      if (chunks.length === 0) return resolve({});
      try {
        const v = JSON.parse(Buffer.concat(chunks).toString('utf8'));
        resolve(v && typeof v === 'object' ? v : {});
      } catch {
        reject(Object.assign(new Error('ungültiges JSON'), { status: 400 }));
      }
    });
    req.on('error', reject);
  });
}

function clientIp(req) {
  const fwd = req.headers['x-forwarded-for'];
  if (typeof fwd === 'string' && fwd.length) return fwd.split(',')[0].trim();
  return req.socket.remoteAddress || '?';
}

/** true if the call is allowed (max calls per window per key) */
function allow(key, max, windowMs) {
  const now = Date.now();
  let e = limits.get(key);
  if (!e || e.reset < now) {
    e = { count: 0, reset: now + windowMs };
    limits.set(key, e);
  }
  e.count++;
  return e.count <= max;
}

function dashed(uuid) {
  const u = String(uuid).replace(/-/g, '').toLowerCase();
  if (!/^[0-9a-f]{32}$/.test(u)) return null;
  return `${u.slice(0, 8)}-${u.slice(8, 12)}-${u.slice(12, 16)}-${u.slice(16, 20)}-${u.slice(20)}`;
}

function cleanText(v, max) {
  if (typeof v !== 'string') return '';
  // no control characters and no Minecraft formatting codes
  const t = v.replace(/[\u0000-\u001f\u007f-\u009f§]/g, '').trim();
  return t.length > max ? t.slice(0, max) : t;
}

function pick(set, v) {
  return typeof v === 'string' && set.has(v) ? v : '';
}

function sanitizeStatus(s, now) {
  const out = { cosmetics: { cape: '', head: '', headColor: '', wings: '', wingsColor: '', extra: '', extraColor: '' }, song: null, emote: null };
  // minimee: a mini player (any Minecraft name) as necklace, in the pocket or on the head
  if (!s || typeof s !== 'object') return out;
  const c = s.cosmetics && typeof s.cosmetics === 'object' ? s.cosmetics : {};
  out.cosmetics = {
    cape: pick(CAPES, c.cape),
    head: pick(HEADS, c.head),
    headColor: pick(COLORS, c.headColor),
    wings: pick(WINGS, c.wings),
    wingsColor: pick(COLORS, c.wingsColor),
    extra: pick(EXTRAS, c.extra),
    extraColor: pick(COLORS, c.extraColor),
  };
  if (c.minimee && typeof c.minimee === 'object' && typeof c.minimee.name === 'string'
      && /^[A-Za-z0-9_]{1,16}$/.test(c.minimee.name) && MINIMEE_MODES.has(c.minimee.mode)) {
    out.cosmetics.minimee = { name: c.minimee.name, mode: c.minimee.mode };
  }
  if (s.song && typeof s.song === 'object') {
    const title = cleanText(s.song.title, 80);
    if (title) {
      out.song = { title, artist: cleanText(s.song.artist, 60) };
      // optional playback position (ms) and length (ms) for the timestamp above the name
      const len = Number(s.song.len);
      const pos = Number(s.song.pos);
      if (Number.isFinite(len) && len > 0 && len <= 24 * 3600 * 1000 && Number.isFinite(pos)) {
        out.song.len = Math.round(len);
        out.song.pos = Math.round(Math.max(0, Math.min(len, pos)));
        out.song.playing = s.song.playing !== false;
        out.song.at = now;
      }
    }
  }
  if (s.emote && typeof s.emote === 'object' && EMOTES.has(s.emote.id)) {
    const age = Math.max(0, Math.min(60000, Number(s.emote.age) || 0));
    out.emote = { id: s.emote.id, start: now - age };
  }
  return out;
}

async function hasJoined(name, serverId) {
  const url = `${SESSION_SERVER}/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`;
  const res = await fetch(url, { headers: { 'User-Agent': 'NovaServer/1.0' }, signal: AbortSignal.timeout(8000) });
  if (res.status !== 200) return null;
  const p = await res.json();
  const uuid = dashed(p.id);
  return uuid ? { uuid, name: String(p.name || name) } : null;
}

function newSession(uuid, name) {
  const token = crypto.randomBytes(32).toString('hex');
  sessions.set(token, { uuid, name, expires: Date.now() + TOKEN_TTL, lastSync: 0 });
  return token;
}

function session(req) {
  const h = req.headers.authorization || '';
  const m = /^Bearer ([0-9a-f]{64})$/.exec(h);
  if (!m) return null;
  const s = sessions.get(m[1]);
  if (!s || s.expires < Date.now()) return null;
  return s;
}

const routes = {
  'GET /': async (req, res) => send(res, 200, { name: 'Nova Server', ok: true, players: statuses.size }),
  'GET /health': async (req, res) => send(res, 200, { ok: true }),

  'POST /v1/auth/start': async (req, res) => {
    if (!allow(`start:${clientIp(req)}`, 20, 60000)) return send(res, 429, { error: 'zu viele Anfragen' });
    const body = await readBody(req);
    const name = String(body.name || '');
    if (!/^[A-Za-z0-9_]{1,16}$/.test(name)) return send(res, 400, { error: 'ungültiger Name' });
    const serverId = crypto.randomBytes(20).toString('hex');
    pending.set(serverId, { name, created: Date.now() });
    send(res, 200, { serverId });
  },

  'POST /v1/auth/finish': async (req, res) => {
    if (!allow(`finish:${clientIp(req)}`, 20, 60000)) return send(res, 429, { error: 'zu viele Anfragen' });
    const body = await readBody(req);
    const p = pending.get(String(body.serverId || ''));
    if (!p || p.name !== body.name || Date.now() - p.created > PENDING_TTL) return send(res, 403, { error: 'unbekannte Anmeldung' });
    pending.delete(String(body.serverId));
    let profile;
    try {
      profile = await hasJoined(p.name, String(body.serverId));
    } catch (e) {
      return send(res, 502, { error: 'Mojang nicht erreichbar' });
    }
    if (!profile) return send(res, 403, { error: 'Mojang hat die Anmeldung nicht bestätigt' });
    const token = newSession(profile.uuid, profile.name);
    log('login', profile.name, profile.uuid);
    send(res, 200, { token, uuid: profile.uuid });
  },

  'POST /v1/auth/dev': async (req, res) => {
    if (!DEV) return send(res, 404, { error: 'nicht gefunden' });
    const body = await readBody(req);
    const uuid = dashed(body.uuid);
    const name = String(body.name || 'Dev');
    if (!uuid) return send(res, 400, { error: 'ungültige uuid' });
    send(res, 200, { token: newSession(uuid, name), uuid });
  },

  'POST /v1/sync': async (req, res) => {
    const s = session(req);
    if (!s) return send(res, 401, { error: 'nicht angemeldet' });
    const now = Date.now();
    if (!allow(`sync:${s.uuid}`, 10, 5000)) return send(res, 429, { error: 'zu viele Anfragen' });
    const body = await readBody(req);
    const clean = sanitizeStatus(body.status, now);
    statuses.set(s.uuid, { name: s.name, ...clean, updated: now });
    s.lastSync = now;
    const players = {};
    const wanted = Array.isArray(body.players) ? body.players.slice(0, MAX_PLAYERS_PER_SYNC) : [];
    for (const raw of wanted) {
      const uuid = dashed(raw);
      if (!uuid || uuid === s.uuid) continue;
      const st = statuses.get(uuid);
      if (!st || now - st.updated > STATUS_TTL) continue;
      const out = { cosmetics: st.cosmetics };
      if (st.song) {
        const { at, ...song } = st.song;
        if (song.len && song.playing) song.pos = Math.min(song.len, song.pos + (now - at));
        out.song = song;
      }
      if (st.emote && now - st.emote.start < (EMOTES.get(st.emote.id) || 0)) out.emote = { id: st.emote.id, age: now - st.emote.start };
      players[uuid] = out;
    }
    send(res, 200, { players });
  },
};

const server = http.createServer(async (req, res) => {
  const path = (req.url || '/').split('?')[0];
  const handler = routes[`${req.method} ${path}`];
  if (!handler) return send(res, 404, { error: 'nicht gefunden' });
  try {
    await handler(req, res);
  } catch (e) {
    if (!res.headersSent) send(res, e.status || 500, { error: e.status ? e.message : 'Serverfehler' });
    if (!e.status) log('error', e);
  }
});

setInterval(() => {
  const now = Date.now();
  for (const [k, v] of pending) if (now - v.created > PENDING_TTL) pending.delete(k);
  for (const [k, v] of sessions) if (v.expires < now) sessions.delete(k);
  for (const [k, v] of statuses) if (now - v.updated > STATUS_TTL * 4) statuses.delete(k);
  for (const [k, v] of limits) if (v.reset < now) limits.delete(k);
}, 60000).unref();

if (require.main === module) {
  server.listen(PORT, () => {
    log(`Nova Server läuft auf Port ${PORT}`);
    if (DEV) log('ACHTUNG: Entwicklermodus (NOVA_DEV=1) ist an – nur für lokale Tests verwenden!');
  });
}

module.exports = { server, sanitizeStatus, dashed };
