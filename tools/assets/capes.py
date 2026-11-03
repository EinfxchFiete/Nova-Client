import zlib, struct, math, random, colorsys, sys
OUT = sys.argv[1]
W, H = 64, 32

def png(path, px):
    raw = b''.join(b'\x00' + bytes(sum((list(px[y][x]) for x in range(W)), [])) for y in range(H))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def clamp(v): return max(0, min(255, int(v)))

# cape front face: x 1..10, y 1..16 (10x16). "u,v" = position inside the 22x17 cape area
def local(x, y):
    return (x % 22) / 21.0, min(y, 16) / 16.0

def star(px, cx, cy, col):
    for dx, dy in ((0,0),(1,0),(-1,0),(0,1),(0,-1)):
        x, y = cx+dx, cy+dy
        if 0 <= x < W and 0 <= y < H: px[y][x] = col + (255,)

def design(name):
    rnd = random.Random(name)
    px = [[(0,0,0,255)] * W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            u, v = local(x, y)
            n = rnd.random()
            if name == 'nova':
                c = lerp((40, 20, 90), (150, 90, 255), v)
                c = lerp(c, (20, 10, 40), 0.15 * n)
            elif name == 'galaxy':
                c = lerp((8, 6, 30), (40, 20, 80), (math.sin(u * 6 + v * 4) + 1) / 2 * 0.7)
                if n > 0.93: c = (230, 230, 255)
            elif name == 'fire':
                t = v + 0.15 * math.sin(u * 12 + v * 3)
                c = lerp((255, 220, 60), (180, 20, 10), max(0, min(1, t)))
            elif name == 'ocean':
                t = (math.sin(u * 10 + v * 14) + 1) / 2
                c = lerp((10, 60, 140), (40, 170, 220), t * 0.8 + 0.2 * v)
            elif name == 'forest':
                c = lerp((20, 90, 30), (60, 160, 60), (math.sin(v * 9) + 1) / 2)
                c = lerp(c, (10, 50, 20), 0.3 * n)
            elif name == 'gold':
                stripe = (x + y) % 6 < 2
                c = (250, 215, 90) if stripe else (205, 160, 40)
                c = lerp(c, (255, 245, 180), 0.15 * n)
            elif name == 'ice':
                c = lerp((200, 240, 255), (90, 170, 230), v)
                if (x * 7 + y * 3) % 11 == 0: c = (255, 255, 255)
            elif name == 'rainbow':
                r, g, b = colorsys.hsv_to_rgb((v * 0.85) % 1, 0.75, 1)
                c = (r * 255, g * 255, b * 255)
            elif name == 'cherry':
                c = lerp((255, 200, 220), (230, 110, 160), v)
                if n > 0.9: c = (255, 245, 250)
            elif name == 'lava':
                t = (math.sin(u * 9 + v * 5) + math.sin(v * 13)) / 4 + 0.5
                c = lerp((60, 10, 0), (255, 120, 0), t)
                if n > 0.94: c = (255, 230, 120)
            elif name == 'night':
                c = lerp((5, 5, 20), (25, 25, 70), v)
                if n > 0.95: c = (255, 255, 230)
            elif name == 'sunset':
                c = lerp((255, 190, 80), (120, 40, 140), v)
            elif name == 'matrix':
                col = (x * 7) % 5
                c = (0, int(60 + 180 * ((y + col * 3) % 6) / 5), 20) if (x + y * 3) % 4 else (10, 20, 10)
            elif name == 'checker':
                c = (30, 30, 36) if ((x // 2) + (y // 2)) % 2 else (235, 235, 240)
            else:
                c = (128, 128, 128)
            px[y][x] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255)
    # emblem on the back of the cape (back face x 12..21, y 1..16): a small 4-point star
    if name in ('nova', 'galaxy', 'ocean', 'forest', 'fire', 'gold', 'ice', 'cherry'):
        col = {'nova': (255, 255, 255), 'galaxy': (255, 230, 120), 'ocean': (220, 250, 255), 'forest': (220, 255, 200),
               'fire': (255, 255, 210), 'gold': (120, 70, 0), 'ice': (40, 110, 180), 'cherry': (160, 30, 90)}[name]
        cx, cy = 16, 7
        for d in range(4):
            for (dx, dy) in ((d, 0), (-d, 0), (0, d), (0, -d)):
                px[cy + dy][cx + dx] = col + (255,)
        px[cy-1][cx-1] = px[cy-1][cx+1] = px[cy+1][cx-1] = px[cy+1][cx+1] = col + (255,)
    if name == 'galaxy':
        for _ in range(10):
            star(px, rnd.randrange(1, 22), rnd.randrange(1, 17), (255, 255, 255))
    return px

for n in ['nova', 'galaxy', 'fire', 'ocean', 'forest', 'gold', 'ice', 'rainbow', 'cherry', 'checker', 'lava', 'night', 'sunset', 'matrix']:
    png(f'{OUT}/{n}.png', design(n))
print('ok')
