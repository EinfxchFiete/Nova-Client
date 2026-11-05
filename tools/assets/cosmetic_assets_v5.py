"""Nova cosmetics, fifth batch (1.8.0): witch hat, santa hat, chef hat, fox ears, unicorn horn, propeller cap,
ice wings, jetpack, guitar, back sword and four animated capes (fireworks, autumn, bubbles, shooting stars).
Usage: python3 -B tools/assets/cosmetic_assets_v5.py src/main/resources/assets/novaclient/textures
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
def witchhat():
    # brim 12x1x12 (0,0), cone parts 8x3x8 (0,13), 6x3x6 (32,13), 4x3x4 (0,25), 2x3x2 (24,25), band 8x1x8 (0,40)
    px = canvas(64, 64)
    def felt(face, i, j, w, h):
        return grey(210 + noise(i, j))
    box(px, 0, 0, 12, 1, 12, felt)
    box(px, 0, 13, 8, 3, 8, felt)
    box(px, 32, 13, 6, 3, 6, felt)
    box(px, 0, 25, 4, 3, 4, felt)
    box(px, 24, 25, 2, 3, 2, felt)
    def band(face, i, j, w, h):
        if face == 'north' and 2 <= i <= w - 3:
            return rgba((255, 214, 90)) if i in (2, w - 3) else grey(240)   # golden buckle
        return grey(240)
    box(px, 0, 40, 8, 1, 8, band)
    save(f'{C}/witchhat.png', 64, 64, px)

def santahat():
    # fur rim 9x2x9 (0,0), body 8x3x8 (0,12), 6x3x6 (32,12), droop 4x3x4 (0,24), 3x3x3 (20,24), pompom 3x3x3 (40,24)
    px = canvas(64, 64)
    def red(face, i, j, w, h):
        return grey(225 + noise(i, j))
    def fur(face, i, j, w, h):
        return rgba((250 - noise(i, j, 1), 250 - noise(i, j, 1), 250 - noise(i, j, 2)))
    box(px, 0, 0, 9, 2, 9, fur)
    box(px, 0, 12, 8, 3, 8, red)
    box(px, 32, 12, 6, 3, 6, red)
    box(px, 0, 24, 4, 3, 4, red)
    box(px, 20, 24, 3, 3, 3, red)
    box(px, 40, 24, 3, 3, 3, fur)
    save(f'{C}/santahat.png', 64, 64, px)

def chefhat():
    # band 8x2x8 (0,0), puff 10x5x10 (0,12), top 8x1x8 (0,30)
    px = canvas(64, 64)
    def cloth(face, i, j, w, h):
        pleat = 18 if face in ('north', 'south', 'east', 'west') and i % 3 == 0 else 0
        return grey(250 - pleat - noise(i, j) // 2)
    box(px, 0, 0, 8, 2, 8, lambda f, i, j, w, h: grey(238 - noise(i, j) // 2))
    box(px, 0, 12, 10, 5, 10, cloth)
    box(px, 0, 30, 8, 1, 8, cloth)
    save(f'{C}/chefhat.png', 64, 64, px)

def foxears():
    # outer ear 3x4x1 (0,0), tip 1x1x1 (0,8), inner (white, untinted) 1x2x1 (8,8)
    px = canvas(32, 32)
    def outer(face, i, j, w, h):
        if j <= 0 and face != 'down':
            return grey(110)          # darker tip edge
        return grey(235 - noise(i, j))
    box(px, 0, 0, 3, 4, 1, outer)
    box(px, 0, 8, 1, 1, 1, lambda f, i, j, w, h: grey(90))
    box(px, 8, 8, 1, 2, 1, lambda f, i, j, w, h: rgba((250, 246, 240)))
    save(f'{C}/foxears.png', 32, 32, px)

def unicorn():
    # spiral horn segments: 2x2x2 (0,0), 1.5 -> 2x2x2 (8,0), 1x2x1 (16,0), 1x1x1 (24,0)
    px = canvas(32, 16)
    def spiral(face, i, j, w, h):
        return grey(255 if (i + j) % 2 == 0 else 200)
    box(px, 0, 0, 2, 2, 2, spiral)
    box(px, 8, 0, 2, 2, 2, spiral)
    box(px, 16, 0, 1, 2, 1, spiral)
    box(px, 24, 0, 1, 1, 1, spiral)
    save(f'{C}/unicorn.png', 32, 16, px)

def propeller():
    # cap 8x2x8 (0,0), visor 8x1x3 (0,12), stem 1x2x1 (0,18), blade 7x1x1 (8,18)
    px = canvas(64, 32)
    quads = [(230, 57, 53), (253, 216, 53), (67, 160, 71), (30, 136, 229)]
    def cap(face, i, j, w, h):
        if face == 'up':
            q = (0 if i < w / 2 else 1) + (0 if j < h / 2 else 2)
            return rgba(quads[q])
        return rgba(quads[(i * 4 // max(1, w)) % 4])
    box(px, 0, 0, 8, 2, 8, cap)
    box(px, 0, 12, 8, 1, 3, lambda f, i, j, w, h: rgba((30, 136, 229)))
    box(px, 0, 18, 1, 2, 1, lambda f, i, j, w, h: grey(170))
    box(px, 8, 18, 7, 1, 1, lambda f, i, j, w, h: grey(245 - (i % 2) * 20))
    save(f'{C}/propeller.png', 64, 32, px)

# ------------------------------------------------------------------ wings
def ice(x, y):
    """Crystal wing: straight upper edge, sharp icicle tips at the bottom."""
    top = 1.5 + x * 0.15
    tips = 13.0 - x * 0.5 + (3.0 if x % 4 == 1 else 1.2 if x % 4 == 3 else 0)
    if y < top or y > tips:
        return None
    t = (y - top) / max(1, tips - top)
    if x % 4 == 1 and y > tips - 3:
        return rgba((255, 255, 255), 235)        # icicle highlight
    if (x * 3 + int(y) * 5) % 11 == 0 or y <= top + 0.6:
        return rgba((245, 252, 255), 240)        # glints and edge
    return rgba(lerp((190, 235, 255), (110, 185, 245), t), 185)

# ------------------------------------------------------------------ extras (body space)
def jetpack():
    # tank 2x6x2 (0,0) [twice], frame 6x2x1 (0,10), nozzle 2x1x2 (16,0), stripe 2x1x2 (24,0) tinted
    px = canvas(64, 32)
    def metal(face, i, j, w, h):
        shine = 30 if face in ('north', 'south') and i == 0 else 0
        return rgba((176 + shine - noise(i, j), 186 + shine - noise(i, j), 196 + shine - noise(i, j)))
    box(px, 0, 0, 2, 6, 2, metal)
    box(px, 0, 10, 6, 2, 1, lambda f, i, j, w, h: rgba((90, 95, 105)))
    box(px, 16, 0, 2, 1, 2, lambda f, i, j, w, h: rgba((60, 60, 66)))
    box(px, 24, 0, 2, 1, 2, lambda f, i, j, w, h: grey(240))
    save(f'{C}/jetpack.png', 64, 32, px)

def guitar():
    # body 6x7x1 (0,0) tinted, neck 1x7x1 (16,0), head 2x2x1 (22,0), strings 1x9x0 painted on neck front
    px = canvas(64, 32)
    def body(face, i, j, w, h):
        if face == 'south':
            if 2 <= i <= 3 and 2 <= j <= 3:
                return rgba((40, 30, 25))                    # sound hole
            if 2 <= i <= 3 and j == 5:
                return rgba((60, 40, 25))                    # bridge
        edge = 25 if i in (0, w - 1) or j in (0, h - 1) else 0
        return grey(230 - edge - noise(i, j) // 2)
    box(px, 0, 0, 6, 7, 1, body)
    box(px, 16, 0, 1, 7, 1, lambda f, i, j, w, h: rgba((120, 80, 45)) if f != 'south' else rgba((70, 50, 35)))
    box(px, 22, 0, 2, 2, 1, lambda f, i, j, w, h: rgba((90, 60, 35)))
    save(f'{C}/guitar.png', 64, 32, px)

def backsword():
    # blade 1x11x1 (0,0) tinted, guard 4x1x1 (8,0), grip 1x3x1 (8,4), pommel 1x1x1 (8,10)
    px = canvas(32, 16)
    def blade(face, i, j, w, h):
        return grey(250 if face in ('north', 'south') and j % 3 == 0 else 225)
    box(px, 0, 0, 1, 11, 1, blade)
    box(px, 8, 0, 4, 1, 1, lambda f, i, j, w, h: rgba((255, 200, 60)))
    box(px, 8, 4, 1, 3, 1, lambda f, i, j, w, h: rgba((110, 70, 40) if j % 2 == 0 else (85, 55, 30)))
    box(px, 8, 10, 1, 1, 1, lambda f, i, j, w, h: rgba((255, 200, 60)))
    save(f'{C}/backsword.png', 32, 16, px)

# ------------------------------------------------------------------ animated capes
TAU = 6.283185

def fireworks(u, v, x, y, t, rnd):
    c = lerp((10, 8, 30), (30, 20, 60), v)
    bursts = [(0.25, 0.3, 0.0, (255, 80, 80)), (0.7, 0.45, 0.33, (90, 200, 255)), (0.45, 0.75, 0.66, (255, 220, 70))]
    for bx, by, start, col in bursts:
        age = (t - start) % 1.0
        r = age * 0.45
        d = math.hypot((u - bx) * 1.0, (v - by) * 1.4)
        if abs(d - r) < 0.045 and age < 0.8:
            ang = math.atan2(v - by, u - bx)
            if int((ang + math.pi) / TAU * 12) % 2 == 0:
                return lerp(col, c, age * 0.9)
        if d < 0.03 and age < 0.15:
            return (255, 255, 255)
    if (x * 31 + y * 17) % 53 == 0:
        return (200, 200, 230)                     # stars
    return c

def autumn(u, v, x, y, t, rnd):
    c = lerp((200, 110, 40), (120, 50, 20), v)
    leaves = [(0.15, (230, 60, 30)), (0.4, (250, 170, 40)), (0.62, (200, 90, 20)), (0.85, (240, 200, 60)), (0.3, (180, 40, 30))]
    for k, (lx, col) in enumerate(leaves):
        ly = (k * 0.21 + t) % 1.0
        sx = lx + 0.05 * math.sin((t + k * 0.3) * TAU)
        dx, dy = (u - sx) * 14, (v - ly) * 20
        if dx * dx + dy * dy < 1.2 and abs(dx - dy) < 1.1:
            return col
    return c

def bubbles(u, v, x, y, t, rnd):
    c = lerp((30, 170, 200), (10, 60, 120), v)
    for k in range(6):
        bx = (k * 0.17 + 0.07) % 1.0 + 0.03 * math.sin((t * 2 + k) * TAU)
        by = 1.0 - ((k * 0.29 + t) % 1.0)
        r = 0.06 + (k % 3) * 0.02
        d = math.hypot((u - bx), (v - by) * 0.75)
        if abs(d - r) < 0.018:
            return (230, 250, 255)
        if d < r * 0.35 and (u - bx) < 0 and (v - by) < 0:
            return (255, 255, 255)
    return c

def meteor(u, v, x, y, t, rnd):
    c = lerp((5, 5, 25), (25, 10, 50), v)
    if (x * 29 + y * 11) % 47 == 0:
        c = (220, 220, 255)
    for k in range(3):
        p = (t + k / 3) % 1.0
        hx, hy = 1.1 - p * 1.4 + k * 0.15, -0.1 + p * 1.2
        # trail along (+1, -0.85)
        dx, dy = u - hx, v - hy
        along = dx * 0.76 - dy * 0.65
        across = abs(dx * 0.65 + dy * 0.76)
        if 0 <= along < 0.35 and across < 0.02 + along * 0.02:
            return lerp((255, 255, 255), (120, 140, 255), along / 0.35)
    return c

if __name__ == '__main__':
    witchhat(); santahat(); chefhat(); foxears(); unicorn(); propeller()
    jetpack(); guitar(); backsword()
    ca.wing_texture('ice', ice)
    v2.wing_tint('ice', ice, 80)
    for n, fn in (('fireworks', fireworks), ('autumn', autumn), ('bubbles', bubbles), ('meteor', meteor)):
        ca.cape_frames(n, fn)
    print('ok')
