"""Static scan: which mixins of the given mods target classes that Nova Client mixes into (reads class files only)."""
import json, os, sys, glob, struct
root = sys.argv[1]
OURS = {
 'net/minecraft/client/player/AbstractClientPlayer': 'getSkin', 'net/minecraft/client/renderer/entity/player/AvatarRenderer': 'submitNameDisplay',
 'net/minecraft/client/Camera': 'calculateFov, alignWithEntity', 'net/minecraft/client/multiplayer/ClientPacketListener': 'handlePongResponse, handleSetTime, handleEntityEvent',
 'net/minecraft/client/gui/components/ChatComponent': 'addMessage', 'net/minecraft/world/entity/Entity': 'turn',
 'net/minecraft/client/renderer/entity/EntityRenderer': 'getNameTag, shouldRender', 'net/minecraft/client/renderer/entity/ItemEntityRenderer': 'item physics',
 'net/minecraft/client/renderer/entity/state/ItemEntityRenderState': 'item physics', 'net/minecraft/client/renderer/GameRenderer': 'bobHurt',
 'net/minecraft/client/gui/Hud': 'extractTextureOverlay, extractVignette', 'net/minecraft/client/KeyboardHandler': 'keyPress', 'net/minecraft/world/level/Level': 'getRainLevel',
 'net/minecraft/client/renderer/LevelRenderer': 'submitBlockOutline', 'net/minecraft/client/renderer/LightmapRenderStateExtractor': 'extract',
 'net/minecraft/client/renderer/entity/LivingEntityRenderer': 'shouldShowName', 'net/minecraft/client/Minecraft': 'createTitle', 'net/minecraft/client/MouseHandler': 'onButton, onScroll',
 'net/minecraft/client/model/player/PlayerModel': 'setupAnim', 'net/minecraft/client/gui/components/PlayerTabOverlay': 'getNameForDisplay, extractPingIcon, extractRenderState',
 'net/minecraft/client/renderer/ScreenEffectRenderer': 'submitFire, submitWater', 'net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher': 'tryExtractRenderState',
 'net/minecraft/client/particle/ParticleGroup': 'add', 'net/minecraft/client/renderer/fog/FogRenderer': 'setupFog',
 'net/minecraft/client/renderer/entity/FirstPersonHandsAndItemsRenderer': 'submitArmWithItem', 'net/minecraft/world/scores/PlayerScoreEntry': 'scoreboard numbers',
}
def parse(path):
    d = open(path, 'rb').read()
    pos = 8
    n = struct.unpack('>H', d[pos:pos+2])[0]; pos += 2
    cp = [None] * n
    i = 1
    while i < n:
        tag = d[pos]; pos += 1
        if tag == 1:
            l = struct.unpack('>H', d[pos:pos+2])[0]; cp[i] = ('utf8', d[pos+2:pos+2+l].decode('utf8', 'replace')); pos += 2 + l
        elif tag in (3, 4): cp[i] = ('num', d[pos:pos+4]); pos += 4
        elif tag in (5, 6): cp[i] = ('num', d[pos:pos+8]); pos += 8; i += 1
        elif tag == 7: cp[i] = ('class', struct.unpack('>H', d[pos:pos+2])[0]); pos += 2
        elif tag == 8: cp[i] = ('str', struct.unpack('>H', d[pos:pos+2])[0]); pos += 2
        elif tag in (9, 10, 11, 12, 17, 18): pos += 4
        elif tag == 15: pos += 3
        elif tag in (16, 19, 20): pos += 2
        else: raise ValueError(tag)
        i += 1
    u = lambda k: cp[k][1]
    pos += 6
    ni = struct.unpack('>H', d[pos:pos+2])[0]; pos += 2 + 2 * ni
    def value(p):
        t = chr(d[p]); p += 1
        if t in 'BCDFIJSZs': return ('s', u(struct.unpack('>H', d[p:p+2])[0]) if t == 's' else None), p + 2
        if t == 'e': return ('e', None), p + 4
        if t == 'c': return ('c', u(struct.unpack('>H', d[p:p+2])[0])), p + 2
        if t == '@': a, p = ann(p); return ('@', a), p
        if t == '[':
            k = struct.unpack('>H', d[p:p+2])[0]; p += 2; out = []
            for _ in range(k):
                v, p = value(p); out.append(v)
            return ('[', out), p
        raise ValueError(t)
    def ann(p):
        typ = u(struct.unpack('>H', d[p:p+2])[0]); k = struct.unpack('>H', d[p+2:p+4])[0]; p += 4; el = {}
        for _ in range(k):
            name = u(struct.unpack('>H', d[p:p+2])[0]); v, p = value(p + 2); el[name] = v
        return (typ, el), p
    def attrs(p):
        k = struct.unpack('>H', d[p:p+2])[0]; p += 2; out = []
        for _ in range(k):
            name = u(struct.unpack('>H', d[p:p+2])[0]); l = struct.unpack('>I', d[p+2:p+6])[0]
            if name in ('RuntimeInvisibleAnnotations', 'RuntimeVisibleAnnotations'):
                q = p + 6; c = struct.unpack('>H', d[q:q+2])[0]; q += 2
                for _ in range(c):
                    a, q = ann(q); out.append(a)
            p += 6 + l
        return out, p
    def members(p):
        k = struct.unpack('>H', d[p:p+2])[0]; p += 2; out = []
        for _ in range(k):
            name = u(struct.unpack('>H', d[p+2:p+4])[0]); a, p = attrs(p + 6); out.append((name, a))
        return out, p
    fields, pos = members(pos)
    methods, pos = members(pos)
    cls, pos = attrs(pos)
    return cls, methods
def strings(v):
    if v is None: return []
    t, x = v
    if t in ('s', 'c'): return [x]
    if t == '[': return sum((strings(y) for y in x), [])
    return []
for mod in sorted(os.listdir(root)):
    classes = []
    for cfg in glob.glob(f'{root}/{mod}/**/*.mixins.json', recursive=True):
        try: c = json.load(open(cfg))
        except Exception: continue
        pkg = c.get('package', '')
        for key in ('mixins', 'client', 'server'):
            classes += [pkg + '.' + m for m in (c.get(key) or [])]
    hits = []
    for name in classes:
        f = f'{root}/{mod}/' + name.replace('.', '/') + '.class'
        if not os.path.exists(f): continue
        cls, methods = parse(f)
        targets = []
        for typ, el in cls:
            if typ == 'Lorg/spongepowered/asm/mixin/Mixin;':
                targets += [t[1:-1] if t.startswith('L') else t for t in strings(el.get('value'))]
                targets += [t.replace('.', '/') for t in strings(el.get('targets'))]
        for t in targets:
            if t in OURS:
                used = []
                for mname, anns in methods:
                    for typ, el in anns:
                        if typ.startswith('Lorg/spongepowered/') or typ.startswith('Lcom/llamalad7/'):
                            kind = typ.split('/')[-1][:-1]
                            ms = strings(el.get('method'))
                            if kind == 'Overwrite': ms = [mname]
                            if ms or kind in ('Overwrite',):
                                used.append(f'{kind}:{",".join(ms)}')
                hits.append((t.split('/')[-1], name.split('.')[-1], used))
    print(f'== {mod}: {len(classes)} mixins, {len(hits)} on classes Nova also changes')
    for t, n, used in hits:
        print(f'   {t} <- {n}: {used[:10]}   (Nova: {OURS["net/minecraft/" + ""] if False else ""})')
