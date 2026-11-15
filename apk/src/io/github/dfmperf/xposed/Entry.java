package io.github.dfmperf.xposed;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.dfmperf.Prefs;
import io.github.dfmperf.core.Material;
import io.github.dfmperf.core.Repo;

/**
 * LSPosed 入口。
 *
 * 写入时机：ActivityThread.handleBindApplication 的 before hook，
 * 在 Application 创建、任何初始化之前完成，效果等同于游戏启动前就把文件改好。
 * 若该 hook 不可用，退回 Instrumentation.callApplicationOnCreate。
 */
public final class Entry implements IXposedHookLoadPackage {
    private static final String TAG = "DFMPerf";
    private static final String PKG = "com.tencent.tmgp.dfm";
    private static final String EXT_DIR = "/storage/emulated/0/Android/data/" + PKG + "/files";

    private static boolean done;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName)) return;
        if (lp.processName != null && !PKG.equals(lp.processName)) return;
        log("已注入 " + lp.processName);

        try {
            XposedHelpers.findAndHookMethod("android.app.ActivityThread", lp.classLoader,
                    "handleBindApplication", "android.app.ActivityThread$AppBindData",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object data = param.args[0];
                                ApplicationInfo info = (ApplicationInfo)
                                        XposedHelpers.getObjectField(data, "appInfo");
                                String proc = (String) XposedHelpers.getObjectField(data, "processName");
                                if (info == null || !PKG.equals(info.packageName)) return;
                                if (proc != null && !PKG.equals(proc)) return;
                                run(info, systemContext(), new File(EXT_DIR), "提前写入");
                            } catch (Throwable t) {
                                log("提前写入失败: " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            log("hook handleBindApplication 失败: " + t);
        }

        final XC_MethodHook late = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Application app = (Application) param.args[0];
                    if (app == null || done) return;
                    run(app.getApplicationInfo(), app, app.getExternalFilesDir(null), "兜底写入");
                } catch (Throwable t) {
                    log("执行失败: " + t);
                    XposedBridge.log(t);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.Instrumentation", lp.classLoader,
                    "callApplicationOnCreate", Application.class, late);
        } catch (Throwable t) {
            try {
                java.lang.reflect.Method m = android.app.Instrumentation.class
                        .getDeclaredMethod("callApplicationOnCreate", Application.class);
                XposedBridge.hookMethod(m, late);
            } catch (Throwable t2) {
                log("挂载失败: " + t2);
            }
        }
    }

    private static Context systemContext() {
        try {
            Object at = XposedHelpers.callStaticMethod(
                    Class.forName("android.app.ActivityThread"), "currentActivityThread");
            return (Context) XposedHelpers.callMethod(at, "getSystemContext");
        } catch (Throwable t) {
            log("取系统 Context 失败: " + t);
            return null;
        }
    }

    private static synchronized void run(ApplicationInfo info, Context ctx, File externalDir, String stage) {
        if (done) return;
        done = true;
        try {
            String mode = PrefsBridge.getString(ctx, Prefs.MODE, "off");
            int quality = clamp(PrefsBridge.getInt(ctx, Prefs.QUALITY, 3), 0, 5);
            int fps = PrefsBridge.getInt(ctx, Prefs.FPS, 2);
            boolean customRender = PrefsBridge.getBoolean(ctx, Prefs.RENDER_CUSTOM, false);
            int ratio = customRender ? clamp(PrefsBridge.getInt(ctx, Prefs.RATIO, 100), 0, 100) : 100;
            int shortEdge = clamp(PrefsBridge.getInt(ctx, Prefs.SHORT_EDGE, 1080), 200, 8192);
            log(stage + " 设置来源=" + PrefsBridge.source() + " mode=" + mode
                    + " quality=" + quality + " fps=" + fps + " ratio=" + ratio
                    + " shortEdge=" + shortEdge
                    + (PrefsBridge.error().isEmpty() ? "" : " | " + PrefsBridge.error()));

            if (!"apply".equals(mode) && !"defaults".equals(mode)) {
                log("模块未启用改动（mode=" + mode + "）");
                return;
            }

            Repo repo = Repo.forGame(new File(info.dataDir), PKG,
                    new File(info.nativeLibraryDir), externalDir);
            if (!repo.engine.isFile() || repo.gameplay == null || !repo.gameplay.isFile()) {
                log("缺少配置文件，跳过: " + repo.engine);
                return;
            }
            log("设备: " + Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE);

            Material m = Material.resolve(repo.lib, Repo.readFile(repo.gameplay, 2 * 1024 * 1024));
            log("材料来源: " + m.source);

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
