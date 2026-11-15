# DFM 画质工坊 · LSPosed 模块（v1.4）

面向《三角洲行动》国服（`com.tencent.tmgp.dfm`）的画质 / 帧率写入模块。
**通过 LSPosed 在游戏进程内运行，不需要 root**；APK **未加壳、未混淆**，源码就是仓库里的 `src/`，欢迎逆向。

- 产物：`apk/out/DFM-Perf-v1.4.apk`（约 40 KB，`minSdk 26` / `targetSdk 34`，v2+v3 签名）
- 包名：`io.github.dfmperf`
- 日志标签：`DFMPerf`（在 LSPosed 日志里看）

---

## 1. 安装与激活（LSPosed）

1. 安装 `DFM-Perf-v1.0.apk`。
2. 打开 **LSPosed 管理器 → 模块**，启用「DFM 画质工坊」。
3. 点进模块的**作用域**，只勾选 **三角洲行动**（`com.tencent.tmgp.dfm`）。
4. 打开「DFM 画质工坊」App，选画质 / 帧率 / 渲染比例，点 **保存并下次启动生效**。
5. **完全退出游戏**（从最近任务里划掉，确保进程结束），再启动游戏。
6. 生效结果看 LSPosed 日志，应出现类似：
   `DFMPerf: 完成: 已写入并回读校验：终极 / 185 帧 / 渲染 100%（备份 2026...）`

> 必须让游戏**重启**：模块只在进程启动、游戏读取配置之前写入一次。

撤销：
- 想要回到游戏默认：App 里点 **恢复默认**（删除本工具写入的覆盖项）→ 重启游戏。
- 想彻底停用：直接在 LSPosed 里停用模块（App 里没有“关闭模块”按钮了）。
- 本版本**不保留备份文件**：写入前把原内容留在内存里，失败自动回滚，成功后丢弃。

### 首次启动的风险告知（三个选项）

标题：**使用前必读｜风险告知**；五段：账号风险 / 协议风险 / 效果限制 / 兼容风险 / 责任说明；
结尾那句「继续使用，即表示你已阅读并理解以上内容，并自愿承担相关风险。」**加粗且为红色**。

底部三个选项：

| 按钮 | 位置 | 行为 |
|---|---|---|
| **不再提示** | 左下 | 视为已知晓，进入 App，并以后不再弹 |
| **取消** | 右下 | 不同意，**直接退出 App** |
| **确认** | 右下（主按钮） | 视为已知晓，进入 App；下次启动仍提示 |

### 自定义渲染比例（进阶，v1.6）

「渲染比例」和「渲染基准短边」默认**折叠**，由一个开关控制。打开开关会先弹说明（不再提示 / 取消 / 确认），
强调「默认即可，不需要修改」；点取消则开关自动回到关闭。关闭时写入 100%（原生），开启后才使用滑块和短边。

另外，点“保存并下次启动生效 / 恢复默认”时还会各弹一次**二次确认**（只有“取消 / 确认”），避免误触写盘。


---

## 2. 它做了什么

在游戏主进程调用 `Application.onCreate` 之前（hook `Instrumentation.callApplicationOnCreate`），用**游戏自己的 UID** 读取并改写：

| 文件 | 处理 |
|---|---|
| `Engine.ini` | 解密 → 改 `DFMQualityLevel` / `DFMFPSLevel` / `Unlimit` / `PerfGear.SmallPadSize` / `DFMDeviceFrame.White{N}fps` → 重新加密 → 回读校验 |
| `Game.ini` | 改 `[PerfGearAndroidPadSizeConfig]` 的 `Small/Mid/BigPadConfig` 三组渲染分辨率 |
| `UserGameplay.ini` | **只读**，作为本机认证上下文；写入前后校验未被改动 |

写入流程：备份三文件 → 同目录暂存 + fsync → 回读比对 → rename → 再回读 + HMAC 认证 → 失败自动回滚。
备份位置：`/data/data/com.tencent.tmgp.dfm/files/dfm_perf/backups/<时间戳>/`。

容器格式、密钥链、材料定位算法、字段映射的完整依据见仓库根目录
[`逆向分析报告.md`](../逆向分析报告.md)（§4 容器格式、§5 libUE4.so、§8 字段映射）。

**不做的事**：不改安装包、不改 `UserGameplay.ini`、不注入游戏内存、不伪造帧率、不绕过资源表上限。

---

## 3. 源码结构（全部明文，无混淆）

```
apk/
├── AndroidManifest.xml        模块声明（xposedmodule / xposedscope）
├── assets/xposed_init         入口类名
├── res/values/arrays.xml      作用域包名
├── src/io/github/dfmperf/
│   ├── MainActivity.java      设置界面（纯代码构建，无布局 XML）
│   ├── Prefs.java             设置键名
│   ├── xposed/Entry.java      LSPosed 入口与执行逻辑
│   └── core/
│       ├── ConfigCrypto.java  容器加解密（AES-256-ECB + HMAC-SHA1 + Base64）
│       ├── Ini.java           INI 读写（保留行尾/BOM/其它字段）
│       ├── ConfigPlan.java    档位枚举、渲染值计算、生成新配置
│       ├── Material.java      libUE4.so 里的 32 字节材料定位 + HMAC 门禁
│       └── Repo.java          路径解析、备份、原子写入、回滚、还原
├── stubs/de/robv/...          Xposed API 编译占位（**不打进 APK**，运行时由 LSPosed 提供）
├── test/...                   JVM 侧测试（不需要手机）
└── build.ps1                  aapt2 → javac → d8 → zipalign → apksigner 全流程
```

---

## 4. 自己构建

前置：JDK 25（用 Android Studio 自带 JBR 即可）、Android SDK build-tools 36.x、`platforms/android-35/android.jar`。

# 产物: apk\out\DFM-Perf-v2.2.apk
cd apk
powershell -ExecutionPolicy Bypass -File .\build.ps1
# 产物: apk\out\DFM-Perf-v1.4.apk
```

脚本会先把源码复制到 `E:\dfm_apk_build`（纯 ASCII 路径，避开 aapt2 对中文路径的报错），
构建、签名后把 APK 拷回 `apk\out\`。签名用脚本首次自动生成并保存在 apk/ 目录的 `dfmperf.keystore`（口令 `dfmperf`）。

---

## 5. 已验证 / 未验证

**已在电脑上验证（可复现）**

| 项 | 方法 | 结果 |
|---|---|---|
| 加解密互通 | `apk/core` 写出的 Engine.ini 交给**原工具自己的** `ConfigCrypto` 解密 | `QUALITY=5 FPS=11 UNLIMIT=1 PADSIZE=0 WHITE185=1`，回读一致 |
| 渲染值计算 | 1080 短边 × 67% | `724`，三组 PadConfig 全部一致 |
| INI 编辑 | 保留 CRLF/BOM、只改目标键 | 通过 |
| 备份/幂等/回滚/还原 | JVM 上跑 `Repo` 全流程 | 重复写入自动跳过；`removeOverrides`/`restoreLatest` 正常 |
| 材料扫描器 | 用真机同版本 `libUE4.so` 跑 Java 扫描 | **750 ms** 得到 `0x1559300 / 0x157d9c0`，与 Python 独立实现完全一致 |
| 认证门禁 | 用不匹配的 UserGameplay 跑 `Material.resolve` | 正确拒绝，不写任何文件 |
| APK 结构 | `aapt2 dump` / `dexdump` | 包名/版本/`xposedmodule`/`xposed_init` 正确；dex 内**只有**本模块类，无 stub 残留 |

**未验证（必须真机）**

- LSPosed 实际加载与 hook 时机是否早于 UE4 读配置（日志能看到"完成"才算成功）。
- `XSharedPreferences` 在目标机型能否读到设置（日志里若 `quality` 一直是默认值，就是没读到）。
- 游戏是否会在退出时把配置写回、覆盖模块的修改。
- 反作弊是否检测 LSPosed / 模块注入。**这是最大风险，可能会封号。**

---

## 6. 已知限制

1. **只在游戏启动时写一次**，且仅主进程（`com.tencent.tmgp.dfm`）；子进程不处理。
2. **终极档仍有独立的资源表帧率上限**，配置写 185 不代表实测 185；90 帧以上还需要系统向游戏上报足够刷新率。
3. **渲染比例会统一套到 5 个画质档**（与原工具一致），不是逐档递减。
4. **材料偏移按 `libUE4.so` 的 SHA-256 查表**：当前版本已内置；游戏更新后若 SHA 变了，会自动退化为扫描（手机上可能几秒到几十秒，且日志会写明"扫描偏移"）。扫描也失败时会**安全退出**，不写任何文件。
5. `UserGameplay.ini` 变化（换机/清数据/换账号）会导致认证失败，模块只记日志、不动配置。
6. 不保留备份文件：写入前在内存里留原内容，失败自动回滚；成功后就丢弃，磁盘上不多留文件。

---

## 7. 排错

| 现象 | 看什么 |
|---|---|
| 游戏没变化 | LSPosed 日志有无 `DFMPerf: 已注入`；没有 → 作用域没勾选 / 没重启游戏 |
| 日志有"模块未启用改动（mode=off）" | 回 App 点一次「保存并下次启动生效」 |
| 日志有"找不到 Engine.ini" | 游戏还没启动过 / 数据目录不对，先进游戏把画质保存一次 |
| 日志有"材料来源: 扫描偏移" 且很久 | 正常，新版本库在扫描；下次启动会再扫（未做缓存） |
| 日志有"异常/失败" | 把整段日志发出来；模块不会在认证失败时写文件 |
| 想彻底确认改没改 | 用 MT 管理器看 `/data/data/com.tencent.tmgp.dfm/files/dfm_perf/state.txt` |

---

## 8. 风险声明

修改游戏配置可能违反游戏用户协议，使用 LSPosed 也可能被反作弊识别，**可能导致账号受限或封禁**。
本项目仅用于逆向学习与本地研究，不承诺账号安全、不承诺实测帧率。请自行评估后再使用。

---

## 9. 界面（Material 3）

没有引入 AppCompat / Material Components 库，而是按 **Material 3 基线色板**手写了一层轻量组件（`Ui.java`），
保证 APK 只有 40 KB、无第三方依赖、也便于逆向：

- 色板：primary `#6750A4`、surface `#FFFBFE`、surfaceContainer `#F3EDF7`、outlineVariant `#CAC4D0`
- 组件：圆角 20dp 的 tonal card、52dp 高的 filled / outlined 按钮（带 ripple）、12dp 圆角描边输入框、
  带主色的 SeekBar、28dp 圆角的弹窗
- 字阶：24sp 标题 / 15sp 小标题 / 14sp 正文 / 12sp 辅助
- 弹窗按钮排布：左下 `不再提示`，右下 `取消` + `确认`（主按钮）

设计预览图：`apk/out/ui-preview.png`（左右分别是主界面与风险弹窗）。

---

## 10. v1.2：帧率被游戏降回去时怎么查

现象：写入「终极 + 120」后，游戏提示「为了保证游玩体验，本次修改会自动降低帧率至合适水平，是否继续？」然后回到 60。

这句话**不在 APK 里**（已搜 classes.dex / classes2.dex / resources.arsc / libUE4.so，UTF-8、UTF-16、XOR-0x10 四种编码都没有），
说明它是游戏运行时/下载内容里的 UI 文案，属于游戏侧行为，不是模块输出。

v1.2 加了两样只读手段：

1. **每次启动都会在 LSPosed 日志里记录「读取时实际值」**：
   `画质= 帧率枚举= Unlimit= SmallPadSize= White90/120/144/165/185= … Pad=…`
   - 第二次启动时如果 `帧率枚举=2`（60），说明**游戏把配置改回去了**；
   - 如果仍是 `8`（120），说明**配置没被改，是运行时被降帧**（资源表/调度/温控）。
2. **App 里新增「诊断：把配置目录导出到 LSPosed 日志（只读）」**：
   下次启动会把配置目录的文件清单（含 DeviceProfiles.ini / Scalability.ini 等若存在）与各 INI 明文写进日志，
   Token/Hash 自动脱敏。用来判断上限是不是在某个本地 ini 表里。

另外日志还会记录设备型号与游戏 versionCode，便于判断是否为机型白名单问题。
---

## 11. v1.3：修复 hook 挂载失败

日志特征：`DFMPerf: 已注入` 之后立刻 `NoSuchMethodError: No static method findAndHookMethod(...)V`。

原因：编译用的 Xposed 占位类把 `findAndHookMethod` 声明成 `void`，而真实 API 返回 `XC_MethodHook.Unhook`。
dex 里的方法描述符带返回类型（`)V` vs `)Lde/robv/android/xposed/XC_MethodHook;`），所以运行时找不到方法，hook 从未挂上——
也就是说 **v1.2 及更早版本实际上没有改过任何配置**。

v1.3 修正：
- 占位类返回值改为 `XC_MethodHook.Unhook`（`XposedHelpers` / `XposedBridge` 两处）；
- 增加兜底：`findAndHookMethod` 失败时改用 `Instrumentation.class.getDeclaredMethod(...)` + `XposedBridge.hookMethod`；
- 日志会打印实际使用的挂载方式：`hook 方式: findAndHookMethod` 或 `hook 方式: hookMethod`。

看到 `hook 方式` 那行，才说明这次真的挂上了。
---

## 12. v1.4：设置读取通道（ContentProvider）+ 自检

日志特征：`hook 方式: findAndHookMethod` 正常，但下一行 `模块未启用改动（mode=off）`。
说明 hook 已挂上，问题在“模块读不到 App 里保存的设置”（或根本没点保存）。

v1.4 改动：

- 新增 `SettingsProvider`（`content://io.github.dfmperf.settings/<key>`，exported），
  模块优先用它读设置，失败再退回 `XSharedPreferences`，日志会打印 `设置来源=provider|XSharedPreferences|default`。
- 日志新增一行：`设置来源=… mode=… quality=… fps=… ratio=… shortEdge=…`，读不到时附失败原因。
- App 新增按钮「自检：检查模块能否读到设置」，直接查同一个 provider 并显示 5 个值。

只用 `mode / quality / fps / ratio / shortEdge` 五个非敏感值，不暴露任何认证材料。

---

## 13. v1.5：按反馈精简 + 修界面

- 修复预览不刷新：画质、帧率两个下拉框和短边输入框都加了监听，改任一档位，“将写入：…”立刻跟着变（原来只有滑比例会刷新）。
- 移除「还原备份」「关闭模块」「诊断导出」三个功能；App 只剩 **保存并下次启动生效**、**恢复默认**、**自检**。
- 不再生成任何备份文件：写入前把原内容留在内存，失败自动回滚，成功后丢弃。
- 重新排版：卡片/按钮统一 12–20dp 间距，去掉“自检”和“使用步骤”挤在一起的问题；下拉框展开项也加了内边距。
- 预览文案更准确：100% 显示“渲染 100%（原生，不额外限制）”，不再显示容易误解的“短边目标 8192 px”。

---

## 14. v1.6：渲染比例折叠 + 新风险文案 + 代码图标

- **渲染比例改为进阶项**：默认折叠，开关打开后才显示滑块与短边；打开前弹说明（不再提示 / 取消 / 确认），强调“默认即可，不需要修改”。关闭时一律按 100%（原生）写入。
- **风险提示按钮化**：卡片内改成正式的描边按钮「查看完整风险告知」，不再是一行贴着的文字。
- **风险卡片末句**改为「仅供学习研究使用，风险自负。」
- **首次启动风险告知**换成新版五段文案；标题加粗，结尾一句加粗且为红色（Material error 色 `#B3261E`）。
- **图标改为纯矢量**：`res/drawable/ic_launcher_foreground.xml` + `res/mipmap-anydpi-v26/ic_launcher.xml`（adaptive icon），
  没有任何 PNG/JPG，包体基本不变。设计为紫色底 + 白色显示器 + 闪电。
---

## 15. v1.7：新图标 + MIUIX 控件 + 深色模式

**图标（纯矢量，无位图）**
- `res/drawable/ic_launcher_background.xml`：深石墨色对角渐变 #111827 → #080D16
- `res/drawable/ic_launcher_foreground.xml`：1024×1024 viewBox，刻度环渐变 #7C5CFF → #4E8DFF → #23E6BE、右上留缺口、
  环上/右/下三条刻度线、四段准星（#E7FFF9）、中心青绿圆角方块（#20E4B9）+ 更浅内块
- `res/mipmap-anydpi-v26/ic_launcher.xml`：adaptive-icon，背景/前景都指向上面两个 vector，另带 monochrome
- 内容半径 ≤ 310 / 512，落在自适应图标 66dp 安全区内，圆形/方形/水滴蒙版都不会裁到准星和圆环

**控件（模仿 MIUIX）**
- `MiuiSwitch`：56×34dp 胶囊轨道 + 白色圆钮，开为紫色，160ms 动画
- `MiuiSlider`：10dp 粗圆角轨道、28dp 白色圆钮、主色填充已选段，支持拖动

**深色模式**
- `Ui.init(Context)` 按 `uiMode` 切换整套 Material 3 深色色板（surface #141218、primary #D0BCFF 等）
- `values/themes.xml` 与 `values-night/themes.xml` 分别用 Light / Dark 平台主题，状态栏图标颜色随之切换

**交互**
- 风险告知改为**每次进入 App 都弹**；点「取消」直接退出 App；只有点过「不再提示」才永久关闭
- 修正展开「自定义渲染比例」后的间距（开关 → 22dp → 标题 → 滑条 → 22dp → 短边 → 10dp → 说明）
---

## 16. v1.8：滑条手势修复 + 预设档

- **拖动断触修复**：`MiuiSlider` 在 `ACTION_DOWN` 时调用 `getParent().requestDisallowInterceptTouchEvent(true)`，
  拖动期间外层 `ScrollView` 不再抢纵向滑动；`UP/CANCEL` 时恢复。手指稍微上下偏一点也不会中断拖动。
- **只取水平位移**：`updateFromTouch(x)` 只按横坐标算值并夹在轨道范围内，纵向抖动完全忽略。
- **新增四个预设档**：滑条下方一排 `100% / 90% / 80% / 70%` 胶囊按钮，等宽、4dp 间距；
  选中项为主色容器底 + 深色文字，未选中为描边；点一下直接跳到该比例，拖动滑条时选中态同步更新。
---

## 17. v1.9：说明可回看、输入框对齐、范围 0–100、拖动不闪

- **说明随时可看**：展开区底部新增「查看该功能说明」，风险卡片「查看完整风险告知」下方新增「查看渲染比例说明」。
  两处都打开同一个只读说明弹窗（单「知道了」按钮），即使之前点过「不再提示」也能重新看到内容。
- **输入框宽度对齐**：`Ui.field` 统一设为 `MATCH_PARENT`，渲染基准短边与画质/帧率输入框宽度一致。
- **下拉菜单圆角**：Spinner 的 popup 背景改为 16dp 圆角的 `surfaceContainerHigh`，并加 4dp 水平/垂直偏移，观感与输入框对齐。
- **渲染比例范围 0%–100%**：滑条最大 100、直接对应百分比；`ConfigPlan` 与模块侧校验同步放宽到 0。
- **拖动不再闪**：滑条新增 start/commit 回调；拖动过程中只更新文案与「将写入」，**预设档不高亮**；
  松手（或取消）时才按最终值切换高亮。
---

## 18. v2.0：滑条对齐、比例标题重做、瀑布下拉

- **滑条对齐**：轨道改为铺满整宽（0→width），圆钮在两端各留一个半径的内缩；左端与标题、预设档、输入框严格同一条竖线。
- **比例标题重做**：原来是 `100%（原生目标）` 一行小字，现在改成
  大号百分比（30sp 粗体）+ 下方说明（12sp）：
  `100%` / `原生分辨率，不额外限制`；低于 100% 时显示 `目标短边 960 px（1200 × 80%）`。
- **说明入口挪位**：去掉底部整行按钮，改成开关副标题下方的下划线小链接「查看功能说明」，与开关同区；
  风险卡片仍保留「查看渲染比例说明」按钮。
- **瀑布式下拉**：自绘 `WaterfallSelect` 替换系统 Spinner——点击后列表高度 0→N×44dp 动画展开、
  箭头旋转 180°，选中后原路收回；两个下拉互斥（打开一个自动收起另一个）；箭头是 Canvas 画的，不依赖字体符号。
---

## 19. v2.1：下拉回退、去加粗、新增「读取当前配置」

- **下拉改回 Spinner**：瀑布展开效果弃用（`WaterfallSelect` 已删除），恢复系统 Spinner + 16dp 圆角浮层。
- **去加粗**：适配器里强制 `Typeface.NORMAL`，选中的项不再自动变粗；预设档文字同样不加粗。
- **宽度对齐**：所有输入框（画质/帧率/短边）统一 `MATCH_PARENT`，宽度一致。
- **新增「读取当前配置」**：
  1. 点按钮 → 请求读取（写入 `pendingAction=read`）；
  2. 重启游戏 → 模块在启动时**只读**解密 Engine.ini、读取 Game.ini；
  3. 通过 `ContentProvider.call("reportConfig", …)` 把结果回传到 App 的 SharedPreferences；
  4. 回到 App 再点「读取当前配置」即可看到内容（画质/帧率/Unlimit/White 标志/Pad 值/文件大小/设备/游戏版本/读取时间），
     弹窗里可「重新读取」。全过程不写游戏文件。
---

## 20. v2.2：输入框宽度真正对齐 + 删除读取配置

- **宽度对齐的根因**：折叠区 `renderSection` 之前以 `WRAP_CONTENT` 加入卡片，导致里面的 `MATCH_PARENT` 子控件拿到的是
  `AT_MOST` 测量规格，最终各框按自身内容宽度收敛，于是画质/帧率与短边框宽度不一致。
  修复：`renderSection` 显式 `MATCH_PARENT`，三个输入框也统一用 `fullWidth()` 添加，拿到 `EXACTLY` 规格，宽度完全一致。
- **删除「读取当前配置」**：连同 App 按钮、`readConfig()`、模块里的 `pendingAction=read` 分支与 `buildReport` 辅助方法、
  Provider 的 `call("reportConfig")`、以及相关 Prefs 键一起移除，dex 中已无残留。
---

## 21. v1.0（重置版）：去阴影 + 保存后询问启动游戏

- **去掉所有阴影**：`Ui.card()` 与 `Ui.filled()` 的 `elevation` 全部归 0，卡片只保留底色与 1dp 描边，整体更干净。
- **保存后可直接启动游戏**：点「保存并下次启动生效」→ 确认写入 → 保存完成后再弹一个窗：
  「设置已保存，会在下次启动游戏时写入。现在启动《三角洲行动》吗？」→ 确认则 `getLaunchIntentForPackage("com.tencent.tmgp.dfm")` 拉起游戏，取消则留在本页。
- 版本号维持 `versionCode=1` / `versionName=1.0`。