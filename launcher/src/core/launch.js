'use strict';
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const paths = require('./paths');
const minecraft = require('./minecraft');
const fabric = require('./fabric');
const java = require('./java');
const mods = require('./mods');

const GAME_VERSION = '26.3';
const LAUNCHER_NAME = 'NovaLauncher';
const LAUNCHER_VERSION = '1.5.6';

/** group:artifact[:classifier] of a maven name, used to drop duplicate libraries. */
function libKey(name) {
  const p = name.split(':');
  return p.length > 3 ? `${p[0]}:${p[1]}:${p[3]}` : `${p[0]}:${p[1]}`;
}

function substitute(arg, vars) {
  return arg.replace(/\$\{([^}]+)\}/g, (m, k) => (vars[k] !== undefined ? vars[k] : m));
}

function collectArgs(list, vars, features) {
  const out = [];
  for (const entry of list) {
    if (typeof entry === 'string') {
      out.push(substitute(entry, vars));
    } else if (minecraft.rulesAllow(entry.rules, features)) {
      const values = Array.isArray(entry.value) ? entry.value : [entry.value];
      for (const v of values) out.push(substitute(v, vars));
    }
  }
  return out;
}

/**
 * Installs everything needed and starts the game.
 * account: { name, uuid, accessToken, xuid }
 * profile: { id, name, ramMb, novaEnabled, perfMods, jvmArgs, width, height }
 * Returns the child process.
 */
async function launch({ account, profile, resourcesDir, javaPath, progress, onLog }) {
  const instanceDir = path.join(paths.instances(), profile.id);
  fs.mkdirSync(instanceDir, { recursive: true });

  const mc = await minecraft.install(GAME_VERSION, progress);
  const fab = await fabric.install(GAME_VERSION, progress);
  const javaExe = javaPath && fs.existsSync(javaPath) ? javaPath : await java.ensure(mc.version.javaVersion.component, progress);
  await mods.ensure(instanceDir, GAME_VERSION, profile.novaEnabled !== false, resourcesDir, progress, profile.perfMods !== false);

  const fabricKeys = new Set(fab.libraryNames.map(libKey));
  const cp = [
    ...fab.classpath,
    ...mc.classpath.filter((l) => !fabricKeys.has(libKey(l.name))).map((l) => l.path),
    mc.clientJar,
  ];

  const nativesDir = path.join(paths.versions(), GAME_VERSION, 'natives');
  fs.mkdirSync(nativesDir, { recursive: true });
  const vars = {
    auth_player_name: account.name,
    version_name: `fabric-loader-${fab.loader}-${GAME_VERSION}`,
    game_directory: instanceDir,
    assets_root: paths.assets(),
    assets_index_name: mc.version.assetIndex.id,
    auth_uuid: account.uuid,
    auth_access_token: account.accessToken,
    clientid: account.clientId || '',
    auth_xuid: account.xuid || '',
    version_type: 'release',
    natives_directory: nativesDir,
    launcher_name: LAUNCHER_NAME,
    launcher_version: LAUNCHER_VERSION,
    classpath: cp.join(path.delimiter),
    resolution_width: String(profile.width || 1280),
    resolution_height: String(profile.height || 720),
  };
  const features = { has_custom_resolution: Boolean(profile.width && profile.height) };

  const ram = Math.max(1024, Number(profile.ramMb) || 4096);
  const jvm = [`-Xmx${ram}M`, `-Xms${Math.min(ram, 1024)}M`];
  if (mc.logConfig) jvm.push(substitute(mc.logConfig.argument, { path: mc.logConfig.file }));
  jvm.push(...fab.jvm);
  if (profile.jvmArgs && profile.jvmArgs.trim()) jvm.push(...profile.jvmArgs.trim().split(/\s+/));
  jvm.push(...collectArgs(mc.version.arguments.jvm, vars, features));

  const game = [...collectArgs(mc.version.arguments.game, vars, features), ...fab.game];
  const args = [...jvm, fab.mainClass, ...game];

  progress('Minecraft startet', 1, 1);
  const child = spawn(javaExe, args, { cwd: instanceDir, detached: false, windowsHide: false });
  const forward = (stream) => {
    let buf = '';
    stream.on('data', (chunk) => {
      buf += chunk.toString();
      const lines = buf.split(/\r?\n/);
      buf = lines.pop();
      for (const line of lines) onLog(line);
    });
  };
  forward(child.stdout);
  forward(child.stderr);
  return child;
}

/** Hides the access token in logged command lines. */
function redact(args, token) {
  return args.map((a) => (token && a === token ? '***' : a));
}

module.exports = { launch, GAME_VERSION, redact, libKey };
