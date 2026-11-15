import re
path = r"E:\三角洲行动\work\game\libUE4.so"
f = open(path, 'rb')
pat = re.compile(rb'[\x20-\x7e]{6,}')
targets = [b'++UE4+Release', b'UnrealEngine', b'PerfGear', b'DFMQualityLevel', b'DFMFPSLevel',
           b'QualityFPSSetting', b'DFMPerfGearConfig', b'SuperResolution', b'UltraHighResolution',
           b'PadSize', b'UserGameplay', b'Engine.ini', b'Game.ini', b'Token', b'Hash',
           b'AES', b'HMAC', b'EVP_aes', b'PerfGearAndroidPadSizeConfig']
found = {t: 0 for t in targets}
samples = {t: [] for t in targets}
SNIP = 60
carry = b''
offset = 0
while True:
    buf = f.read(1 << 22)
    if not buf:
        break
    data = carry + buf
    for m in pat.finditer(data):
        s = m.group()
        for t in targets:
            if t in s:
                found[t] += 1
                if len(samples[t]) < 4:
                    i = s.find(t)
                    samples[t].append((offset + m.start() - len(carry), s[max(0, i - 20):i + SNIP]))
    carry = data[-64:]
    offset += len(buf)
for t in targets:
    print("%-32s %6d  %s" % (t.decode(), found[t], [ (hex(a), b.decode('latin1')[:90]) for a, b in samples[t] ]))
