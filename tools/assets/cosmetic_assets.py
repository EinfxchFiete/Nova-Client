"""Generates all Nova cosmetic textures (own designs, no Mojang assets)."""
import zlib, struct, math, random, colorsys, os, sys

OUT = sys.argv[1]  # .../assets/novaclient/textures

def save(path, w, h, px):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b''.join(b'\x00' + bytes(sum((list(px[y][x]) for x in range(w)), [])) for y in range(h))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def canvas(w, h, c=(0, 0, 0, 0)): return [[c] * w for _ in range(h)]
def clamp(v): return max(0, min(255, int(v)))
def rgba(c, a=255): return (clamp(c[0]), clamp(c[1]), clamp(c[2]), a)
def lerp(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def fill(px, x, y, w, h, fn):
    """fn(lx, ly, w, h) -> rgba"""
    for j in range(int(h)):
        for i in range(int(w)):
            if 0 <= y + j < len(px) and 0 <= x + i < len(px[0]):
                px[y + j][x + i] = fn(i, j, w, h)

def box(px, u, v, w, h, d, fn):
    """Paint all faces of a Minecraft model box (texOffs u,v, size w,h,d) with fn(face, lx, ly, fw, fh)."""
    faces = {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h),
             'north': (u + d, v + d, w, h), 'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}
    for name, (fx, fy, fw, fh) in faces.items():
        fill(px, fx, fy, fw, fh, lambda i, j, ww, hh, n=name: fn(n, i, j, ww, hh))

# ------------------------------------------------------------------ badges (9x9, GUI atlas sprites)
def badges():
    crown = canvas(9, 9)
    gold, dark, gem = (255, 205, 60), (190, 130, 20), (230, 40, 70)
    rows = ["         ",
            "         ",
            "X   X   X",
            "XX XXX XX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "         "]
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == 'X':
                crown[y][x] = rgba(lerp(gold, dark, y / 8))
    for x in (2, 4, 6):
        crown[5][x] = rgba(gem)
    for x in range(9):
        crown[7][x] = rgba((150, 95, 10))
    save(f'{OUT}/gui/sprites/badge/owner.png', 9, 9, crown)

    star = canvas(9, 9)
    for y in range(9):
        for x in range(9):
            dx, dy = abs(x - 4), abs(y - 4)
            if math.sqrt(dx) + math.sqrt(dy) <= 2.05:
                t = (dx + dy) / 6
                star[y][x] = rgba(lerp((240, 225, 255), (140, 90, 245), t))
    save(f'{OUT}/gui/sprites/badge/nova.png', 9, 9, star)

# ------------------------------------------------------------------ head cosmetics
def bandana():
    px = canvas(64, 32)
    rnd = random.Random(7)
    def pattern(face, i, j, w, h):
        base = 235
        # paisley-like dots and a border line
        if (i * 7 + j * 3) % 9 == 0 or (j == 0 or j == h - 1) and face in ('north', 'south', 'east', 'west'):
            return rgba((120, 120, 120))
        return rgba((base, base, base))
    box(px, 0, 0, 8, 2, 8, pattern)          # band
    box(px, 0, 12, 3, 2, 2, pattern)         # knot
    box(px, 0, 18, 2, 5, 1, pattern)         # tail
    save(f'{OUT}/cosmetics/bandana.png', 64, 32, px)

def crown():
    px = canvas(64, 32)
    def band(face, i, j, w, h):
        t = j / max(1, h - 1)
        c = lerp((255, 220, 90), (200, 140, 30), t)
        if face == 'north' and j == h // 2 and i % 3 == 1:
            return rgba((220, 30, 60) if i % 2 else (40, 120, 230))
        if face in ('east', 'west', 'south') and j == h // 2 and i % 3 == 1:
            return rgba((40, 200, 120))
        return rgba(c)
    box(px, 0, 0, 9, 2, 9, band)             # ring (sides only)
    def spike(face, i, j, w, h):
        return rgba(lerp((255, 240, 150), (230, 170, 40), j / max(1, h - 1)))
    box(px, 0, 14, 1, 2, 1, spike)
    def ball(face, i, j, w, h):
        return rgba((255, 250, 220))
    box(px, 8, 14, 1, 1, 1, ball)
    save(f'{OUT}/cosmetics/crown.png', 64, 32, px)

def tophat():
    px = canvas(64, 64)
    def felt(face, i, j, w, h):
        n = ((i * 13 + j * 7) % 5) * 3
        return rgba((28 + n, 26 + n, 34 + n))
    box(px, 0, 0, 11, 1, 11, felt)            # brim
    box(px, 0, 16, 7, 7, 7, felt)             # crown
    def band(face, i, j, w, h):
        return rgba((245, 245, 245))          # tinted with the chosen colour
    box(px, 0, 40, 7, 2, 7, band)
    save(f'{OUT}/cosmetics/tophat.png', 64, 64, px)

def horns():
    px = canvas(32, 32)
    def base(face, i, j, w, h):
        return rgba(lerp((200, 30, 30), (130, 10, 10), j / max(1, h - 1)))
    def tip(face, i, j, w, h):
        return rgba(lerp((120, 10, 10), (40, 0, 0), j / max(1, h - 1)))
    box(px, 0, 0, 2, 3, 2, base)
    box(px, 0, 8, 1, 3, 1, tip)
    save(f'{OUT}/cosmetics/horns.png', 32, 32, px)

def catears():
    px = canvas(32, 32)
    def ear(face, i, j, w, h):
        if face == 'north' and 0 < i < w - 1 and j > 0:
            return rgba((255, 170, 190))
        return rgba((245, 245, 245))          # tinted with the chosen colour
    box(px, 0, 0, 3, 3, 1, ear)
    save(f'{OUT}/cosmetics/catears.png', 32, 32, px)

def halo():
    px = canvas(16, 16, rgba((255, 255, 255)))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                px[y][x] = rgba((255, 250, 210))
    save(f'{OUT}/cosmetics/halo.png', 16, 16, px)

# ------------------------------------------------------------------ wings: a 16x16 plane per side (front at 0,0 / back at 16,0)
def wing_texture(name, shade):
    px = canvas(64, 32)
    for face_x in (0, 16):
        for y in range(16):
            for x in range(16):
                c = shade(x, y)
                if c is not None:
                    # the back face is mirrored horizontally
                    tx = face_x + (x if face_x == 0 else 15 - x)
                    px[y][tx] = c
    save(f'{OUT}/cosmetics/wings_{name}.png', 64, 32, px)

def angel(x, y):
    # x: 0 = at the back, 15 = wing tip ; y: 0 = top
    top = 2 + x * 0.25
    bottom = 9 + (15 - x) * 0.45 + (2 if x % 4 < 2 else 0)
    if y < top or y > bottom:
        return None
    feather = (x % 4 == 0) or (y > bottom - 2)
    t = (y - top) / max(1, bottom - top)
    c = lerp((255, 255, 255), (215, 220, 240), t)
    if feather:
        c = lerp(c, (180, 185, 210), 0.6)
    return rgba(c)

def dragon(x, y):
    top = 1 + x * 0.15
    edge = 15 - x * 0.35 - 3 * abs(math.sin(x * 0.75))
    if y < top or y > edge:
        return None
    if x % 5 == 0 or y <= top + 0.8:
        return rgba((60, 25, 80))             # bones
    t = y / 16
    return rgba(lerp((150, 60, 200), (80, 20, 120), t), 235)

def butterfly(x, y):
    cx, cy = 7.5, 7.5
    upper = ((x - 8) / 8) ** 2 + ((y - 5) / 5.2) ** 2 <= 1
    lower = ((x - 5) / 5.5) ** 2 + ((y - 11.5) / 4) ** 2 <= 1
    if not (upper or lower):
        return None
    d = math.hypot((x - 8) / 8, (y - 5) / 5.2) if upper else math.hypot((x - 5) / 5.5, (y - 11.5) / 4)
    if d > 0.82:
        return rgba((25, 20, 30)) if (x + y) % 3 else rgba((250, 250, 250))
    if (x + 2 * y) % 7 == 0:
        return rgba((25, 20, 30))
    return rgba(lerp((255, 160, 30), (230, 80, 20), d))

# ------------------------------------------------------------------ animated capes (64x32, 16 frames)
def cape_frames(name, fn, frames=16):
    for f in range(frames):
        t = f / frames
        px = canvas(64, 32, rgba((0, 0, 0)))
        rnd = random.Random(f * 7919 + hash(name) % 1000)
        for y in range(32):
            for x in range(64):
                u, v = (x % 22) / 21.0, min(y, 16) / 16.0
                px[y][x] = rgba(fn(u, v, x, y, t, rnd))
        save(f'{OUT}/cape/anim/{name}_{f}.png', 64, 32, px)

def galaxy_anim(u, v, x, y, t, rnd):
    c = lerp((8, 6, 30), (60, 25, 110), (math.sin(u * 6 + v * 4 + t * 6.283) + 1) / 2 * 0.8)
    s = (x * 37 + y * 91) % 101
    if s < 4:
        tw = (math.sin(t * 6.283 * 2 + s * 1.7) + 1) / 2
        c = lerp(c, (255, 255, 255), tw)
    return c

def fire_anim(u, v, x, y, t, rnd):
    f = v + 0.18 * math.sin(u * 14 + t * 6.283 * 2) + 0.1 * math.sin(v * 20 - t * 6.283 * 3)
    f = max(0.0, min(1.0, f))
    c = lerp((255, 230, 90), (190, 25, 10), f)
    if ((x * 13 + int(y + t * 32)) % 17) == 0:
        c = lerp(c, (255, 255, 200), 0.6)
    return c

def rainbow_anim(u, v, x, y, t, rnd):
    r, g, b = colorsys.hsv_to_rgb((v * 0.8 + t) % 1, 0.75, 1)
    return (r * 255, g * 255, b * 255)

def ocean_anim(u, v, x, y, t, rnd):
    w = (math.sin(u * 10 + v * 14 - t * 6.283) + 1) / 2
    c = lerp((10, 55, 140), (40, 170, 220), w * 0.8 + 0.2 * v)
    if w > 0.93:
        c = lerp(c, (230, 250, 255), 0.7)
    return c

def matrix_anim(u, v, x, y, t, rnd):
    col = x % 22
    speed = 1 + (col * 7) % 3
    head = (t * 17 * speed + col * 5) % 20
    d = (head - y) % 20
    if d < 1:
        return (200, 255, 200)
    if d < 8 and (x + y) % 2 == 0:
        return (0, 230 - d * 25, 40)
    return (5, 18, 8)

def nova_pulse(u, v, x, y, t, rnd):
    p = (math.sin(t * 6.283) + 1) / 2
    c = lerp((35, 15, 80), (140, 80, 250), v * 0.6 + p * 0.4)
    dx, dy = abs(x - 16), abs(y - 8)
    if 12 <= x <= 20 and 4 <= y <= 12 and math.sqrt(dx) + math.sqrt(dy) <= 2 + p:
        c = lerp((255, 255, 255), (200, 170, 255), (dx + dy) / 6)
    return c

badges(); bandana(); crown(); tophat(); horns(); catears(); halo()
wing_texture('angel', angel); wing_texture('dragon', dragon); wing_texture('butterfly', butterfly)
for n, fn in (('galaxy', galaxy_anim), ('fire', fire_anim), ('rainbow', rainbow_anim), ('ocean', ocean_anim), ('matrix', matrix_anim), ('nova', nova_pulse)):
    cape_frames(n, fn)
print('ok')
