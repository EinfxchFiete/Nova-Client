'use strict';
// Developer test: clicks STARTEN in the real UI and checks that Minecraft + Nova start and log into the console page.
// A placeholder account is written into the test config because no Microsoft login is possible in CI.
const { _electron: electron } = require('playwright');
const path = require('path');
const fs = require('fs');

const out = path.join(__dirname, 'output', 'ui');
const home = path.join(__dirname, 'output', 'home');
fs.mkdirSync(out, { recursive: true });
fs.mkdirSync(home, { recursive: true });
const cfgFile = path.join(home, 'launcher.json');
const cfg = fs.existsSync(cfgFile) ? JSON.parse(fs.readFileSync(cfgFile, 'utf8')) : {};
cfg.accounts = [{
  uuid: '0123456789abcdef0123456789abcdef', name: 'NovaTester', skins: [], capes: [],
  refreshToken: null, mcToken: { enc: false, data: Buffer.from('invalid-test-token').toString('base64') }, mcExpiresAt: Date.now() + 3600e3,
}];
cfg.activeAccount = '0123456789abcdef0123456789abcdef';
cfg.profiles = [{ id: 'test-profile', name: 'UI Test', ramMb: 3072, novaEnabled: true, perfMods: false, jvmArgs: '', width: 0, height: 0, color: '#8b5cf6', lastPlayed: 0 }];
cfg.activeProfile = 'test-profile';
fs.writeFileSync(cfgFile, JSON.stringify(cfg, null, 2));
if (process.argv[2]) {
  const mods = path.join(home, 'instances', 'test-profile', 'mods');
  fs.mkdirSync(mods, { recursive: true });
  fs.copyFileSync(process.argv[2], path.join(mods, path.basename(process.argv[2])));
}

(async () => {
  const app = await electron.launch({ executablePath: require('electron'), args: ['--no-sandbox', path.join(__dirname, '..')], env: { ...process.env, NOVA_LAUNCHER_HOME: home } });
  const page = await app.firstWindow();
  await page.waitForSelector('body[data-ready="1"]');
  await page.waitForTimeout(800);
  await page.screenshot({ path: path.join(out, '10_play_logged_in.png') });
  await page.click('#launch-btn');
  await page.waitForTimeout(1500);
  await page.screenshot({ path: path.join(out, '11_launch_progress.png') });
  await page.waitForFunction(() => document.querySelector('#status-pill').textContent === 'Spiel läuft', null, { timeout: 300000 });
  await page.click('.nav[data-page=console]');
  await page.waitForFunction(() => /Nova Client .* loaded with/.test(document.querySelector('#log').textContent), null, { timeout: 120000 });
  await page.waitForFunction(() => /Created: .*atlas/.test(document.querySelector('#log').textContent), null, { timeout: 120000 });
  await page.screenshot({ path: path.join(out, '12_console_running.png') });
  await page.click('#game-kill');
  await page.waitForFunction(() => document.querySelector('#status-pill').textContent === 'Bereit', null, { timeout: 30000 });
  await app.close();
  console.log('UI LAUNCH TEST PASSED');
})().catch((e) => {
  console.error('UI LAUNCH TEST FAILED', e.message || e);
  process.exit(1);
});
