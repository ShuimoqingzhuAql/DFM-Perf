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


def phone_base(title):
    img = Image.new("RGB", (W, H), SCREEN)
    d = ImageDraw.Draw(img)
    d.text((22, 12), "9:41", font=f(11, True), fill=ON_SURF)
    d.rectangle((W - 60, 16, W - 22, 24), fill=ON_SURF)
    d.text((22, 44), title, font=f(24, True), fill=ON_SURF)
    d.text((22, 82), "三角洲行动 · 国服 · LSPosed 模块 · v1.5", font=f(11), fill=ON_VAR)
    return img, d


def label(d, y, s):
    d.text((32, y), s, font=f(11, True), fill=ON_VAR)
    return y + 22


def field(d, y, s):
    rr_ = (28, y, W - 28, y + 46)
    d.rounded_rectangle(rr_, radius=12, outline=OUTLINE_V, width=2)
    d.text((42, y + 13), s, font=f(14), fill=ON_SURF)
    cx = W - 52
    d.line((cx, y + 20, cx + 7, y + 27), fill=ON_VAR, width=2)
    d.line((cx + 7, y + 27, cx, y + 34), fill=ON_VAR, width=2)
    return y + 56


main, dm = phone_base("DFM 画质工坊")
y = 112

# 当前状态
dm.rounded_rectangle((16, y, W - 16, y + 92), radius=20, fill=SURF_C)
dm.text((36, y + 16), "当前状态", font=f(11, True), fill=ON_VAR)
dm.text((36, y + 38), "模式：写入设置（下次启动游戏生效）", font=f(13), fill=ON_SURF)
dm.text((36, y + 62), "已保存：终极 / 185 帧 / 渲染 100%", font=f(12), fill=ON_VAR)
y += 104

# 目标设置
card_top = y
card_h = 452
dm.rounded_rectangle((16, y, W - 16, y + card_h), radius=20, fill=SURF_C)
yy = y + 18
dm.text((36, yy), "目标设置", font=f(14, True), fill=ON_SURF)
yy += 34
yy = label(dm, yy, "画质档位")
yy = field(dm, yy, "终极")
yy = label(dm, yy, "帧率档位")
yy = field(dm, yy, "185 帧")
yy = label(dm, yy, "渲染比例")
dm.text((36, yy), "100%（原生目标）", font=f(13, True), fill=PRIMARY)
yy += 24
dm.line((40, yy + 14, W - 60, yy + 14), fill=OUTLINE_V, width=4)
dm.ellipse((W - 68, yy + 4, W - 48, yy + 24), fill=PRIMARY)
yy += 34
yy = label(dm, yy, "渲染基准短边（像素）")
dm.rounded_rectangle((28, yy, W - 28, yy + 46), radius=12, outline=OUTLINE_V, width=2)
dm.text((42, yy + 13), "1200", font=f(14), fill=ON_SURF)
yy += 56
para(dm, (32, yy), "100% 时这一项不生效（写入 BackBuffer=8192，交给引擎决定）；低于 100% 时按“短边 × 比例”计算。",
     f(10), ON_VAR, W - 64, lh=16)
yy += 40
dm.text((32, yy), "将写入：终极 / 185 帧 / 渲染 100%（原生，不额外限制）", font=f(11, True), fill=PRIMARY)
y += card_h + 18

# 按钮
dm.rounded_rectangle((16, y, W - 16, y + 52), radius=26, fill=PRIMARY)
dm.text((W // 2, y + 26), "保存并下次启动生效", font=f(14, True), fill=ON_PRIMARY, anchor="mm")
y += 66
dm.rounded_rectangle((16, y, W - 16, y + 52), radius=26, outline=OUTLINE_V, width=2)
dm.text((W // 2, y + 26), "恢复默认", font=f(14, True), fill=PRIMARY, anchor="mm")
y += 64
dm.rounded_rectangle((16, y, W - 16, y + 50), radius=25, outline=OUTLINE_V, width=2)
dm.text((W // 2, y + 25), "自检：检查模块能否读到设置", font=f(13, True), fill=PRIMARY, anchor="mm")
y += 68

# 使用步骤
dm.rounded_rectangle((16, y, W - 16, y + 180), radius=20, fill=SURF_C)
dm.text((36, y + 18), "使用步骤", font=f(14, True), fill=ON_SURF)
para(dm, (36, y + 46),
     "1. 在 LSPosed 中启用本模块，作用域勾选“三角洲行动”。\n"
     "2. 选好档位，点“保存并下次启动生效”。\n"
     "3. 完全退出游戏后重新启动。\n"
     "4. 结果看 LSPosed 日志，标签 DFMPerf。",
     f(11), ON_VAR, W - 80, lh=19)
y += 196

# 风险
dm.rounded_rectangle((16, y, W - 16, y + 104), radius=20, fill=SURF_C)
dm.text((36, y + 18), "风险提示", font=f(14, True), fill=ON_SURF)
para(dm, (36, y + 46), "修改游戏配置可能触发反作弊，导致账号受限或封禁；不保证实测帧率。",
     f(11), ON_VAR, W - 80, lh=18)
dm.text((36, y + 82), "查看完整风险告知", font=f(11, True), fill=PRIMARY)

# ---------------- 风险弹窗（与之前一致） ----------------
dlg, dd = phone_base("DFM 画质工坊")
dlg = Image.blend(dlg, Image.new("RGB", (W, H), (0, 0, 0)), 0.45)
dd = ImageDraw.Draw(dlg)
top, bot = 70, 820
dd.rounded_rectangle((24, top, W - 24, bot), radius=28, fill=SCREEN)
dd.text((48, top + 26), "使用前必读 · 风险告知", font=f(20, True), fill=ON_SURF)
yy = top + 74
blocks = [
    ("", "本工具通过 LSPosed 修改《三角洲行动》的配置文件（Engine.ini / Game.ini），"
         "用来选择画质与帧率档位。它不修改游戏安装包，也不碰账号数据，但下面这些风险请你先看清楚："),
    ("1. 账号风险", "修改游戏配置、使用 LSPosed 等框架注入，可能被反作弊系统识别，导致账号被限制、"
                    "封禁或数据异常。后果由你自行承担。"),
    ("2. 协议风险", "这类修改可能违反游戏用户协议与服务条款，官方有权按协议处理。"),
    ("3. 效果不保证", "写进去的是“配置档位”，不等于实测帧率。实际帧率受设备性能、系统刷新率上报、"
                       "发热降频，以及“终极”画质独立资源表上限影响。"),
    ("4. 数据风险", "写入前会在内存里保留原内容，失败自动回滚；但断电、强杀进程仍可能损坏配置。"),
    ("5. 兼容风险", "游戏更新可能改变配置结构或加密材料，届时模块会校验失败并拒绝写入。"),
    ("6. 无担保", "本工具为逆向研究用途，按“现状”提供，不对封号、数据丢失等后果作任何担保。"),
]
for head, body in blocks:
    if head:
        dd.text((48, yy), head, font=f(12, True), fill=ON_SURF)
        yy += 22
    yy = para(dd, (48, yy), body, f(11), ON_VAR, W - 104, lh=18)
    yy += 10
dd.text((48, yy + 4), "继续即表示你已理解并自愿承担上述风险。", font=f(11, True), fill=ON_SURF)
by = bot - 58
dd.text((48, by + 14), "不再提示", font=f(12, True), fill=ON_VAR)
dd.text((W - 48 - 128, by + 14), "取消", font=f(12, True), fill=ON_VAR)
dd.rounded_rectangle((W - 48 - 92, by, W - 48, by + 40), radius=20, fill=PRIMARY)
dd.text(((W - 48 - 92 + W - 48) // 2, by + 20), "确认", font=f(12, True), fill=ON_PRIMARY, anchor="mm")

canvas = Image.new("RGB", (W * 2 + 90, H + 80), BG)
cd = ImageDraw.Draw(canvas)
canvas.paste(main, (30, 40))
canvas.paste(dlg, (W + 60, 40))
cd.text((30, 12), "主界面（v1.5：去掉备份/关闭/诊断，重新排版）", font=f(12, True), fill=ON_SURF)
cd.text((W + 60, 12), "首次启动风险弹窗（三选项）", font=f(12, True), fill=ON_SURF)
os.makedirs(r"E:\三角洲行动\apk\out", exist_ok=True)
out = r"E:\三角洲行动\apk\out\ui-preview-v15.png"
canvas.save(out)
print("saved", out, canvas.size)
