'use strict';
// Microsoft -> Xbox Live -> XSTS -> Minecraft login using the OAuth device code flow.
// Needs an Azure app (client id) that Mojang has approved for the Minecraft API.
const { fetchRetry } = require('./net');

const MS = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';
const SCOPE = 'XboxLive.signin offline_access';

class AuthError extends Error {
  constructor(message, code) {
    super(message);
    this.code = code;
  }
}

async function form(url, params) {
  const res = await fetchRetry(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(params).toString(),
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json };
}

async function postJson(url, body, headers = {}) {
  const res = await fetchRetry(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  let json = {};
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    json = { raw: text };
  }
  return { status: res.status, json };
}

function explainMsError(json) {
  const code = json.error || 'unknown';
  if (code === 'invalid_client' || code === 'unauthorized_client' || /AADSTS700016|AADSTS7000218|AADSTS70002/.test(json.error_description || '')) {
    return new AuthError('Die Client-ID ist ungültig oder "Öffentliche Clientflows" sind in Azure nicht aktiviert. Siehe Einstellungen → Microsoft-Login.', 'client');
  }
  return new AuthError(`Microsoft-Login fehlgeschlagen: ${json.error_description || code}`, code);
}

/** Step 1: returns { deviceCode, userCode, verificationUri, interval, expiresIn, message }. */
async function startDeviceCode(clientId) {
  if (!clientId) throw new AuthError('Keine Client-ID eingetragen. Siehe Einstellungen → Microsoft-Login.', 'client');
  const { status, json } = await form(`${MS}/devicecode`, { client_id: clientId, scope: SCOPE });
  if (status !== 200) throw explainMsError(json);
  return {
    deviceCode: json.device_code,
    userCode: json.user_code,
    verificationUri: json.verification_uri,
    interval: json.interval || 5,
    expiresIn: json.expires_in || 900,
    message: json.message,
  };
}

/** Step 2: polls until the user confirmed in the browser. Returns the Microsoft token response. */
async function pollDeviceCode(clientId, code, isCancelled) {
  let interval = code.interval;
  const deadline = Date.now() + code.expiresIn * 1000;
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, interval * 1000));
    if (isCancelled()) throw new AuthError('Anmeldung abgebrochen', 'cancelled');
    const { status, json } = await form(`${MS}/token`, {
      grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
      client_id: clientId,
      device_code: code.deviceCode,
    });
    if (status === 200) return json;
    if (json.error === 'authorization_pending') continue;
    if (json.error === 'slow_down') {
      interval += 5;
      continue;
    }
    if (json.error === 'expired_token') throw new AuthError('Der Code ist abgelaufen. Bitte erneut anmelden.', 'expired');
    if (json.error === 'authorization_declined') throw new AuthError('Anmeldung wurde abgelehnt.', 'declined');
    throw explainMsError(json);
  }
  throw new AuthError('Der Code ist abgelaufen. Bitte erneut anmelden.', 'expired');
}

async function refreshMicrosoft(clientId, refreshToken) {
  const { status, json } = await form(`${MS}/token`, {
    grant_type: 'refresh_token',
    client_id: clientId,
    refresh_token: refreshToken,
    scope: SCOPE,
  });
  if (status !== 200) throw new AuthError('Sitzung abgelaufen – bitte das Konto neu anmelden.', 'refresh');
  return json;
}

const XERR = {
  2148916233: 'Dieses Microsoft-Konto hat kein Xbox-Profil. Melde dich einmal auf minecraft.net an und erstelle ein Profil.',
  2148916235: 'Xbox Live ist in deinem Land nicht verfügbar.',
  2148916236: 'Für dieses Konto ist eine Altersbestätigung nötig (Xbox-Seite).',
  2148916237: 'Für dieses Konto ist eine Altersbestätigung nötig (Xbox-Seite).',
  2148916238: 'Dieses Konto gehört einem Kind und muss von einem Erwachsenen zu einer Familie hinzugefügt werden.',
};

/** Microsoft access token -> Minecraft session { accessToken, expiresAt, uuid, name, skins, capes }. */
async function minecraftLogin(msAccessToken) {
  const xbl = await postJson('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT',
  });
  if (xbl.status !== 200) throw new AuthError(`Xbox-Login fehlgeschlagen (HTTP ${xbl.status})`, 'xbl');
  const uhs = xbl.json.DisplayClaims.xui[0].uhs;

  const xsts = await postJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.json.Token] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT',
  });
  if (xsts.status !== 200) {
    throw new AuthError(XERR[xsts.json.XErr] || `Xbox-Berechtigung fehlgeschlagen (${xsts.json.XErr || xsts.status})`, 'xsts');
  }

  const mc = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', {
    identityToken: `XBL3.0 x=${uhs};${xsts.json.Token}`,
  });
  if (mc.status === 403 || mc.status === 401) {
    throw new AuthError(
      'Mojang hat diese Client-ID noch nicht für Minecraft freigeschaltet. Stelle den Antrag unter aka.ms/mce-reviewappid (siehe Einstellungen).',
      'not-approved',
    );
  }
  if (mc.status !== 200) throw new AuthError(`Minecraft-Login fehlgeschlagen (HTTP ${mc.status})`, 'mc');
  const accessToken = mc.json.access_token;
  const profile = await getProfile(accessToken);
  return { accessToken, expiresAt: Date.now() + (mc.json.expires_in || 86400) * 1000 - 60000, ...profile };
}

async function getProfile(accessToken) {
  const res = await fetchRetry('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (res.status === 404) throw new AuthError('Dieses Konto besitzt Minecraft: Java Edition nicht.', 'no-game');
  if (!res.ok) throw new AuthError(`Profil konnte nicht geladen werden (HTTP ${res.status})`, 'profile');
  const p = await res.json();
  return { uuid: p.id, name: p.name, skins: p.skins || [], capes: p.capes || [] };
}

module.exports = { startDeviceCode, pollDeviceCode, refreshMicrosoft, minecraftLogin, getProfile, AuthError };
