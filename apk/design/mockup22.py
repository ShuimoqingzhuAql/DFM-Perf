from PIL import Image, ImageDraw, ImageFont
import os

W, H = 540, 1120
FONT = r"C:\Windows\Fonts\msyh.ttc"
FONTB = r"C:\Windows\Fonts\msyhbd.ttc"
LIGHT = dict(surface=(255, 251, 254), card=(243, 237, 247), on=(28, 27, 31), onvar=(73, 69, 79),
             primary=(103, 80, 164), outline=(202, 196, 208), track=(217, 217, 222), knob=(255, 255, 255),
             pc=(234, 221, 255), opc=(33, 0, 93))
DARK = dict(surface=(20, 18, 24), card=(33, 31, 38), on=(230, 224, 233), onvar=(202, 196, 208),
            primary=(208, 188, 255), outline=(73, 69, 79), track=(58, 58, 60), knob=(230, 224, 233),
            pc=(79, 55, 139), opc=(234, 221, 255))


def f(size, bold=False):
    return ImageFont.truetype(FONTB if bold else FONT, size)


def wrap(d, text, font, max_w):
    lines, cur = [], ""
    for ch in text:
        if ch == "\n":
            lines.append(cur); cur = ""; continue
        if d.textlength(cur + ch, font=font) > max_w and cur:
            lines.append(cur); cur = ch
        else:
            cur += ch
    if cur:
        lines.append(cur)
    return lines


def para(d, xy, text, font, fill, max_w, lh=None):
    x, y = xy
    lh = lh or int(font.size * 1.5)
    for line in wrap(d, text, font, max_w):
        d.text((x, y), line, font=font, fill=fill)
        y += lh
    return y


def select_box(d, p, y, text, open_=False):
    d.rounded_rectangle((28, y, W - 28, y + 48), radius=12, outline=p["outline"], width=2)
    d.text((44, y + 14), text, font=f(14), fill=p["on"])
    cx = W - 46; cy = y + 26 if not open_ else y + 22
    if open_:
        d.line((cx - 6, cy + 4, cx, cy - 2), fill=p["onvar"], width=2)
        d.line((cx, cy - 2, cx + 6, cy + 4), fill=p["onvar"], width=2)
    else:
        d.line((cx - 6, cy - 4, cx, cy + 2), fill=p["onvar"], width=2)
        d.line((cx, cy + 2, cx + 6, cy - 4), fill=p["onvar"], width=2)
    return y + 56


def screen(p, expanded=False, open_select=False):
    img = Image.new("RGB", (W, H), p["surface"])
    d = ImageDraw.Draw(img)
    d.text((22, 12), "9:41", font=f(11, True), fill=p["on"])
    d.rectangle((W - 60, 16, W - 22, 24), fill=p["on"])
    d.text((22, 44), "DFM 画质工坊", font=f(24, True), fill=p["on"])
    d.text((22, 82), "三角洲行动 · 国服 · LSPosed 模块 · v2.2", font=f(11), fill=p["onvar"])
    y = 112
    d.rounded_rectangle((16, y, W - 16, y + 90), radius=20, fill=p["card"])
    d.text((36, y + 16), "当前状态", font=f(11, True), fill=p["onvar"])
    d.text((36, y + 38), "模式：写入设置（下次启动游戏生效）", font=f(13), fill=p["on"])
    d.text((36, y + 62), "已保存：终极 / 120 帧 / 渲染 100%", font=f(12), fill=p["onvar"])
    y += 102

    card_top = y
    y += 18
    d.text((36, y), "目标设置", font=f(14, True), fill=p["on"]); y += 30
    d.text((32, y), "画质档位", font=f(11, True), fill=p["onvar"]); y += 22
    y = select_box(d, p, y, "终极", open_select)
    if open_select:
        for i, item in enumerate(["流畅", "标准", "精致", "高清", "极致", "终极"]):
            sel = (item == "终极")
            ry = y + 6 + i * 44
            d.rounded_rectangle((28, ry, W - 28, ry + 44), radius=10, fill=p["card"])
            d.text((48, ry + 12), item, font=f(13, True) if sel else f(13),
                   fill=p["primary"] if sel else p["on"])
        y += 6 + 6 * 44 + 10
    d.text((32, y), "帧率档位", font=f(11, True), fill=p["onvar"]); y += 22
    y = select_box(d, p, y, "120 帧")

    y += 6
    d.text((32, y), "自定义渲染比例（进阶）", font=f(13, True), fill=p["on"])
    d.text((32, y + 22), "默认关闭，保持原生分辨率。", font=f(10), fill=p["onvar"])
    d.text((32, y + 40), "查看功能说明", font=f(10, True), fill=p["primary"])
    tw, th = 56, 34
    tx, ty = W - 32 - tw, y + 4
    d.rounded_rectangle((tx, ty, tx + tw, ty + th), radius=th // 2,
                        fill=(p["primary"] if expanded else p["track"]))
    kr = 15
    kx = tx + tw - kr - 3 if expanded else tx + kr + 3
    d.ellipse((kx - kr, ty + th // 2 - kr, kx + kr, ty + th // 2 + kr), fill=p["knob"])
    y += 60

    if expanded:
        d.text((32, y + 10), "100%", font=f(26, True), fill=p["on"])
        d.text((32, y + 48), "原生分辨率，不额外限制", font=f(10), fill=p["onvar"])
        y += 74
        d.rounded_rectangle((32, y, W - 32, y + 10), radius=5, fill=p["track"])
        d.rounded_rectangle((32, y, W - 32, y + 10), radius=5, fill=p["primary"])
        d.ellipse((W - 32 - 28, y - 9, W - 32, y + 19), fill=p["knob"])
        y += 34
        cw = (W - 64 - 3 * 8) // 4
        for i, v in enumerate([100, 90, 80, 70]):
            cx0 = 32 + i * (cw + 8)
            if i == 0:
                d.rounded_rectangle((cx0, y, cx0 + cw, y + 38), radius=14, fill=p["pc"])
                d.text((cx0 + cw // 2, y + 19), str(v) + "%", font=f(11), fill=p["opc"], anchor="mm")
            else:
                d.rounded_rectangle((cx0, y, cx0 + cw, y + 38), radius=14, outline=p["outline"], width=2)
                d.text((cx0 + cw // 2, y + 19), str(v) + "%", font=f(11), fill=p["onvar"], anchor="mm")
        y += 58
        d.text((32, y), "渲染基准短边（像素）", font=f(11, True), fill=p["onvar"]); y += 22
        d.rounded_rectangle((28, y, W - 28, y + 48), radius=12, outline=p["outline"], width=2)
        d.text((44, y + 14), "1200", font=f(14), fill=p["on"]); y += 56
        para(d, (32, y), "100% 时这一项不生效；低于 100% 时按“短边 × 比例”计算目标分辨率。",
             f(10), p["onvar"], W - 64, lh=15)
        y += 34

    d.text((32, y), "将写入：终极 / 120 帧 / 渲染 100%（原生，不额外限制）", font=f(11, True), fill=p["primary"])
    y += 28
    d.rounded_rectangle((16, card_top, W - 16, y), radius=20, outline=p["outline"], width=1)
    y += 18

    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, fill=p["primary"])
    txt = (255, 255, 255) if p is LIGHT else (56, 30, 114)
    d.text((W // 2, y + 25), "保存并下次启动生效", font=f(14, True), fill=txt, anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, outline=p["outline"], width=2)
    d.text((W // 2, y + 25), "恢复默认", font=f(14, True), fill=p["primary"], anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 48), radius=24, outline=p["outline"], width=2)
    d.text((W // 2, y + 24), "自检：检查模块能否读到设置", font=f(13, True), fill=p["primary"], anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 150), radius=20, fill=p["card"])
    d.text((36, y + 18), "风险提示", font=f(14, True), fill=p["on"])
    para(d, (36, y + 44), "修改游戏配置可能触发反作弊，导致账号受限或封禁。仅供学习研究使用，风险自负。",
         f(11), p["onvar"], W - 80, lh=17)
    d.rounded_rectangle((32, y + 104, W - 32, y + 142), radius=20, outline=p["outline"], width=2)
    d.text((W // 2, y + 123), "查看完整风险告知", font=f(11, True), fill=p["primary"], anchor="mm")
    return img


panels = [("折叠", screen(LIGHT, False, False)),
          ("展开（滑条与标题/预设对齐）", screen(LIGHT, True, False)),
          ("深色 · 展开", screen(DARK, True, False))]
canvas = Image.new("RGB", (W * 3 + 120, H + 80), (238, 236, 240))
cd = ImageDraw.Draw(canvas)
for i, (t, im) in enumerate(panels):
    x = 30 + i * (W + 30)
    canvas.paste(im, (x, 40))
    cd.text((x, 12), t, font=f(12, True), fill=(28, 27, 31))
os.makedirs(r"E:\三角洲行动\apk\out", exist_ok=True)
canvas.save(r"E:\三角洲行动\apk\out\ui-preview-v22.png")
print("ok")
