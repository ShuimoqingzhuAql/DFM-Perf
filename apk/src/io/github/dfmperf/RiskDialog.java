package io.github.dfmperf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 风险告知 / 进阶功能说明 / 二次确认 三类弹窗，Material 3 风格。 */
public final class RiskDialog {
    private RiskDialog() {}

    // ------------------------------------------------------------ 三选项通用
    private interface Body {
        void fill(LinearLayout parent, Context c);
    }

    private static void showThree(final Activity activity, final boolean gate, String title,
                                  Body body, String foot, boolean footRed, boolean footBold,
                                  final Runnable onConfirm, final Runnable onNever, final Runnable onCancel) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(!gate);

        LinearLayout root = Ui.column(activity);
        root.setBackground(Ui.fill(activity, Ui.SURFACE, 28));
        int p = Ui.dp(activity, 24);
        root.setPadding(p, p, p, Ui.dp(activity, 12));

        root.addView(Ui.text(activity, title, 22, Ui.ON_SURFACE, true));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout content = Ui.column(activity);
        content.setPadding(0, Ui.dp(activity, 12), 0, Ui.dp(activity, 4));
        body.fill(content, activity);

        if (foot != null) {
            TextView f = Ui.text(activity, foot, 14, footRed ? Ui.ERROR : Ui.ON_SURFACE, footBold);
            f.setPadding(0, Ui.dp(activity, 14), 0, 0);
            content.addView(f);
        }
        scroll.addView(content);

        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int screenH = activity.getResources().getDisplayMetrics().heightPixels;
        int bodyW = (int) (screenW * 0.92) - Ui.dp(activity, 48);
        content.measure(View.MeasureSpec.makeMeasureSpec(bodyW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int scrollH = Math.min(content.getMeasuredHeight(), (int) (screenH * 0.52));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, scrollH));

        LinearLayout actions = Ui.row(activity);
        actions.setPadding(0, Ui.dp(activity, 8), 0, 0);

        if (onNever != null) {
            Button never = Ui.textButton(activity, "不再提示");
            never.setTextColor(Ui.ON_SURFACE_VARIANT);
            never.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dialog.dismiss();
                    onNever.run();
                }
            });
            actions.addView(never);
        }

        View spacer = new View(activity);
        actions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        Button cancel = Ui.textButton(activity, "取消");
        cancel.setTextColor(Ui.ON_SURFACE_VARIANT);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                if (onCancel != null) onCancel.run();
                if (gate) activity.finish();
            }
        });
        actions.addView(cancel);

        Button ok = Ui.filled(activity, "确认");
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(activity, 48));
        olp.leftMargin = Ui.dp(activity, 8);
        ok.setLayoutParams(olp);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.run();
            }
        });
        actions.addView(ok);
        root.addView(actions);

        dialog.setContentView(root);
        dialog.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
            @Override public void onCancel(android.content.DialogInterface d) {
                if (onCancel != null) onCancel.run();
                if (gate) activity.finish();
            }
        });
        dialog.show();
        styleWindow(dialog, activity, 0.92, 0);
    }

    private static void para(LinearLayout body, Context c, String heading, String text) {
        if (heading != null && !heading.isEmpty()) {
            TextView h = Ui.text(c, heading, 15, Ui.ON_SURFACE, true);
            h.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 2));
            body.addView(h);
        }
        body.addView(Ui.text(c, text, 14, Ui.ON_SURFACE_VARIANT, false));
    }

    // ------------------------------------------------------------ 风险告知
    private static Body riskBody() {
        return new Body() {
            @Override public void fill(LinearLayout body, Context c) {
                para(body, c, null,
                        "本工具通过 LSPosed 修改《三角洲行动》的 Engine.ini 和 Game.ini，用于设置画质与帧率档位。"
                                + "工具不修改游戏安装包或账号数据，但配置修改和框架注入仍可能带来以下风险：");
                para(body, c, "账号风险",
                        "游戏的反作弊系统可能识别配置修改或 LSPosed 注入，并对账号采取限制或封禁措施。使用前请自行评估。");
                para(body, c, "协议风险",
                        "修改游戏配置或使用注入框架可能违反游戏的用户协议或服务条款，官方可能据此处理并对此进行封号。");
                para(body, c, "效果限制",
                        "所选帧率是配置请求，不代表游戏实际帧率。最终表现取决于设备性能、屏幕刷新率、系统上报和发热降频等因素；");
                para(body, c, "兼容风险",
                        "游戏更新可能改变配置格式或认证材料。校验不通过时，工具会拒绝写入。"
                                + "拒绝写入只能降低配置损坏风险，不能保证账号安全。");
                para(body, c, "责任说明",
                        "本工具仅供学习研究使用，按现状提供，不提供任何明示或默示担保。"
                                + "使用造成的账号、数据或设备问题，由使用者自行承担。");
            }
        };
    }

    private static final String RISK_FOOT =
            "继续使用，即表示你已阅读并理解以上内容，并自愿承担相关风险。";

    public static void showRisk(final Activity activity, final boolean gate, final Runnable onAccept) {
        showThree(activity, gate, "使用前必读｜风险告知", riskBody(), RISK_FOOT, true, true,
                new Runnable() {
                    @Override public void run() {
                        if (onAccept != null) onAccept.run();
                    }
                },
                new Runnable() {
                    @Override public void run() {
                        Prefs.setRiskNeverShow(activity, true);
                        if (onAccept != null) onAccept.run();
                    }
                }, null);
    }

    // ------------------------------------------------------ 自定义渲染比例说明
    // ------------------------------------------------------ 自定义渲染比例说明
    private static Body renderBody() {
        return new Body() {
            @Override public void fill(LinearLayout body, Context c) {
                para(body, c, null,
                        "打开后可以调低游戏的 3D 渲染分辨率，用来减轻 GPU 压力、让高帧率更稳；画面会变软，UI 不受影响。");
                para(body, c, "什么时候需要",
                        "只有当你发现「目标帧率稳不住，且机身不算烫」时才值得调低。发热降频、CPU 瓶颈、场景锁帧这几种情况，"
                                + "降低渲染比例基本没有帮助。");
                para(body, c, "怎么算",
                        "渲染比例 100% 表示使用原生分辨率（写入 BackBuffer=8192，交给引擎决定）；"
                                + "低于 100% 时按「渲染基准短边 × 比例」计算目标分辨率。");
                para(body, c, "可调范围",
                        "0%–100%。数值越低越省 GPU、画面越软；低于 60% 通常已经明显发糊，不建议长期使用。");
                para(body, c, "建议",
                        "默认即可，不需要修改。确实想降低分辨率再打开；一般 75%–85% 就够了。"
                                + "「渲染基准短边」填你屏幕的实际短边像素（如 1200×2608 的屏幕填 1200）。");
            }
        };
    }

    private static final String RENDER_FOOT =
            "默认即可，不需要修改。确实需要降低渲染分辨率时，再打开开关并调整。";

    public static void showRenderHint(final Activity activity, final Runnable onEnable, final Runnable onCancel) {
        showThree(activity, false, "自定义渲染比例 · 进阶", renderBody(),
                "默认即可，不需要修改。确实需要降低渲染分辨率时，再点“确认”或“不再提示”。", false, true,
                new Runnable() {
                    @Override public void run() {
                        onEnable.run();
                    }
                },
                new Runnable() {
                    @Override public void run() {
                        Prefs.setRenderHintNever(activity, true);
                        onEnable.run();
                    }
                }, onCancel);
    }

    /** 只读查看：即使点过“不再提示”，也能从这里重新看说明。 */
    public static void showRenderInfo(final Activity activity) {
        showInfoBody(activity, "自定义渲染比例 · 说明", renderBody(), RENDER_FOOT, false);
    }

    /** 只读查看风险告知：App 内点击时用，只要一个「知道了」。 */
    public static void showRiskInfo(final Activity activity) {
        showInfoBody(activity, "使用前必读｜风险告知", riskBody(), RISK_FOOT, true);
    }

    // ------------------------------------------------------------ 画质档位说明
    private static Body ultimateBody() {
        return new Body() {
            @Override public void fill(LinearLayout body, Context c) {
                para(body, c, null,
                        "靶场里「终极」可以跑到 185 帧，但大战场和实战对局会被压回 60 帧。");
                para(body, c, "原因",
                        "游戏的云端配置目前没有给「终极」画质开放高帧率机型，所以进入实战后，"
                                + "帧率会被云端按默认档位重新压回 60。");
                para(body, c, "怎么办",
                        "想要稳定高帧率，请把画质改成「极致」——极致档不受这个限制，可以跑到屏幕刷新率上限。");
                para(body, c, "什么时候终极才会生效",
                        "如果以后官方云控放开了支持终极档 120 甚至更高帧率的机型，这里的设置才会真正生效。");
            }
        };
    }

    public static void showUltimateHint(final Activity activity, final Runnable onSwitchToExtreme) {
        showThree(activity, false, "终极画质的高帧率限制", ultimateBody(), null, false, true,
                new Runnable() {
                    @Override public void run() {
                        if (onSwitchToExtreme != null) onSwitchToExtreme.run();
                    }
                },
                new Runnable() {
                    @Override public void run() {
                        Prefs.setUltimateHintNever(activity, true);
                    }
                }, null);
    }

    public static void showUltimateInfo(final Activity activity) {
        showInfoBody(activity, "画质档位说明", ultimateBody(), null, false);
    }

    // ------------------------------------------------------------ 自检与数值说明
    public static void showValueHelp(final Activity activity) {
        showInfoBody(activity, "这些值是什么意思", new Body() {
            @Override public void fill(LinearLayout body, Context c) {
                para(body, c, null,
                        "自检里看到的都是写进游戏配置的内部值，平时不需要手动改。");
                para(body, c, "mode",
                        "模块下次启动游戏时做什么。off 什么都不做；apply 按档位写入；defaults 删除本工具写入的覆盖项。");
                para(body, c, "quality",
                        "画质枚举。0 流畅、1 标准、2 精致、3 高清、4 极致、5 终极。");
                para(body, c, "fps",
                        "帧率枚举，不是帧数本身。2 是 60 帧，7 是 90，8 是 120，9 是 144，10 是 165，11 是 185。");
                para(body, c, "renderCustom 与 ratio",
                        "是否启用自定义渲染比例（1 开、0 关），以及比例本身。ratio 为 100 表示原生分辨率。");
                para(body, c, "shortEdge",
                        "渲染基准短边像素，比如 1200。");
            }
        }, null, false);
    }

    private static void showInfoBody(Activity activity, String title, Body body, String foot, boolean footRed) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.column(activity);
        root.setBackground(Ui.fill(activity, Ui.SURFACE, 28));
        int p = Ui.dp(activity, 24);
        root.setPadding(p, p, p, Ui.dp(activity, 12));
        root.addView(Ui.text(activity, title, 22, Ui.ON_SURFACE, true));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout content = Ui.column(activity);
        content.setPadding(0, Ui.dp(activity, 12), 0, Ui.dp(activity, 4));
        body.fill(content, activity);
        if (foot != null) {
            TextView f = Ui.text(activity, foot, 14, footRed ? Ui.ERROR : Ui.ON_SURFACE, true);
            f.setPadding(0, Ui.dp(activity, 14), 0, 0);
            content.addView(f);
        }
        scroll.addView(content);
        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int screenH = activity.getResources().getDisplayMetrics().heightPixels;
        int bodyW = (int) (screenW * 0.92) - Ui.dp(activity, 48);
        content.measure(View.MeasureSpec.makeMeasureSpec(bodyW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int scrollH = Math.min(content.getMeasuredHeight(), (int) (screenH * 0.52));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, scrollH));

        LinearLayout actions = Ui.row(activity);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, Ui.dp(activity, 12), 0, 0);
        Button ok = Ui.textButton(activity, "知道了");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        actions.addView(ok);
        root.addView(actions);
        dialog.setContentView(root);
        dialog.show();
        styleWindow(dialog, activity, 0.92, 0);
    }
    // ------------------------------------------------------------ 二次确认
    public static void confirm(Activity activity, String title, String body,
                               String confirmLabel, final Runnable onConfirm) {
        confirm(activity, title, body, "取消", confirmLabel, onConfirm);
    }

    public static void confirm(Activity activity, String title, String body, String cancelLabel,
                               String confirmLabel, final Runnable onConfirm) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = Ui.column(activity);
        root.setBackground(Ui.fill(activity, Ui.SURFACE, 28));
        int p = Ui.dp(activity, 24);
        root.setPadding(p, p, p, Ui.dp(activity, 12));

        root.addView(Ui.text(activity, title, 22, Ui.ON_SURFACE, true));
        TextView text = Ui.text(activity, body, 14, Ui.ON_SURFACE_VARIANT, false);
        text.setPadding(0, Ui.dp(activity, 12), 0, 0);
        text.setTextIsSelectable(true);
        // 先量出正文真实高度，只有超过上限才用固定高度滚动；短文本不再留大片空白
        int screenWc = activity.getResources().getDisplayMetrics().widthPixels;
        int maxHc = Math.min((int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55),
                Ui.dp(activity, 420));
        int bodyWc = (int) (screenWc * 0.9) - Ui.dp(activity, 48);
        text.measure(View.MeasureSpec.makeMeasureSpec(bodyWc, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int textH = Math.min(text.getMeasuredHeight(), maxHc);
        ScrollView bodyScroll = new ScrollView(activity);
        bodyScroll.addView(text);
        root.addView(bodyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, textH));

        LinearLayout actions = Ui.row(activity);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, Ui.dp(activity, 12), 0, 0);
        Button cancel = Ui.textButton(activity, cancelLabel == null ? "取消" : cancelLabel);
        cancel.setTextColor(Ui.ON_SURFACE_VARIANT);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        actions.addView(cancel);

        Button ok = Ui.filled(activity, confirmLabel);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(activity, 48));
        olp.leftMargin = Ui.dp(activity, 8);
        ok.setLayoutParams(olp);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.run();
            }
        });
        actions.addView(ok);
        root.addView(actions);

        dialog.setContentView(root);
        dialog.show();
        styleWindow(dialog, activity, 0.9, 0);
    }

    // ------------------------------------------------------------ 信息弹窗
    public static void info(Activity activity, String title, String body) {
        info(activity, title, body, null, null);
    }

    public static void info(Activity activity, String title, String body,
                            String actionLabel, final Runnable action) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.column(activity);
        root.setBackground(Ui.fill(activity, Ui.SURFACE, 28));
        int p = Ui.dp(activity, 24);
        root.setPadding(p, p, p, Ui.dp(activity, 12));
        root.addView(Ui.text(activity, title, 22, Ui.ON_SURFACE, true));
        TextView text = Ui.text(activity, body, 14, Ui.ON_SURFACE_VARIANT, false);
        text.setPadding(0, Ui.dp(activity, 12), 0, 0);
        root.addView(text);
        LinearLayout actions = Ui.row(activity);
        if (actionLabel != null) {
            Button extra = Ui.textButton(activity, actionLabel);
            extra.setTextColor(Ui.ON_SURFACE_VARIANT);
            extra.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dialog.dismiss();
                    if (action != null) action.run();
                }
            });
            actions.addView(extra);
            View spacer = new View(activity);
            actions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        } else {
            actions.setGravity(Gravity.END);
        }
        Button ok = Ui.textButton(activity, "知道了");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        actions.addView(ok);
        root.addView(actions);
        dialog.setContentView(root);
        dialog.show();
        styleWindow(dialog, activity, 0.9, 0);
    }

    // ------------------------------------------------------------ 首次启动：作用域提醒
    public static void showScopeHint(final Activity activity) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.column(activity);
        root.setBackground(Ui.fill(activity, Ui.SURFACE, 28));
        int p = Ui.dp(activity, 24);
        root.setPadding(p, p, p, Ui.dp(activity, 12));
        root.addView(Ui.text(activity, "别忘了勾选作用域", 22, Ui.ON_SURFACE, true));
        TextView text = Ui.text(activity,
                "本模块只对《三角洲行动》生效。\n\n"
                        + "请打开 LSPosed 管理器 → 模块 → DFM 画质工坊 → 作用域，"
                        + "勾选「三角洲行动」（com.tencent.tmgp.dfm）。\n\n"
                        + "没有勾选的话，即使在这里保存了设置，也不会写入游戏。",
                14, Ui.ON_SURFACE_VARIANT, false);
        text.setPadding(0, Ui.dp(activity, 12), 0, 0);
        root.addView(text);

        LinearLayout actions = Ui.row(activity);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, Ui.dp(activity, 12), 0, 0);
        final Button ok = Ui.filled(activity, "1 秒");
        ok.setEnabled(false);
        ok.setAlpha(0.55f);
        ok.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(activity, 48)));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        actions.addView(ok);
        root.addView(actions);

        dialog.setContentView(root);
        dialog.show();
        styleWindow(dialog, activity, 0.9, 0);
        // 按钮写着“1 秒”，实际停留 1.5 秒，给用户留出读完这段话的时间
        ok.postDelayed(new Runnable() {
            @Override public void run() {
                ok.setText("知道了");
                ok.setEnabled(true);
                ok.setAlpha(1f);
            }
        }, 1500L);
    }

    private static void styleWindow(Dialog dialog, Activity activity, double wRatio, double hRatio) {
        Window w = dialog.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setDimAmount(0.45f);
        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int screenH = activity.getResources().getDisplayMetrics().heightPixels;
        int width = (int) (screenW * wRatio);
        int height = hRatio > 0 ? (int) (screenH * hRatio) : ViewGroup.LayoutParams.WRAP_CONTENT;
        w.setLayout(width, height);
        w.setGravity(Gravity.CENTER);
    }

}
