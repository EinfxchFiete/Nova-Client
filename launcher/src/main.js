'use strict';
const { app, BrowserWindow, ipcMain, dialog, shell, safeStorage } = require('electron');
const fs = require('fs');
const path = require('path');
const paths = require('./core/paths');
const store = require('./core/store');
const auth = require('./core/auth');
const skins = require('./core/skins');
const { launch, GAME_VERSION } = require('./core/launch');
const { fetchRetry } = require('./core/net');

const RESOURCES = path.join(__dirname, '..', 'resources');
// Steve, Minecraft's default skin, straight from Mojang's skin server (not shipped with the launcher)
const STEVE_URL = 'https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb';
let win = null;
let game = null;
let loginCancelled = false;

// ---------------------------------------------------------------- token storage

function encrypt(text) {
  if (!text) return null;
  if (safeStorage.isEncryptionAvailable()) return { enc: true, data: safeStorage.encryptString(text).toString('base64') };
  return { enc: false, data: Buffer.from(text).toString('base64') };
}

function decrypt(value) {
  if (!value) return null;
  try {
    const buf = Buffer.from(value.data, 'base64');
    return value.enc ? safeStorage.decryptString(buf) : buf.toString();
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------- helpers

function send(channel, payload) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, payload);
}

function publicState() {
  const s = store.load();
  return {
    gameVersion: GAME_VERSION,
    version: app.getVersion(),
    dataDir: paths.root(),
    settings: s.settings,
    accounts: s.accounts.map((a) => ({ uuid: a.uuid, name: a.name, active: a.uuid === s.activeAccount, capes: a.capes || [], skins: a.skins || [] })),
    activeAccount: s.activeAccount,
    profiles: s.profiles,
    activeProfile: s.activeProfile,
    skins: s.skins.map((k) => ({ id: k.id, name: k.name, variant: k.variant })),
    running: Boolean(game),
  };
}

function activeAccount() {
  const s = store.load();
  return s.accounts.find((a) => a.uuid === s.activeAccount) || null;
}

/** Returns a valid Minecraft session for the account, refreshing tokens when needed. */
async function session(account) {
  const s = store.load();
  const cached = decrypt(account.mcToken);
  if (cached && account.mcExpiresAt && account.mcExpiresAt > Date.now()) return cached;
  const refresh = decrypt(account.refreshToken);
  if (!refresh) throw new Error('Bitte das Konto neu anmelden.');
  const ms = await auth.refreshMicrosoft(s.settings.clientId, refresh);
  const mc = await auth.minecraftLogin(ms.access_token);
  Object.assign(account, {
    name: mc.name,
    skins: mc.skins,
    capes: mc.capes,
    refreshToken: encrypt(ms.refresh_token || refresh),
    mcToken: encrypt(mc.accessToken),
    mcExpiresAt: mc.expiresAt,
  });
  store.save();
  return mc.accessToken;
}

async function dataUrl(url) {
  const res = await fetchRetry(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return `data:image/png;base64,${Buffer.from(await res.arrayBuffer()).toString('base64')}`;
}

/** Path of the cached Steve skin, downloading it once from Mojang. null if offline. */
async function steveSkinFile() {
  const file = path.join(paths.root(), 'cache', 'steve.png');
  try {
    if (!skins.validateSkin(fs.readFileSync(file))) return file;
  } catch {
    /* not cached yet */
  }
  try {
    const res = await fetchRetry(STEVE_URL);
    if (res.ok) {
      const buf = Buffer.from(await res.arrayBuffer());
      if (!skins.validateSkin(buf)) {
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, buf);
        return file;
      }
    }
  } catch {
    /* offline */
  }
  return null;
}

/** Steve (or, without internet, the bundled Nova skin). */
async function defaultSkin() {
  const steve = await steveSkinFile();
  if (steve) return { url: fileDataUrl(steve), variant: 'classic', source: 'steve' };
  return { url: fileDataUrl(path.join(RESOURCES, 'default-skin.png')), variant: 'classic', source: 'nova' };
}

function fileDataUrl(file) {
  return `data:image/png;base64,${fs.readFileSync(file).toString('base64')}`;
}

function instanceDir(profileId) {
  return path.join(paths.instances(), profileId);
}

// ---------------------------------------------------------------- IPC

function handle(channel, fn) {
  ipcMain.handle(channel, async (_e, ...args) => {
    try {
      return { ok: true, value: await fn(...args) };
    } catch (e) {
      return { ok: false, error: e && e.message ? e.message : String(e) };
    }
  });
}

function registerIpc() {
  handle('state', () => publicState());

  handle('window', (action) => {
    if (action === 'minimize') win.minimize();
    else if (action === 'maximize') win.isMaximized() ? win.unmaximize() : win.maximize();
    else if (action === 'close') win.close();
  });

  handle('open-external', (url) => {
    if (typeof url === 'string' && /^https:\/\//.test(url)) shell.openExternal(url);
  });

  handle('settings:set', (patch) => {
    const s = store.load();
    const allowed = ['clientId', 'closeOnLaunch', 'javaPath', 'defaultRamMb'];
    for (const k of allowed) if (k in patch) s.settings[k] = patch[k];
    if (typeof s.settings.clientId === 'string') s.settings.clientId = s.settings.clientId.trim();
    store.save();
    return publicState();
  });

  // ---- accounts
  handle('auth:start', async () => {
    const s = store.load();
    loginCancelled = false;
    const code = await auth.startDeviceCode(s.settings.clientId);
    (async () => {
      try {
        const ms = await auth.pollDeviceCode(s.settings.clientId, code, () => loginCancelled);
        send('auth:status', { stage: 'minecraft' });
        const mc = await auth.minecraftLogin(ms.access_token);
        const existing = s.accounts.find((a) => a.uuid === mc.uuid);
        const entry = existing || { uuid: mc.uuid };
        Object.assign(entry, {
          name: mc.name,
          skins: mc.skins,
          capes: mc.capes,
          refreshToken: encrypt(ms.refresh_token),
          mcToken: encrypt(mc.accessToken),
          mcExpiresAt: mc.expiresAt,
        });
        if (!existing) s.accounts.push(entry);
        s.activeAccount = mc.uuid;
        store.save();
        send('auth:status', { stage: 'done', name: mc.name });
      } catch (e) {
        send('auth:status', { stage: 'error', error: e.message });
      }
    })();
    return { userCode: code.userCode, verificationUri: code.verificationUri, expiresIn: code.expiresIn };
  });

  handle('auth:cancel', () => {
    loginCancelled = true;
  });

  handle('accounts:select', (uuid) => {
    const s = store.load();
    if (s.accounts.some((a) => a.uuid === uuid)) s.activeAccount = uuid;
    store.save();
    return publicState();
  });

  handle('accounts:remove', (uuid) => {
    const s = store.load();
    s.accounts = s.accounts.filter((a) => a.uuid !== uuid);
    if (s.activeAccount === uuid) s.activeAccount = s.accounts.length ? s.accounts[0].uuid : null;
    store.save();
    return publicState();
  });

  handle('accounts:refresh', async () => {
    const a = activeAccount();
    if (!a) return publicState();
    const token = await session(a);
    const p = await auth.getProfile(token);
    Object.assign(a, { name: p.name, skins: p.skins, capes: p.capes });
    store.save();
    return publicState();
  });

  /** Skin texture of the active account as data URL (or Steve when not logged in). */
  handle('accounts:skin', async () => {
    const a = activeAccount();
    const skin = a && (a.skins || []).find((k) => k.state === 'ACTIVE');
    if (skin) {
      try {
        return { url: await dataUrl(skin.url), variant: (skin.variant || 'CLASSIC').toLowerCase(), source: 'account' };
      } catch {
        /* fall through to default */
      }
    }
    return defaultSkin();
  });

  handle('capes:texture', async (url) => {
    if (typeof url !== 'string' || !/^https?:\/\/textures\.minecraft\.net\//.test(url)) throw new Error('Ungültige Cape-Adresse');
    return dataUrl(url);
  });

  handle('capes:set', async (capeId) => {
    const a = activeAccount();
    if (!a) throw new Error('Kein Konto angemeldet');
    const token = await session(a);
    await skins.setCape(token, capeId || null);
    const p = await auth.getProfile(token);
    Object.assign(a, { skins: p.skins, capes: p.capes });
    store.save();
    return publicState();
  });

  // ---- skin library
  handle('skins:add', async () => {
    const r = await dialog.showOpenDialog(win, { title: 'Skin auswählen', filters: [{ name: 'Minecraft-Skin', extensions: ['png'] }], properties: ['openFile'] });
    if (r.canceled || r.filePaths.length === 0) return publicState();
    const buf = fs.readFileSync(r.filePaths[0]);
    const problem = skins.validateSkin(buf);
    if (problem) throw new Error(problem);
    return addSkin(buf, path.basename(r.filePaths[0], '.png'));
  });

  handle('skins:addBuffer', (base64, name) => addSkin(Buffer.from(base64, 'base64'), name));

  handle('skins:texture', (id) => {
    const k = store.load().skins.find((x) => x.id === id);
    if (!k) throw new Error('Skin nicht gefunden');
    return fileDataUrl(path.join(paths.skins(), `${k.id}.png`));
  });

  handle('skins:update', (id, patch) => {
    const k = store.load().skins.find((x) => x.id === id);
    if (k) {
      if (patch.name) k.name = String(patch.name).slice(0, 32);
      if (patch.variant) k.variant = patch.variant === 'slim' ? 'slim' : 'classic';
      store.save();
    }
    return publicState();
  });

  handle('skins:remove', (id) => {
    const s = store.load();
    s.skins = s.skins.filter((x) => x.id !== id);
    fs.rmSync(path.join(paths.skins(), `${id}.png`), { force: true });
    store.save();
    return publicState();
  });

  handle('skins:apply', async (id) => {
    const a = activeAccount();
    if (!a) throw new Error('Melde dich zuerst mit deinem Microsoft-Konto an.');
    const k = store.load().skins.find((x) => x.id === id);
    if (!k) throw new Error('Skin nicht gefunden');
    const token = await session(a);
    await skins.uploadSkin(token, fs.readFileSync(path.join(paths.skins(), `${k.id}.png`)), k.variant);
    const p = await auth.getProfile(token);
    Object.assign(a, { skins: p.skins, capes: p.capes });
    store.save();
    return publicState();
  });

  // ---- profiles
  handle('profiles:create', (data) => {
    const s = store.load();
    const p = {
      id: store.newId('profil'),
      name: String((data && data.name) || 'Neues Profil').slice(0, 32),
      ramMb: s.settings.defaultRamMb,
      novaEnabled: true,
      jvmArgs: '',
      width: 0,
      height: 0,
      color: (data && data.color) || '#3b82f6',
      lastPlayed: 0,
    };
    s.profiles.push(p);
    s.activeProfile = p.id;
    store.save();
    return publicState();
  });

  handle('profiles:update', (id, patch) => {
    const p = store.load().profiles.find((x) => x.id === id);
    if (!p) throw new Error('Profil nicht gefunden');
    for (const k of ['name', 'ramMb', 'novaEnabled', 'jvmArgs', 'width', 'height', 'color']) {
      if (k in patch) p[k] = patch[k];
    }
    p.name = String(p.name || 'Profil').slice(0, 32);
    p.ramMb = Math.max(1024, Math.min(32768, Number(p.ramMb) || 4096));
    store.save();
    return publicState();
  });

  handle('profiles:select', (id) => {
    const s = store.load();
    if (s.profiles.some((p) => p.id === id)) s.activeProfile = id;
    store.save();
    return publicState();
  });

  handle('profiles:delete', (id) => {
    const s = store.load();
    if (s.profiles.length <= 1) throw new Error('Das letzte Profil kann nicht gelöscht werden.');
    s.profiles = s.profiles.filter((p) => p.id !== id);
    if (s.activeProfile === id) s.activeProfile = s.profiles[0].id;
    store.save();
    return publicState();
  });

  handle('profiles:open', (id, sub) => {
    const dir = path.join(instanceDir(id), sub === 'mods' || sub === 'screenshots' ? sub : '');
    fs.mkdirSync(dir, { recursive: true });
    shell.openPath(dir);
  });

  // ---- game
  handle('game:launch', async () => {
    if (game) throw new Error('Minecraft läuft bereits.');
    const s = store.load();
    const a = activeAccount();
    if (!a) throw new Error('Melde dich zuerst mit deinem Microsoft-Konto an.');
    const profile = s.profiles.find((p) => p.id === s.activeProfile) || s.profiles[0];
    const token = await session(a);
    profile.lastPlayed = Date.now();
    store.save();
    game = await launch({
      account: { name: a.name, uuid: a.uuid, accessToken: token, xuid: '', clientId: s.settings.clientId },
      profile,
      resourcesDir: RESOURCES,
      javaPath: s.settings.javaPath,
      progress: (stage, done, total) => send('game:progress', { stage, done, total }),
      onLog: (line) => send('game:log', line),
    });
    send('game:state', { running: true });
    game.on('exit', (code) => {
      game = null;
      send('game:state', { running: false, code });
      if (win && !win.isDestroyed()) {
        win.show();
      }
    });
    if (s.settings.closeOnLaunch) win.hide();
    return true;
  });

  handle('game:kill', () => {
    if (game) game.kill();
  });
}

function addSkin(buf, name) {
  const problem = skins.validateSkin(buf);
  if (problem) throw new Error(problem);
  const s = store.load();
  const id = store.newId('skin');
  fs.mkdirSync(paths.skins(), { recursive: true });
  fs.writeFileSync(path.join(paths.skins(), `${id}.png`), buf);
  s.skins.push({ id, name: String(name || 'Skin').slice(0, 32), variant: 'classic' });
  store.save();
  return publicState();
}

// ---------------------------------------------------------------- window

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 790,
    minWidth: 1000,
    minHeight: 640,
    frame: false,
    backgroundColor: '#0c0c11',
    title: 'Nova Launcher',
    icon: path.join(RESOURCES, 'icon.png'),
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  win.once('ready-to-show', () => win.show());
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (e) => e.preventDefault());
  win.loadFile(path.join(__dirname, 'ui', 'index.html'));
}

app.whenReady().then(() => {
  registerIpc();
  createWindow();
});

app.on('window-all-closed', () => {
  app.quit();
});
