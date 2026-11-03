# Builds launcher/resources/icon.ico (PNG entries 256/64/48/32/16) from launcher/resources/icon.png.
# Run from launcher/: python3 ../tools/assets/make_ico.py
import zlib, struct
def read_png(path):
    d=open(path,'rb').read(); i=8; idat=b''
    while i<len(d):
        l=struct.unpack('>I',d[i:i+4])[0]; t=d[i+4:i+8]; c=d[i+8:i+8+l]
        if t==b'IHDR': w,h=struct.unpack('>II',c[:8])
        if t==b'IDAT': idat+=c
        i+=12+l
    raw=zlib.decompress(idat); stride=w*4; rows=[]; prev=bytearray(stride); p=0
    for y in range(h):
        f=raw[p]; p+=1; line=bytearray(raw[p:p+stride]); p+=stride
        for x in range(stride):
            a=line[x-4] if x>=4 else 0; b=prev[x]; c=prev[x-4] if x>=4 else 0
            if f==1: line[x]=(line[x]+a)&255
            elif f==2: line[x]=(line[x]+b)&255
            elif f==3: line[x]=(line[x]+(a+b)//2)&255
            elif f==4:
                pa=abs(b-c); pb=abs(a-c); pc=abs(a+b-2*c)
                line[x]=(line[x]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
        rows.append(bytes(line)); prev=line
    return w,h,rows
def png_bytes(w,h,rows):
    raw=b''.join(b'\x00'+r for r in rows)
    def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
W,H,rows=read_png('resources/icon.png')
images=[]
for s in (256,64,48,32,16):
    sr=[bytes(b''.join(rows[y*H//s][ (x*W//s)*4:(x*W//s)*4+4] for x in range(s))) for y in range(s)]
    images.append((s,png_bytes(s,s,sr)))
hdr=struct.pack('<HHH',0,1,len(images)); off=6+16*len(images); dirs=b''; data=b''
for s,b in images:
    dirs+=struct.pack('<BBBBHHII',0 if s==256 else s,0 if s==256 else s,0,0,1,32,len(b),off+len(data)); data+=b
open('resources/icon.ico','wb').write(hdr+dirs+data)
print('ico ok', len(hdr+dirs+data))
