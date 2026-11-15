package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

/** 编译占位（运行时由 LSPosed 提供）。 */
public final class XC_LoadPackage {
    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}
