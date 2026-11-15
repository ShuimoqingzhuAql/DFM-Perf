from PIL import Image, ImageDraw, ImageFont
import os

W, H = 540, 1120
BG = (238, 236, 240)
SCREEN = (255, 251, 254)
PRIMARY = (103, 80, 164)
ON_PRIMARY = (255, 255, 255)
SURF_C = (243, 237, 247)
ON_SURF = (28, 27, 31)
ON_VAR = (73, 69, 79)
OUTLINE_V = (202, 196, 208)

FONT = r"C:\Windows\Fonts\msyh.ttc"
FONTB = r"C:\Windows\Fonts\msyhbd.ttc"


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


def label(d, y, s):
    d.text((32, y), s, font=f(11, True), fill=ON_VAR)
    return y + 22


def field(d, y, s):
    d.rounded_rectangle((28, y, W - 28, y + 44), radius=12, outline=OUTLINE_V, width=2)
    d.text((42, y + 12), s, font=f(14), fill=ON_SURF)
    cx = W - 52
    d.line((cx, y + 19, cx + 7, y + 26), fill=ON_VAR, width=2)
    d.line((cx + 7, y + 26, cx, y + 33), fill=ON_VAR, width=2)
    return y + 54


def draw_main(expanded):
    img = Image.new("RGB", (W, H), SCREEN)
    d = ImageDraw.Draw(img)
    d.text((22, 12), "9:41", font=f(11, True), fill=ON_SURF)
    d.rectangle((W - 60, 16, W - 22, 24), fill=ON_SURF)
    d.text((22, 44), "DFM 画质工坊", font=f(24, True), fill=ON_SURF)
    d.text((22, 82), "三角洲行动 · 国服 · LSPosed 模块 · v1.6", font=f(11), fill=ON_VAR)

    y = 112
    # 当前状态
    d.rounded_rectangle((16, y, W - 16, y + 90), radius=20, fill=SURF_C)
    d.text((36, y + 16), "当前状态", font=f(11, True), fill=ON_VAR)
    d.text((36, y + 38), "模式：写入设置（下次启动游戏生效）", font=f(13), fill=ON_SURF)
    d.text((36, y + 62), "已保存：终极 / 120 帧 / 渲染 100%", font=f(12), fill=ON_VAR)
    y += 102

    # 目标设置
    height = 300 + (200 if expanded else 0)
    d.rounded_rectangle((16, y, W - 16, y + height), radius=20, fill=SURF_C)
    yy = y + 18
    d.text((36, yy), "目标设置", font=f(14, True), fill=ON_SURF)
    yy += 32
    yy = label(d, yy, "画质档位")
    yy = field(d, yy, "终极")
    yy = label(d, yy, "帧率档位")
    yy = field(d, yy, "120 帧")

    # 开关行
    yy += 6
    d.text((32, yy), "自定义渲染比例（进阶）", font=f(13, True), fill=ON_SURF)
    d.text((32, yy + 22), "默认关闭，保持原生分辨率。", font=f(10), fill=ON_VAR)
    track_x = W - 32 - 52
    d.rounded_rectangle((track_x, yy + 4, track_x + 52, yy + 32), radius=14,
                        fill=(PRIMARY if expanded else (202, 196, 208)))
    knob_x = track_x + 36 if expanded else track_x + 16
    d.ellipse((knob_x - 14, yy + 4, knob_x + 14, yy + 32), fill=(255, 255, 255))
    yy += 52

    if expanded:
        d.text((32, yy), "100%（原生目标）", font=f(12, True), fill=PRIMARY)
        yy += 22
        d.line((40, yy + 12, W - 60, yy + 12), fill=OUTLINE_V, width=4)
        d.ellipse((W - 68, yy + 2, W - 48, yy + 22), fill=PRIMARY)
        yy += 32
        yy = label(d, yy, "渲染基准短边（像素）")
        d.rounded_rectangle((28, yy, W - 28, yy + 44), radius=12, outline=OUTLINE_V, width=2)
        d.text((42, yy + 12), "1200", font=f(14), fill=ON_SURF)
        yy += 54
        para(d, (32, yy), "100% 时这一项不生效；低于 100% 时按“短边 × 比例”计算目标分辨率。",
             f(10), ON_VAR, W - 64, lh=15)
        yy += 32

    d.text((32, yy), "将写入：终极 / 120 帧 / 渲染 100%（原生，不额外限制）", font=f(11, True), fill=PRIMARY)
    y += height + 16

    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, fill=PRIMARY)
    d.text((W // 2, y + 25), "保存并下次启动生效", font=f(14, True), fill=ON_PRIMARY, anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 50), radius=25, outline=OUTLINE_V, width=2)
    d.text((W // 2, y + 25), "恢复默认", font=f(14, True), fill=PRIMARY, anchor="mm")
    y += 62
    d.rounded_rectangle((16, y, W - 16, y + 48), radius=24, outline=OUTLINE_V, width=2)
    d.text((W // 2, y + 24), "自检：检查模块能否读到设置", font=f(13, True), fill=PRIMARY, anchor="mm")
    y += 66

    d.rounded_rectangle((16, y, W - 16, y + 150), radius=20, fill=SURF_C)
    d.text((36, y + 18), "使用步骤", font=f(14, True), fill=ON_SURF)
    para(d, (36, y + 46), "1. 在 LSPosed 中启用本模块，作用域勾选“三角洲行动”。\n"
                          "2. 选好档位，点“保存并下次启动生效”。\n"
                          "3. 完全退出游戏后重新启动。\n"
                          "4. 结果看 LSPosed 日志，标签 DFMPerf。",
         f(11), ON_VAR, W - 80, lh=18)
    y += 166

    d.rounded_rectangle((16, y, W - 16, y + 150), radius=20, fill=SURF_C)
    d.text((36, y + 18), "风险提示", font=f(14, True), fill=ON_SURF)
    para(d, (36, y + 44), "修改游戏配置可能触发反作弊，导致账号受限或封禁；可能违反用户协议；不保证实测帧率。仅供学习研究使用，风险自负。",
         f(11), ON_VAR, W - 80, lh=17)
    by = y + 104
    d.rounded_rectangle((32, by, W - 32, by + 40), radius=20, outline=OUTLINE_V, width=2)
    d.text((W // 2, by + 20), "查看完整风险告知", font=f(12, True), fill=PRIMARY, anchor="mm")
    return img


def draw_icon(size=432):
    s = size / 108.0
    img = Image.new("RGB", (size, size), PRIMARY)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((26 * s, 30 * s, 82 * s, 70 * s), radius=8 * s,
                        outline=(255, 255, 255), width=int(5 * s))
    d.line((54 * s, 70 * s, 54 * s, 78 * s), fill=(255, 255, 255), width=int(5 * s))
    d.line((44 * s, 80 * s, 64 * s, 80 * s), fill=(255, 255, 255), width=int(5 * s))
    bolt = [(57, 34), (44, 56), (52, 56), (48, 68), (62, 46), (53, 46)]
    d.polygon([(x * s, y * s) for x, y in bolt], fill=(255, 255, 255))
    # 圆形裁剪，模拟自适应图标
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
    out = Image.new("RGB", (size, size), (255, 255, 255))
    out.paste(img, (0, 0), mask)
    return out


os.makedirs(r"E:\三角洲行动\apk\out", exist_ok=True)
a = draw_main(False)
b = draw_main(True)
canvas = Image.new("RGB", (W * 2 + 90, H + 80), BG)
cd = ImageDraw.Draw(canvas)
canvas.paste(a, (30, 40))
canvas.paste(b, (W + 60, 40))
cd.text((30, 12), "默认：进阶关闭（折叠）", font=f(13, True), fill=ON_SURF)
cd.text((W + 60, 12), "打开开关后：展开渲染比例与短边", font=f(13, True), fill=ON_SURF)
canvas.save(r"E:\三角洲行动\apk\out\ui-preview-v16.png")

icon = draw_icon(432)
icon.save(r"E:\三角洲行动\apk\out\icon-preview.png")
print("saved")
