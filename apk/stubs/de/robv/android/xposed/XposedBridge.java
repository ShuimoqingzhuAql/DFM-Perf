package de.robv.android.xposed;

import java.lang.reflect.Member;

/** 编译占位（运行时由 LSPosed 提供）。返回值类型必须与真实 API 一致。 */
public final class XposedBridge {
    private XposedBridge() {}

    public static void log(String text) {}
    public static void log(Throwable t) {}

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        throw new UnsupportedOperationException();
    }
}
