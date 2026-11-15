package io.github.dfmperf.xposed;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.dfmperf.Prefs;
import io.github.dfmperf.core.ConfigCrypto;
import io.github.dfmperf.core.ConfigPlan;
import io.github.dfmperf.core.Diagnostics;
import io.github.dfmperf.core.Material;
import io.github.dfmperf.core.Repo;

/**
 * LSPosed 入口：在游戏主进程创建 Application 之前，把目标档位写进 Engine.ini / Game.ini。
 * 运行在游戏进程内（游戏自己的 UID），因此不需要 root。
 *
 * 只做两件事：按设置写入、或删除本工具写入的覆盖项。不保留备份、不常驻、不改游戏代码。
 */
public final class Entry implements IXposedHookLoadPackage {
    private static final String TAG = "DFMPerf";
    private static final String PKG = "com.tencent.tmgp.dfm";
    private static boolean done;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName)) return;
        if (lp.processName != null && !PKG.equals(lp.processName)) return; // 只处理主进程
        log("已注入 " + lp.processName);

        final XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Application app = (Application) param.args[0];
                    if (app != null) run(app);
                } catch (Throwable t) {
                    log("执行失败: " + t);
                    XposedBridge.log(t);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lp.classLoader,
                    "callApplicationOnCreate", Application.class, hook);
            log("hook 方式: findAndHookMethod");
        } catch (Throwable t1) {
            log("findAndHookMethod 失败(" + t1 + ")，改用反射 hookMethod");
            try {
                java.lang.reflect.Method m = android.app.Instrumentation.class
                        .getDeclaredMethod("callApplicationOnCreate", Application.class);
                XposedBridge.hookMethod(m, hook);
                log("hook 方式: hookMethod");
            } catch (Throwable t2) {
                log("挂载失败: " + t2);
                XposedBridge.log(t2);
            }
        }
    }

    private static synchronized void run(Application app) {
        if (done) return;
        done = true;
        try {
            String mode = PrefsBridge.getString(app, Prefs.MODE, "off");
            int quality = clamp(PrefsBridge.getInt(app, Prefs.QUALITY, 3), 0, 5);
            int fps = PrefsBridge.getInt(app, Prefs.FPS, 2);
            boolean customRender = PrefsBridge.getBoolean(app, Prefs.RENDER_CUSTOM, false);
            int ratio = customRender ? clamp(PrefsBridge.getInt(app, Prefs.RATIO, 100), 0, 100) : 100;
            int shortEdge = clamp(PrefsBridge.getInt(app, Prefs.SHORT_EDGE, 1080), 200, 8192);
            log("设置来源=" + PrefsBridge.source() + " mode=" + mode + " quality=" + quality
                    + " fps=" + fps + " 自定义渲染=" + customRender + " ratio=" + ratio
                    + " shortEdge=" + shortEdge
                    + (PrefsBridge.error().isEmpty() ? "" : " | " + PrefsBridge.error()));

            if (!"apply".equals(mode) && !"defaults".equals(mode)) {
                log("模块未启用改动（mode=" + mode + "）；请回 App 选好档位后点“保存并下次启动生效”");
                return;
            }

            ApplicationInfo info = app.getApplicationInfo();
            File dataDir = new File(info.dataDir);
            Repo repo = Repo.forGame(dataDir, PKG, new File(info.nativeLibraryDir), app.getExternalFilesDir(null));
            if (!repo.engine.isFile()) {
                log("找不到 Engine.ini，跳过（请先启动一次游戏）: " + repo.engine);
                return;
            }
            if (repo.gameplay == null || !repo.gameplay.isFile()) {
                log("找不到 UserGameplay.ini，跳过: " + repo.gameplay);
                return;
            }
            log("设备: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                    + " / Android " + android.os.Build.VERSION.RELEASE);

            byte[] gameplay = Repo.readFile(repo.gameplay, 2 * 1024 * 1024);
            Material m = Material.resolve(repo.lib, gameplay);
            log("材料来源: " + m.source + "，编码 0x" + Integer.toHexString(m.encoding));

            byte[] eng = null;
            byte[] gam = null;
            try {
                eng = Repo.readFile(repo.engine, 2 * 1024 * 1024);
                gam = Repo.readFile(repo.game, 2 * 1024 * 1024);
                log("读取时实际值: " + Diagnostics.summary(ConfigCrypto.decodeIni(eng, m.key),
                        new String(gam, "UTF-8")));
            } catch (Throwable t) {
                log("读取当前值失败: " + t);
            }

            String result = "defaults".equals(mode)
                    ? repo.removeOverrides(m)
                    : repo.apply(m, quality, fps, ratio, shortEdge);
            log("完成: " + result);
        } catch (Throwable t) {
            log("异常: " + t);
            XposedBridge.log(t);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static void log(String s) {
        XposedBridge.log(TAG + ": " + s);
    }
}
