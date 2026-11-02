'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const UA = 'NovaLauncher/1.0 (+https://github.com/EinfxchFiete/Nova-Client)';

async function fetchRetry(url, options = {}, tries = 4) {
  let lastErr;
  for (let i = 0; i < tries; i++) {
    try {
      const res = await fetch(url, { ...options, headers: { 'User-Agent': UA, ...(options.headers || {}) } });
      if (res.status >= 500 || res.status === 429) throw new Error(`HTTP ${res.status} für ${url}`);
      return res;
    } catch (e) {
      lastErr = e;
      await new Promise((r) => setTimeout(r, 500 * 2 ** i));
    }
  }
  throw lastErr;
}

async function getJson(url) {
  const res = await fetchRetry(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} für ${url}`);
  return res.json();
}

function sha1File(file) {
  const h = crypto.createHash('sha1');
  h.update(fs.readFileSync(file));
  return h.digest('hex');
}

/** True if the file exists and matches the expected sha1/size (when given). */
function isValid(file, sha1, size) {
  try {
    const st = fs.statSync(file);
    if (size != null && st.size !== size) return false;
    if (sha1) return sha1File(file) === sha1.toLowerCase();
    return st.size > 0;
  } catch {
    return false;
  }
}

/** Downloads url to dest (atomic), verifying sha1 if given. Skips valid existing files. */
async function download(url, dest, sha1, size) {
  if (isValid(dest, sha1, size)) return false;
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  for (let attempt = 0; attempt < 3; attempt++) {
    const res = await fetchRetry(url);
    if (!res.ok) throw new Error(`Download fehlgeschlagen (HTTP ${res.status}): ${url}`);
    const buf = Buffer.from(await res.arrayBuffer());
    if (sha1 && crypto.createHash('sha1').update(buf).digest('hex') !== sha1.toLowerCase()) {
      if (attempt === 2) throw new Error(`Prüfsumme falsch: ${url}`);
      continue;
    }
    const tmp = `${dest}.part`;
    fs.writeFileSync(tmp, buf);
    fs.renameSync(tmp, dest);
    return true;
  }
  return false;
}

/** Runs async tasks with limited concurrency, reporting progress. */
async function pool(tasks, concurrency, onProgress) {
  let done = 0;
  let index = 0;
  const total = tasks.length;
  async function worker() {
    while (index < total) {
      const t = tasks[index++];
      await t();
      done++;
      if (onProgress) onProgress(done, total);
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, total) }, worker));
}

module.exports = { fetchRetry, getJson, download, pool, isValid, UA };
