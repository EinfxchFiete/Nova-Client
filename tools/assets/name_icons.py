"""Built-in name icons for Nova Client (shown in front of your own name): bolt, heart, star, flame, diamond.
Each file is 32x64: two 32x32 frames below each other (normal look on top, shiny variant below) - the same
layout players can use for their own icons in config/novaclient/icons/.
Usage: python3 -B tools/assets/name_icons.py src/main/resources/assets/novaclient/textures/icons
Own designs, no assets of other games or clients."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cosmetic_assets import canvas, save

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
S = 32
SS = 4  # supersampling

def inside_poly(pts, x, y):
    c = False
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c

def render(shape, colour):
    """shape(x, y) -> bool in 0..1 coordinates; colour(x, y) -> (r, g, b). Dark outline around the shape."""
    def cov(px, py, grow=0.0):
        hit = 0
        for sy in range(SS):
            for sx in range(SS):
                x = (px + (sx + 0.5) / SS) / S
                y = (py + (sy + 0.5) / SS) / S
                if grow:
                    ok = any(shape(x + dx * grow, y + dy * grow) for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (.7, .7), (-.7, .7), (.7, -.7), (-.7, -.7)))
                else:
                    ok = shape(x, y)
                hit += ok
        return hit / (SS * SS)
    px = canvas(S, S)
    for y in range(S):
        for x in range(S):
            a = cov(x, y)
            o = cov(x, y, 0.05)
            if o <= 0:
                continue
            r, g, b = colour((x + 0.5) / S, (y + 0.5) / S)
            dark = (r * 0.25, g * 0.25, b * 0.25)
            t = a / o if o else 0
            px[y][x] = (int(dark[0] + (r - dark[0]) * t), int(dark[1] + (g - dark[1]) * t), int(dark[2] + (b - dark[2]) * t), int(255 * o))
    return px

def grad(top, bottom):
    return lambda x, y: tuple(top[i] + (bottom[i] - top[i]) * y for i in range(3))

def shiny(top, bottom):
    def c(x, y):
        base = grad(top, bottom)(x, y)
        s = max(0.0, 1 - abs((x + y) - 0.75) * 6)   # diagonal highlight
        return tuple(base[i] + (255 - base[i]) * s * 0.8 for i in range(3))
    return c

BOLT = [(0.62, 0.06), (0.2, 0.55), (0.45, 0.55), (0.34, 0.94), (0.8, 0.42), (0.54, 0.42)]
def bolt(x, y): return inside_poly(BOLT, x, y)
def heart(x, y):
    X, Y = (x - 0.5) * 2.6, (0.48 - y) * 2.6
    return (X * X + Y * Y - 1) ** 3 - X * X * Y ** 3 <= 0
def star(x, y):
    pts = []
    for i in range(10):
        r = 0.44 if i % 2 == 0 else 0.19
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((0.5 + math.cos(a) * r, 0.52 + math.sin(a) * r))
    return inside_poly(pts, x, y)
FLAME = [(0.5, 0.05), (0.72, 0.36), (0.66, 0.5), (0.8, 0.44), (0.82, 0.7), (0.66, 0.92), (0.34, 0.92), (0.18, 0.7), (0.24, 0.44), (0.38, 0.52), (0.36, 0.3)]
def flame(x, y): return inside_poly(FLAME, x, y)
DIAMOND = [(0.28, 0.14), (0.72, 0.14), (0.92, 0.4), (0.5, 0.92), (0.08, 0.4)]
def diamond(x, y): return inside_poly(DIAMOND, x, y)

ICONS = {
    'blitz': (bolt, grad((255, 70, 70), (230, 30, 50)), shiny((255, 225, 90), (255, 160, 20))),
    'herz': (heart, grad((255, 90, 120), (220, 30, 70)), shiny((255, 150, 200), (240, 70, 160))),
    'stern': (star, grad((255, 225, 90), (250, 170, 30)), shiny((200, 240, 255), (110, 170, 255))),
    'flamme': (flame, grad((255, 220, 80), (240, 70, 20)), shiny((140, 230, 255), (40, 110, 255))),
    'diamant': (diamond, grad((150, 245, 255), (40, 170, 230)), shiny((230, 170, 255), (150, 70, 240))),
}

if __name__ == '__main__':
    for name, (shape, c1, c2) in ICONS.items():
        a = render(shape, c1)
        b = render(shape, c2)
        save(f'{OUT}/{name}.png', S, S * 2, a + b)
    print('ok')
