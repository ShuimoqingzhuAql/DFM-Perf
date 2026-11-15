package de.robv.android.xposed;

import java.lang.reflect.Member;

/** 编译占位（运行时由 LSPosed 提供）。 */
public abstract class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        private Object result;

        public Object getResult() { return result; }
        public void setResult(Object r) { this.result = r; }
        public Throwable getThrowable() { return null; }
    }

    /** 真实 API 里 unhook() 返回 void。 */
    public static class Unhook {
        public void unhook() {}
    }
}
