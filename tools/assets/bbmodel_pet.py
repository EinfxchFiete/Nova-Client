"""Converts a Blockbench model (.bbmodel, free format, per-face UVs) into a Nova pet: a box-UV texture
(textures/pets/<name>.png) and a part list (models/pets/<name>.json) that dev.novaclient.cosmetics.PetModels builds
into ModelParts at runtime.
Usage: python3 -I tools/assets/bbmodel_pet.py <model.bbmodel> <name> src/main/resources/assets/novaclient
Only use models whose license allows it (the blobfish by QueenOfAnts: "free to use/edit ... on your projects")."""
import base64, json, math, os, struct, sys, zlib

def read_png(data):
    i = 8; idat = b''
    while i < len(data):
        l = struct.unpack('>I', data[i:i + 4])[0]; t = data[i + 4:i + 8]; c = data[i + 8:i + 8 + l]
        if t == b'IHDR':
            w, h, bd, ct = struct.unpack('>IIBB', c[:10])
        if t == b'IDAT':
            idat += c
        i += 12 + l
    if bd != 8 or ct not in (2, 6):
        raise SystemExit('only 8-bit RGB/RGBA textures are supported')
    bpp = 4 if ct == 6 else 3
    raw = zlib.decompress(idat); stride = w * bpp; rows = []; prev = bytearray(stride); pos = 0
    for _ in range(h):
        f = raw[pos]; pos += 1; line = bytearray(raw[pos:pos + stride]); pos += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0; b = prev[x]; c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + ((a + b) >> 1)) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[x * bpp:x * bpp + bpp]) + ((255,) if bpp == 3 else ()) for x in range(w)])
        prev = line
    return w, h, rows

def write_png(path, w, h, rows):
    raw = b''.join(b'\x00' + bytes(sum((list(p) for p in r), [])) for r in rows)
    def ch(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + ch(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                           + ch(b'IDAT', zlib.compress(raw, 9)) + ch(b'IEND', b''))

def main():
    src, name, out = sys.argv[1], sys.argv[2], sys.argv[3]
    d = json.load(open(src, encoding='utf-8'))
    tex = d['textures'][0]['source'].split(',', 1)[1]
    tw, th, pixels = read_png(base64.b64decode(tex))
    res_w = d.get('resolution', {}).get('width', tw)
    res_h = d.get('resolution', {}).get('height', th)
    sx, sy = tw / res_w, th / res_h
    elements = {e['uuid']: e for e in d['elements']}

    # Minecraft model space: y points down and x is mirrored compared to Blockbench
    def j(p):
        return [-p[0], -p[1], p[2]]

    out_w, out_h = 64, 64
    out_px = [[(0, 0, 0, 0)] * out_w for _ in range(out_h)]
    cursor = [0, 0, 0]  # x, y, row height

    def place(cw, ch_):
        if cursor[0] + cw > out_w:
            cursor[0] = 0; cursor[1] += cursor[2]; cursor[2] = 0
        u, v = cursor[0], cursor[1]
        cursor[0] += cw; cursor[2] = max(cursor[2], ch_)
        if cursor[1] + cursor[2] > out_h:
            raise SystemExit('texture too small')
        return u, v

    def copy(face, dx, dy, dw, dh):
        if face is None or dw <= 0.001 or dh <= 0.001:
            return
        u1, v1, u2, v2 = face['uv']
        for yy in range(max(1, math.ceil(dh - 1e-6))):
            for xx in range(max(1, math.ceil(dw - 1e-6))):
                su = u1 + (xx + 0.5) / max(dw, 1e-6) * (u2 - u1)
                sv = v1 + (yy + 0.5) / max(dh, 1e-6) * (v2 - v1)
                px_ = min(tw - 1, max(0, int(su * sx))); py_ = min(th - 1, max(0, int(sv * sy)))
                if 0 <= dy + yy < out_h and 0 <= dx + xx < out_w:
                    out_px[dy + yy][dx + xx] = pixels[py_][px_]

    parts = []

    def walk(group, parent_pivot, parent_name):
        pivot = group.get('origin', [0, 0, 0])
        jp, jpp = j(pivot), j(parent_pivot)
        part = {'name': group['name'], 'parent': parent_name,
                'offset': [jp[i] - jpp[i] for i in range(3)],
                'rotation': [math.radians(-r) if i < 2 else math.radians(r) for i, r in enumerate(group.get('rotation', [0, 0, 0]))],
                'cubes': []}
        parts.append(part)
        for c in group.get('children', []):
            if isinstance(c, dict):
                walk(c, pivot, group['name'])
                continue
            e = elements.get(c)
            if e is None or e.get('type', 'cube') != 'cube' or e['name'] == 'hitbox':
                continue
            f, t = e['from'], e['to']
            w, h, dd = abs(t[0] - f[0]), abs(t[1] - f[1]), abs(t[2] - f[2])
            cw, chh = math.ceil(2 * dd + 2 * w), math.ceil(dd + h)
            u, v = place(cw, chh)
            faces = e.get('faces', {})
            # same layout as the vanilla box UV (and tools/assets/cosmetic_assets.box)
            copy(faces.get('up'), u + math.ceil(dd), v, w, dd)
            copy(faces.get('down'), u + math.ceil(dd + w), v, w, dd)
            copy(faces.get('east'), u, v + math.ceil(dd), dd, h)
            copy(faces.get('north'), u + math.ceil(dd), v + math.ceil(dd), w, h)
            copy(faces.get('west'), u + math.ceil(dd + w), v + math.ceil(dd), dd, h)
            copy(faces.get('south'), u + math.ceil(2 * dd + w), v + math.ceil(dd), w, h)
            jmin = [-max(f[0], t[0]) - jp[0], -max(f[1], t[1]) - jp[1], min(f[2], t[2]) - jp[2]]
            part['cubes'].append({'uv': [u, v], 'from': jmin, 'size': [w, h, dd], 'inflate': e.get('inflate', 0)})

    for g in d['outliner']:
        if isinstance(g, dict) and g['name'] != 'hitbox':
            walk(g, [0, 0, 0], None)
    write_png(f'{out}/textures/pets/{name}.png', out_w, out_h, out_px)
    os.makedirs(f'{out}/models/pets', exist_ok=True)
    json.dump({'texture_size': [out_w, out_h], 'parts': parts}, open(f'{out}/models/pets/{name}.json', 'w'), indent=1)
    print('ok', name, len(parts), 'parts', sum(len(p['cubes']) for p in parts), 'cubes')

if __name__ == '__main__':
    main()
