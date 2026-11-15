package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 编译占位：真实实现由 LSPosed 在运行时提供（不会打进 APK）。
 * 签名与 LSPosed/XposedBridge API 82+ 保持一致。
 */
public interface IXposedHookLoadPackage {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}
