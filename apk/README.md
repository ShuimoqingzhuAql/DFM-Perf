# apk 源码

这是 DFM 画质工坊的完整源码，没加壳也没混淆，改完直接跑 `build.ps1` 就能出包。

## 目录

```
apk/
├── AndroidManifest.xml      模块声明、入口、图标 alias
├── assets/xposed_init       LSPosed 入口类名
├── res/                     矢量图标与主题
├── src/io/github/dfmperf/
│   ├── MainActivity.java    设置界面
│   ├── RiskDialog.java      风险告知与二次确认
│   ├── MiuiSwitch.java      自绘开关
│   ├── MiuiSlider.java      自绘滑条
│   ├── SettingsProvider.java 设置读取通道
│   ├── Prefs.java           设置键名
│   ├── xposed/Entry.java    LSPosed 入口与写入逻辑
│   └── core/
│       ├── ConfigCrypto.java 容器加解密
│       ├── Ini.java          INI 读写
│       ├── ConfigPlan.java   档位与渲染值计算
│       ├── Material.java     libUE4.so 材料定位
│       └── Repo.java         读取、写入、回滚
├── stubs/                   Xposed API 编译占位（不进 APK）
└── test/                    JVM 侧测试
```

## 构建

需要 JDK 25、Android SDK build-tools 36.x、`platforms/android-35`。

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

产物在 `out/`。脚本会先把源码复制到 `E:\dfm_apk_build` 再编译，因为 aapt2 碰到中文路径会报错。签名密钥存在 `dfmperf.keystore`，第一次运行自动生成，口令 `dfmperf`。

## 实现要点

- 入口用 `Instrumentation.callApplicationOnCreate` 的 before hook，跑在游戏 `Application.onCreate` 之前，保证在引擎读配置之前写完。
- 材料定位先按 `libUE4.so` 的 SHA-256 查固定偏移，查不到就在可执行段里找“两条 ADRP + 两条 LDR q”的指令桩，任何候选都要过 `UserGameplay.ini` 的 HMAC 才算数。
- 写入走临时文件加回读校验，两个文件都验证通过才算完成；中途任何一步失败就用内存里的原内容回滚，磁盘上不留备份文件。
- `UserGameplay.ini` 只读，前后都用文件大小和内容确认没被改动。

## 排错

先看 LSPosed 日志，标签 `DFMPerf`，正常会有这几行：

```
已注入 com.tencent.tmgp.dfm
hook 方式: findAndHookMethod
设置来源=provider mode=apply quality=5 fps=8 ...
材料来源: 已知偏移 ...
读取时实际值: 画质=... 帧率枚举=...
完成: 已写入并回读校验：终极 / 120 帧 / 渲染 100%
```

只到“模块未启用改动”说明没保存设置；卡在“材料来源”说明认证没过，游戏可能更新了；写入类报错会带具体文件名和步骤。

## 注意

改的是游戏自己的配置文件，可能触发反作弊，风险自负。
