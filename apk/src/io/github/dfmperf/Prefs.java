package io.github.dfmperf;

import android.content.Context;
import android.content.SharedPreferences;

/** 设置项的键名与取值（App 写、模块读）。 */
public final class Prefs {
    public static final String NAME = "settings";

    public static final String MODE = "mode";              // apply / defaults / off
    public static final String QUALITY = "quality";        // 0..5
    public static final String FPS = "fps";                // 2,7,8,9,10,11
    public static final String RATIO = "ratio";            // 25..100（仅 renderCustom 时生效）
    public static final String SHORT_EDGE = "shortEdge";   // 200..8192
    public static final String RENDER_CUSTOM = "renderCustom"; // 是否启用自定义渲染比例

    public static final String RISK_NEVER = "riskNeverShow";
    public static final String RENDER_HINT_NEVER = "renderHintNever";
    public static final String ULTIMATE_HINT_NEVER = "ultimateHintNever";
    public static final String SCOPE_HINT_SHOWN = "scopeHintShown";

    private Prefs() {}

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean riskNeverShow(Context c) {
        return get(c).getBoolean(RISK_NEVER, false);
    }

    public static void setRiskNeverShow(Context c, boolean v) {
        get(c).edit().putBoolean(RISK_NEVER, v).commit();
        makeReadable(c);
    }

    public static boolean renderHintNever(Context c) {
        return get(c).getBoolean(RENDER_HINT_NEVER, false);
    }

    public static void setRenderHintNever(Context c, boolean v) {
        get(c).edit().putBoolean(RENDER_HINT_NEVER, v).commit();
        makeReadable(c);
    }

    public static boolean scopeHintShown(Context c) {
        return get(c).getBoolean(SCOPE_HINT_SHOWN, false);
    }

    public static void setScopeHintShown(Context c, boolean v) {
        get(c).edit().putBoolean(SCOPE_HINT_SHOWN, v).commit();
        makeReadable(c);
    }

    public static boolean ultimateHintNever(Context c) {
        return get(c).getBoolean(ULTIMATE_HINT_NEVER, false);
    }

    public static void setUltimateHintNever(Context c, boolean v) {
        get(c).edit().putBoolean(ULTIMATE_HINT_NEVER, v).commit();
        makeReadable(c);
    }

    /** 让模块的两种读取通道都能拿到最新值。 */
    public static void makeReadable(Context c) {
        try {
            java.io.File f = new java.io.File(c.getApplicationInfo().dataDir,
                    "shared_prefs/" + NAME + ".xml");
            if (f.isFile()) f.setReadable(true, false);
        } catch (Throwable ignored) {
        }
    }
}
