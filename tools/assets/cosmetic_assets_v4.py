"""Nova cosmetics, fourth batch (1.8.0): animated phoenix wings (4 frames, closed flame feathers) and more.
Usage: python3 -B tools/assets/cosmetic_assets_v4.py src/main/resources/assets/novaclient/textures
Own designs, no Mojang assets."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cosmetic_assets as ca
import cosmetic_assets_v2 as v2
from cosmetic_assets import canvas, rgba, lerp, box, save

OUT = sys.argv[1]
ca.OUT = OUT
v2.OUT = OUT
C = f'{OUT}/cosmetics'
TAU = 6.283185

def wing_png(path, shade):
    """A 16x16 wing plane: front face at u 0..15, back face (mirrored) at u 16..31 of a 64x32 texture."""
    px = canvas(64, 32)
    for face_x in (0, 16):
        for y in range(16):
            for x in range(16):
                c = shade(x, y)
                if c is not None:
                    px[y][face_x + (x if face_x == 0 else 15 - x)] = c
    save(path, 64, 32, px)

def phoenix(frame, grey=False):
    """Closed flame feathers: no holes, rounded feather tips, colours from a hot yellow core to red tips.
    The flames flicker a little from frame to frame (tips and brightness move)."""
    def shade(x, y):
        # top edge: high at the spine, slightly rising towards the tip
        top = 2.2 - x * 0.13
        # feather length: short at the spine, longest in the middle/outer part
        base = 8.5 + 6.0 * math.sin(math.pi * min(1.0, (x + 1) / 13.0) * 0.85)
        # rounded feather tips, 3 px wide, plus a little flicker per frame
        k = (x % 3) / 2.0
        tip = (1 - math.cos(TAU * (x % 3 + 0.5) / 3)) * 0.8
        flicker = 0.9 * math.sin(frame * TAU / 4 + x * 1.3)
        bottom = min(15.0, base - tip + flicker)
        if y < top or y > bottom:
            return None
        t = (y - top) / max(1.0, bottom - top)      # 0 at the top, 1 at the tip
        c = lerp((255, 248, 170), (255, 176, 30), min(1.0, t * 1.7))
        if t > 0.55:
            c = lerp(c, (240, 60, 20), (t - 0.55) / 0.45)
        # moving glow waves (fire): brighter bands run down the feathers
        wave = 0.94 + 0.10 * math.sin(y * 0.9 - frame * TAU / 4 + x * 0.35)
        c = (min(255, c[0] * wave), min(255, c[1] * wave), min(255, c[2] * wave))
        # feather lines
        if x % 3 == 2 and t > 0.25:
            c = lerp(c, (230, 95, 20), 0.35)
        alpha = 255 if bottom - y > 0.8 else 190
        if grey:
            l = (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255
            v = 90 + 165 * l
            return rgba((v, v, v), alpha)
        return rgba(c, alpha)
    return shade

if __name__ == '__main__':
    for f in range(4):
        wing_png(f'{C}/wings_phoenix_f{f}.png', phoenix(f))
        wing_png(f'{C}/wings_phoenix_tint_f{f}.png', phoenix(f, grey=True))
    print('ok')
