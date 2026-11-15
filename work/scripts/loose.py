import struct, collections
path = r"E:\三角洲行动\work\game\libUE4.so"
f = open(path, 'rb'); hdr = f.read(64)
e_phoff = struct.unpack_from('<Q', hdr, 32)[0]
e_phentsize, e_phnum = struct.unpack_from('<HH', hdr, 54)
f.seek(e_phoff); ph = f.read(e_phentsize * e_phnum)
loads = []
for i in range(e_phnum):
    o = i * e_phentsize
    t = struct.unpack_from('<I', ph, o)[0]; fl = struct.unpack_from('<I', ph, o + 4)[0]
    off, va, pa, fsz, msz, al = struct.unpack_from('<QQQQQQ', ph, o + 8)
    if t == 1:
        loads.append((off, va, fsz, fl))
code = [l for l in loads if l[3] & 1 and l[2]]

def u32(b, o): return struct.unpack_from('<I', b, o)[0]
def adrp(w): return (w & 0x9F000000) == 0x90000000
def loadq(w): return (w & 0xFFC00000) == 0x3DC00000

cnt = collections.Counter(); samples = []
for off, va, fsz, fl in code:
    f.seek(off); buf = f.read(fsz)
    for pos in range(0, len(buf) - 32, 4):
        w16 = u32(buf, pos + 16)
        if (w16 & 0xFFFFFFE0) != 0x52800020:
            continue
        w = [u32(buf, pos + 4 * k) for k in range(8)]
        cnt['A_anchor'] += 1
        a = adrp(w[0]) and adrp(w[1]) and loadq(w[2]) and loadq(w[3])
        b = adrp(w[0]) and loadq(w[1]) and adrp(w[2]) and loadq(w[3])
        if a or b:
            cnt['pairs'] += 1
            if (w[5] & 0xFFC00000) == 0x39000000 and (w[6] & 0xFFC00000) == 0xAD000000 and (w[7] & 0xFC000000) == 0x14000000:
                cnt['pairs+strb+stp+b'] += 1
                if len(samples) < 10:
                    samples.append((off + pos, [hex(x) for x in w]))
print(dict(cnt))
for s in samples:
    print(s[0], s[1])

# synthetic self-test of the matcher core
def match(buf, pos):
    w = [u32(buf, pos + 4 * k) for k in range(8)]
    if adrp(w[0]) and adrp(w[1]) and loadq(w[2]) and loadq(w[3]):
        adrp1, ldr1, adrp2, ldr2 = w[0], w[3], w[1], w[2]
        j2 = 4
    elif adrp(w[0]) and loadq(w[1]) and adrp(w[2]) and loadq(w[3]):
        adrp1, ldr1, adrp2, ldr2 = w[0], w[1], w[2], w[3]
        j2 = 8
    else:
        return None
    if ((ldr1 >> 5) & 31) != (adrp1 & 31): return None
    if ((ldr2 >> 5) & 31) != (adrp2 & 31): return None
    if (w[5] & 0xFFC00000) != 0x39000000 or (w[5] & 31) != (w[4] & 31): return None
    if (w[6] & 0xFFC00000) != 0xAD000000: return None
    if (w[7] & 0xFC000000) != 0x14000000: return None
    i3, i4 = ldr1 & 31, ldr2 & 31
    i5, i6 = w[6] & 31, (w[6] >> 10) & 31
    if i3 == i4: return None
    if not ((i5 == i3 and i6 == i4) or (i5 == i4 and i6 == i3)): return None
    return (adrp1, ldr1, adrp2, ldr2, j2)

# adrp x8, #0x1000 ; ldr q0,[x8] ; adrp x9,#0x2000 ; ldr q1,[x9] ;
# movz w0,#1 ; strb w0,[x1] ; stp q0,q1,[sp] ; b .-4
syn = struct.pack('<8I', 0x90000028, 0x3DC00100, 0x90000049, 0x3DC00121,
                  0x52800020, 0x39000020, 0xAD0003E0, 0x17FFFFFF)
print("selftest:", match(syn, 0))
