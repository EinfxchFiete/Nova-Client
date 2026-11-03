# Generates launcher/resources/default-skin.png (own "Nova hoodie" skin, offline fallback) and launcher/resources/icon.png (256x256).
# Run from launcher/: python3 ../tools/assets/launcher_assets.py
import zlib, struct, math
def png(path, W, H, px):
    raw = b''.join(b'\x00' + bytes(sum((list(px[y][x]) for x in range(W)), [])) for y in range(H))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

# ---- default skin 64x64 (own design: purple hoodie)
W=H=64
px=[[(0,0,0,0)]*W for _ in range(H)]
def rect(x,y,w,h,c):
    for j in range(y,y+h):
        for i in range(x,x+w): px[j][i]=c+(255,)
SKIN=(224,172,140); HAIR=(58,40,30); HOOD=(110,60,200); HOOD2=(140,90,230); PANTS=(40,40,52); SHOE=(25,25,30)
rect(0,0,32,16,SKIN)                       # head faces
for fx in (8,16): rect(fx,0,8,8,HAIR)      # top/bottom (bottom = chin, keep skin)
rect(16,0,8,8,SKIN)
for fx in (0,8,16,24): rect(fx,8,8,2,HAIR) # hair fringe on all sides
rect(24,8,8,8,HAIR); rect(0,8,8,6,HAIR); rect(16,8,8,6,HAIR)
rect(9,12,2,1,(255,255,255)); rect(13,12,2,1,(255,255,255)); rect(10,12,1,1,(90,50,180)); rect(13,12,1,1,(90,50,180))
rect(11,14,2,1,(180,110,90))
rect(16,16,24,16,HOOD); rect(20,20,8,12,HOOD2); rect(23,21,2,6,(230,230,240))   # body + zipper
rect(40,16,16,16,HOOD); rect(40,30,16,2,SKIN)                                    # right arm + hand
rect(32,48,16,16,HOOD); rect(32,62,16,2,SKIN)                                    # left arm + hand
rect(0,16,16,16,PANTS); rect(0,30,16,2,SHOE)                                     # right leg
rect(16,48,16,16,PANTS); rect(16,62,16,2,SHOE)                                   # left leg
# fix top/bottom faces of limbs to plain colours
rect(44,16,8,4,HOOD); rect(36,48,8,4,HOOD); rect(4,16,8,4,PANTS); rect(20,48,8,4,PANTS)
png('resources/default-skin.png',64,64,px)

# ---- icon 256x256
W=H=256
px=[]
for y in range(H):
    row=[]
    for x in range(W):
        cx,cy=x-127.5,y-127.5
        if max(abs(cx),abs(cy))>=120:
            row.append((0,0,0,0)); continue
        # rounded corners
        if abs(cx)>96 and abs(cy)>96 and math.hypot(abs(cx)-96,abs(cy)-96)>24:
            row.append((0,0,0,0)); continue
        t=(x+y)/510
        bg=(int(22+20*t),int(16+10*t),int(40+30*t),255)
        s=abs(cx)**0.5+abs(cy)**0.5
        if s<13.5:
            k=1-math.hypot(cx,cy)/120
            row.append((int(160+95*k),int(110+130*k),255,255))
        else:
            row.append(bg)
    px.append(row)
png('resources/icon.png',256,256,px)
print('ok')
