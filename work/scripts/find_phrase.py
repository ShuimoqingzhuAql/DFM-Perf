import os, re, sys

files = [
    r"E:\三角洲行动\work\gamedex\classes.dex",
    r"E:\三角洲行动\work\gamedex\classes2.dex",
    r"E:\三角洲行动\work\gamedex\resources.arsc",
]
phrases = [
    "为了保证游玩体验",
    "自动降低帧率",
    "合适水平",
    "是否继续",
    "降低帧率",
    "游玩体验",
    "帧率",
]

def variants(s):
    out = {}
    out["utf8"] = s.encode("utf-8")
    out["utf16le"] = s.encode("utf-16le")
    out["xor10_utf8"] = bytes(c ^ 0x10 for c in s.encode("utf-8"))
    out["xor10_utf16"] = bytes(c ^ 0x10 for c in s.encode("utf-16le"))
    return out

for path in files:
    data = open(path, "rb").read()
    print("==", os.path.basename(path), len(data))
    for p in phrases:
        hits = []
        for name, b in variants(p).items():
            i = data.find(b)
            if i >= 0:
                hits.append("%s@0x%x" % (name, i))
        if hits:
            print("  %-14s %s" % (p, ", ".join(hits)))
