"""Nova cosmetics, sixth batch (1.8.1): mushroom hat, pirate hat, alien antennae, dragonfly wings and two animated capes
(aquarium, clouds).
Usage: python3 -B tools/assets/cosmetic_assets_v6.py src/main/resources/assets/novaclient/textures
Own designs, no Mojang assets. Grey parts are tinted in code with the chosen colour."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cosmetic_assets as ca
import cosmetic_assets_v2 as v2
from cosmetic_assets import canvas, rgba, lerp, box, save

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
ca.OUT = OUT
v2.OUT = OUT
C = f'{OUT}/cosmetics'

def grey(v, a=255):
    return rgba((v, v, v), a)

def noise(i, j, k=0):
    return ((i * 7 + j * 13 + k * 5) % 5) * 4

# ------------------------------------------------------------------ heads
def mushroom():
    # cap base 11x2x11 (0,0) tinted, cap top 8x2x8 (0,14) tinted, spot 2x1x2 (44,0) white, stem ring 9x1x9 (0,26) untinted
    px = canvas(64, 64)
    def cap(face, i, j, w, h):
        return grey(235 - noise(i, j))
    box(px, 0, 0, 11, 2, 11, cap)
    box(px, 0, 14, 8, 2, 8, cap)
    box(px, 44, 0, 2, 1, 2, lambda f, i, j, w, h: rgba((250, 248, 240)))
    box(px, 0, 26, 9, 1, 9, lambda f, i, j, w, h: rgba((236, 224, 200)))
    save(f'{C}/mushroom.png', 64, 64, px)

def piratehat():
    # brim 12x1x12 (0,0), crown 9x3x9 (0,13), band 9x1x9 (0,27) tinted, skull 3x3x0 painted on crown front
    px = canvas(64, 64)
    def felt(face, i, j, w, h):
        return rgba((38 + noise(i, j) // 2, 36 + noise(i, j) // 2, 44 + noise(i, j) // 2))
    box(px, 0, 0, 12, 1, 12, felt)
    def crown(face, i, j, w, h):
        if face == 'north':
            skull = ["   ", " # ", "###", "# #"]
            # skull in the middle of the front, 3 wide
            x0 = w // 2 - 1
            if 0 <= i - x0 < 3 and 0 <= j < 3:
                row = [" # ", "###", "# #"][j]
                if row[i - x0] == '#':
                    return rgba((235, 235, 225))
        return felt(face, i, j, w, h)
    box(px, 0, 13, 9, 3, 9, crown)
    box(px, 0, 27, 9, 1, 9, lambda f, i, j, w, h: grey(240))
    save(f'{C}/piratehat.png', 64, 64, px)

def antennae():
    # stalk 1x5x1 (0,0) dark, ball 2x2x2 (8,0) tinted
    px = canvas(32, 16)
    box(px, 0, 0, 1, 5, 1, lambda f, i, j, w, h: rgba((50, 50, 58)))
    box(px, 8, 0, 2, 2, 2, lambda f, i, j, w, h: grey(255 if (i + j) % 3 else 215))
    save(f'{C}/antennae.png', 32, 16, px)

# ------------------------------------------------------------------ wings
def dragonfly(x, y):
    """Two long thin lobes with dark veins, see-through."""
    upper = ((x - 7.5) / 8.0) ** 2 + ((y - 3.5) / 2.6) ** 2 <= 1
    lower = ((x - 6.5) / 7.0) ** 2 + ((y - 8.5) / 2.4) ** 2 <= 1
    if not (upper or lower):
        return None
    edge = (((x - 7.5) / 8.0) ** 2 + ((y - 3.5) / 2.6) ** 2 > 0.7) if upper else (((x - 6.5) / 7.0) ** 2 + ((y - 8.5) / 2.4) ** 2 > 0.7)
    if edge or x % 5 == 2:
        return rgba((60, 110, 120), 230)         # edge and veins
    if x >= 13:
        return rgba((110, 160, 200), 190)        # darker tips
    return rgba(lerp((150, 255, 225), (130, 200, 255), x / 15), 160)

# ------------------------------------------------------------------ animated capes
TAU = 6.283185

def aquarium(u, v, x, y, t, rnd):
    c = lerp((30, 120, 200), (10, 50, 110), v)
    # light rays
    if int(u * 8 + v * 3 + t * 2) % 5 == 0:
        c = lerp(c, (90, 170, 230), 0.25)
    fish = [(0.25, (255, 140, 40), 0.0), (0.55, (255, 220, 60), 0.4), (0.8, (240, 80, 120), 0.7)]
    for fy, col, off in fish:
        fx = (t + off) % 1.0 * 1.3 - 0.15
        dx, dy = (u - fx) * 12, (v - fy) * 18
        if dx * dx + dy * dy < 1.0:
            return col
        if -2.0 < dx < -0.8 and abs(dy) < (-dx - 0.8) * 0.9:
            return lerp(col, (0, 0, 0), 0.2)    # tail
    for k in range(4):
        bx = (k * 0.27 + 0.1) % 1.0
        by = 1.0 - ((k * 0.31 + t * 1.5) % 1.0)
        if abs(math.hypot(u - bx, (v - by) * 0.7) - 0.03) < 0.012:
            return (220, 245, 255)
    return c

def clouds(u, v, x, y, t, rnd):
    c = lerp((110, 180, 250), (190, 225, 255), v)
    for k, (cy, size, speed) in enumerate([(0.2, 0.16, 1), (0.5, 0.2, 1), (0.8, 0.14, 2)]):
        cx = (u - (t * speed + k * 0.37)) % 1.0
        for bx, by, r in ((0.0, 0.0, 1.0), (0.12, -0.05, 0.8), (-0.12, 0.02, 0.75)):
            d = math.hypot((cx - 0.5 - bx * size * 2), (v - cy - by) * 1.3)
            if d < size * r * 0.6:
                return lerp((255, 255, 255), (225, 235, 250), d / (size * 0.6))
    return c

if __name__ == '__main__':
    mushroom(); piratehat(); antennae()
    ca.wing_texture('dragonfly', dragonfly)
    v2.wing_tint('dragonfly', dragonfly, 80)
    for n, fn in (('aquarium', aquarium), ('clouds', clouds)):
        ca.cape_frames(n, fn)
    print('ok')
