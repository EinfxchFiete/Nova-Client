'use strict';
const os = require('os');
const path = require('path');

/** Root folder of the launcher (can be overridden for tests with NOVA_LAUNCHER_HOME). */
function root() {
  if (process.env.NOVA_LAUNCHER_HOME) return process.env.NOVA_LAUNCHER_HOME;
  if (process.platform === 'win32') {
    return path.join(process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming'), '.novalauncher');
  }
  if (process.platform === 'darwin') {
    return path.join(os.homedir(), 'Library', 'Application Support', 'novalauncher');
  }
  return path.join(os.homedir(), '.novalauncher');
}

module.exports = {
  root,
  versions: () => path.join(root(), 'versions'),
  libraries: () => path.join(root(), 'libraries'),
  assets: () => path.join(root(), 'assets'),
  runtime: () => path.join(root(), 'runtime'),
  instances: () => path.join(root(), 'instances'),
  skins: () => path.join(root(), 'skins'),
  configFile: () => path.join(root(), 'launcher.json'),
};
