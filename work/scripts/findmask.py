import struct, os, sys

path = r"E:\三角洲行动\work\game\libUE4.so"
size = os.path.getsize(path)
f = open(path, 'rb')
hdr = f.read(64)
e_phoff = struct.unpack_from('<Q', hdr, 32)[0]
e_phentsize, e_phnum = struct.unpack_from('<HH', hdr, 54)
f.seek(e_phoff); ph = f.read(e_phentsize * e_phnum)
loads = []
for i in range(e_phnum):
    o = i * e_phentsize
    p_type = struct.unpack_from('<I', ph, o)[0]
    p_flags = struct.unpack_from('<I', ph, o + 4)[0]
    p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from('<QQQQQQ', ph, o + 8)
    if p_type == 1:
        loads.append(dict(off=p_offset, va=p_vaddr, fsz=p_filesz, flags=p_flags))
code = [l for l in loads if l['flags'] & 1 and l['fsz']]


def u32(b, o):
    return struct.unpack_from('<I', b, o)[0]


def adrp(w): return (w & 0x9F000000) == 0x90000000
def loadq(w): return (w & 0xFFC00000) == 0x3DC00000


def signext(v, bits):
    m = 1 << (bits - 1)
    v &= (1 << bits) - 1
    return (v ^ m) - m


def adpr_addr(w, pc):
    imm = signext((((w >> 29) & 3) | (((w >> 5) & 0x7FFFF) << 2)), 21) << 12
    return (pc & ~0xFFF) + imm


def data_off(va, n):
    res = -1
    for l in loads:
        if va >= l['va'] and va - l['va'] + n <= l['fsz']:
            add = l['off'] + (va - l['va'])
            if res >= 0 and res != add:
                raise Exception('ambiguous mapping')
            if l['flags'] & 2:
                return -1
            res = add
    return res


cands = []
LEG1, LEG2 = 24240560, 24385264
for reg in code:
    base = reg['off']; vbase = reg['va']; n = reg['fsz']
    f.seek(base)
    chunk = f.read(n)
    assert len(chunk) == n
    pos = 0
    while pos + 32 <= len(chunk):
        w16 = u32(chunk, pos + 16)
        key = (0x01010101 * (((w16 >> 24) ^ 0x52) & 0xFF)) & 0xFFFFFFFF
        if ((w16 ^ key) & 0xFFFFFFE0) != 0x52800020:
            pos += 4
            continue
        w = [u32(chunk, pos + 4 * k) ^ key for k in range(8)]
        if adrp(w[0]) and adrp(w[1]) and loadq(w[2]) and loadq(w[3]):
            adrp1, ldr1, adrp2, ldr2 = w[0], w[2], w[1], w[3]
            j2 = 4 + vbase + pos
        elif adrp(w[0]) and loadq(w[1]) and adrp(w[2]) and loadq(w[3]):
            adrp1, ldr1, adrp2, ldr2 = w[0], w[1], w[2], w[3]
            j2 = 8 + vbase + pos
        else:
            pos += 4
            continue
        if ((ldr1 >> 5) & 31) != (adrp1 & 31): pos += 4; continue
        if ((ldr2 >> 5) & 31) != (adrp2 & 31): pos += 4; continue
        if (w[5] & 0xFFC00000) != 0x39000000: pos += 4; continue
        if (w[5] & 31) != (w[4] & 31): pos += 4; continue
        if (w[6] & 0xFFC00000) != 0xAD000000: pos += 4; continue
        if (w[7] & 0xFC000000) != 0x14000000: pos += 4; continue
        i3 = ldr1 & 31; i4 = ldr2 & 31; i5 = w[6] & 31; i6 = (w[6] >> 10) & 31
        if i3 == i4: pos += 4; continue
        if not ((i5 == i3 and i6 == i4) or (i5 == i4 and i6 == i3)): pos += 4; continue
        disp = signext(w[7] & 0x3FFFFFF, 26) << 2
        if disp >= 0 or disp < -4096: pos += 4; continue
        a1 = adpr_addr(adrp1, vbase + pos) + ((ldr1 >> 10) & 0xFFF) * 16
        a2 = adpr_addr(adrp2, j2) + ((ldr2 >> 10) & 0xFFF) * 16
        o1 = data_off(a1, 16); o2 = data_off(a2, 16)
        if o1 < 0 or o2 < 0: pos += 4; continue
        first, second = (o1, o2) if i5 == i3 else (o2, o1)
        cands.append((base + pos, first, second, a1, a2))
        pos += 4
    del chunk

print("candidates:", len(cands))
for c in cands:
    pc, o1, o2, a1, a2 = c
    print("  pc=0x%x  seg1=0x%x(%d) seg2=0x%x(%d) va1=0x%x va2=0x%x" % (pc, o1, o1, o2, o2, a1, a2))
    f.seek(o1); b1 = f.read(16)
    f.seek(o2); b2 = f.read(16)
    print("    seg1=%s seg2=%s" % (b1.hex(), b2.hex()))
    print("    xor00=%s" % (b1 + b2).hex())
    print("    xorE2=%s" % bytes(x ^ 0xE2 for x in b1 + b2).hex())

print("legacy offsets 0x%x 0x%x" % (LEG1, LEG2))
f.seek(LEG1); b1 = f.read(16)
f.seek(LEG2); b2 = f.read(16)
print("  legacy seg1=%s seg2=%s" % (b1.hex(), b2.hex()))
print("  legacy xor00=%s" % (b1 + b2).hex())
print("  legacy xorE2=%s" % bytes(x ^ 0xE2 for x in b1 + b2).hex())
