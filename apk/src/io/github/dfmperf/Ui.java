package io.github.dfmperf;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 极简 Material 3 视觉工具类（不依赖 AppCompat / Material Components 库）。
 * 颜色在 {@link #init(Context)} 里按系统深色模式赋值，App 与弹窗共用。
 */
public final class Ui {
    // Material 3 浅色
    public static int PRIMARY = 0xFF6750A4;
    public static int ON_PRIMARY = 0xFFFFFFFF;
    public static int PRIMARY_CONTAINER = 0xFFEADDFF;
    public static int ON_PRIMARY_CONTAINER = 0xFF21005D;
    public static int SURFACE = 0xFFFFFBFE;
    public static int SURFACE_CONTAINER = 0xFFF3EDF7;
    public static int SURFACE_CONTAINER_HIGH = 0xFFECE6F0;
    public static int SURFACE_VARIANT = 0xFFE7E0EC;
    public static int ON_SURFACE = 0xFF1C1B1F;
    public static int ON_SURFACE_VARIANT = 0xFF49454F;
    public static int OUTLINE = 0xFF79747E;
    public static int OUTLINE_VARIANT = 0xFFCAC4D0;
    public static int ERROR = 0xFFB3261E;
    public static int TRACK = 0xFFD9D9DE;
    public static int KNOB = 0xFFFFFFFF;

    private static boolean dark;

    private Ui() {}

    public static boolean isDark() {
        return dark;
    }

    /** 在 Activity.onCreate 最开始调用。 */
    public static void init(Context c) {
        dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (dark) {
            PRIMARY = 0xFFD0BCFF;
            ON_PRIMARY = 0xFF381E72;
            PRIMARY_CONTAINER = 0xFF4F378B;
            ON_PRIMARY_CONTAINER = 0xFFEADDFF;
            SURFACE = 0xFF141218;
            SURFACE_CONTAINER = 0xFF211F26;
            SURFACE_CONTAINER_HIGH = 0xFF2B2930;
            SURFACE_VARIANT = 0xFF49454F;
            ON_SURFACE = 0xFFE6E0E9;
            ON_SURFACE_VARIANT = 0xFFCAC4D0;
            OUTLINE = 0xFF938F99;
            OUTLINE_VARIANT = 0xFF49454F;
            ERROR = 0xFFF2B8B5;
            TRACK = 0xFF3A3A3C;
            KNOB = 0xFFE6E0E9;
        } else {
            PRIMARY = 0xFF6750A4;
            ON_PRIMARY = 0xFFFFFFFF;
            PRIMARY_CONTAINER = 0xFFEADDFF;
            ON_PRIMARY_CONTAINER = 0xFF21005D;
            SURFACE = 0xFFFFFBFE;
            SURFACE_CONTAINER = 0xFFF3EDF7;
            SURFACE_CONTAINER_HIGH = 0xFFECE6F0;
            SURFACE_VARIANT = 0xFFE7E0EC;
            ON_SURFACE = 0xFF1C1B1F;
            ON_SURFACE_VARIANT = 0xFF49454F;
            OUTLINE = 0xFF79747E;
            OUTLINE_VARIANT = 0xFFCAC4D0;
            ERROR = 0xFFB3261E;
            TRACK = 0xFFD9D9DE;
            KNOB = 0xFFFFFFFF;
        }
    }


    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int statusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dp(c, 24);
    }

    public static Drawable fill(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static Drawable stroke(Context c, int color, float radiusDp, float widthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0x00000000);
        d.setCornerRadius(dp(c, radiusDp));
        d.setStroke(dp(c, widthDp), color);
        return d;
    }

    public static Drawable ripple(Context c, Drawable content) {
        int base = PRIMARY;
        int rippleColor = (base & 0x00FFFFFF) | 0x33000000;
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setLineSpacing(dp(c, 2), 1f);
        return t;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Material 3 「filled card」：圆角 20dp、surfaceContainer 底、1dp 阴影。 */
    public static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        l.setBackground(fill(c, SURFACE_CONTAINER, 20));
        l.setElevation(0f);
        int p = dp(c, 20);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static Button filled(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(ON_PRIMARY);
        b.setBackground(ripple(c, fill(c, PRIMARY, 24)));
        b.setMinHeight(dp(c, 52));
        b.setPadding(dp(c, 24), 0, dp(c, 24), 0);
        b.setStateListAnimator(null);
        b.setElevation(0f);
        return b;
    }

    public static Button outlined(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(PRIMARY);
        b.setBackground(ripple(c, stroke(c, OUTLINE_VARIANT, 24, 1)));
        b.setMinHeight(dp(c, 52));
        b.setPadding(dp(c, 20), 0, dp(c, 20), 0);
        b.setStateListAnimator(null);
        return b;
    }

    public static Button textButton(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(PRIMARY);
        b.setBackground(ripple(c, fill(c, 0x00000000, 24)));
        b.setMinHeight(dp(c, 44));
        b.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        b.setStateListAnimator(null);
        return b;
    }

    /** 输入框外框：12dp 圆角 + 1dp outline 描边，内部控件背景置空。 */
    public static LinearLayout field(Context c, View inner) {
        LinearLayout box = column(c);
        box.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.setBackground(stroke(c, OUTLINE_VARIANT, 12, 1));
        int p = dp(c, 12);
        box.setPadding(p, p, p, p);
        inner.setBackground(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        box.addView(inner, lp);
        return box;
    }
}
