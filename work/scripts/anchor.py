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
cnt = collections.Counter(); anchors = []
for off, va, fsz, fl in code:
    f.seek(off); buf = f.read(fsz)
    for pos in range(0, len(buf) - 32, 4):
        w16 = struct.unpack_from('<I', buf, pos + 16)[0]
        if (w16 & 0xFFFFFFE0) != 0x52800020:
            continue
        w0 = struct.unpack_from('<I', buf, pos)[0]
        w4 = struct.unpack_from('<I', buf, pos + 4)[0]
        w8 = struct.unpack_from('<I', buf, pos + 8)[0]
        w12 = struct.unpack_from('<I', buf, pos + 12)[0]
        w20 = struct.unpack_from('<I', buf, pos + 20)[0]
        w24 = struct.unpack_from('<I', buf, pos + 24)[0]
        w28 = struct.unpack_from('<I', buf, pos + 28)[0]
        cnt['anchor'] += 1
        if (w20 & 0xFFC00000) == 0x39000000: cnt['strb'] += 1
        if (w24 & 0xFFC00000) == 0xAD000000: cnt['stp_q'] += 1
        if (w28 & 0xFC000000) == 0x14000000: cnt['b'] += 1
        if (w0 & 0x9F000000) == 0x90000000: cnt['adrp@0'] += 1
        if (w4 & 0x9F000000) == 0x90000000: cnt['adrp@4'] += 1
        if (w8 & 0x9F000000) == 0x90000000: cnt['adrp@8'] += 1
        if (w4 & 0xFFC00000) == 0x3DC00000: cnt['ldrq@4'] += 1
        if (w8 & 0xFFC00000) == 0x3DC00000: cnt['ldrq@8'] += 1
        if (w12 & 0xFFC00000) == 0x3DC00000: cnt['ldrq@12'] += 1
        if len(anchors) < 15:
            anchors.append((off + pos, w0, w4, w8, w12, w16, w20, w24, w28))
    del buf
print(dict(cnt))
for a in anchors:
    print(" ".join("%08x" % x for x in a))
