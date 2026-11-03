'use strict';
/* global skinview3d */
const api = window.nova;
const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];

let state = null;
let playViewer = null;
let skinsViewer = null;
let selectedProfile = null;
let selectedSkin = null;
let gameRunning = false;
let logLines = [];

const COLORS = ['#8b5cf6', '#3b82f6', '#06b6d4', '#10b981', '#84cc16', '#eab308', '#f97316', '#ef4444', '#ec4899', '#64748b'];
const NEWS = [
  { t: 'Nova Client 1.6.0', title: 'Noch mehr Cosmetics', g: 'linear-gradient(135deg,#f97316,#ec4899)', text: '8 neue Kopf-Cosmetics, Extras (Rucksack, Schwänze, Schal), 3 neue Flügel, 3 animierte Capes, 5 Emotes, Minimap, Schadenszahlen und mehr.' },
  { t: 'Nova Client 1.5.0', title: 'Minimees & Song-Cover', g: 'linear-gradient(135deg,#db2777,#f59e0b)', text: 'Minimees als Kette, an der Hosentasche oder auf dem Kopf, Song-Cover und Zeit über dem Namen, 7 neue animierte Capes, 15 neue Begleiter und /playtime.' },
  { t: 'Verbessert', title: 'Ein Cosmetics-Menü', g: 'linear-gradient(135deg,#8b5cf6,#22d3ee)', text: 'Alle Cosmetics in einem Menü mit 3D-Vorschau, einfärbbare Flügel, Hörner und Kronen-Steine, echter Ping und Wegpunkte pro Welt.' },
  { t: 'Nova Client 1.4.1', title: 'Nova-Server ist online', g: 'linear-gradient(135deg,#0ea5e9,#8b5cf6)', text: 'Der Nova-Server ist fest eingebaut: Alle Nova-Spieler sehen automatisch gegenseitig Cosmetics, Songs, Emotes und Abzeichen.' },
];

// ---------------------------------------------------------------- helpers

function toast(msg, isError) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = `toast${isError ? ' err' : ''}`;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.add('hidden'), isError ? 6500 : 3000);
}

async function call(channel, ...args) {
  try {
    return await api.invoke(channel, ...args);
  } catch (e) {
    toast(e.message, true);
    e.shown = true;
    throw e;
  }
}

// errors from call() were already shown to the user as a toast
window.addEventListener('unhandledrejection', (e) => {
  if (e.reason && e.reason.shown) e.preventDefault();
});

function el(tag, attrs = {}, ...children) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') e.className = v;
    else if (k === 'style') e.style.cssText = v;
    else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else e.setAttribute(k, v);
  }
  for (const c of children) if (c != null) e.append(c);
  return e;
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

/** Draws the face (+ hat layer) of a skin texture onto an 8x8 canvas. */
async function drawHead(canvas, src) {
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  ctx.clearRect(0, 0, 8, 8);
  try {
    const img = await loadImage(src);
    ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 8, 8);
    ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 8, 8);
  } catch {
    ctx.fillStyle = '#444';
    ctx.fillRect(0, 0, 8, 8);
  }
}

/** Flat front view of a skin (16x32 canvas). */
async function drawFront(canvas, src, slim) {
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  ctx.clearRect(0, 0, 16, 32);
  const img = await loadImage(src);
  const legacy = img.height === 32;
  const aw = slim ? 3 : 4;
  ctx.drawImage(img, 8, 8, 8, 8, 4, 0, 8, 8);
  ctx.drawImage(img, 20, 20, 8, 12, 4, 8, 8, 12);
  ctx.drawImage(img, 44, 20, aw, 12, 4 - aw, 8, aw, 12);
  if (legacy) ctx.drawImage(img, 44, 20, aw, 12, 12, 8, aw, 12);
  else ctx.drawImage(img, 36, 52, aw, 12, 12, 8, aw, 12);
  ctx.drawImage(img, 4, 20, 4, 12, 4, 20, 4, 12);
  if (legacy) ctx.drawImage(img, 4, 20, 4, 12, 8, 20, 4, 12);
  else ctx.drawImage(img, 20, 52, 4, 12, 8, 20, 4, 12);
  ctx.drawImage(img, 40, 8, 8, 8, 4, 0, 8, 8);
}

/** Back of a cape texture (10x16 at 1,1) on a 10x16 canvas. */
async function drawCape(canvas, src) {
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  const img = await loadImage(src);
  const scale = img.width / 64;
  ctx.drawImage(img, 1 * scale, 1 * scale, 10 * scale, 16 * scale, 0, 0, 10, 16);
}

function activeProfile() {
  return state.profiles.find((p) => p.id === state.activeProfile) || state.profiles[0];
}

function activeAccount() {
  return state.accounts.find((a) => a.active) || null;
}

function ago(ts) {
  if (!ts) return 'noch nie gespielt';
  const m = Math.round((Date.now() - ts) / 60000);
  if (m < 1) return 'gerade eben';
  if (m < 60) return `vor ${m} Min.`;
  const h = Math.round(m / 60);
  if (h < 24) return `vor ${h} Std.`;
  return `vor ${Math.round(h / 24)} Tagen`;
}

// ---------------------------------------------------------------- navigation

function showPage(name) {
  $$('.nav').forEach((b) => b.classList.toggle('active', b.dataset.page === name));
  $$('.page').forEach((p) => p.classList.toggle('active', p.id === `page-${name}`));
  if (name === 'profiles') renderProfiles();
  if (name === 'skins') renderSkinsPage();
  if (name === 'settings') renderSettings();
  if (name === 'console') renderLog();
  requestAnimationFrame(resizeViewers);
}

$$('.nav').forEach((b) => b.addEventListener('click', () => showPage(b.dataset.page)));
$$('[data-win]').forEach((b) => b.addEventListener('click', () => api.invoke('window', b.dataset.win)));
document.addEventListener('click', (e) => {
  const link = e.target.closest('[data-link]');
  if (link) {
    e.preventDefault();
    api.invoke('open-external', link.dataset.link);
  }
  if (!e.target.closest('#account-btn') && !e.target.closest('#account-menu')) $('#account-menu').classList.add('hidden');
  if (!e.target.closest('#profile-pick') && !e.target.closest('#profile-popover')) $('#profile-popover').classList.add('hidden');
});

// ---------------------------------------------------------------- skin viewers

function makeViewer(canvas, opts = {}) {
  const v = new skinview3d.SkinViewer({ canvas, width: canvas.clientWidth || 400, height: canvas.clientHeight || 400 });
  v.fov = 40;
  v.zoom = opts.zoom || 0.82;
  v.background = null;
  v.animation = new skinview3d.WalkingAnimation();
  v.animation.speed = 0.55;
  v.autoRotate = Boolean(opts.rotate);
  v.autoRotateSpeed = 0.6;
  v.controls.enableZoom = Boolean(opts.allowZoom);
  return v;
}

function resizeViewers() {
  for (const [v, c] of [[playViewer, $('#play-skin')], [skinsViewer, $('#skins-skin')]]) {
    if (v && c.clientWidth > 0) v.setSize(c.clientWidth, c.clientHeight);
  }
}
window.addEventListener('resize', resizeViewers);

$('#anim-toggle').addEventListener('change', (e) => {
  if (playViewer) playViewer.animation.paused = !e.target.checked;
});

async function refreshPlaySkin() {
  const skin = await call('accounts:skin');
  const acc = activeAccount();
  $('#nametag-name').textContent = acc ? acc.name : 'Nicht eingeloggt';
  $('#nametag').classList.toggle('logged-out', !acc);
  if (playViewer) await playViewer.loadSkin(skin.url, { model: skin.variant === 'slim' ? 'slim' : 'default' });
  const capeActive = acc && acc.capes.find((c) => c.state === 'ACTIVE');
  if (playViewer) {
    if (capeActive) {
      try {
        await playViewer.loadCape(await call('capes:texture', capeActive.url));
      } catch {
        playViewer.loadCape(null);
      }
    } else {
      playViewer.loadCape(null);
    }
  }
  await drawHead($('#account-head'), skin.url);
}

// ---------------------------------------------------------------- top bar / accounts

function renderTop() {
  const acc = activeAccount();
  $('#account-name').textContent = acc ? acc.name : 'Anmelden';
  $('#sub-version').textContent = `Minecraft ${state.gameVersion} · Fabric · Launcher ${state.version}`;
  const p = activeProfile();
  $('#launch-profile').textContent = p.name;
  setStatus();
}

function setStatus(text, cls) {
  const pill = $('#status-pill');
  pill.className = `status-pill ${cls || (gameRunning ? 'running' : '')}`;
  pill.textContent = text || (gameRunning ? 'Spiel läuft' : 'Bereit');
}

$('#account-btn').addEventListener('click', async () => {
  const menu = $('#account-menu');
  if (!menu.classList.contains('hidden')) {
    menu.classList.add('hidden');
    return;
  }
  menu.innerHTML = '';
  if (state.accounts.length === 0) menu.append(el('div', { class: 'empty', style: 'padding:8px' }, 'Noch kein Konto angemeldet.'));
  for (const a of state.accounts) {
    const head = el('canvas', { width: 8, height: 8 });
    const row = el('div', { class: 'acc-row', onclick: async () => {
      state = await call('accounts:select', a.uuid);
      menu.classList.add('hidden');
      renderTop();
      refreshPlaySkin();
    } }, head, el('span', { class: 'name' }, a.name), a.active ? el('span', { class: 'tag' }, 'AKTIV') : null,
    el('button', { title: 'Abmelden', onclick: async (e) => {
      e.stopPropagation();
      state = await call('accounts:remove', a.uuid);
      menu.classList.add('hidden');
      renderTop();
      refreshPlaySkin();
    } }, '×'));
    menu.append(row);
    if (a.active) {
      api.invoke('accounts:skin').then((s) => drawHead(head, s.url)).catch(() => {});
    } else {
      const skin = (a.skins || []).find((k) => k.state === 'ACTIVE');
      if (!skin) drawHead(head, 'data:,');
    }
  }
  menu.append(el('button', { class: 'btn primary acc-add', onclick: () => {
    menu.classList.add('hidden');
    startLogin();
  } }, '+ Microsoft-Konto hinzufügen'));
  menu.classList.remove('hidden');
});

async function startLogin() {
  const modal = $('#login-modal');
  const body = $('#login-body');
  modal.classList.remove('hidden');
  body.innerHTML = '<p><span class="spinner"></span>Verbinde mit Microsoft…</p>';
  try {
    const code = await api.invoke('auth:start');
    body.innerHTML = '';
    body.append(
      el('p', {}, 'Öffne die Microsoft-Seite und gib diesen Code ein:'),
      el('div', { class: 'code' }, code.userCode),
      el('div', { class: 'row-gap' },
        el('button', { class: 'btn primary', onclick: () => {
          navigator.clipboard.writeText(code.userCode).catch(() => {});
          api.invoke('open-external', code.verificationUri);
        } }, 'Code kopieren & Seite öffnen')),
      el('p', { id: 'login-wait' }, el('span', { class: 'spinner' }), 'Warte auf deine Bestätigung im Browser…'),
    );
  } catch (e) {
    body.innerHTML = '';
    body.append(el('div', { class: 'error-box' }, e.message),
      el('p', {}, 'Den Login richtest du unter ', el('a', { href: '#', onclick: (ev) => {
        ev.preventDefault();
        modal.classList.add('hidden');
        showPage('settings');
      } }, 'Optionen → Microsoft-Login'), ' ein.'));
  }
}

api.on('auth:status', async (s) => {
  const wait = $('#login-wait');
  if (s.stage === 'minecraft' && wait) wait.innerHTML = '<span class="spinner"></span>Melde bei Minecraft an…';
  if (s.stage === 'done') {
    $('#login-modal').classList.add('hidden');
    state = await call('state');
    renderTop();
    refreshPlaySkin();
    toast(`Angemeldet als ${s.name}`);
  }
  if (s.stage === 'error') {
    const body = $('#login-body');
    body.innerHTML = '';
    body.append(el('div', { class: 'error-box' }, s.error));
  }
});

$('#login-cancel').addEventListener('click', () => {
  api.invoke('auth:cancel');
  $('#login-modal').classList.add('hidden');
});

// ---------------------------------------------------------------- play

function renderNews() {
  const list = $('#news-list');
  list.innerHTML = '';
  for (const n of NEWS) {
    list.append(el('div', { class: 'news-card' }, el('div', { class: 't' }, n.t),
      el('div', { class: 'img', style: `background:${n.g}` }, el('span', {}, n.title)), el('p', {}, n.text)));
  }
}

$('#profile-pick').addEventListener('click', () => {
  const pop = $('#profile-popover');
  if (!pop.classList.contains('hidden')) {
    pop.classList.add('hidden');
    return;
  }
  pop.innerHTML = '';
  for (const p of state.profiles) {
    pop.append(el('div', { class: `pp-row${p.id === state.activeProfile ? ' active' : ''}`, onclick: async () => {
      state = await call('profiles:select', p.id);
      pop.classList.add('hidden');
      renderTop();
    } }, el('span', { class: 'pp-dot', style: `background:${p.color}` }), el('span', { class: 'name' }, p.name),
    el('span', { class: 'meta' }, `${Math.round(p.ramMb / 1024)} GB · ${p.novaEnabled ? 'Nova' : 'Vanilla+Fabric'}`)));
  }
  pop.append(el('div', { class: 'pp-row', onclick: () => {
    pop.classList.add('hidden');
    showPage('profiles');
  } }, el('span', { class: 'name', style: 'color:var(--accent2)' }, 'Profile verwalten…')));
  pop.classList.remove('hidden');
});

$('#launch-btn').addEventListener('click', async () => {
  if (gameRunning) {
    showPage('console');
    return;
  }
  if (!activeAccount()) {
    startLogin();
    return;
  }
  $('#launch-btn').disabled = true;
  $('#progress').classList.remove('hidden');
  setStatus('Wird vorbereitet', 'busy');
  logLines = [];
  try {
    await api.invoke('game:launch');
  } catch (e) {
    toast(e.message, true);
    $('#progress').classList.add('hidden');
    setStatus();
  } finally {
    $('#launch-btn').disabled = false;
  }
});

api.on('game:progress', (p) => {
  const pct = p.total > 0 ? Math.round((p.done / p.total) * 100) : 0;
  $('#progress-bar').style.width = `${pct}%`;
  $('#progress-text').textContent = p.total > 1 ? `${p.stage} · ${p.done}/${p.total}` : p.stage;
  setStatus(p.stage, 'busy');
});

api.on('game:state', (s) => {
  gameRunning = s.running;
  $('#launch-label').textContent = s.running ? 'LÄUFT' : 'STARTEN';
  if (s.running) {
    setTimeout(() => $('#progress').classList.add('hidden'), 1500);
  } else {
    $('#progress').classList.add('hidden');
    if (s.code && s.code !== 0) toast(`Minecraft wurde mit Fehlercode ${s.code} beendet – siehe Konsole.`, true);
  }
  setStatus();
});

// ---------------------------------------------------------------- console

const pendingLog = { level: 'INFO', inThrowable: false };

/** Mojang's log config prints XML; turn it back into readable lines. */
function parseLogLine(line) {
  const ev = line.match(/<log4j:Event [^>]*level="(\w+)"/);
  if (ev) {
    pendingLog.level = ev[1];
    return null;
  }
  let m = line.match(/<log4j:Message><!\[CDATA\[(.*?)(\]\]><\/log4j:Message>)?$/);
  if (m) return { level: pendingLog.level, text: m[1] };
  m = line.match(/<log4j:Throwable><!\[CDATA\[(.*?)(\]\]><\/log4j:Throwable>)?$/);
  if (m) {
    pendingLog.inThrowable = !m[2];
    return { level: 'ERROR', text: m[1] };
  }
  if (pendingLog.inThrowable) {
    if (line.includes(']]>')) pendingLog.inThrowable = false;
    const t = line.replace(/\]\]><\/log4j:Throwable>.*/, '');
    return t.trim() ? { level: 'ERROR', text: t } : null;
  }
  if (/^\s*<\/?log4j:/.test(line) || !line.trim()) return null;
  return { level: /error|exception/i.test(line) ? 'ERROR' : 'INFO', text: line };
}

function addLogLine(entry) {
  logLines.push(entry);
  if (logLines.length > 5000) logLines.splice(0, logLines.length - 5000);
  if ($('#page-console').classList.contains('active')) appendLogDom(entry);
}

function appendLogDom(entry) {
  const log = $('#log');
  const nearBottom = log.scrollHeight - log.scrollTop - log.clientHeight < 40;
  const cls = /Nova Client/.test(entry.text) ? 'l-NOVA' : `l-${entry.level}`;
  log.append(el('div', { class: cls }, entry.text));
  if (nearBottom) log.scrollTop = log.scrollHeight;
}

function renderLog() {
  const log = $('#log');
  log.innerHTML = '';
  if (logLines.length === 0) log.append(el('div', { class: 'empty' }, 'Noch keine Ausgabe. Starte das Spiel, dann erscheinen hier die Meldungen.'));
  for (const e of logLines) appendLogDom(e);
}

api.on('game:log', (line) => {
  const entry = parseLogLine(line);
  if (entry) addLogLine(entry);
});

$('#log-clear').addEventListener('click', () => {
  logLines = [];
  renderLog();
});
$('#log-copy').addEventListener('click', () => {
  navigator.clipboard.writeText(logLines.map((l) => `[${l.level}] ${l.text}`).join('\n'));
  toast('Log kopiert');
});
$('#game-kill').addEventListener('click', () => api.invoke('game:kill'));

// ---------------------------------------------------------------- profiles

function renderProfiles() {
  const grid = $('#profile-grid');
  grid.innerHTML = '';
  if (!selectedProfile || !state.profiles.some((p) => p.id === selectedProfile)) selectedProfile = state.activeProfile;
  for (const p of state.profiles) {
    grid.append(el('div', { class: `profile-card${p.id === selectedProfile ? ' selected' : ''}`, onclick: () => {
      selectedProfile = p.id;
      renderProfiles();
    } },
    p.id === state.activeProfile ? el('span', { class: 'badge' }, 'AKTIV') : null,
    el('div', { class: 'icon', style: `background:${p.color}` }, p.name.trim().charAt(0).toUpperCase() || 'N'),
    el('div', { class: 'name' }, p.name),
    el('div', { class: 'meta' }, `${(p.ramMb / 1024).toFixed(1).replace('.0', '')} GB RAM · ${p.novaEnabled ? 'Nova Client' : 'ohne Nova'}`),
    el('div', { class: 'meta' }, ago(p.lastPlayed))));
  }
  grid.append(el('div', { class: 'profile-card new', onclick: async () => {
    state = await call('profiles:create', { name: `Profil ${state.profiles.length + 1}`, color: COLORS[state.profiles.length % COLORS.length] });
    selectedProfile = state.activeProfile;
    renderProfiles();
    renderTop();
  } }, '+ Neues Profil'));
  renderProfileEditor();
}

function renderProfileEditor() {
  const ed = $('#profile-editor');
  const p = state.profiles.find((x) => x.id === selectedProfile);
  ed.innerHTML = '';
  if (!p) return;
  const update = async (patch, rerender) => {
    state = await call('profiles:update', p.id, patch);
    renderTop();
    if (rerender) renderProfiles();
  };
  const name = el('input', { value: p.name, maxlength: 32 });
  name.addEventListener('change', () => update({ name: name.value }, true));
  const ram = el('input', { type: 'range', min: 2048, max: 16384, step: 512, value: p.ramMb });
  const ramLabel = el('b', {}, `${(p.ramMb / 1024).toFixed(1)} GB`);
  ram.addEventListener('input', () => { ramLabel.textContent = `${(ram.value / 1024).toFixed(1)} GB`; });
  ram.addEventListener('change', () => update({ ramMb: Number(ram.value) }, true));
  const nova = el('input', { type: 'checkbox' });
  nova.checked = p.novaEnabled !== false;
  nova.addEventListener('change', () => update({ novaEnabled: nova.checked }, true));
  const jvm = el('input', { value: p.jvmArgs || '', placeholder: 'z. B. -XX:+UseZGC', spellcheck: 'false' });
  jvm.addEventListener('change', () => update({ jvmArgs: jvm.value }));
  const w = el('input', { type: 'number', min: 0, value: p.width || '', placeholder: 'Breite' });
  const h = el('input', { type: 'number', min: 0, value: p.height || '', placeholder: 'Höhe' });
  const res = () => update({ width: Number(w.value) || 0, height: Number(h.value) || 0 });
  w.addEventListener('change', res);
  h.addEventListener('change', res);
  const colors = el('div', { class: 'colors' });
  for (const c of COLORS) {
    colors.append(el('button', { class: c === p.color ? 'sel' : '', style: `background:${c}`, onclick: () => update({ color: c }, true) }));
  }
  ed.append(
    el('div', { class: 'panel-title' }, 'Profil bearbeiten'),
    el('label', { class: 'field' }, el('span', {}, 'Name'), name),
    el('div', { class: 'field' }, el('span', {}, 'Farbe')), colors,
    el('label', { class: 'field' }, el('span', {}, 'Arbeitsspeicher: ', ramLabel), ram),
    el('label', { class: 'toggle-row' }, el('span', {}, 'Nova Client aktiv'), el('label', { class: 'switch' }, nova, el('i'))),
    el('div', { class: 'field' }, el('span', {}, 'Fenstergröße (leer = Standard)'), el('div', { class: 'res-row' }, w, h)),
    el('label', { class: 'field' }, el('span', {}, 'Zusätzliche Java-Argumente'), jvm),
    el('div', { class: 'editor-actions' },
      p.id !== state.activeProfile ? el('button', { class: 'btn primary', onclick: async () => {
        state = await call('profiles:select', p.id);
        renderProfiles();
        renderTop();
      } }, 'Als aktiv wählen') : null,
      el('button', { class: 'btn', onclick: () => call('profiles:open', p.id, 'mods') }, 'Mods-Ordner'),
      el('button', { class: 'btn', onclick: () => call('profiles:open', p.id, '') }, 'Spielordner'),
      el('button', { class: 'btn danger', onclick: async () => {
        if (!confirm(`Profil „${p.name}“ löschen? Der Spielordner bleibt auf der Festplatte erhalten.`)) return;
        state = await call('profiles:delete', p.id);
        selectedProfile = state.activeProfile;
        renderProfiles();
        renderTop();
      } }, 'Löschen')),
  );
}

// ---------------------------------------------------------------- skins

async function renderSkinsPage() {
  state = await call('state');
  if (!skinsViewer) skinsViewer = makeViewer($('#skins-skin'), { rotate: true, allowZoom: true, zoom: 0.75 });
  resizeViewers();
  const grid = $('#skin-grid');
  grid.innerHTML = '';
  const acc = activeAccount();
  // current account skin card
  const current = await call('accounts:skin');
  const curCanvas = el('canvas', { width: 16, height: 32 });
  grid.append(el('div', { class: `skin-card${selectedSkin === 'current' ? ' sel' : ''}`, onclick: () => selectSkin('current', current.url, current.variant) },
    el('span', { class: 'cur' }, 'AKTUELL'), curCanvas, el('div', { class: 'name' }, acc ? acc.name : current.source === 'steve' ? 'Steve' : 'Standard')));
  drawFront(curCanvas, current.url, current.variant === 'slim').catch(() => {});
  for (const k of state.skins) {
    const c = el('canvas', { width: 16, height: 32 });
    const card = el('div', { class: `skin-card${selectedSkin === k.id ? ' sel' : ''}`, 'data-skin': k.id, onclick: async () => selectSkin(k.id, await call('skins:texture', k.id), k.variant) },
      el('button', { class: 'del', title: 'Entfernen', onclick: async (e) => {
        e.stopPropagation();
        state = await call('skins:remove', k.id);
        if (selectedSkin === k.id) selectedSkin = null;
        renderSkinsPage();
      } }, '×'), c, el('div', { class: 'name' }, k.name));
    grid.append(card);
    api.invoke('skins:texture', k.id).then((url) => drawFront(c, url, k.variant === 'slim')).catch(() => {});
  }
  if (state.skins.length === 0) grid.append(el('div', { class: 'empty' }, 'Füge PNG-Skins (64×64) hinzu, um sie hier zu sammeln.'));
  if (!selectedSkin) selectSkin('current', current.url, current.variant);
  else updateSkinControls();
  renderCapes();
}

async function selectSkin(id, url, variant) {
  selectedSkin = id;
  $$('.skin-card').forEach((c) => c.classList.toggle('sel', (c.dataset.skin || 'current') === id));
  await skinsViewer.loadSkin(url, { model: variant === 'slim' ? 'slim' : 'default' });
  updateSkinControls();
}

function updateSkinControls() {
  const k = state.skins.find((x) => x.id === selectedSkin);
  const variant = k ? k.variant : 'classic';
  $$('#variant-seg button').forEach((b) => {
    b.classList.toggle('sel', b.dataset.variant === variant);
    b.disabled = !k;
  });
  $('#apply-skin').disabled = !k;
  $('#skin-hint').textContent = k
    ? (activeAccount() ? 'Wird für alle Server übernommen, auf denen du spielst.' : 'Zum Anwenden zuerst oben rechts anmelden.')
    : 'Das ist dein aktueller Skin. Wähle einen Skin aus der Bibliothek, um ihn anzuwenden.';
}

$$('#variant-seg button').forEach((b) => b.addEventListener('click', async () => {
  const k = state.skins.find((x) => x.id === selectedSkin);
  if (!k) return;
  state = await call('skins:update', k.id, { variant: b.dataset.variant });
  await selectSkin(k.id, await call('skins:texture', k.id), b.dataset.variant);
  renderSkinsPage();
}));

$('#add-skin').addEventListener('click', async () => {
  state = await call('skins:add');
  renderSkinsPage();
});

$('#apply-skin').addEventListener('click', async () => {
  const k = state.skins.find((x) => x.id === selectedSkin);
  if (!k) return;
  $('#apply-skin').disabled = true;
  try {
    state = await call('skins:apply', k.id);
    toast(`Skin „${k.name}“ ist jetzt auf deinem Konto aktiv`);
    refreshPlaySkin();
    renderSkinsPage();
  } finally {
    $('#apply-skin').disabled = false;
  }
});

function renderCapes() {
  const list = $('#cape-list');
  list.innerHTML = '';
  const acc = activeAccount();
  if (!acc) {
    list.append(el('div', { class: 'empty' }, 'Melde dich an, um die Capes deines Kontos (z. B. Migrator- oder Event-Capes) auszuwählen.'));
    return;
  }
  const none = el('div', { class: `cape-item${acc.capes.every((c) => c.state !== 'ACTIVE') ? ' sel' : ''}`, onclick: async () => {
    state = await call('capes:set', null);
    renderCapes();
    refreshPlaySkin();
  } }, el('canvas', { width: 10, height: 16 }), 'Keins');
  list.append(none);
  if (acc.capes.length === 0) list.append(el('div', { class: 'empty' }, 'Dieses Konto besitzt keine offiziellen Capes. Nova-Capes wählst du im Spiel unter Cosmetics.'));
  for (const c of acc.capes) {
    const cv = el('canvas', { width: 10, height: 16 });
    list.append(el('div', { class: `cape-item${c.state === 'ACTIVE' ? ' sel' : ''}`, onclick: async () => {
      state = await call('capes:set', c.id);
      renderCapes();
      refreshPlaySkin();
    } }, cv, c.alias || 'Cape'));
    call('capes:texture', c.url).then((u) => drawCape(cv, u)).catch(() => {});
  }
}

// ---------------------------------------------------------------- settings

function renderSettings() {
  const s = state.settings;
  $('#client-id').value = s.clientId || '';
  $('#ram-default').value = s.defaultRamMb;
  $('#ram-default-label').textContent = `${(s.defaultRamMb / 1024).toFixed(1)} GB`;
  $('#close-on-launch').checked = Boolean(s.closeOnLaunch);
  $('#java-path').value = s.javaPath || '';
  $('#data-dir').textContent = `Daten-Ordner: ${state.dataDir}`;
}

const saveSetting = async (patch) => {
  state = await call('settings:set', patch);
  renderSettings();
};
$('#client-id').addEventListener('change', (e) => saveSetting({ clientId: e.target.value }));
$('#ram-default').addEventListener('input', (e) => { $('#ram-default-label').textContent = `${(e.target.value / 1024).toFixed(1)} GB`; });
$('#ram-default').addEventListener('change', (e) => saveSetting({ defaultRamMb: Number(e.target.value) }));
$('#close-on-launch').addEventListener('change', (e) => saveSetting({ closeOnLaunch: e.target.checked }));
$('#java-path').addEventListener('change', (e) => saveSetting({ javaPath: e.target.value.trim() }));

// ---------------------------------------------------------------- start

(async () => {
  state = await call('state');
  gameRunning = state.running;
  renderTop();
  renderNews();
  playViewer = makeViewer($('#play-skin'), { zoom: 0.6 });
  resizeViewers();
  await refreshPlaySkin();
  document.body.dataset.ready = '1';
})();
