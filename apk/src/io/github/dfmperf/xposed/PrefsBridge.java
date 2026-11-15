package io.github.dfmperf.xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import de.robv.android.xposed.XSharedPreferences;
import io.github.dfmperf.Prefs;

/**
 * 读取 App 里的设置：优先走 ContentProvider（可靠），失败再退回 XSharedPreferences。
 * 两条路都失败时返回默认值，并在日志里说明用了哪条。
 */
public final class PrefsBridge {
    private static final String AUTHORITY = "io.github.dfmperf.settings";
    private static String source = "none";
    private static String error = "";
    private static XSharedPreferences fallback;

    private PrefsBridge() {}

    public static String source() {
        return source;
    }

    public static String error() {
        return error;
    }

    private static String query(Context c, String key) {
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(
                    Uri.parse("content://" + AUTHORITY + "/" + key), null, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                return cur.getString(0);
            }
            error = "provider 无数据";
        } catch (Throwable t) {
            error = "provider 失败: " + t;
        } finally {
            if (cur != null) cur.close();
        }
        return null;
    }

    private static void ensureFallback() {
        if (fallback != null) return;
        try {
            fallback = new XSharedPreferences("io.github.dfmperf", Prefs.NAME);
            fallback.reload();
        } catch (Throwable t) {
            error = error + " | XSharedPreferences 失败: " + t;
        }
    }

    public static String getString(Context c, String key, String def) {
        String v = query(c, key);
        if (v != null) {
            source = "provider";
            return v;
        }
        ensureFallback();
        if (fallback != null) {
            source = "XSharedPreferences";
            return fallback.getString(key, def);
        }
        source = "default";
        return def;
    }

    public static int getInt(Context c, String key, int def) {
        String v = query(c, key);
        if (v != null) {
            source = "provider";
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        ensureFallback();
        if (fallback != null) {
            source = "XSharedPreferences";
            return fallback.getInt(key, def);
        }
        source = "default";
        return def;
    }

    public static boolean getBoolean(Context c, String key, boolean def) {
        String v = query(c, key);
        if (v != null) {
            source = "provider";
            return "1".equals(v.trim()) || "true".equalsIgnoreCase(v.trim());
        }
        ensureFallback();
        if (fallback != null) {
            source = "XSharedPreferences";
            return fallback.getBoolean(key, def);
        }
        source = "default";
        return def;
    }
}
