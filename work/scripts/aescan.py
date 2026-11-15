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
cnt = collections.Counter(); hits = []
for off, va, fsz, fl in code:
    f.seek(off); buf = f.read(fsz)
    for pos in range(0, len(buf) - 4, 4):
        w = struct.unpack_from('<I', buf, pos)[0]
        m = w & 0xFFFFFC00
        if m in (0x4E284800, 0x4E285800, 0x4E286800, 0x4E287800):
            cnt[hex(m)] += 1
            if len(hits) < 6:
                hits.append((hex(off + pos), hex(w)))
    del buf
print("ARMv8 crypto AES opcodes:", dict(cnt))
print("samples:", hits)

data = open(path, 'rb').read()
for label, off in (("PerfGear narrow", 0x15bf78c), ("SmallPadConfig", 0x231f9fa), ("SuperResolution", 0x1a218c5), ("SystemSettings", 0x16e3646)):
    chunk = data[off - 40:off + 60]
    print("%-18s %#x  raw=%s" % (label, off, chunk.hex()))
    print("%-18s       dec=%r" % ("", bytes(c ^ 0x10 for c in chunk)))

sha1iv = bytes.fromhex("0123456789abcdeffedcba9876543210")
print("SHA1 h0..h3 little-endian:", hex(data.find(sha1iv)) if data.find(sha1iv) >= 0 else '-')
print("SHA1 h0..h3 big-endian   :", hex(data.find(bytes.fromhex('67452301efcdab8998badcfe10325476'))) if data.find(bytes.fromhex('67452301efcdab8998badcfe10325476')) >= 0 else '-')
