'use strict';
const { contextBridge, ipcRenderer } = require('electron');

const invoke = async (channel, ...args) => {
  const r = await ipcRenderer.invoke(channel, ...args);
  if (!r.ok) throw new Error(r.error);
  return r.value;
};

contextBridge.exposeInMainWorld('nova', {
  invoke,
  on: (channel, fn) => {
    const allowed = ['auth:status', 'game:progress', 'game:log', 'game:state'];
    if (!allowed.includes(channel)) return;
    ipcRenderer.on(channel, (_e, payload) => fn(payload));
  },
});
