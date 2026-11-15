import os
path = r"E:\三角洲行动\work\game\libUE4.so"
data = open(path, "rb").read()
print("size", len(data))

def v(s):
    return {
        "utf8": s.encode("utf-8"),
        "utf16": s.encode("utf-16le"),
        "x10utf8": bytes(c ^ 0x10 for c in s.encode("utf-8")),
        "x10utf16": bytes(c ^ 0x10 for c in s.encode("utf-16le")),
    }

for p in ["为了保证游玩体验", "自动降低帧率", "合适水平", "是否继续", "降低帧率",
          "游玩体验", "帧率", "设备帧率", "画质"]:
    hits = []
    for name, b in v(p).items():
        i = data.find(b)
        if i >= 0:
            hits.append("%s@0x%x" % (name, i))
    print("%-12s %s" % (p, ", ".join(hits) if hits else "-"))

print("--- 相关键名（XOR 0x10 形式）---")
for p in ["DFMDeviceFrame", "White120fps", "White90fps", "DeviceFrame", "FrameRate",
          "PerfGear", "DeviceLevel", "bEnableHighFrameRate", "FrameRateLimit", "MaxFPS"]:
    b = bytes(c ^ 0x10 for c in p.encode("utf-8"))
    idxs = []
    i = data.find(b)
    while i >= 0 and len(idxs) < 5:
        idxs.append(hex(i))
        i = data.find(b, i + 1)
    print("%-24s count=%d at %s" % (p, data.count(b), idxs))
