package io.github.dfmperf;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import io.github.dfmperf.core.ConfigPlan;

/** Material 3 风格设置界面（支持深色模式）。写入 SharedPreferences，LSPosed 模块在游戏启动时读取。 */
public class MainActivity extends Activity {
    private Spinner qualitySpinner;
    private Spinner fpsSpinner;
    private MiuiSlider ratioBar;
    private EditText shortEdgeEdit;
    private TextView ratioValue;
    private TextView ratioCaption;
    private TextView targetText;
    private TextView statusText;
    private MiuiSwitch renderSwitch;
    private MiuiSwitch iconSwitch;
    private boolean iconSuppress = false;
    private static final String LAUNCHER_ALIAS = "io.github.dfmperf.Launcher";
    private LinearLayout renderSection;

    private boolean suppress = false;
    private boolean dragging = false;
    private final int[] presetValues = {100, 90, 80, 70};
    private TextView[] presetChips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        applyWindow();
        setContentView(buildContent());
        loadPrefs();
        updateLabels();
        if (!Prefs.riskNeverShow(this)) {
            RiskDialog.showRisk(this, true, new Runnable() {
                @Override public void run() { }
            });
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    private void applyWindow() {
        Window w = getWindow();
        w.setStatusBarColor(Ui.SURFACE);
        w.setNavigationBarColor(Ui.SURFACE);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = w.getDecorView().getSystemUiVisibility();
            if (Ui.isDark()) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private View buildContent() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.SURFACE);

        LinearLayout bar = Ui.column(this);
        int pad = Ui.dp(this, 20);
        bar.setPadding(pad, Ui.statusBarHeight(this) + Ui.dp(this, 14), pad, Ui.dp(this, 6));
        bar.addView(Ui.text(this, "DFM 画质工坊", 24, Ui.ON_SURFACE, true));
        TextView subtitle = Ui.text(this, "三角洲行动 · 国服 · LSPosed 模块 · v1.0", 12, Ui.ON_SURFACE_VARIANT, false);
        subtitle.setPadding(0, Ui.dp(this, 2), 0, 0);
        bar.addView(subtitle);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        LinearLayout content = Ui.column(this);
        content.setClipChildren(false);
        content.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 52));

        // ---- 当前状态 ----
        LinearLayout statusCard = Ui.card(this);
        statusCard.addView(Ui.text(this, "当前状态", 13, Ui.ON_SURFACE_VARIANT, true));
        statusText = Ui.text(this, "", 15, Ui.ON_SURFACE, false);
        statusText.setPadding(0, Ui.dp(this, 6), 0, 0);
        statusCard.addView(statusText);
        add(content, statusCard, 0);

        // ---- 目标设置 ----
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "目标设置", 15, Ui.ON_SURFACE, true));

        card.addView(label("画质档位"));
        qualitySpinner = spinner(ConfigPlan.QUALITY_LABELS);
        card.addView(Ui.field(this, qualitySpinner), fullWidth());

        card.addView(label("帧率档位"));
        fpsSpinner = spinner(ConfigPlan.FPS_LABELS);
        card.addView(Ui.field(this, fpsSpinner), fullWidth());

        // ---- 进阶开关 ----
        LinearLayout switchRow = Ui.row(this);
        switchRow.setPadding(0, Ui.dp(this, 22), 0, 0);
        LinearLayout switchText = Ui.column(this);
        switchText.addView(Ui.text(this, "自定义渲染比例（进阶）", 15, Ui.ON_SURFACE, true));
        switchText.addView(Ui.text(this, "默认关闭，保持原生分辨率。", 12, Ui.ON_SURFACE_VARIANT, false));
        TextView link = Ui.text(this, "查看功能说明", 12, Ui.PRIMARY, false);
        link.setPaintFlags(link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link.setPadding(0, Ui.dp(this, 6), 0, 0);
        link.setClickable(true);
        link.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { RiskDialog.showRenderInfo(MainActivity.this); }
        });
        switchText.addView(link);
        switchRow.addView(switchText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        renderSwitch = new MiuiSwitch(this);
        switchRow.addView(renderSwitch);
        card.addView(switchRow);

        // ---- 折叠区 ----
        renderSection = Ui.column(this);
        renderSection.setPadding(0, Ui.dp(this, 4), 0, 0);

        ratioValue = Ui.text(this, "100%", 30, Ui.ON_SURFACE, true);
        ratioValue.setPadding(0, Ui.dp(this, 18), 0, 0);
        renderSection.addView(ratioValue);

        ratioCaption = Ui.text(this, "", 12, Ui.ON_SURFACE_VARIANT, false);
        ratioCaption.setPadding(0, Ui.dp(this, 2), 0, 0);
        renderSection.addView(ratioCaption);

        ratioBar = new MiuiSlider(this);
        ratioBar.setMax(100);
        ratioBar.setProgress(100);
        LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sliderLp.topMargin = Ui.dp(this, 10);
        renderSection.addView(ratioBar, sliderLp);

        renderSection.addView(buildPresets());

        TextView shortEdgeLabel = label("渲染基准短边（像素）");
        shortEdgeLabel.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 6));
        renderSection.addView(shortEdgeLabel);

        shortEdgeEdit = new EditText(this);
        shortEdgeEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        shortEdgeEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        shortEdgeEdit.setTextColor(Ui.ON_SURFACE);
        shortEdgeEdit.setText(Integer.toString(defaultShortEdge()));
        renderSection.addView(Ui.field(this, shortEdgeEdit), fullWidth());

        TextView helper = Ui.text(this,
                "100% 时这一项不生效；低于 100% 时按“短边 × 比例”计算目标分辨率。",
                12, Ui.ON_SURFACE_VARIANT, false);
        helper.setPadding(0, Ui.dp(this, 10), 0, 0);
        renderSection.addView(helper);

        renderSection.setVisibility(View.GONE);
        card.addView(renderSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        targetText = Ui.text(this, "", 14, Ui.PRIMARY, true);
        targetText.setPadding(0, Ui.dp(this, 20), 0, 0);
        card.addView(targetText);
        add(content, card, 12);

        // ---- 操作 ----
        Button save = Ui.filled(this, "保存并下次启动生效");
        save.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmSave(); }
        });
        add(content, save, 16);

        Button defaults = Ui.outlined(this, "恢复默认");
        defaults.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        defaults.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                RiskDialog.confirm(MainActivity.this, "恢复默认",
                        "将删除本工具写入的画质、帧率与渲染覆盖项，默认档位由游戏下次启动重建。\n\n"
                                + "不会改动账号数据、安装包与 UserGameplay.ini。",
                        "恢复默认", new Runnable() {
                            @Override public void run() { saveCurrentValues("defaults"); }
                        });
            }
        });
        add(content, defaults, 10);

        Button check = Ui.outlined(this, "自检：检查模块能否读到设置");
        check.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selfCheck(); }
        });
        add(content, check, 10);

        // ---- 桌面图标 ----
        LinearLayout iconCard = Ui.card(this);
        iconCard.addView(Ui.text(this, "桌面图标", 15, Ui.ON_SURFACE, true));
        LinearLayout iconRow = Ui.row(this);
        iconRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        LinearLayout iconText = Ui.column(this);
        iconText.addView(Ui.text(this, "在桌面显示图标", 15, Ui.ON_SURFACE, true));
        iconText.addView(Ui.text(this, "关闭后桌面不再显示；在 LSPosed 模块页点右下角圆形按钮仍可进入。",
                12, Ui.ON_SURFACE_VARIANT, false));
        iconRow.addView(iconText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        iconSwitch = new MiuiSwitch(this);
        iconRow.addView(iconSwitch);
        iconCard.addView(iconRow);
        add(content, iconCard, 16);

        iconSuppress = true;
        iconSwitch.setChecked(aliasEnabled(), false);
        iconSuppress = false;
        iconSwitch.setOnCheckedChangeListener(new MiuiSwitch.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(MiuiSwitch view, boolean isChecked) {
                if (iconSuppress) return;
                if (isChecked) {
                    setAliasEnabled(true);
                    Toast.makeText(MainActivity.this, "已恢复桌面图标", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 先弹确认，用户取消就保持显示
                iconSuppress = true;
                iconSwitch.setChecked(true);
                iconSuppress = false;
                RiskDialog.confirm(MainActivity.this, "隐藏桌面图标",
                        "隐藏后桌面不再显示本应用图标。\n\n"
                                + "重新打开：LSPosed 管理器 → 模块 → DFM 画质工坊 → 点右下角的圆形按钮（▶），"
                                + "即可进入本页把图标重新打开。\n\n"
                                + "如果 LSPosed 里找不到入口，也可以在终端执行：\n"
                                + "su -c am start -n io.github.dfmperf/.MainActivity",
                        "隐藏", new Runnable() {
                            @Override public void run() {
                                setAliasEnabled(false);
                                iconSuppress = true;
                                iconSwitch.setChecked(false, true);
                                iconSuppress = false;
                                Toast.makeText(MainActivity.this, "已隐藏桌面图标", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // ---- 使用步骤 ----
        LinearLayout steps = Ui.card(this);
        steps.addView(Ui.text(this, "使用步骤", 15, Ui.ON_SURFACE, true));
        steps.addView(bodyText(
                "1. 在 LSPosed 中启用本模块，作用域勾选“三角洲行动”（com.tencent.tmgp.dfm）。\n\n"
              + "2. 在本页选好档位，点“保存并下次启动生效”。\n\n"
              + "3. 完全退出游戏（从最近任务划掉）后重新启动，模块会在游戏读配置之前写入。\n\n"
              + "4. 结果看 LSPosed 日志，标签 DFMPerf，例如“完成: 已写入并回读校验：终极 / 120 帧 / 渲染 100%”。\n\n"
              + "5. 在游戏内改动画质或帧率只会影响当次会话，重启游戏后会回到这里保存的档位。"));
        add(content, steps, 20);

        // ---- 风险 ----
        LinearLayout risk = Ui.card(this);
        risk.addView(Ui.text(this, "风险提示", 15, Ui.ON_SURFACE, true));
        risk.addView(bodyText("修改游戏配置可能触发反作弊，导致账号受限或封禁；可能违反用户协议；不保证实测帧率。仅供学习研究使用，风险自负。"));
        Button readRisk = Ui.outlined(this, "查看完整风险告知");
        readRisk.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        readRisk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { RiskDialog.showRiskInfo(MainActivity.this); }
        });
        add(risk, readRisk, 14);

        Button renderInfo = Ui.outlined(this, "查看渲染比例说明");
        renderInfo.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        renderInfo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { RiskDialog.showRenderInfo(MainActivity.this); }
        });
        add(risk, renderInfo, 8);
        add(content, risk, 12);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        renderSwitch.setOnCheckedChangeListener(new MiuiSwitch.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(MiuiSwitch view, boolean isChecked) {
                if (suppress) return;
                if (!isChecked) {
                    setRenderSectionVisible(false);
                    updateLabels();
                    return;
                }
                if (Prefs.renderHintNever(MainActivity.this)) {
                    setRenderSectionVisible(true);
                    updateLabels();
                    return;
                }
                RiskDialog.showRenderHint(MainActivity.this, new Runnable() {
                    @Override public void run() { setRenderSectionVisible(true); updateLabels(); }
                }, new Runnable() {
                    @Override public void run() {
                        suppress = true;
                        renderSwitch.setChecked(false);
                        suppress = false;
                        setRenderSectionVisible(false);
                        updateLabels();
                    }
                });
            }
        });

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLabels();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        qualitySpinner.setOnItemSelectedListener(listener);
        fpsSpinner.setOnItemSelectedListener(listener);

        ratioBar.setOnStartTracking(new MiuiSlider.OnStartTracking() {
            @Override public void onStartTracking(MiuiSlider view) { dragging = true; updateLabels(); }
        });
        ratioBar.setOnCommit(new MiuiSlider.OnCommit() {
            @Override public void onCommit(MiuiSlider view) { dragging = false; updateLabels(); }
        });
        ratioBar.setOnProgressChanged(new MiuiSlider.OnProgressChanged() {
            @Override public void onProgressChanged(MiuiSlider view, int progress) { updateLabels(); }
        });
        shortEdgeEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updateLabels(); }
        });
        return root;
    }

    /** 滑条下方的四个预设档。 */
    private View buildPresets() {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 12), 0, 0);
        presetChips = new TextView[presetValues.length];
        for (int i = 0; i < presetValues.length; i++) {
            final int value = presetValues[i];
            TextView chip = Ui.text(this, value + "%", 13, Ui.ON_SURFACE_VARIANT, false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            chip.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.leftMargin = Ui.dp(this, i == 0 ? 0 : 4);
            lp.rightMargin = Ui.dp(this, i == presetValues.length - 1 ? 0 : 4);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    renderSwitch.setChecked(true);
                    setRenderSectionVisible(true);
                    ratioBar.setProgress(value);
                }
            });
            row.addView(chip);
            presetChips[i] = chip;
        }
        return row;
    }

    private void updatePresetChips(int ratio) {
        if (presetChips == null) return;
        boolean on = renderSwitch != null && renderSwitch.isChecked();
        for (int i = 0; i < presetChips.length; i++) {
            boolean selected = on && ratio == presetValues[i];
            TextView chip = presetChips[i];
            if (selected) {
                chip.setBackground(Ui.ripple(this, Ui.fill(this, Ui.PRIMARY_CONTAINER, 14)));
                chip.setTextColor(Ui.ON_PRIMARY_CONTAINER);
            } else {
                chip.setBackground(Ui.ripple(this, Ui.stroke(this, Ui.OUTLINE_VARIANT, 14, 1)));
                chip.setTextColor(Ui.ON_SURFACE_VARIANT);
            }
        }
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void setRenderSectionVisible(boolean visible) {
        renderSection.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void add(LinearLayout parent, View v, int gapAboveDp) {
        ViewGroup.LayoutParams existing = v.getLayoutParams();
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (existing != null && existing.height > 0) height = existing.height;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.topMargin = Ui.dp(this, gapAboveDp);
        parent.addView(v, lp);
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 13, Ui.ON_SURFACE_VARIANT, true);
        t.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 6));
        return t;
    }

    private TextView bodyText(String s) {
        TextView t = Ui.text(this, s, 13, Ui.ON_SURFACE_VARIANT, false);
        t.setPadding(0, Ui.dp(this, 10), 0, 0);
        return t;
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                v.setTextColor(Ui.ON_SURFACE);
                v.setTypeface(null, android.graphics.Typeface.NORMAL);
                v.setPadding(0, Ui.dp(MainActivity.this, 2), 0, Ui.dp(MainActivity.this, 2));
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                v.setTextColor(Ui.ON_SURFACE);
                v.setTypeface(null, android.graphics.Typeface.NORMAL);
                v.setPadding(Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 12),
                        Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 12));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setPopupBackgroundDrawable(Ui.fill(this, Ui.SURFACE_CONTAINER_HIGH, 16));
        s.setDropDownHorizontalOffset(Ui.dp(this, 4));
        s.setDropDownVerticalOffset(Ui.dp(this, 4));
        s.setPadding(0, 0, 0, 0);
        return s;
    }

    private int defaultShortEdge() {
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        return Math.min(w, h);
    }

    private void updateLabels() {
        if (ratioBar == null || qualitySpinner == null || fpsSpinner == null || targetText == null) return;
        boolean custom = renderSwitch != null && renderSwitch.isChecked();
        int ratio = custom ? ratioBar.getProgress() : 100;
        int edge = parseIntSafe(shortEdgeEdit.getText().toString(), defaultShortEdge());
        int px = ConfigPlan.padPixels(ratio, edge);
        ratioValue.setText(ratio + "%");
        if (ratio == 100) {
            ratioCaption.setText("原生分辨率，不额外限制");
        } else {
            ratioCaption.setText("目标短边 " + px + " px（" + edge + " × " + ratio + "%）");
        }
        int q = Math.max(0, qualitySpinner.getSelectedItemPosition());
        int fpsEnum = ConfigPlan.FPS_VALUES[Math.max(0, fpsSpinner.getSelectedItemPosition())];
        int fps = ConfigPlan.fpsForInternal(fpsEnum);
        if (ratio == 100) {
            targetText.setText("将写入：" + ConfigPlan.qualityLabel(q) + " / " + fps
                    + " 帧 / 渲染 100%（原生，不额外限制）");
        } else {
            targetText.setText("将写入：" + ConfigPlan.qualityLabel(q) + " / " + fps
                    + " 帧 / 渲染 " + ratio + "%（短边目标 " + px + " px）");
        }
        if (!dragging) updatePresetChips(ratio);
    }

    private void loadPrefs() {
        SharedPreferences sp = Prefs.get(this);
        int q = sp.getInt(Prefs.QUALITY, 3);
        int f = sp.getInt(Prefs.FPS, 2);
        int ratio = sp.getInt(Prefs.RATIO, 100);
        int shortEdge = sp.getInt(Prefs.SHORT_EDGE, defaultShortEdge());
        boolean custom = sp.getBoolean(Prefs.RENDER_CUSTOM, false);
        qualitySpinner.setSelection(Math.max(0, Math.min(5, q)), false);
        int idx = 0;
        for (int i = 0; i < ConfigPlan.FPS_VALUES.length; i++) {
            if (ConfigPlan.FPS_VALUES[i] == f) idx = i;
        }
        fpsSpinner.setSelection(idx, false);
        ratioBar.setProgress(Math.max(0, Math.min(100, ratio)));
        shortEdgeEdit.setText(Integer.toString(shortEdge));
        suppress = true;
        renderSwitch.setChecked(custom, false);
        suppress = false;
        setRenderSectionVisible(custom);
        refreshStatus();
    }

    private void refreshStatus() {
        SharedPreferences sp = Prefs.get(this);
        String mode = sp.getString(Prefs.MODE, "off");
        boolean custom = sp.getBoolean(Prefs.RENDER_CUSTOM, false);
        int ratio = custom ? sp.getInt(Prefs.RATIO, 100) : 100;
        statusText.setText("模式：" + modeName(mode) + "\n已保存："
                + ConfigPlan.qualityLabel(sp.getInt(Prefs.QUALITY, 3)) + " / "
                + ConfigPlan.fpsLabel(sp.getInt(Prefs.FPS, 2)) + " / 渲染 " + ratio + "%");
    }

    private static String modeName(String mode) {
        if ("apply".equals(mode)) return "写入设置（下次启动游戏生效）";
        if ("defaults".equals(mode)) return "恢复默认（下次启动游戏执行）";
        return "已关闭";
    }

    private void confirmSave() {
        final int q = Math.max(0, qualitySpinner.getSelectedItemPosition());
        final int f = ConfigPlan.FPS_VALUES[Math.max(0, fpsSpinner.getSelectedItemPosition())];
        final boolean custom = renderSwitch.isChecked();
        final int ratio = custom ? ratioBar.getProgress() : 100;
        final int shortEdge = parseIntSafe(shortEdgeEdit.getText().toString(), defaultShortEdge());
        if (custom && (shortEdge < 200 || shortEdge > 8192)) {
            Toast.makeText(this, "短边像素须为 200-8192", Toast.LENGTH_SHORT).show();
            return;
        }
        String body = "即将写入：" + ConfigPlan.qualityLabel(q) + " / " + ConfigPlan.fpsForInternal(f)
                + " 帧 / 渲染 " + ratio + "%\n\n目标文件：Engine.ini、Game.ini\n\n"
                + "写入前会在内存里保留原内容，失败自动回滚（不生成备份文件）。"
                + "改配置仍有被反作弊识别的风险，且“配置档位 ≠ 实测帧率”。建议先完全退出游戏。";
        RiskDialog.confirm(this, "确认写入", body, "确认写入", new Runnable() {
            @Override public void run() {
                save(q, f, ratio, shortEdge, "apply");
            }
        });
    }

    private void saveCurrentValues(String mode) {
        SharedPreferences sp = Prefs.get(this);
        save(sp.getInt(Prefs.QUALITY, 3), sp.getInt(Prefs.FPS, 2),
                sp.getInt(Prefs.RATIO, 100), sp.getInt(Prefs.SHORT_EDGE, defaultShortEdge()), mode);
    }

    private void save(int q, int f, int ratio, int shortEdge, String mode) {
        boolean custom = renderSwitch.isChecked();
        int storedRatio = custom ? ratio : 100;
        Prefs.get(this).edit()
                .putString(Prefs.MODE, mode)
                .putInt(Prefs.QUALITY, q)
                .putInt(Prefs.FPS, f)
                .putInt(Prefs.RATIO, storedRatio)
                .putInt(Prefs.SHORT_EDGE, shortEdge)
                .putBoolean(Prefs.RENDER_CUSTOM, custom)
                .commit();
        Prefs.makeReadable(this);
        refreshStatus();
        updateLabels();
        Toast.makeText(this, "已保存，重启游戏后生效", Toast.LENGTH_LONG).show();
        if ("apply".equals(mode)) {
            RiskDialog.confirm(this, "已保存",
                    "设置已保存，会在下次启动游戏时写入。\n\n现在启动《三角洲行动》吗？",
                    "启动游戏", new Runnable() {
                        @Override public void run() { launchGame(); }
                    });
        }
    }

    private void launchGame() {
        try {
            android.content.Intent intent =
                    getPackageManager().getLaunchIntentForPackage("com.tencent.tmgp.dfm");
            if (intent == null) {
                // 兜底：显式构造 LAUNCHER intent（仍需 <queries> 才能解析到）
                intent = new android.content.Intent(android.content.Intent.ACTION_MAIN)
                        .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                        .setPackage("com.tencent.tmgp.dfm")
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "启动失败：" + t + "\n请手动打开游戏", Toast.LENGTH_LONG).show();
        }
    }

    private boolean aliasEnabled() {
        try {
            int state = getPackageManager().getComponentEnabledSetting(
                    new android.content.ComponentName(getPackageName(), LAUNCHER_ALIAS));
            return state != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Throwable t) {
            return true;
        }
    }

    private void setAliasEnabled(boolean enabled) {
        try {
            getPackageManager().setComponentEnabledSetting(
                    new android.content.ComponentName(getPackageName(), LAUNCHER_ALIAS),
                    enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Throwable t) {
            Toast.makeText(this, "操作失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void selfCheck() {        String[][] keys = {
                {Prefs.MODE, "mode"},
                {Prefs.QUALITY, "quality（画质枚举）"},
                {Prefs.FPS, "fps（帧率枚举）"},
                {Prefs.RENDER_CUSTOM, "renderCustom（自定义渲染）"},
                {Prefs.RATIO, "ratio（渲染比例）"},
                {Prefs.SHORT_EDGE, "shortEdge（短边）"}
        };
        StringBuilder sb = new StringBuilder();
        for (String[] k : keys) {
            String v = null;
            android.database.Cursor c = null;
            try {
                c = getContentResolver().query(
                        android.net.Uri.parse("content://io.github.dfmperf.settings/" + k[0]),
                        null, null, null, null);
                if (c != null && c.moveToFirst()) v = c.getString(0);
            } catch (Throwable t) {
                v = "读取失败: " + t;
            } finally {
                if (c != null) c.close();
            }
            sb.append(k[1]).append(" = ").append(v == null ? "<无数据>" : v).append('\n');
        }
        sb.append("\n通过 ContentProvider 读取。游戏进程里的模块走同一条通道；")
          .append("若这里能看到 mode = apply，就说明设置能被模块读到。");
        RiskDialog.info(this, "设置自检", sb.toString());
    }

    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
