package de.robv.android.xposed;

/**
 * 编译占位（运行时由 LSPosed 提供）。
 *
 * 注意：签名必须与真实 API 完全一致，包括**返回值类型**。
 * 真实定义：
 *   public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader,
 *                                                        String methodName, Object... args)
 * 若这里写成 void，dex 里方法描述符会变成 ...)V，运行时就会 NoSuchMethodError。
 */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader,
                                                         String methodName, Object... parameterTypesAndCallback) {
        throw new UnsupportedOperationException();
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName,
                                                         Object... parameterTypesAndCallback) {
        throw new UnsupportedOperationException();
    }

    public static Object getObjectField(Object obj, String fieldName) {
        throw new UnsupportedOperationException();
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new UnsupportedOperationException();
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new UnsupportedOperationException();
    }
}
