'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const paths = require('./paths');

const DEFAULTS = () => ({
  settings: { clientId: '', closeOnLaunch: false, javaPath: '', defaultRamMb: 4096 },
  accounts: [],
  activeAccount: null,
  profiles: [{ id: 'nova-standard', name: 'Nova Standard', ramMb: 4096, novaEnabled: true, jvmArgs: '', width: 0, height: 0, color: '#8b5cf6', lastPlayed: 0 }],
  activeProfile: 'nova-standard',
  skins: [],
});

let state = null;

function load() {
  if (state) return state;
  try {
    const raw = JSON.parse(fs.readFileSync(paths.configFile(), 'utf8'));
    const d = DEFAULTS();
    state = { ...d, ...raw, settings: { ...d.settings, ...(raw.settings || {}) } };
    if (!Array.isArray(state.profiles) || state.profiles.length === 0) state.profiles = d.profiles;
  } catch {
    state = DEFAULTS();
  }
  return state;
}

function save() {
  fs.mkdirSync(path.dirname(paths.configFile()), { recursive: true });
  const tmp = `${paths.configFile()}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(state, null, 2));
  fs.renameSync(tmp, paths.configFile());
}

function newId(prefix) {
  return `${prefix}-${crypto.randomBytes(4).toString('hex')}`;
}

module.exports = { load, save, newId };
