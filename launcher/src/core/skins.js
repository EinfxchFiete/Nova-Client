'use strict';
const { fetchRetry } = require('./net');

const API = 'https://api.minecraftservices.com/minecraft/profile';

async function check(res, what) {
  if (res.status === 401) throw new Error('Sitzung abgelaufen – bitte erneut anmelden.');
  if (res.status === 429) throw new Error('Zu viele Änderungen – bitte kurz warten.');
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${what} fehlgeschlagen (HTTP ${res.status}) ${text.slice(0, 120)}`);
  }
  return res.json().catch(() => ({}));
}

/** Uploads a 64x64 (or 64x32) PNG as the account skin. variant: "classic" | "slim". */
async function uploadSkin(accessToken, pngBuffer, variant) {
  const body = new FormData();
  body.append('variant', variant === 'slim' ? 'slim' : 'classic');
  body.append('file', new Blob([pngBuffer], { type: 'image/png' }), 'skin.png');
  const res = await fetchRetry(`${API}/skins`, { method: 'POST', headers: { Authorization: `Bearer ${accessToken}` }, body });
  return check(res, 'Skin-Upload');
}

/** Shows a cape you own (capeId) or hides the cape (null). */
async function setCape(accessToken, capeId) {
  const res = capeId
    ? await fetchRetry(`${API}/capes/active`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ capeId }),
      })
    : await fetchRetry(`${API}/capes/active`, { method: 'DELETE', headers: { Authorization: `Bearer ${accessToken}` } });
  return check(res, 'Cape-Änderung');
}

/** Basic validation: PNG signature and 64x64 / 64x32 size. */
function validateSkin(buf) {
  if (buf.length < 24 || buf.readUInt32BE(0) !== 0x89504e47) return 'Die Datei ist kein PNG-Bild.';
  const w = buf.readUInt32BE(16);
  const h = buf.readUInt32BE(20);
  if (w !== 64 || (h !== 64 && h !== 32)) return `Ein Skin muss 64×64 Pixel groß sein (diese Datei: ${w}×${h}).`;
  return null;
}

module.exports = { uploadSkin, setCape, validateSkin };
