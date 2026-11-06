"""Converts a Blockbench model (.bbmodel, free/generic format, per-face UVs) into a Nova pet:
  textures/pets/<name>.png   the texture (atlas of the used textures; animated textures keep their frames stacked)
  models/pets/<name>.json    bones (groups), quads (already placed in model space, with UVs) and animations
dev.novaclient.cosmetics.PetModels loads the JSON, PetRenderer poses the bones and draws the quads.

Usage: python3 -I tools/assets/bbmodel_pet.py <model.bbmodel> <name> src/main/resources/assets/novaclient [scale]
Only use models whose authors allow it (see README "Credits").

Conventions (from Blockbench's own source): model space in pixels, y up, front = north (-z); Euler order ZYX;
animation values are added to the group rotation / position. Projects older than format 5.0 stored x/y rotation
and x position of keyframes inverted - they are converted like Blockbench does when it opens them.
Face corners follow Minecraft's block model layout (FaceInfo), counter-clockwise seen from outside."""
import base64, json, math, os, struct, sys, zlib

ANIMS = ('idle', 'walk', 'pet', 'fly')

def read_png(data):
    i = 8; idat = b''; w = h = bd = ct = None
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
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + ch(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                + ch(b'IDAT', zlib.compress(raw, 9)) + ch(b'IEND', b''))

def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.0  # molang expressions are not supported (none in the converted models)

def rot_zyx(v, r):
    """Rotates v by Euler angles r (degrees), order ZYX like Blockbench: R = Rz * Ry * Rx."""
    x, y, z = v
    a = math.radians(r[0]); c, s = math.cos(a), math.sin(a); y, z = y * c - z * s, y * s + z * c
    a = math.radians(r[1]); c, s = math.cos(a), math.sin(a); x, z = x * c + z * s, -x * s + z * c
    a = math.radians(r[2]); c, s = math.cos(a), math.sin(a); x, y = x * c - y * s, x * s + y * c
    return [x, y, z]

def face_corners(f, t):
    """Corners per face in Minecraft's FaceInfo order (u0v0, u0v1, u1v1, u1v0)."""
    x0, y0, z0 = f; x1, y1, z1 = t
    return {
        'down': [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
        'up': [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)],
        'north': [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
        'south': [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
        'west': [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
        'east': [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)],
    }

def area(p):
    a = [p[1][i] - p[0][i] for i in range(3)]; b = [p[3][i] - p[0][i] for i in range(3)]
    c = [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    return math.sqrt(sum(x * x for x in c))

def main():
    src, name, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with open(src, encoding='utf-8') as fh:
        d = json.load(fh)
    version = tuple(int(x) for x in str(d.get('meta', {}).get('format_version', '5.0')).split('.')[:2])
    legacy = version < (5, 0)
    res = d.get('resolution', {})
    elements = {e['uuid']: e for e in d['elements']}
    groups = {g['uuid']: g for g in d.get('groups', [])}

    # ---------------------------------------------------------------- textures
    textures = d['textures']
    used = set()
    for e in d['elements']:
        for fc in e.get('faces', {}).values():
            if fc.get('texture') is not None:
                used.add(fc['texture'])
    used = sorted(used)
    tex = {}
    for i in used:
        t = textures[i]
        tw, th, px = read_png(base64.b64decode(t['source'].split(',', 1)[1]))
        uw = t.get('uv_width') or res.get('width', tw)
        uh = t.get('uv_height') or res.get('height', th)
        scale = tw / uw
        frame_h = round(uh * scale)
        frames = max(1, th // frame_h) if frame_h > 0 else 1
        tex[i] = {'w': tw, 'h': th, 'px': px, 'scale_u': tw / uw, 'scale_v': scale, 'frame_h': min(frame_h, th),
                  'frames': frames, 'frame_time': t.get('frame_time', 1) or 1, 'frame_order': t.get('frame_order', '')}
    animated = [i for i in used if tex[i]['frames'] > 1]
    if animated and len(used) > 1:
        raise SystemExit('animated texture together with other textures is not supported')
    frames = tex[used[0]]['frames'] if animated else 1
    atlas_w = max(tex[i]['w'] for i in used)
    offset = {}; y = 0
    for i in used:
        offset[i] = y
        y += tex[i]['frame_h'] if not animated else tex[i]['h']
    frame_h = tex[used[0]]['frame_h'] if animated else y
    atlas = [[(0, 0, 0, 0)] * atlas_w for _ in range(frame_h * frames)]
    for i in used:
        for yy in range(tex[i]['frame_h'] if not animated else tex[i]['h']):
            for xx in range(tex[i]['w']):
                atlas[offset[i] + yy][xx] = tex[i]['px'][yy][xx]
    translucent = sum(1 for r in atlas for p in r if 0 < p[3] < 250) > 8

    # ---------------------------------------------------------------- bones and quads
    bones = []; quads = []; index = {}

    def group_data(node):
        g = dict(groups.get(node.get('uuid'), {}))
        g.update({k: v for k, v in node.items() if k != 'children'})
        return g

    def walk(node, parent):
        g = group_data(node)
        if g.get('name') == 'hitbox' or g.get('visibility') is False or g.get('export') is False:
            return
        bi = len(bones)
        bones.append({'name': g['name'], 'parent': parent, 'origin': [round(v, 5) for v in g.get('origin', [0, 0, 0])],
                      'rotation': [round(v, 5) for v in (g.get('rotation') or [0, 0, 0])]})
        index.setdefault(g['name'], bi)
        for c in node.get('children', []):
            if isinstance(c, dict):
                walk(c, bi)
                continue
            e = elements.get(c)
            if e is None or e.get('type', 'cube') != 'cube' or e.get('name') == 'hitbox' or e.get('visibility') is False \
                    or e.get('export') is False:
                continue
            f, t = list(e['from']), list(e['to'])
            inf = e.get('inflate') or 0
            for k in range(3):
                if f[k] <= t[k]:
                    f[k] -= inf; t[k] += inf
                else:
                    f[k] += inf; t[k] -= inf
            origin = e.get('origin', [0, 0, 0]); rot = e.get('rotation') or [0, 0, 0]
            corners = face_corners(f, t)
            for side, fc in e.get('faces', {}).items():
                ti = fc.get('texture')
                if ti is None or ti not in tex or fc.get('enabled') is False:
                    continue
                pts = corners[side]
                if area(pts) < 1e-6:
                    continue
                if any(rot):
                    pts = [[origin[k] + v for k, v in enumerate(rot_zyx([p[k] - origin[k] for k in range(3)], rot))] for p in pts]
                T = tex[ti]
                u1, v1, u2, v2 = fc['uv']
                def U(u): return (u * T['scale_u']) / atlas_w
                def V(v): return (offset[ti] if not animated else 0) / frame_h + (v * T['scale_v']) / frame_h
                uvc = [(U(u1), V(v1)), (U(u1), V(v2)), (U(u2), V(v2)), (U(u2), V(v1))]
                r = (int(fc.get('rotation', 0) or 0) // 90) % 4
                q = [bi]
                for k in range(4):
                    uu, vv = uvc[(k + r) % 4]
                    q += [round(pts[k][0], 4), round(pts[k][1], 4), round(pts[k][2], 4), round(uu, 6), round(vv, 6)]
                quads.append(q)

    for n in d['outliner']:
        if isinstance(n, dict):
            walk(n, -1)
    if not quads:
        raise SystemExit('no visible cubes')
    xs = [q[1 + 5 * k] for q in quads for k in range(4)]
    ys = [q[2 + 5 * k] for q in quads for k in range(4)]
    zs = [q[3 + 5 * k] for q in quads for k in range(4)]
    bounds = [round(min(xs), 3), round(min(ys), 3), round(min(zs), 3), round(max(xs), 3), round(max(ys), 3), round(max(zs), 3)]

    # ---------------------------------------------------------------- animations
    animations = {}
    for a in d.get('animations', []):
        if a['name'] not in ANIMS:
            continue
        out_bones = {}
        for uuid, an in a.get('animators', {}).items():
            bname = an.get('name') or groups.get(uuid, {}).get('name')
            if an.get('type', 'bone') != 'bone' or bname not in index:
                continue
            chans = {}
            for k in an.get('keyframes', []):
                ch = k['channel']
                if ch not in ('rotation', 'position', 'scale'):
                    continue
                dps = k['data_points']
                vals = []
                for dp in (dps[0], dps[-1]):
                    v = [num(dp.get('x', 0)), num(dp.get('y', 0)), num(dp.get('z', 0))]
                    if legacy and ch in ('rotation', 'position'):
                        v[0] = -v[0]
                    if legacy and ch == 'rotation':
                        v[1] = -v[1]
                    vals += [round(x, 4) for x in v]
                interp = {'catmullrom': 1, 'step': 2}.get(k.get('interpolation'), 0)
                chans.setdefault(ch, []).append([round(k['time'], 5)] + vals + [interp])
            for ch in chans:
                chans[ch].sort(key=lambda r: r[0])
            # drop channels that never change anything
            neutral = {'rotation': 0.0, 'position': 0.0, 'scale': 1.0}
            chans = {ch: ks for ch, ks in chans.items() if any(abs(x - neutral[ch]) > 1e-4 for r in ks for x in r[1:7])}
            if chans:
                out_bones[str(index[bname])] = chans
        animations[a['name']] = {'length': a.get('length', 1) or 1, 'loop': a.get('loop', 'loop'), 'bones': out_bones}

    T0 = tex[used[0]]
    order = [int(x) for x in str(T0['frame_order']).replace(',', ' ').split()] if animated and T0['frame_order'] else None
    model = {'version': 2, 'texture': {'width': atlas_w, 'height': frame_h, 'frames': frames,
                                       'frame_time': T0['frame_time'] if animated else 1, 'frame_order': order},
             'translucent': translucent, 'scale': float(sys.argv[4]) if len(sys.argv) > 4 else 1.0,
             'bounds': bounds, 'bones': bones, 'quads': quads, 'animations': animations}
    write_png(f'{out}/textures/pets/{name}.png', atlas_w, frame_h * frames, atlas)
    os.makedirs(f'{out}/models/pets', exist_ok=True)
    with open(f'{out}/models/pets/{name}.json', 'w') as fh:
        json.dump(model, fh, separators=(',', ':'))
    print('ok', name, len(bones), 'bones', len(quads), 'quads', 'frames', frames, 'anims', sorted(animations),
          'height', round(bounds[4] - bounds[1], 2), 'translucent' if translucent else '')

if __name__ == '__main__':
    main()
