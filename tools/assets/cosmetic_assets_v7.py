"""Nova cosmetics, seventh batch (1.8.5): graduation cap, mohawk, cyber visor, rubber duck, face mask,
back shield, quiver, balloon and two animated capes (lightning grid "circuit", "lavalamp").
Usage: python3 -B tools/assets/cosmetic_assets_v7.py src/main/resources/assets/novaclient/textures
Own designs, no Mojang assets and no assets of other clients. Grey parts are tinted in code with the chosen colour."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cosmetic_assets as ca
from cosmetic_assets import canvas, rgba, lerp, box, save

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
ca.OUT = OUT
C = f'{OUT}/cosmetics'

def grey(v, a=255):
    return rgba((v, v, v), a)

def noise(i, j, k=0):
    return ((i * 7 + j * 13 + k * 5) % 5) * 4

# ------------------------------------------------------------------ heads
def gradcap():
    # cap 8x2x8 (0,0) dark, board 11x1x11 (0,12) dark, button 1x1x1 (0,26) + tassel 1x4x1 (8,26) tinted
    px = canvas(64, 64)
    def felt(face, i, j, w, h):
        n = noise(i, j) // 2
        return rgba((30 + n, 30 + n, 36 + n))
    box(px, 0, 0, 8, 2, 8, felt)
    def board(face, i, j, w, h):
        if face in ('up', 'down') and (i in (0, w - 1) or j in (0, h - 1)):
            return rgba((52, 52, 60))
        return felt(face, i, j, w, h)
    box(px, 0, 12, 11, 1, 11, board)
    box(px, 0, 26, 1, 1, 1, lambda f, i, j, w, h: grey(255))
    box(px, 8, 26, 1, 4, 1, lambda f, i, j, w, h: grey(255 - j * 14 if j < 3 else 200))
    save(f'{C}/gradcap.png', 64, 64, px)

def mohawk():
    # strip 1x4x9 (0,0) tinted: spiky top (transparent gaps on the side faces)
    px = canvas(32, 32)
    def hair(face, i, j, w, h):
        if face in ('east', 'west') and j == 0 and i % 2 == 1:
            return (0, 0, 0, 0)
        return grey(255 - j * 22 - noise(i, j))
    box(px, 0, 0, 1, 4, 9, hair)
    save(f'{C}/mohawk.png', 32, 32, px)

def visor():
    # glass 9x2x1 (0,0) tinted + glowing, temples 1x1x5 (0,4) dark
    px = canvas(64, 32)
    def glass(face, i, j, w, h):
        if face == 'north':
            return grey(255 if j == 0 else 205) if i % 4 != 3 else grey(170)
        return grey(190)
    box(px, 0, 0, 9, 2, 1, glass)
    box(px, 0, 4, 1, 1, 5, lambda f, i, j, w, h: rgba((40, 42, 50)))
    save(f'{C}/visor.png', 64, 32, px)

def duck():
    # body 4x3x5 (0,0) tinted, head 3x3x3 (0,8) tinted with eyes, beak 2x1x1 (0,14) orange, tail 2x1x1 (8,14) tinted
    px = canvas(32, 32)
    box(px, 0, 0, 4, 3, 5, lambda f, i, j, w, h: grey(250 - noise(i, j) - (20 if f == 'down' else 0)))
    def head(face, i, j, w, h):
        if face in ('east', 'west') and j == 1 and i == (0 if face == 'west' else w - 1):
            return rgba((20, 20, 24))
        return grey(255 - noise(i, j) // 2)
    box(px, 0, 8, 3, 3, 3, head)
    box(px, 0, 14, 2, 1, 1, lambda f, i, j, w, h: rgba((255, 140, 30)))
    box(px, 8, 14, 2, 1, 1, lambda f, i, j, w, h: grey(235))
    save(f'{C}/duck.png', 32, 32, px)

def facemask():
    # front 8x3x1 (0,0) tinted with folds, straps 1x1x5 (0,4) white
    px = canvas(64, 32)
    box(px, 0, 0, 8, 3, 1, lambda f, i, j, w, h: grey(250 if j != 1 else 222))
    box(px, 0, 4, 1, 1, 5, lambda f, i, j, w, h: rgba((240, 240, 240)))
    save(f'{C}/facemask.png', 64, 32, px)

# ------------------------------------------------------------------ extras
def shield():
    # plate 8x9x1 (0,0) tinted with a lighter cross, boss 2x2x1 (20,0) + rim nails gold
    px = canvas(64, 32)
    def plate(face, i, j, w, h):
        if face == 'south':
            if i in (0, w - 1) or j in (0, h - 1):
                return grey(120)
            if i in (3, 4) or j in (3, 4):
                return grey(255)
            return grey(200 - noise(i, j))
        if face == 'north':
            return rgba((110, 80, 50))
        return grey(120)
    box(px, 0, 0, 8, 9, 1, plate)
    box(px, 20, 0, 2, 2, 1, lambda f, i, j, w, h: rgba((255, 205, 70) if (i + j) % 2 == 0 else (225, 165, 40)))
    save(f'{C}/shield.png', 64, 32, px)

def quiver():
    # tube 3x9x2 (0,0) tinted leather with two straps, arrow end 1x3x1 (12,0): shaft + coloured feathers
    px = canvas(32, 32)
    def tube(face, i, j, w, h):
        if face == 'up':
            return rgba((40, 28, 20))
        if j in (1, 6):
            return grey(150)
        return grey(240 - noise(i, j))
    box(px, 0, 0, 3, 9, 2, tube)
    def arrow(face, i, j, w, h):
        if j == 0:
            return rgba((240, 240, 240))
        if j == 1:
            return rgba((230, 60, 60))
        return rgba((150, 110, 70))
    box(px, 12, 0, 1, 3, 1, arrow)
    save(f'{C}/quiver.png', 32, 32, px)

def balloon():
    # string 1x12x1 (0,0) white, balloon 5x6x5 (0,14) tinted with a highlight, knot 1x1x1 (8,0) tinted
    px = canvas(32, 32)
    box(px, 0, 0, 1, 12, 1, lambda f, i, j, w, h: rgba((235, 235, 235)))
    box(px, 8, 0, 1, 1, 1, lambda f, i, j, w, h: grey(200))
    def skin(face, i, j, w, h):
        if face == 'south' and i == 1 and j in (1, 2):
            return grey(255)
        if face == 'down':
            return grey(190)
        return grey(235 - j * 5)
    box(px, 0, 14, 5, 6, 5, skin)
    save(f'{C}/balloon.png', 32, 32, px)

# ------------------------------------------------------------------ animated capes
def circuit(u, v, x, y, t, rnd):
    """Dark board with glowing traces; light pulses run along them."""
    c = (10, 22, 26)
    on_trace = (x % 4 == 1) or (y % 5 == 2 and (x // 4 + y // 5) % 2 == 0)
    if on_trace:
        c = (20, 90, 80)
        pulse = ((y * 0.06 + x * 0.013 + (x % 4) * 0.21) - t) % 1.0
        if pulse < 0.12:
            c = lerp((120, 255, 220), (20, 90, 80), pulse / 0.12)
    if x % 4 == 1 and y % 5 == 2:
        c = (255, 220, 90)          # solder points
    return c

def lavalamp(u, v, x, y, t, rnd):
    """Soft blobs rising and sinking."""
    f = 0.0
    for k in range(4):
        bx = 0.2 + 0.2 * k + 0.08 * math.sin(6.283185 * (t + k * 0.3))
        by = 0.5 + 0.42 * math.sin(6.283185 * (t + k * 0.27))
        d2 = (u - bx) ** 2 + ((v - by) * 1.6) ** 2
        f += 0.012 / (d2 + 0.004)
    base = lerp((70, 20, 110), (30, 10, 70), v)
    if f > 1.0:
        return lerp((255, 120, 60), (255, 220, 90), min(1.0, (f - 1.0) * 0.6))
    if f > 0.8:
        return lerp(base, (255, 100, 80), 0.6)
    return base

if __name__ == '__main__':
    gradcap(); mohawk(); visor(); duck(); facemask()
    shield(); quiver(); balloon()
    for n, fn in (('circuit', circuit), ('lavalamp', lavalamp)):
        ca.cape_frames(n, fn)
    print('ok')
