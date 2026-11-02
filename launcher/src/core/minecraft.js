'use strict';
const fs = require('fs');
const path = require('path');
const os = require('os');
const paths = require('./paths');
const { getJson, download, pool } = require('./net');

const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';

function osName() {
  if (process.platform === 'win32') return 'windows';
  if (process.platform === 'darwin') return 'osx';
  return 'linux';
}

/** Evaluates Mojang library/argument rules. */
function rulesAllow(rules, features = {}) {
  if (!rules || rules.length === 0) return true;
  let allowed = false;
  for (const rule of rules) {
    let matches = true;
    if (rule.os) {
      if (rule.os.name && rule.os.name !== osName()) matches = false;
      if (rule.os.arch && rule.os.arch !== (process.arch === 'ia32' ? 'x86' : process.arch)) matches = false;
    }
    if (rule.features) {
      for (const [k, v] of Object.entries(rule.features)) {
        if (Boolean(features[k]) !== v) matches = false;
      }
    }
    if (matches) allowed = rule.action === 'allow';
  }
  return allowed;
}

/** Maven coordinate "group:artifact:version[:classifier]" -> relative path. */
function mavenPath(name) {
  const [group, artifact, version, classifier] = name.split(':');
  const file = `${artifact}-${version}${classifier ? `-${classifier}` : ''}.jar`;
  return path.join(...group.split('.'), artifact, version, file);
}

async function versionJson(id) {
  const file = path.join(paths.versions(), id, `${id}.json`);
  try {
    const manifest = await getJson(MANIFEST);
    const entry = manifest.versions.find((v) => v.id === id);
    if (!entry) throw new Error(`Minecraft ${id} nicht gefunden`);
    await download(entry.url, file, entry.sha1);
  } catch (e) {
    if (!fs.existsSync(file)) throw e;
  }
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

/** Downloads client jar, libraries, assets and the logging config. Returns launch info. */
async function install(id, progress) {
  progress('Minecraft-Daten werden geladen', 0, 1);
  const v = await versionJson(id);
  const tasks = [];
  const classpath = [];

  const clientJar = path.join(paths.versions(), id, `${id}.jar`);
  tasks.push(() => download(v.downloads.client.url, clientJar, v.downloads.client.sha1, v.downloads.client.size));

  for (const lib of v.libraries) {
    if (!rulesAllow(lib.rules)) continue;
    const art = lib.downloads && lib.downloads.artifact;
    if (!art) continue;
    const dest = path.join(paths.libraries(), art.path);
    classpath.push({ name: lib.name, path: dest });
    tasks.push(() => download(art.url, dest, art.sha1, art.size));
  }

  let logConfig = null;
  if (v.logging && v.logging.client && v.logging.client.file) {
    const f = v.logging.client.file;
    logConfig = { file: path.join(paths.assets(), 'log_configs', f.id), argument: v.logging.client.argument };
    tasks.push(() => download(f.url, logConfig.file, f.sha1, f.size));
  }

  const indexFile = path.join(paths.assets(), 'indexes', `${v.assetIndex.id}.json`);
  await download(v.assetIndex.url, indexFile, v.assetIndex.sha1, v.assetIndex.size);
  const index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
  for (const obj of Object.values(index.objects)) {
    const sub = obj.hash.substring(0, 2);
    const dest = path.join(paths.assets(), 'objects', sub, obj.hash);
    tasks.push(() => download(`https://resources.download.minecraft.net/${sub}/${obj.hash}`, dest, obj.hash, obj.size));
  }

  await pool(tasks, 16, (d, t) => progress('Minecraft wird heruntergeladen', d, t));
  return { version: v, clientJar, classpath, logConfig };
}

module.exports = { install, rulesAllow, mavenPath, osName, versionJson };
