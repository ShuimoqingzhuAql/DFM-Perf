# DFM 画质工坊

《三角洲行动》国服（`com.tencent.tmgp.dfm`）的画质 / 帧率配置工具，提供两种形态：

| 形态 | 说明 |
|---|---|
| **LSPosed 模块 APK** | `DFM-Perf-v1.0.apk`，免 root，游戏启动时在进程内写入配置 |
| **Shell 脚本** | `dfm_perf.sh`（需 root + openssl），另有 MT 管理器用的无参包装脚本 |

两者写入的是同一套配置：`Engine.ini`（加密容器）里的画质/帧率字段，以及 `Game.ini` 里的渲染分辨率。

> ⚠️ 修改游戏配置可能触发反作弊，导致账号受限或封禁；可能违反用户协议；不保证实测帧率。
> 本项目仅供学习研究，使用风险自负。

---

## 目录结构

```
.
├── DFM-Perf-v1.0.apk          # 可直接安装的 LSPosed 模块
├── apk/                        # 模块完整源码（未加密、未混淆）
│   ├── src/io/github/dfmperf/  # 界面、配置引擎、LSPosed 入口
│   ├── res/                    # 纯矢量图标 + 主题
│   ├── stubs/                  # Xposed API 编译占位（不进 APK）
│   ├── test/                   # JVM 侧测试
│   ├── build.ps1               # aapt2 → javac → d8 → zipalign → apksigner
│   └── README.md               # 模块详细说明（安装、排错、限制）
├── 逆向分析报告.md              # 完整逆向报告（容器格式、libUE4 材料定位、事务、字段映射）
├── REA_独立分析.md              # 独立的第二通道方法级分析
├── dfm_perf.sh                 # root 脚本版
├── mt_*.sh                     # MT 管理器用包装脚本
├── interop/                    # 与原工具 ConfigCrypto 的互操作测试
├── work/scripts/               # 逆向用的 Python 分析脚本
└── ui-preview-v100.png         # 界面预览
```

## 安装（LSPosed）

1. 安装 `DFM-Perf-v1.0.apk`；
2. LSPosed 管理器 → 模块 → 启用「DFM 画质工坊」；
3. 作用域只勾选 **三角洲行动**（`com.tencent.tmgp.dfm`）；
4. 打开 App 选择画质 / 帧率（可选渲染比例）→ 保存并下次启动生效；
5. 完全退出游戏后启动，模块会在游戏读配置之前写入。

结果看 LSPosed 日志，标签 `DFMPerf`。

## 从源码构建

需要 JDK 25（Android Studio 自带 JBR 即可）、Android SDK build-tools 36.x、`platforms/android-35/android.jar`。

```powershell
cd apk
powershell -ExecutionPolicy Bypass -File .\build.ps1
# 产物: apk\out\DFM-Perf-v1.0.apk
```

`build.ps1` 会先把源码复制到纯 ASCII 路径再构建（避开 aapt2 对中文路径的报错），签名密钥首次运行自动生成在 `apk/dfmperf.keystore`（口令 `dfmperf`）。

## 原理概要

```
libUE4.so 内 32 字节材料
   └─ 解密 UserGameplay.ini 的 Hash → 40 位十六进制摘要
        └─ key = 材料 ⊕ 摘要
             └─ 解密 Engine.ini 的 Token → 明文 INI
```

容器格式：`Base64(HMAC-SHA1(block) || AES-256-ECB(block))`，无填充、定长头。
模块 hook `Instrumentation.callApplicationOnCreate`，在 `Application.onCreate` 之前完成「读 → 改 → 重新加密 → 回读校验 → 原子写入」，失败自动回滚。

细节见 [`逆向分析报告.md`](逆向分析报告.md)。

## 已知限制

- 游戏内改动画质/帧率会覆盖当次会话，重启后回到 App 保存的档位；
- 「终极」档有独立的资源表帧率上限，配置档位 ≠ 实测帧率；
- 阵亡镜头、设置界面等场景的 30 帧锁是游戏设计，文件层改不了；
- 游戏更新若改变 `libUE4.so` 布局，模块会走扫描回退；认证失败时拒绝写入。

## 免责声明

本项目仅用于逆向工程学习与本地研究，按「现状」提供，不提供任何担保。
因使用造成的账号、数据或设备问题，由使用者自行承担。

## 许可证

本项目使用 **MIT License**（见 [LICENSE](LICENSE)）：

- 允许商用、修改、再发布、私有使用；
- 唯一要求是保留版权声明与许可证文本；
- 不提供任何担保。

> 如果你希望更彻底（放弃署名、等同公有领域），可以把 LICENSE 换成 Unlicense 或 CC0-1.0，我可以帮你替换。