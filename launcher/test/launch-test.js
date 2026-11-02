'use strict';
// Integration test (developer only): installs everything into a temporary launcher folder and
// starts the game with a placeholder session. The real launcher only starts with a Microsoft login.
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

process.env.NOVA_LAUNCHER_HOME = process.env.NOVA_LAUNCHER_HOME || path.join(__dirname, 'output', 'home');
const { launch } = require('../src/core/launch');

const extraMod = process.argv[2];
const profile = { id: 'test-profile', name: 'Test', ramMb: 3072, novaEnabled: true, jvmArgs: '' };
const account = { name: 'NovaTester', uuid: crypto.randomUUID().replace(/-/g, ''), accessToken: 'invalid-test-token', xuid: '0' };

(async () => {
  const started = Date.now();
  let lastStage = '';
  const log = [];
  const instanceMods = path.join(process.env.NOVA_LAUNCHER_HOME, 'instances', profile.id, 'mods');
  if (extraMod) {
    fs.mkdirSync(instanceMods, { recursive: true });
    fs.copyFileSync(extraMod, path.join(instanceMods, path.basename(extraMod)));
  }
  const child = await launch({
    account,
    profile,
    resourcesDir: path.join(__dirname, '..', 'resources'),
    progress: (stage, done, total) => {
      if (stage !== lastStage) {
        console.log(`[${((Date.now() - started) / 1000).toFixed(1)}s] ${stage} (${total})`);
        lastStage = stage;
      }
    },
    onLog: (line) => {
      log.push(line);
      if (/Nova Client .* loaded|ERROR|Exception|Backend library|Created: |Stopping/.test(line)) console.log('  game> ' + line);
    },
  });
  const timeout = setTimeout(() => {
    const ok = log.some((l) => /Nova Client .* loaded with \d+ modules/.test(l)) && log.some((l) => /Created: .*textures\/atlas/.test(l));
    console.log(ok ? 'LAUNCH TEST PASSED' : 'LAUNCH TEST FAILED');
    fs.mkdirSync(path.join(__dirname, 'output'), { recursive: true });
    fs.writeFileSync(path.join(__dirname, 'output', 'game.log'), log.join('\n'));
    child.kill();
    process.exit(ok ? 0 : 1);
  }, Number(process.env.WAIT_MS || 90000));
  child.on('exit', (code) => {
    clearTimeout(timeout);
    fs.mkdirSync(path.join(__dirname, 'output'), { recursive: true });
    fs.writeFileSync(path.join(__dirname, 'output', 'game.log'), log.join('\n'));
    console.log('game exited early with code ' + code + ' - LAUNCH TEST FAILED');
    process.exit(1);
  });
})().catch((e) => {
  console.error('LAUNCH TEST FAILED:', e);
  process.exit(1);
});
