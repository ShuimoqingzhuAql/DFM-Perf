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
PRIMARY_C = (234, 221, 255)

FONT = r"C:\Windows\Fonts\msyh.ttc"
FONTB = r"C:\Windows\Fonts\msyhbd.ttc"


def f(size, bold=False):
    return ImageFont.truetype(FONTB if bold else FONT, size)


def rr(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


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
    # 状态栏
    d.text((22, 12), "9:41", font=f(11, True), fill=ON_SURF)
    d.rectangle((W - 60, 16, W - 22, 24), fill=ON_SURF)
    # 顶栏
    d.text((22, 44), title, font=f(24, True), fill=ON_SURF)
    d.text((22, 82), "三角洲行动 · 国服 · LSPosed 模块 · v1.1", font=f(11), fill=ON_VAR)
    return img, d


def card(d, y, h, x=16, w=W - 32):
    rr(d, (x, y, x + w, y + h), 20, fill=SURF_C)
    return y


def label(d, y, s):
    d.text((32, y), s, font=f(11, True), fill=ON_VAR)
    return y + 22


def field(d, y, s, chevron=True):
    rr(d, (28, y, W - 28, y + 46), 12, outline=OUTLINE_V, width=2)
    d.text((42, y + 13), s, font=f(14), fill=ON_SURF)
    if chevron:
        cx = W - 52
        d.line((cx, y + 20, cx + 7, y + 27), fill=ON_VAR, width=2)
        d.line((cx + 7, y + 27, cx, y + 34), fill=ON_VAR, width=2)
    return y + 56


# ---------------- 主界面 ----------------
main, dm = phone_base("DFM 画质工坊")
y = 118
# 当前状态卡
rr(dm, (16, y, W - 16, y + 92), 20, fill=SURF_C)
dm.text((36, y + 16), "当前状态", font=f(11, True), fill=ON_VAR)
dm.text((36, y + 38), "模式：写入设置", font=f(14), fill=ON_SURF)
dm.text((36, y + 62), "已保存：高清 / 60 帧 / 渲染 100%", font=f(12), fill=ON_VAR)
y += 104

# 目标设置卡
card_h = 430
rr(dm, (16, y, W - 16, y + card_h), 20, fill=SURF_C)
yy = y + 18
dm.text((36, yy), "目标设置", font=f(14, True), fill=ON_SURF)
yy += 34
yy = label(dm, yy, "画质档位")
yy = field(dm, yy, "高清")
yy = label(dm, yy, "帧率档位")
yy = field(dm, yy, "60 帧")
yy = label(dm, yy, "渲染比例")
dm.text((36, yy), "100%（原生目标）", font=f(13, True), fill=PRIMARY)
yy += 24
dm.line((40, yy + 14, W - 60, yy + 14), fill=OUTLINE_V, width=4)
dm.ellipse((W - 68, yy + 4, W - 48, yy + 24), fill=PRIMARY)
yy += 34
yy = label(dm, yy, "渲染基准短边（像素）")
rr(dm, (28, yy, W - 28, yy + 46), 12, outline=OUTLINE_V, width=2)
dm.text((42, yy + 13), "1080", font=f(14), fill=ON_SURF)
yy += 56
dm.text((32, yy), "100% 时忽略此项，交给引擎决定", font=f(10), fill=ON_VAR)
yy += 20
dm.text((32, yy), "将写入：高清 / 60 帧 / 渲染 100%", font=f(12, True), fill=PRIMARY)
y += card_h + 14

# 主按钮
rr(dm, (16, y, W - 16, y + 52), 26, fill=PRIMARY)
dm.text((W // 2, y + 26), "保存并下次启动生效", font=f(14, True), fill=ON_PRIMARY, anchor="mm")
y += 64
# 两个描边按钮
bw = (W - 32 - 10) // 2
for i, t in enumerate(["恢复默认", "还原备份"]):
    x = 16 + i * (bw + 10)
    rr(dm, (x, y, x + bw, y + 50), 25, outline=OUTLINE_V, width=2)
    dm.text((x + bw // 2, y + 25), t, font=f(13, True), fill=PRIMARY, anchor="mm")
y += 62

# 使用步骤卡（部分）
rr(dm, (16, y, W - 16, y + 190), 20, fill=SURF_C)
dm.text((36, y + 18), "使用步骤", font=f(14, True), fill=ON_SURF)
steps = ("1. 在 LSPosed 中启用本模块，作用域勾选“三角洲行动”。\n"
         "2. 选好档位，点“保存并下次启动生效”。\n"
         "3. 完全退出游戏后重新启动。\n"
         "4. 结果看 LSPosed 日志，标签 DFMPerf。")
para(dm, (36, y + 46), steps, f(11), ON_VAR, W - 80, lh=19)

# ---------------- 风险弹窗 ----------------
dlg, dd = phone_base("DFM 画质工坊")
dlg = Image.blend(dlg, Image.new("RGB", (W, H), (0, 0, 0)), 0.45)
dd = ImageDraw.Draw(dlg)

top, bot = 70, 820
rr(dd, (24, top, W - 24, bot), 28, fill=SCREEN)
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
    ("4. 数据风险", "写入前会自动备份三个 INI；但断电、强杀进程、存储故障等极端情况仍可能损坏配置。"),
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

# 底部三按钮
by = bot - 58
dd.text((48, by + 14), "不再提示", font=f(12, True), fill=ON_VAR)
# 取消
cw = 56
dd.text((W - 48 - 128, by + 14), "取消", font=f(12, True), fill=ON_VAR)
# 确认（填充胶囊）
rr(dd, (W - 48 - 92, by, W - 48, by + 40), 20, fill=PRIMARY)
dd.text(((W - 48 - 92 + W - 48) // 2, by + 20), "确认", font=f(12, True), fill=ON_PRIMARY, anchor="mm")

# 合成
canvas = Image.new("RGB", (W * 2 + 90, H + 80), BG)
cd = ImageDraw.Draw(canvas)
canvas.paste(main, (30, 40))
canvas.paste(dlg, (W + 60, 40))
cd.text((30, 12), "主界面（Material 3）", font=f(13, True), fill=ON_SURF)
cd.text((W + 60, 12), "首次启动风险弹窗（三选项）", font=f(13, True), fill=ON_SURF)
os.makedirs(r"E:\三角洲行动\apk\out", exist_ok=True)
out = r"E:\三角洲行动\apk\out\ui-preview.png"
canvas.save(out)
print("saved", out, canvas.size)
