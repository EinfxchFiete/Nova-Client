'use strict';
// Sets the icon and version info of the Windows exe without Wine (pure JS via resedit).
const fs = require('fs');
const path = require('path');
const ResEdit = require('resedit');

const exe = process.argv[2];
const ico = path.join(__dirname, '..', 'resources', 'icon.ico');
const data = fs.readFileSync(exe);
const pe = ResEdit.NtExecutable.from(data);
const res = ResEdit.NtExecutableResource.from(pe);
const iconFile = ResEdit.Data.IconFile.from(fs.readFileSync(ico));
ResEdit.Resource.IconGroupEntry.replaceIconsForResource(res.entries, 1, 1033, iconFile.icons.map((i) => i.data));
const vi = ResEdit.Resource.VersionInfo.fromEntries(res.entries)[0];
if (vi) {
  vi.setStringValues({ lang: 1033, codepage: 1200 }, {
    ProductName: 'Nova Launcher', FileDescription: 'Nova Launcher', CompanyName: 'Nova', OriginalFilename: 'Nova Launcher.exe',
  });
  vi.setFileVersion(1, 0, 0, 0, 1033);
  vi.setProductVersion(1, 0, 0, 0, 1033);
  vi.outputToResourceEntries(res.entries);
}
res.outputResource(pe);
fs.writeFileSync(exe, Buffer.from(pe.generate()));
console.log('icon + version set:', exe);
