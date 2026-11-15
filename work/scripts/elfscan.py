import struct, sys, re, json, os

path = r"E:\三角洲行动\work\game\libUE4.so"
f = open(path, 'rb')
data_len = os.path.getsize(path)

hdr = f.read(64)
assert hdr[:4] == b'\x7fELF' and hdr[4] == 2 and hdr[5] == 1
e_type = struct.unpack_from('<H', hdr, 16)[0]
e_machine = struct.unpack_from('<H', hdr, 18)[0]
e_phoff = struct.unpack_from('<Q', hdr, 32)[0]
e_shoff = struct.unpack_from('<Q', hdr, 40)[0]
e_phentsize, e_phnum = struct.unpack_from('<HH', hdr, 54)
e_shentsize, e_shnum = struct.unpack_from('<HH', hdr, 58)

print("ELF: type=%d machine=%d phnum=%d shnum=%d size=%d" % (e_type, e_machine, e_phnum, e_shnum, data_len))

# sanity-check against the app's header validator: e_type 3, machine 183, phnum==64
print("header check (app): e_type=%d machine=%d phentsize=%d" % (e_type, e_machine, e_phentsize))

loads = []
f.seek(e_phoff)
ph = f.read(e_phentsize * e_phnum)
for i in range(e_phnum):
    o = i * e_phentsize
    p_type = struct.unpack_from('<I', ph, o)[0]
    p_flags = struct.unpack_from('<I', ph, o + 4)[0]
    p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = struct.unpack_from('<QQQQQQ', ph, o + 8)
    if p_type == 1:
        loads.append((p_offset, p_vaddr, p_filesz, p_memsz, p_flags))

for l in loads:
    print("LOAD off=0x%x va=0x%x filesz=0x%x memsz=0x%x flags=%d" % l)

sections = []
if e_shoff and e_shnum and e_shnum <= 1024:
    f.seek(e_shoff)
    sh = f.read(e_shentsize * e_shnum)
    for i in range(e_shnum):
        o = i * e_shentsize
        sh_name, sh_type = struct.unpack_from('<II', sh, o)
        sh_flags, sh_addr, sh_offset, sh_size = struct.unpack_from('<QQQQ', sh, o + 8)
        sections.append((sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size))
    # section name table
    if sections:
        stroff = sections[0][4]
        strsize = sections[0][5]
        f.seek(stroff)
        shstr = f.read(strsize)
        def nm(x):
            if x >= len(shstr): return ''
            e = shstr.find(b'\0', x)
            return shstr[x:e].decode('utf-8', 'replace')
        for s in sections:
            n = nm(s[0])
            if n in ('.text', '.rodata', '.comment', '.note.gnu.build-id', '.data', '.init_array'):
                print("SECTION %-20s type=%d flags=0x%x addr=0x%x off=0x%x size=0x%x" % (n, s[1], s[2], s[3], s[4], s[5]))

# Executable scan regions = PT_LOAD with PF_X
code = [(o, va, fsz) for (o, va, fsz, msz, fl) in loads if fl & 1 and fsz]
total = sum(c[2] for c in code)
print("exec bytes:", total)
