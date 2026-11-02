'use strict';
const fs = require('fs');
const path = require('path');
const paths = require('./paths');
const { getJson, download, pool } = require('./net');

const RUNTIMES = 'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json';

function platformKey() {
  if (process.platform === 'win32') return process.arch === 'arm64' ? 'windows-arm64' : process.arch === 'ia32' ? 'windows-x86' : 'windows-x64';
  if (process.platform === 'darwin') return process.arch === 'arm64' ? 'mac-os-arm64' : 'mac-os';
  return process.arch === 'ia32' ? 'linux-i386' : 'linux';
}

function javaExecutable(home) {
  if (process.platform === 'win32') return path.join(home, 'bin', 'javaw.exe');
  if (process.platform === 'darwin') return path.join(home, 'jre.bundle', 'Contents', 'Home', 'bin', 'java');
  return path.join(home, 'bin', 'java');
}

/** Downloads the Java runtime Mojang uses for this version (e.g. java-runtime-epsilon = Java 25). */
async function ensure(component, progress) {
  const home = path.join(paths.runtime(), component, platformKey());
  const exe = javaExecutable(home);
  progress('Java wird vorbereitet', 0, 1);
  const all = await getJson(RUNTIMES);
  const list = all[platformKey()] && all[platformKey()][component];
  if (!list || list.length === 0) {
    if (fs.existsSync(exe)) return exe;
    throw new Error(`Java (${component}) ist für ${platformKey()} nicht verfügbar`);
  }
  const manifest = await getJson(list[0].manifest.url);
  const tasks = [];
  const links = [];
  for (const [rel, entry] of Object.entries(manifest.files)) {
    const dest = path.join(home, ...rel.split('/'));
    if (entry.type === 'directory') {
      fs.mkdirSync(dest, { recursive: true });
    } else if (entry.type === 'file') {
      const raw = entry.downloads.raw;
      tasks.push(async () => {
        await download(raw.url, dest, raw.sha1, raw.size);
        if (entry.executable && process.platform !== 'win32') fs.chmodSync(dest, 0o755);
      });
    } else if (entry.type === 'link') {
      links.push([dest, entry.target]);
    }
  }
  await pool(tasks, 12, (d, t) => progress('Java wird heruntergeladen', d, t));
  if (process.platform !== 'win32') {
    for (const [dest, target] of links) {
      try {
        fs.mkdirSync(path.dirname(dest), { recursive: true });
        if (!fs.existsSync(dest)) fs.symlinkSync(target, dest);
      } catch {
        /* links are not important for running the game */
      }
    }
  }
  if (!fs.existsSync(exe)) throw new Error('Java wurde nicht korrekt installiert');
  return exe;
}

module.exports = { ensure, platformKey, javaExecutable };
