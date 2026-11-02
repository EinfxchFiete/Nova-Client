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
 * Makes sure the instance mods folder contains Fabric API (from Modrinth) and,
 * if enabled, the bundled Nova Client. User mods in the folder are left alone.
 */
async function ensure(instanceDir, gameVersion, novaEnabled, resourcesDir, progress) {
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
  const query = `game_versions=${encodeURIComponent(JSON.stringify([gameVersion]))}&loaders=${encodeURIComponent(JSON.stringify(['fabric']))}`;
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

  const novaDest = path.join(modsDir, NOVA_JAR_NAME);
  if (novaEnabled) {
    fs.writeFileSync(novaDest, fs.readFileSync(bundledNovaJar(resourcesDir)));
  } else {
    fs.rmSync(novaDest, { force: true });
  }
  fs.writeFileSync(metaFile, JSON.stringify({ fabricApi: fabricFile }, null, 2));
  progress('Mods bereit', 1, 1);
  return { modsDir, fabricApi: fabricFile };
}

module.exports = { ensure, bundledNovaJar };
