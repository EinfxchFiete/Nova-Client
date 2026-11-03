"""Nova cosmetics, second batch (1.5.0): tintable textures, new crown, more animated capes.
Usage: python3 tools/assets/cosmetic_assets_v2.py src/main/resources/assets/novaclient/textures
Own designs, no Mojang assets. Reuses the helpers of cosmetic_assets.py."""
import math, colorsys, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cosmetic_assets as ca
from cosmetic_assets import canvas, rgba, lerp, box, save

OUT = sys.argv[1]
ca.OUT = OUT

def gray(c, lo=95):
    """Luminance of an rgba pixel mapped to a light grey, so multiplying with a tint keeps the shading."""
    l = (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255
    v = lo + (255 - lo) * l
    return rgba((v, v, v), c[3])

# ------------------------------------------------------------------ tintable wings (same shape, grey)
def wing_tint(name, shade, lo=95):
    def g(x, y):
        c = shade(x, y)
        return None if c is None else gray(c, lo)
    px = canvas(64, 32)
    for face_x in (0, 16):
        for y in range(16):
            for x in range(16):
                c = g(x, y)
                if c is not None:
                    px[y][face_x + (x if face_x == 0 else 15 - x)] = c
    save(f'{OUT}/cosmetics/wings_{name}_tint.png', 64, 32, px)

# ------------------------------------------------------------------ tintable horns
def horns_tint():
    px = canvas(32, 32)
    box(px, 0, 0, 2, 3, 2, lambda f, i, j, w, h: rgba(lerp((255, 255, 255), (200, 200, 200), j / max(1, h - 1))))
    box(px, 0, 8, 1, 3, 1, lambda f, i, j, w, h: rgba(lerp((190, 190, 190), (120, 120, 120), j / max(1, h - 1))))
    save(f'{OUT}/cosmetics/horns_tint.png', 32, 32, px)

# ------------------------------------------------------------------ crown v2: gold ring + separate gems (tinted)
def crown2():
    px = canvas(64, 32)
    def band(face, i, j, w, h):
        c = lerp((255, 222, 95), (196, 138, 28), j / max(1, h - 1))
        if j == 0:
            c = lerp(c, (255, 245, 180), 0.5)
        if (i + j) % 5 == 0:
            c = lerp(c, (150, 100, 15), 0.25)
        return rgba(c)
    box(px, 0, 0, 10, 2, 10, band)                   # ring 10x2x10 (sides only)
    box(px, 0, 14, 1, 2, 1, lambda f, i, j, w, h: rgba(lerp((255, 240, 150), (226, 166, 38), j)))  # spike
    save(f'{OUT}/cosmetics/crown.png', 64, 32, px)

    gems = canvas(32, 32)
    def facet(face, i, j, w, h):
        v = 175 + 80 * ((i + j) % 2) if face in ('north', 'south', 'east', 'west') else 230
        if i == 0 and j == 0:
            v = 255
        return rgba((v, v, v))
    box(gems, 0, 0, 2, 1, 1, facet)                  # front/back gem
    box(gems, 0, 4, 1, 1, 2, facet)                  # side gem
    box(gems, 0, 8, 1, 1, 1, lambda f, i, j, w, h: rgba((250, 250, 250)))  # pearls on the spikes
    save(f'{OUT}/cosmetics/crown_gems.png', 32, 32, gems)

# ------------------------------------------------------------------ more animated capes
TAU = 6.283185

def aurora(u, v, x, y, t, rnd):
    c = lerp((4, 8, 28), (10, 20, 50), v)
    for k, (col, off) in enumerate((((60, 255, 160), 0.0), ((120, 120, 255), 0.33), ((220, 90, 255), 0.66))):
        band = 0.35 + 0.18 * math.sin(u * 7 + t * TAU + off * TAU) + k * 0.12
        d = abs(v - band)
        if d < 0.12:
            c = lerp(c, col, (1 - d / 0.12) * 0.85)
    if (x * 31 + y * 17) % 97 == 0:
        c = lerp(c, (255, 255, 255), 0.8)
    return c

def lava(u, v, x, y, t, rnd):
    n = math.sin(u * 9 + t * TAU) * 0.5 + math.sin(v * 13 - t * TAU) * 0.5 + math.sin((u + v) * 11 + t * TAU * 2) * 0.3
    n = (n + 1.3) / 2.6
    if n > 0.62:
        return lerp((255, 120, 10), (255, 235, 120), (n - 0.62) / 0.38)
    return lerp((40, 8, 4), (150, 30, 5), n / 0.62)

def storm(u, v, x, y, t, rnd):
    c = lerp((40, 44, 60), (18, 20, 30), v)
    cloud = math.sin(u * 8 + t * TAU) + math.sin(u * 3 - v * 6 + t * TAU)
    if cloud > 1.2 and v < 0.45:
        c = lerp(c, (90, 95, 115), 0.6)
    flash = t > 0.62 and t < 0.75
    path = 0.5 + 0.12 * math.sin(v * 25) + 0.05 * math.sin(v * 60)
    if flash and abs(u - path) < 0.045 and v > 0.1:
        return (255, 255, 200)
    if flash:
        c = lerp(c, (180, 190, 230), 0.35)
    if (x + int(y + t * 64)) % 7 == 0 and (x * 3) % 5 == 0:
        c = lerp(c, (140, 160, 210), 0.5)    # rain
    return c

def sakura(u, v, x, y, t, rnd):
    c = lerp((255, 214, 232), (238, 150, 190), v)
    for k in range(6):
        px_ = (k * 0.17 + 0.05 * math.sin(t * TAU + k)) % 1.0
        py_ = (k * 0.29 + t) % 1.0
        if abs(u - px_) < 0.06 and abs(v - py_) < 0.05:
            return (255, 255, 255) if k % 2 else (255, 120, 170)
    if abs(v - 0.15 - 0.05 * math.sin(u * 9)) < 0.03:
        c = lerp(c, (110, 60, 50), 0.8)      # branch
    return c

def ender(u, v, x, y, t, rnd):
    c = lerp((14, 6, 24), (40, 10, 60), v)
    s = (x * 53 + y * 29) % 89
    if s < 6:
        phase = (t + s / 6) % 1
        c = lerp(c, (200, 80, 255), math.sin(phase * math.pi))
    if 0.36 < v < 0.44 and (0.2 < u < 0.32 or 0.68 < u < 0.8):
        blink = 0.0 if 0.45 < t < 0.52 else 1.0
        c = lerp(c, (220, 120, 255), blink)  # eyes
    return c

def snow(u, v, x, y, t, rnd):
    c = lerp((22, 40, 90), (60, 100, 170), v)
    if v > 0.88:
        c = (235, 240, 250)
    for k in range(9):
        fx = (k * 0.113 + 0.03 * math.sin(t * TAU * 2 + k)) % 1.0
        fy = (k * 0.37 + t) % 1.0
        if abs(u - fx) < 0.05 and abs(v - fy) < 0.035:
            return (255, 255, 255)
    return c

def neon(u, v, x, y, t, rnd):
    if v < 0.5:
        c = lerp((40, 0, 70), (255, 60, 150), v * 2)
        sd = math.hypot(u - 0.5, (v - 0.48) * 1.4)
        if sd < 0.28 and not (int(v * 30) % 3 == 0 and v > 0.3):
            c = lerp((255, 230, 80), (255, 80, 140), (v - 0.2) * 2.5)
        return c
    gy = (v - 0.5) * 2
    line = (gy * 6 + t) % 1 < 0.12 or abs(((u - 0.5) / max(0.1, gy + 0.2)) * 4 % 1) < 0.1
    return (0, 240, 255) if line else (25, 0, 45)

if __name__ == '__main__':
    for n, fn, lo in (('angel', ca.angel, 120), ('dragon', ca.dragon, 70), ('butterfly', ca.butterfly, 60)):
        wing_tint(n, fn, lo)
    horns_tint()
    crown2()
    for n, fn in (('aurora', aurora), ('lava', lava), ('storm', storm), ('sakura', sakura), ('ender', ender), ('snow', snow), ('neon', neon)):
        ca.cape_frames(n, fn)
    print('ok')

# ------------------------------------------------------------------ minimee necklace chain (grey links, tinted gold/silver in code)
def chain():
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = 235 if (y // 2) % 2 == 0 else 165
            px[y][x] = rgba((v, v, v))
    save(f'{OUT}/cosmetics/chain.png', 16, 16, px)

if __name__ == '__main__':
    chain()
