'use strict';
const path = require('path');
const paths = require('./paths');
const { getJson, download, pool } = require('./net');
const { mavenPath } = require('./minecraft');

const META = 'https://meta.fabricmc.net/v2';

async function latestLoader(gameVersion) {
  const loaders = await getJson(`${META}/versions/loader/${encodeURIComponent(gameVersion)}`);
  const stable = loaders.find((l) => l.loader.stable) || loaders[0];
  if (!stable) throw new Error(`Kein Fabric Loader für ${gameVersion}`);
  return stable.loader.version;
}

/** Installs Fabric libraries; returns { mainClass, classpath, jvm, game }. */
async function install(gameVersion, progress) {
  progress('Fabric wird vorbereitet', 0, 1);
  const loader = await latestLoader(gameVersion);
  const profile = await getJson(`${META}/versions/loader/${encodeURIComponent(gameVersion)}/${encodeURIComponent(loader)}/profile/json`);
  const classpath = [];
  const tasks = [];
  for (const lib of profile.libraries) {
    const rel = mavenPath(lib.name);
    const dest = path.join(paths.libraries(), rel);
    classpath.push(dest);
    const base = (lib.url || 'https://maven.fabricmc.net/').replace(/\/?$/, '/');
    tasks.push(() => download(base + rel.split(path.sep).join('/'), dest, lib.sha1, lib.size));
  }
  await pool(tasks, 8, (d, t) => progress('Fabric wird heruntergeladen', d, t));
  return {
    loader,
    mainClass: profile.mainClass,
    classpath,
    jvm: (profile.arguments && profile.arguments.jvm) || [],
    game: (profile.arguments && profile.arguments.game) || [],
    libraryNames: profile.libraries.map((l) => l.name),
  };
}

module.exports = { install, latestLoader };
