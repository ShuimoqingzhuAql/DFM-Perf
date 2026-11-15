package de.robv.android.xposed;

/** 编译占位（运行时由 LSPosed 提供）。 */
public class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {}

    public void reload() {}
    public boolean makeWorldReadable() { return true; }
    public String getString(String key, String defValue) { return defValue; }
    public int getInt(String key, int defValue) { return defValue; }
    public long getLong(String key, long defValue) { return defValue; }
    public boolean getBoolean(String key, boolean defValue) { return defValue; }
}
