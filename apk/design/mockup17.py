from PIL import Image, ImageDraw, ImageFont
import os

W, H = 540, 1120
FONT = r"C:\Windows\Fonts\msyh.ttc"
FONTB = r"C:\Windows\Fonts\msyhbd.ttc"

LIGHT = dict(surface=(255, 251, 254), card=(243, 237, 247), on=(28, 27, 31), onvar=(73, 69, 79),
             primary=(103, 80, 164), outline=(202, 196, 208), track=(217, 217, 222), knob=(255, 255, 255),
             bgpage=(238, 236, 240))
DARK = dict(surface=(20, 18, 24), card=(33, 31, 38), on=(230, 224, 233), onvar=(202, 196, 208),
            primary=(208, 188, 255), outline=(73, 69, 79), track=(58, 58, 60), knob=(230, 224, 233),
            bgpage=(10, 10, 14))


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


def draw_screen(p, expanded, dark):
    img = Image.new("RGB", (W, H), p["surface"])
    d = ImageDraw.Draw(img)
    d.text((22, 12), "9:41", font=f(11, True), fill=p["on"])
    d.rectangle((W - 60, 16, W - 22, 24), fill=p["on"])
    d.text((22, 44), "DFM 画质工坊", font=f(24, True), fill=p["on"])
    d.text((22, 82), "三角洲行动 · 国服 · LSPosed 模块 · v1.7", font=f(11), fill=p["onvar"])
    y = 112
    d.rounded_rectangle((16, y, W - 16, y + 90), radius=20, fill=p["card"])
    d.text((36, y + 16), "当前状态", font=f(11, True), fill=p["onvar"])
    d.text((36, y + 38), "模式：写入设置（下次启动游戏生效）", font=f(13), fill=p["on"])
    d.text((36, y + 62), "已保存：终极 / 120 帧 / 渲染 100%", font=f(12), fill=p["onvar"])
    y += 102

    height = 330 + (215 if expanded else 0)
    d.rounded_rectangle((16, y, W - 16, y + height), radius=20, fill=p["card"])
    yy = y + 18
    d.text((36, yy), "目标设置", font=f(14, True), fill=p["on"])
    yy += 34
    d.text((32, yy), "画质档位", font=f(11, True), fill=p["onvar"]); yy += 22
    d.rounded_rectangle((28, yy, W - 28, yy + 44), radius=12, outline=p["outline"], width=2)
    d.text((42, yy + 12), "终极", font=f(14), fill=p["on"]); yy += 54
    d.text((32, yy), "帧率档位", font=f(11, True), fill=p["onvar"]); yy += 22
    d.rounded_rectangle((28, yy, W - 28, yy + 44), radius=12, outline=p["outline"], width=2)
    d.text((42, yy + 12), "120 帧", font=f(14), fill=p["on"]); yy += 54

    # 开关
    yy += 8
    d.text((32, yy), "自定义渲染比例（进阶）", font=f(13, True), fill=p["on"])
    d.text((32, yy + 22), "默认关闭，保持原生分辨率。", font=f(10), fill=p["onvar"])
    tw, th = 56, 34
    tx, ty = W - 32 - tw, yy + 2
    d.rounded_rectangle((tx, ty, tx + tw, ty + th), radius=th // 2,
                        fill=(p["primary"] if expanded else p["track"]))
    kr = 15
    kx = tx + tw - kr - 3 if expanded else tx + kr + 3
    d.ellipse((kx - kr, ty + th // 2 - kr, kx + kr, ty + th // 2 + kr), fill=p["knob"])
    yy += 52

    if expanded:
        yy += 18
        d.text((32, yy), "100%（原生目标）", font=f(12, True), fill=p["primary"]); yy += 24
        # MIUIX 滑条
        tr_h = 10
        d.rounded_rectangle((46, yy, W - 46, yy + tr_h), radius=5, fill=p["track"])
        d.rounded_rectangle((46, yy, W - 52, yy + tr_h), radius=5, fill=p["primary"])
        d.ellipse((W - 64, yy - 9, W - 36, yy + 19), fill=p["knob"])
        yy += 44
        d.text((32, yy), "渲染基准短边（像素）", font=f(11, True), fill=p["onvar"]); yy += 22
        d.rounded_rectangle((28, yy, W - 28, yy + 44), radius=12, outline=p["outline"], width=2)
        d.text((42, yy + 12), "1200", font=f(14), fill=p["on"]); yy += 54
        para(d, (32, yy), "100% 时这一项不生效；低于 100% 时按“短边 × 比例”计算目标分辨率。",
             f(10), p["onvar"], W - 64, lh=15)
        yy += 34

    d.text((32, yy), "将写入：终极 / 120 帧 / 渲染 100%（原生，不额外限制）", font=f(11, True), fill=p["primary"])
    y += height + 16
    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, fill=p["primary"])
    txt = (255, 255, 255) if not dark else (56, 30, 114)
    d.text((W // 2, y + 25), "保存并下次启动生效", font=f(14, True), fill=txt, anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, outline=p["outline"], width=2)
    d.text((W // 2, y + 25), "恢复默认", font=f(14, True), fill=p["primary"], anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 48), radius=24, outline=p["outline"], width=2)
    d.text((W // 2, y + 24), "自检：检查模块能否读到设置", font=f(13, True), fill=p["primary"], anchor="mm")
    y += 66
    d.rounded_rectangle((16, y, W - 16, y + 120), radius=20, fill=p["card"])
    d.text((36, y + 18), "风险提示", font=f(14, True), fill=p["on"])
    para(d, (36, y + 44), "修改游戏配置可能触发反作弊，导致账号受限或封禁。仅供学习研究使用，风险自负。",
         f(11), p["onvar"], W - 80, lh=17)
    by = y + 86
    d.rounded_rectangle((32, by, W - 32, by + 0), radius=0, outline=p["outline"])
    return img


panels = [("浅色 · 折叠", draw_screen(LIGHT, False, False)),
          ("浅色 · 展开（MIUIX 开关/滑条）", draw_screen(LIGHT, True, False)),
          ("深色 · 展开", draw_screen(DARK, True, True))]
canvas = Image.new("RGB", (W * 3 + 120, H + 80), (238, 236, 240))
cd = ImageDraw.Draw(canvas)
for i, (title, im) in enumerate(panels):
    x = 30 + i * (W + 30)
    canvas.paste(im, (x, 40))
    cd.text((x, 12), title, font=f(12, True), fill=(28, 27, 31))
os.makedirs(r"E:\三角洲行动\apk\out", exist_ok=True)
canvas.save(r"E:\三角洲行动\apk\out\ui-preview-v17.png")
print("ok", canvas.size)
