import re
path = r"E:\三角洲行动\work\game\libUE4.so"
data = open(path, 'rb').read()

def xor10(s):
    return bytes(c ^ 0x10 for c in s.encode())

needles = ["PerfGear", "PerfGearAndroidPadSizeConfig", "DFMQualityLevel", "DFMFPSLevel",
           "QualityFPSSetting", "SystemSettings", "Engine.ini", "Game.ini", "UserGameplay.ini",
           "SmallPadConfig", "SuperResolution", "DFMPerfGearConfig", "DeltaForce"]
print("== XOR 0x10 encoded UE name candidates ==")
for n in needles:
    b = xor10(n)
    i = data.find(b)
    print("%-34s encoded=%-40r found_at=%s count=%d" % (n, b, hex(i) if i >= 0 else '-', data.count(b)))

print("\n== plain ASCII ==")
for n in ["PerfGear", "DFMQualityLevel", "SuperResolution", "UserGameplay", "DeltaForce", "Saved/Config"]:
    print("%-22s %s count=%d" % (n, hex(data.find(n.encode())) if data.find(n.encode()) >= 0 else '-', data.count(n.encode())))

print("\n== crypto primitives ==")
sbox = bytes.fromhex("637c777bf26b6fc53001672bfed7ab76")
print("AES sbox prefix        :", hex(data.find(sbox)) if data.find(sbox) >= 0 else '-', data.count(sbox))
sha1_iv = bytes.fromhex("0123456789abcdeffedcba9876543210c3d2e1f0")
print("SHA1 IV(le bytes)      :", hex(data.find(bytes.fromhex("67452301efcdab8998badcfe10325476c3d2e1f0"))) if data.find(bytes.fromhex("67452301efcdab8998badcfe10325476c3d2e1f0")) >= 0 else '-')
print("HMAC ipad/opad 0x3636  :", data.count(b"\x36" * 8), data.count(b"\x5c" * 8))
print("base64 alphabet        :", hex(data.find(b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")) if data.find(b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/") >= 0 else '-')

print("\n== UE build id strings ==")
for m in re.finditer(rb'\+{2}UE[45]\+Release-[0-9.]+', data):
    print("  ", hex(m.start()), m.group().decode())
    if m.start() > 0 and len(list(re.finditer(rb'\+{2}UE[45]\+Release-[0-9.]+', data[:m.start()]))) > 8:
        break
print("\n== material surroundings (0x1559300, 0x157d9c0) ==")
for off in (0x1559300, 0x157d9c0):
    print("  @%#x: %s" % (off, data[off - 16:off + 32].hex()))
