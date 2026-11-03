# Generates src/main/resources/assets/novaclient/icon.png (128x128, own design: 4-point star).
# Run from the repository root: python3 tools/assets/mod_icon.py
import zlib, struct, math
W=H=128
rows=[]
for y in range(H):
    row=bytearray([0])
    for x in range(W):
        cx,cy=x-63.5,y-63.5
        r=math.hypot(cx,cy)
        # dark rounded background
        inside = max(abs(cx),abs(cy))<60
        if not inside:
            row+=bytes([0,0,0,0]); continue
        t=(x+y)/254
        bg=(int(20+10*t),int(18+8*t),int(30+16*t),255)
        # 4-point star
        s=abs(cx)**0.5+abs(cy)**0.5
        if s<9.2:
            k=1-r/60
            c=(int(150+105*k),int(110+120*k),255,255)
            row+=bytes(c)
        else:
            row+=bytes(bg)
    rows.append(bytes(row))
raw=b''.join(rows)
def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',W,H,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
open('src/main/resources/assets/novaclient/icon.png','wb').write(png)
