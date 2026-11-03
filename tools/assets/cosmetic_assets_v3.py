"""Nova cosmetics, third batch (1.6.0): new head cosmetics, extras (backpack, tails, scarf), new wings, capes.
Usage: python3 tools/assets/cosmetic_assets_v3.py src/main/resources/assets/novaclient/textures
Own designs, no Mojang assets. Grey parts are tinted in code with the chosen colour."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cosmetic_assets as ca
import cosmetic_assets_v2 as v2
from cosmetic_assets import canvas, rgba, lerp, box, save

OUT = sys.argv[1]
ca.OUT = OUT
v2.OUT = OUT
C = f'{OUT}/cosmetics'

def grey(v):
    return rgba((v, v, v))

def knit(face, i, j, w, h):
    return grey(235 if (i + j) % 2 == 0 else 205)

# ------------------------------------------------------------------ heads
def partyhat():
    px = canvas(64, 32)
    def stripes(face, i, j, w, h):
        return grey(250 if (i + j) % 4 < 2 else 175)
    box(px, 0, 0, 6, 2, 6, stripes)
    box(px, 0, 8, 4, 2, 4, stripes)
    box(px, 0, 14, 2, 2, 2, stripes)
    box(px, 0, 18, 2, 2, 2, lambda f, i, j, w, h: grey(255))      # pompom (own part)
    save(f'{C}/partyhat.png', 64, 32, px)

def cowboyhat():
    px = canvas(64, 64)
    def leather(face, i, j, w, h):
        n = ((i * 7 + j * 13) % 5) * 4
        return rgba((150 + n, 98 + n, 52 + n))
    box(px, 0, 0, 13, 1, 13, leather)
    box(px, 0, 16, 9, 4, 9, leather)
    box(px, 0, 32, 9, 1, 9, lambda f, i, j, w, h: grey(240))       # band (tinted)
    save(f'{C}/cowboyhat.png', 64, 64, px)

def bunnyears():
    px = canvas(32, 32)
    def ear(face, i, j, w, h):
        if face == 'north' and j > 0:
            return rgba((255, 175, 195))
        return grey(245)
    box(px, 0, 0, 2, 6, 1, ear)
    save(f'{C}/bunnyears.png', 32, 32, px)

def headphones():
    px = canvas(64, 32)
    box(px, 0, 0, 10, 1, 2, lambda f, i, j, w, h: grey(230))
    box(px, 0, 4, 1, 5, 2, lambda f, i, j, w, h: grey(200))
    def cup(face, i, j, w, h):
        if face in ('east', 'west') and 0 < i < w - 1 and 0 < j < h - 1:
            return grey(150)                                       # cushion
        return grey(245)
    box(px, 0, 12, 2, 3, 4, cup)
    save(f'{C}/headphones.png', 64, 32, px)

def sunglasses():
    px = canvas(64, 32)
    def frame(face, i, j, w, h):
        if face == 'north' and j == 1 and (1 <= i <= 3 or 5 <= i <= 7):
            return rgba((30, 30, 40))                              # lenses stay dark
        if face == 'north' and j == 0 and (1 <= i <= 3 or 5 <= i <= 7):
            return rgba((70, 70, 90))
        return grey(245)
    box(px, 0, 0, 9, 2, 1, frame)
    box(px, 0, 4, 1, 1, 5, lambda f, i, j, w, h: grey(235))
    save(f'{C}/sunglasses.png', 64, 32, px)

def vikinghelmet():
    px = canvas(64, 32)
    def iron(face, i, j, w, h):
        n = 20 if (i in (0, w // 2) or face == 'up' and j == h // 2) else 0
        return rgba((165 - n, 168 - n, 176 - n))
    box(px, 0, 0, 9, 3, 9, iron)
    box(px, 0, 12, 10, 1, 10, lambda f, i, j, w, h: grey(235))    # rim band (tinted)
    def bone(face, i, j, w, h):
        return rgba(lerp((245, 238, 220), (205, 195, 170), j / max(1, h - 1)))
    box(px, 0, 24, 2, 3, 2, bone)
    box(px, 8, 24, 1, 3, 1, bone)
    save(f'{C}/vikinghelmet.png', 64, 32, px)

def flowercrown():
    px = canvas(64, 32)
    box(px, 0, 0, 10, 1, 10, lambda f, i, j, w, h: rgba((70, 150, 60) if (i + j) % 3 else (50, 120, 45)))
    def petal(face, i, j, w, h):
        return grey(255) if face != 'up' else rgba((255, 230, 120))
    box(px, 0, 14, 1, 1, 1, petal)
    save(f'{C}/flowercrown.png', 64, 32, px)

def beanie():
    px = canvas(64, 32)
    box(px, 0, 0, 9, 4, 9, knit)
    box(px, 0, 14, 9, 2, 9, lambda f, i, j, w, h: grey(215 if i % 2 else 190))
    box(px, 40, 0, 3, 3, 3, lambda f, i, j, w, h: grey(255))
    save(f'{C}/beanie.png', 64, 32, px)

# ------------------------------------------------------------------ extras (body)
def backpack():
    px = canvas(64, 32)
    def bag(face, i, j, w, h):
        if face == 'south' and j == 1:
            return grey(170)
        return grey(235 if (i * 3 + j) % 7 else 210)
    box(px, 0, 0, 6, 7, 3, bag)
    box(px, 0, 12, 4, 3, 1, lambda f, i, j, w, h: grey(205))
    box(px, 0, 18, 1, 6, 1, lambda f, i, j, w, h: rgba((60, 60, 66)))  # straps
    save(f'{C}/backpack.png', 64, 32, px)

def tails():
    px = canvas(32, 32)
    box(px, 0, 0, 1, 1, 3, lambda f, i, j, w, h: grey(235))             # thin segment (cat, devil)
    box(px, 0, 6, 3, 3, 3, lambda f, i, j, w, h: grey(240 if (i + j) % 3 else 215))  # fluffy (fox)
    box(px, 0, 14, 3, 3, 3, lambda f, i, j, w, h: grey(255))            # white fox tip
    box(px, 0, 22, 2, 2, 1, lambda f, i, j, w, h: grey(245))            # devil arrow
    save(f'{C}/tails.png', 32, 32, px)

def scarf():
    px = canvas(64, 32)
    def stripes(face, i, j, w, h):
        return grey(245 if (i // 2) % 2 == 0 else 200)
    box(px, 0, 0, 9, 2, 5, stripes)
    box(px, 0, 10, 2, 5, 1, lambda f, i, j, w, h: grey(245 if j % 2 == 0 else 200))
    save(f'{C}/scarf.png', 64, 32, px)

# ------------------------------------------------------------------ wings
def bat(x, y):
    top = 1 + x * 0.1
    scallop = 13 - x * 0.3 - 2.5 * abs(math.sin((x + 1) * 0.8))
    if y < top or y > scallop:
        return None
    if x % 4 == 0 or y <= top + 0.7:
        return rgba((35, 25, 40))
    return rgba(lerp((80, 55, 85), (45, 30, 55), y / 16), 240)

def fairy(x, y):
    upper = ((x - 7) / 7.5) ** 2 + ((y - 4.5) / 4.8) ** 2 <= 1
    lower = ((x - 4) / 4.5) ** 2 + ((y - 11) / 3.6) ** 2 <= 1
    if not (upper or lower):
        return None
    d = math.hypot((x - 7) / 7.5, (y - 4.5) / 4.8) if upper else math.hypot((x - 4) / 4.5, (y - 11) / 3.6)
    if d > 0.86:
        return rgba((255, 255, 255), 230)
    if (x + y) % 5 == 0:
        return rgba((255, 255, 255), 200)
    return rgba(lerp((200, 170, 255), (140, 230, 255), d), 150)

def phoenix(x, y):
    top = 2 + x * 0.2
    bottom = 10 + (15 - x) * 0.4 + (3 if x % 3 == 0 else 0)
    if y < top or y > bottom:
        return None
    t = (y - top) / max(1, bottom - top)
    c = lerp((255, 230, 90), (230, 60, 20), t)
    if x % 3 == 0:
        c = lerp(c, (255, 120, 30), 0.5)
    return rgba(c)

# ------------------------------------------------------------------ more animated capes
TAU = 6.283185

def hearts(u, v, x, y, t, rnd):
    c = lerp((255, 120, 170), (200, 40, 110), v)
    for k in range(5):
        hx = (k * 0.21 + 0.03 * math.sin(t * TAU + k)) % 1.0
        hy = (1.0 - (k * 0.33 + t)) % 1.0
        dx, dy = (u - hx) * 12, (v - hy) * 18
        if (dx * dx + (dy - abs(dx) * 0.8) ** 2) < 1.0:
            return (255, 255, 255)
    return c

def glitch(u, v, x, y, t, rnd):
    band = int(v * 16 + t * 16) % 16
    shift = 0.08 if (band * 7 + int(t * 16)) % 5 == 0 else 0
    uu = (u + shift) % 1
    c = lerp((20, 20, 30), (40, 40, 60), uu)
    if int(uu * 10 + v * 6) % 4 == 0:
        c = (0, 255, 200) if band % 3 else (255, 0, 120)
    if (x * 13 + y * 7 + int(t * 64)) % 41 == 0:
        c = (255, 255, 255)
    return c

def portal(u, v, x, y, t, rnd):
    dx, dy = (u - 0.5) * 2, (v - 0.5) * 1.6
    r = math.hypot(dx, dy)
    a = math.atan2(dy, dx)
    s = (math.sin(a * 3 + r * 10 - t * TAU) + 1) / 2
    c = lerp((40, 0, 70), (170, 60, 255), s * max(0, 1 - r * 0.7))
    if r < 0.15:
        c = lerp((255, 255, 255), c, r / 0.15)
    return c

if __name__ == '__main__':
    partyhat(); cowboyhat(); bunnyears(); headphones(); sunglasses(); vikinghelmet(); flowercrown(); beanie()
    backpack(); tails(); scarf()
    for n, fn in (('bat', bat), ('fairy', fairy), ('phoenix', phoenix)):
        ca.wing_texture(n, fn)
        v2.wing_tint(n, fn, 80)
    for n, fn in (('hearts', hearts), ('glitch', glitch), ('portal', portal)):
        ca.cape_frames(n, fn)
    print('ok')
