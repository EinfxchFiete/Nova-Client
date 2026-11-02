'use strict';
// Developer UI test: drives the Electron app with Playwright and saves screenshots.
const { _electron: electron } = require('playwright');
const path = require('path');
const fs = require('fs');

const out = path.join(__dirname, 'output', 'ui');
fs.mkdirSync(out, { recursive: true });
const home = path.join(__dirname, 'output', 'ui-home');
fs.rmSync(home, { recursive: true, force: true });

function check(cond, msg) {
  if (!cond) throw new Error('UI TEST FAILED: ' + msg);
}

(async () => {
  const app = await electron.launch({
    executablePath: require('electron'),
    args: ['--no-sandbox', path.join(__dirname, '..')],
    env: { ...process.env, NOVA_LAUNCHER_HOME: home },
  });
  const page = await app.firstWindow();
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await page.waitForSelector('body[data-ready="1"]', { timeout: 30000 });
  await page.setViewportSize({ width: 1280, height: 790 }).catch(() => {});
  await page.waitForTimeout(1200);
  await page.screenshot({ path: path.join(out, '01_play.png') });

  // profile popover
  await page.click('#profile-pick');
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(out, '02_profile_popover.png') });
  await page.click('.stage', { position: { x: 30, y: 30 } });

  // launch without account -> login dialog with a clear error (no client id yet)
  await page.click('#launch-btn');
  await page.waitForSelector('#login-modal:not(.hidden) .error-box', { timeout: 15000 });
  const err1 = await page.textContent('#login-body .error-box');
  check(/Client-ID/.test(err1), 'missing client id message: ' + err1);
  await page.screenshot({ path: path.join(out, '03_login_no_client.png') });
  await page.click('#login-cancel');

  // invalid client id -> Microsoft rejects it with an understandable message
  await page.click('.nav[data-page=settings]');
  await page.fill('#client-id', '00000000-0000-0000-0000-000000000000');
  await page.press('#client-id', 'Tab');
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(out, '04_settings.png') });
  await page.click('.nav[data-page=play]');
  await page.click('#account-btn');
  await page.waitForTimeout(200);
  await page.screenshot({ path: path.join(out, '05_account_menu.png') });
  await page.click('.acc-add');
  await page.waitForSelector('#login-modal:not(.hidden) .error-box', { timeout: 30000 });
  const err2 = await page.textContent('#login-body .error-box');
  console.log('invalid client id ->', err2);
  check(err2.length > 10, 'no error for invalid client id');
  await page.screenshot({ path: path.join(out, '06_login_invalid_client.png') });
  await page.click('#login-cancel');

  // profiles: create, rename, ram, select
  await page.click('.nav[data-page=profiles]');
  await page.click('.profile-card.new');
  await page.waitForTimeout(300);
  await page.fill('#profile-editor input >> nth=0', 'PvP Turnier');
  await page.press('#profile-editor input >> nth=0', 'Tab');
  await page.waitForTimeout(300);
  const state = await page.evaluate(() => window.nova.invoke('state'));
  check(state.profiles.length === 2, 'profile not created');
  check(state.profiles.some((p) => p.name === 'PvP Turnier'), 'profile not renamed');
  check(state.activeProfile === state.profiles.find((p) => p.name === 'PvP Turnier').id, 'new profile should be active');
  await page.screenshot({ path: path.join(out, '07_profiles.png') });

  // skins: add two skins through the API (file dialog cannot be automated), check preview + variant switch
  const skinB64 = fs.readFileSync(path.join(__dirname, '..', 'resources', 'default-skin.png')).toString('base64');
  await page.evaluate((b) => window.nova.invoke('skins:addBuffer', b, 'Nova Hoodie'), skinB64);
  const wrong = await page.evaluate(() => window.nova.invoke('skins:addBuffer', 'aGVsbG8=', 'kaputt').then(() => 'ok', (e) => e.message));
  check(/PNG/.test(wrong), 'invalid skin should be rejected: ' + wrong);
  await page.click('.nav[data-page=skins]');
  await page.waitForTimeout(800);
  await page.click('.skin-card[data-skin]');
  await page.waitForTimeout(500);
  await page.click('#variant-seg button[data-variant=slim]');
  await page.waitForTimeout(800);
  const st2 = await page.evaluate(() => window.nova.invoke('state'));
  check(st2.skins[0].variant === 'slim', 'variant not saved');
  const applyDisabled = await page.isDisabled('#apply-skin');
  check(!applyDisabled, 'apply button should be enabled for a library skin');
  await page.screenshot({ path: path.join(out, '08_skins.png') });

  // apply without account -> clear error toast
  await page.click('#apply-skin');
  await page.waitForSelector('#toast.err:not(.hidden)', { timeout: 5000 });
  check(/Microsoft-Konto an/.test(await page.textContent('#toast')), 'apply without login should ask to log in');

  await page.click('.nav[data-page=console]');
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(out, '09_console.png') });

  // persisted config
  const cfg = JSON.parse(fs.readFileSync(path.join(home, 'launcher.json'), 'utf8'));
  check(cfg.settings.clientId === '00000000-0000-0000-0000-000000000000', 'client id not saved');
  check(!JSON.stringify(cfg).includes('accessToken'), 'no tokens expected in config');

  check(errors.length === 0, 'renderer errors: ' + errors.join(' | '));
  await app.close();
  console.log('UI TEST PASSED');
})().catch(async (e) => {
  console.error(e.message || e);
  process.exit(1);
});
