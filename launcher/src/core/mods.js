'use strict';
const fs = require('fs');
const path = require('path');
const { getJson, download } = require('./net');

const NOVA_JAR_NAME = 'nova-client.jar';

/** Path of the Nova Client jar shipped with the launcher. */
function bundledNovaJar(resourcesDir) {
  return path.join(resourcesDir, NOVA_JAR_NAME);
}

/**
 * Free performance mods from Modrinth (more FPS, less RAM). Each one is optional: if Modrinth has no version for
 * this Minecraft version, it is skipped. `match` recognises a copy the user put into the mods folder himself.
 */
const PERF_MODS = [
  { slug: 'sodium', name: 'Sodium', match: /^sodium[-_]/i },
  { slug: 'lithium', name: 'Lithium', match: /^lithium[-_]/i },
  { slug: 'ferrite-core', name: 'FerriteCore', match: /^ferritecore[-_]/i },
  { slug: 'entityculling', name: 'EntityCulling', match: /^entityculling[-_]/i },
  { slug: 'immediatelyfast', name: 'ImmediatelyFast', match: /^immediatelyfast[-_]/i },
];

function modrinthQuery(gameVersion) {
  return `game_versions=${encodeURIComponent(JSON.stringify([gameVersion]))}&loaders=${encodeURIComponent(JSON.stringify(['fabric']))}`;
}

/** Newest release (or, if there is none, the newest version) of a Modrinth project for this game version. */
async function newestFile(slug, gameVersion) {
  const versions = await getJson(`https://api.modrinth.com/v2/project/${slug}/version?${modrinthQuery(gameVersion)}`);
  if (!Array.isArray(versions) || versions.length === 0) return null;
  const version = versions.find((v) => v.version_type === 'release') || versions[0];
  const file = version.files.find((f) => f.primary) || version.files[0];
  return file && /^https:\/\//.test(file.url) && /\.jar$/i.test(file.filename) ? file : null;
}

/** Installs, updates or removes the performance mods; failures only skip a mod. Returns the managed file names. */
async function ensurePerfMods(modsDir, gameVersion, enabled, managedBefore, progress) {
  const managed = {};
  const own = new Set(Object.values(managedBefore || {}));
  const present = fs.readdirSync(modsDir);
  for (let i = 0; i < PERF_MODS.length; i++) {
    const mod = PERF_MODS[i];
    const old = managedBefore ? managedBefore[mod.slug] : null;
    if (!enabled) {
      if (old) fs.rmSync(path.join(modsDir, old), { force: true });
      continue;
    }
    // the user has his own copy of this mod: never install a second one (Fabric would not start)
    if (present.some((f) => mod.match.test(f) && !own.has(f))) continue;
    progress(`${mod.name} wird geladen`, i, PERF_MODS.length);
    try {
      const file = await newestFile(mod.slug, gameVersion);
      if (!file) {
        if (old) fs.rmSync(path.join(modsDir, old), { force: true });
        continue;
      }
      if (old && old !== file.filename) fs.rmSync(path.join(modsDir, old), { force: true });
      await download(file.url, path.join(modsDir, file.filename), file.hashes.sha1, file.size);
      managed[mod.slug] = file.filename;
    } catch (e) {
      // offline or Modrinth down: keep the version we already have
      if (old && fs.existsSync(path.join(modsDir, old))) managed[mod.slug] = old;
    }
  }
  return managed;
}

/**
 * Makes sure the instance mods folder contains Fabric API (from Modrinth), the performance mods (if enabled) and,
 * if enabled, the bundled Nova Client. User mods in the folder are left alone.
 */
async function ensure(instanceDir, gameVersion, novaEnabled, resourcesDir, progress, perfMods = true) {
  const modsDir = path.join(instanceDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const metaFile = path.join(instanceDir, 'nova-managed.json');
  let managed = {};
  try {
    managed = JSON.parse(fs.readFileSync(metaFile, 'utf8'));
  } catch {
    managed = {};
  }

  progress('Fabric API wird gesucht', 0, 1);
  const query = modrinthQuery(gameVersion);
  let fabricFile = managed.fabricApi;
  try {
    const versions = await getJson(`https://api.modrinth.com/v2/project/fabric-api/version?${query}`);
    if (versions.length > 0) {
      const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
      if (managed.fabricApi && managed.fabricApi !== file.filename) {
        fs.rmSync(path.join(modsDir, managed.fabricApi), { force: true });
      }
      progress('Fabric API wird heruntergeladen', 0, 1);
      await download(file.url, path.join(modsDir, file.filename), file.hashes.sha1, file.size);
      fabricFile = file.filename;
    }
  } catch (e) {
    if (!fabricFile || !fs.existsSync(path.join(modsDir, fabricFile))) {
      throw new Error(`Fabric API konnte nicht geladen werden: ${e.message}`);
    }
  }

  const perf = await ensurePerfMods(modsDir, gameVersion, perfMods, managed.perf, progress);

  const novaDest = path.join(modsDir, NOVA_JAR_NAME);
  if (novaEnabled) {
    fs.writeFileSync(novaDest, fs.readFileSync(bundledNovaJar(resourcesDir)));
  } else {
    fs.rmSync(novaDest, { force: true });
  }
  fs.writeFileSync(metaFile, JSON.stringify({ fabricApi: fabricFile, perf }, null, 2));
  progress('Mods bereit', 1, 1);
  return { modsDir, fabricApi: fabricFile, perf };
}

module.exports = { ensure, bundledNovaJar, PERF_MODS, ensurePerfMods };
