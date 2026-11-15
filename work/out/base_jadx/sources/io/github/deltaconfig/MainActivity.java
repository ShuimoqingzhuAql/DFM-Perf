package io.github.deltaconfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import io.github.deltaconfig.GameRepository;
import io.github.deltaconfig.core.ConfigCrypto;
import io.github.deltaconfig.core.ConfigPatcher;
import io.github.deltaconfig.core.OperationFailure;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class MainActivity extends Activity {
    private static final int ACCENT = -7524004;
    private static final int BG = -2567;
    private static final int BORDER = -1125918;
    private static final int BOTTOM = -419432196;
    private static final int CARD = -687866626;
    private static final int DISABLED = -729881;
    private static final int DISABLED_INK = -7047547;
    private static final int ERROR_LOG_LIMIT = 524288;
    private static final int ICE = -4333314;
    private static final int INK = -11850944;
    private static final int LAVENDER = -3296037;
    private static final int MUTED = -9415835;
    private static final int PINK = -14115;
    private static final String PROJECT_URL = "https://github.com/Kunari-Ely/DeltaConfig";
    private static final String RISK_NOTICE = "本软件需要 root 权限，会读取并修改《三角洲行动》的游戏配置文件。使用 root 或修改游戏配置可能触发反作弊检测，导致账号受限或封禁。请充分了解并接受上述风险后再使用。";
    private static final int RISK_NOTICE_VERSION = 1;
    private static final int ROSE = -20532;
    private boolean busy;
    private TextView currentLabel;
    private Button errorButton;
    private Spinner fps;
    private Spinner gameTargetPicker;
    private TextView gameTargetStatus;
    private boolean initializing;
    private String lastErrorReport;
    private TextView limitLabel;
    private AlertDialog pendingActionDialog;
    private Button previewButton;
    private ProgressBar progress;
    private Spinner quality;
    private SeekBar ratio;
    private TextView ratioLabel;
    private Button readButton;
    private Button refreshTargetsButton;
    private Button reportButton;
    private GameRepository repository;
    private Button resetButton;
    private Button restoreButton;
    private Button rootButton;
    private GameTarget selectedTarget;
    private EditText shortEdge;
    private GameRepository.Snapshot snapshot;
    private TextView statusBody;
    private TextView statusTitle;
    private TextView targetLabel;
    private boolean updatingTargets;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private String lastReport = "尚无操作记录。";

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.selectedTarget = GameTarget.initialSelection(getSharedPreferences("game_target", 0).getString("package", null), installedTargets());
        this.repository = new GameRepository(getApplicationContext(), this.selectedTarget.packageName);
        rememberTarget();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(8208);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(RISK_NOTICE_VERSION);
        linearLayout.setBackground(new AppBackgroundDrawable(getResources()));
        linearLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return MainActivity.lambda$onCreate$0(view, windowInsets);
            }
        });
        setContentView(linearLayout);
        linearLayout.requestApplyInsets();
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout column = column();
        column.setPadding(dp(22), dp(24), dp(22), dp(20));
        scrollView.addView(column);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout column2 = column();
        column2.setPadding(dp(12), dp(10), dp(12), dp(12));
        column2.setBackground(shape(-1275069700, 16777215, 18));
        TextView text = text("本机配置工具  /  ROOT", 12, ACCENT, true);
        text.setLetterSpacing(0.09f);
        column2.addView(text);
        add(column2, text("鼠鼠工具箱", 29, INK, true), 8);
        add(column2, text("本机解密 · 自动备份 · 随时还原", 14, MUTED, false), 8);
        column.addView(column2);
        LinearLayout card = card();
        card.addView(text("风险告知", 17, ACCENT, true));
        add(card, text(RISK_NOTICE, 14, INK, false), 8);
        add(column, card, 20);
        LinearLayout card2 = card();
        card2.addView(text("游戏发行版", 17, INK, true));
        this.gameTargetPicker = selectionSpinner(targetChoices());
        this.gameTargetPicker.setContentDescription("选择要读取、修改和还原的三角洲游戏发行版");
        this.gameTargetPicker.setSelection(targetIndex(this.selectedTarget));
        add(card2, this.gameTargetPicker, 8);
        this.gameTargetStatus = text("", 13, MUTED, false);
        add(card2, this.gameTargetStatus, 8);
        this.refreshTargetsButton = button("刷新已安装状态", false);
        this.refreshTargetsButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m11lambda$onCreate$1$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(card2, this.refreshTargetsButton, 10);
        add(column, card2, 16);
        LinearLayout card3 = card();
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        this.progress = new ProgressBar(this);
        this.progress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        linearLayout2.addView(this.progress, new LinearLayout.LayoutParams(dp(24), dp(24)));
        this.statusTitle = text("等待读取", 17, INK, true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.leftMargin = dp(12);
        linearLayout2.addView(this.statusTitle, layoutParams);
        card3.addView(linearLayout2);
        this.statusBody = text("首次使用时，请在 root 管理器中允许本应用。", 14, MUTED, false);
        add(card3, this.statusBody, 12);
        this.currentLabel = text("", 13, MUTED, false);
        add(card3, this.currentLabel, 10);
        this.errorButton = button("查看本次错误", false);
        this.errorButton.setVisibility(8);
        this.errorButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m14lambda$onCreate$2$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(card3, this.errorButton, 10);
        this.rootButton = button("授权 root", false);
        this.rootButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m15lambda$onCreate$3$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(card3, this.rootButton, 12);
        add(card3, text("检查 root 授权；配置文件访问另行验证。已记住授权时不会再次弹窗；被拒绝请到 root 管理器允许。", 12, MUTED, false), 8);
        add(column, card3, 24);
        LinearLayout card4 = card();
        card4.addView(text("目标设置", 19, INK, true));
        add(card4, text("画质档位", 14, MUTED, true), 20);
        this.quality = spinner("请选择目标画质", ConfigPatcher.QUALITY_LABELS);
        add(card4, this.quality, 5);
        add(card4, text("帧率档位", 14, MUTED, true), 18);
        this.fps = spinner("请选择目标帧率", ConfigPatcher.FPS_LABELS);
        add(card4, this.fps, 5);
        this.limitLabel = text("", 13, MUTED, false);
        add(card4, this.limitLabel, 10);
        add(card4, divider(), 20);
        this.ratioLabel = text("目标渲染比例  100%", 17, INK, true);
        add(card4, this.ratioLabel, 20);
        this.ratio = new SeekBar(this);
        this.ratio.setMax(75);
        this.ratio.setProgress(75);
        this.ratio.setContentDescription("待写入的目标渲染比例，25% 至 100%，不是实测值");
        this.ratio.setProgressTintList(controlColors(ROSE, DISABLED));
        this.ratio.setSecondaryProgressTintList(controlColors(PINK, DISABLED));
        this.ratio.setProgressBackgroundTintList(controlColors(ICE, DISABLED));
        this.ratio.setThumbTintList(controlColors(ACCENT, LAVENDER));
        add(card4, this.ratio, 12);
        LinearLayout linearLayout3 = new LinearLayout(this);
        TextView text2 = text("25%", 12, MUTED, false);
        TextView text3 = text("100% · 原生目标", 12, MUTED, false);
        linearLayout3.addView(text2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        text3.setGravity(5);
        linearLayout3.addView(text3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        add(card4, linearLayout3, 0);
        this.targetLabel = text("", 13, MUTED, false);
        add(card4, this.targetLabel, 12);
        add(card4, text("渲染基准短边（像素）", 14, MUTED, true), 18);
        this.shortEdge = new EditText(this);
        this.shortEdge.setSingleLine(true);
        this.shortEdge.setInputType(2);
        this.shortEdge.setTextColor(controlColors(INK, MUTED));
        this.shortEdge.setHintTextColor(MUTED);
        this.shortEdge.setBackgroundTintList(controlColors(ACCENT, BORDER));
        this.shortEdge.setHighlightColor(PINK);
        this.shortEdge.setTextSize(17.0f);
        this.shortEdge.setMinHeight(dp(48));
        this.shortEdge.setText(Integer.toString(displayShortEdge()));
        this.shortEdge.setContentDescription("渲染基准短边，默认取当前系统显示尺寸");
        add(card4, this.shortEdge, 4);
        add(card4, text("低于 100% 时按此短边计算；若游戏窗口尺寸不同，可手动校准。100% 使用游戏现有的 Surface 尺寸限制逻辑。", 12, MUTED, false), 8);
        add(column, card4, 16);
        LinearLayout card5 = card();
        card5.addView(text("写入后还需在游戏中确认", 16, INK, true));
        add(card5, text("配置档位不等于实测帧数。系统刷新率、设备支持状态及终极画质的独立资源上限，仍可能降低实际帧率。", 13, MUTED, false), 8);
        add(card5, text("修改会统一设置所有画质档位的分辨率请求。", 13, MUTED, false), 8);
        add(card5, text("游戏更新后会重新检测本机配置，认证与字段结构通过后开放修改；首次识别新库可能需要更久。", 13, MUTED, false), 8);
        add(column, card5, 16);
        LinearLayout card6 = card();
        card6.addView(text("DeltaConfig", 19, INK, true));
        add(card6, text("源码已在 GitHub 开放，欢迎查看与交流。觉得有用，可以自愿点一颗 Star 支持项目。", 13, MUTED, false), 8);
        Button button = button("查看源码", false);
        button.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m16lambda$onCreate$4$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(card6, button, 14);
        Button button2 = button("支持项目 · Star", false);
        button2.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m17lambda$onCreate$5$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(card6, button2, 8);
        add(column, card6, 16);
        this.reportButton = button("查看最近操作记录", false);
        this.reportButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m18lambda$onCreate$6$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(column, this.reportButton, 12);
        Button button3 = button("查看错误日志", false);
        button3.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m19lambda$onCreate$7$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(column, button3, 8);
        TextView text4 = text("1.10  ·  ARM64 · 按发行版检测游戏配置", 11, MUTED, false);
        text4.setPadding(dp(10), dp(8), dp(10), dp(8));
        text4.setBackground(shape(-855639300, 16777215, 12));
        add(column, text4, 16);
        LinearLayout column3 = column();
        column3.setPadding(dp(22), dp(12), dp(22), dp(14));
        column3.setBackgroundColor(BOTTOM);
        this.previewButton = button("确认修改", true);
        this.previewButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m20lambda$onCreate$8$iogithubdeltaconfigMainActivity(view);
            }
        });
        column3.addView(this.previewButton, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout linearLayout4 = new LinearLayout(this);
        this.readButton = button("重新读取", false);
        this.restoreButton = button("还原最近备份", false);
        this.readButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m21lambda$onCreate$9$iogithubdeltaconfigMainActivity(view);
            }
        });
        this.restoreButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m12lambda$onCreate$10$iogithubdeltaconfigMainActivity(view);
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        layoutParams2.rightMargin = dp(5);
        linearLayout4.addView(this.readButton, layoutParams2);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        layoutParams3.leftMargin = dp(5);
        linearLayout4.addView(this.restoreButton, layoutParams3);
        add(column3, linearLayout4, 8);
        this.resetButton = button("恢复默认画质设置", false);
        this.resetButton.setOnClickListener(new View.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m13lambda$onCreate$11$iogithubdeltaconfigMainActivity(view);
            }
        });
        add(column3, this.resetButton, 8);
        linearLayout.addView(column3);
        AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() { // from class: io.github.deltaconfig.MainActivity.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                MainActivity.this.updateHints();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        this.quality.setOnItemSelectedListener(onItemSelectedListener);
        this.fps.setOnItemSelectedListener(onItemSelectedListener);
        this.ratio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.deltaconfig.MainActivity.2
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                MainActivity.this.updateHints();
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        this.shortEdge.addTextChangedListener(new TextWatcher() { // from class: io.github.deltaconfig.MainActivity.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                MainActivity.this.updateHints();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
            }
        });
        this.gameTargetPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: io.github.deltaconfig.MainActivity.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (MainActivity.this.updatingTargets) {
                    return;
                }
                GameTarget gameTarget = GameTarget.values()[i];
                if (MainActivity.this.busy) {
                    MainActivity.this.updatingTargets = true;
                    MainActivity.this.gameTargetPicker.setSelection(MainActivity.this.targetIndex(MainActivity.this.selectedTarget));
                    MainActivity.this.updatingTargets = false;
                } else if (gameTarget != MainActivity.this.selectedTarget) {
                    MainActivity.this.switchTarget(gameTarget);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        migrateSavedError();
        updateTargetStatus();
        this.lastReport = targetReport(this.selectedTarget, "尚无操作记录。");
        updateHints();
        setBusy(false, "请先阅读风险告知", "确认前不会申请 root 权限或访问游戏配置。");
        requireRiskAcknowledgement(new MainActivity$$ExternalSyntheticLambda5(this));
    }

    static /* synthetic */ WindowInsets lambda$onCreate$0(View view, WindowInsets windowInsets) {
        view.setPadding(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), windowInsets.getSystemWindowInsetBottom());
        return windowInsets.consumeSystemWindowInsets();
    }

    /* renamed from: lambda$onCreate$1$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$onCreate$1$iogithubdeltaconfigMainActivity(View view) {
        refreshInstalledTargets();
    }

    /* renamed from: lambda$onCreate$2$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m14lambda$onCreate$2$iogithubdeltaconfigMainActivity(View view) {
        showErrorDetails();
    }

    /* renamed from: lambda$onCreate$3$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m15lambda$onCreate$3$iogithubdeltaconfigMainActivity(View view) {
        if (!this.busy) {
            requireRiskAcknowledgement(new MainActivity$$ExternalSyntheticLambda32(this));
        }
    }

    /* renamed from: lambda$onCreate$4$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$onCreate$4$iogithubdeltaconfigMainActivity(View view) {
        openProjectPage();
    }

    /* renamed from: lambda$onCreate$5$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$onCreate$5$iogithubdeltaconfigMainActivity(View view) {
        openProjectPage();
    }

    /* renamed from: lambda$onCreate$6$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$onCreate$6$iogithubdeltaconfigMainActivity(View view) {
        showReport();
    }

    /* renamed from: lambda$onCreate$7$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$onCreate$7$iogithubdeltaconfigMainActivity(View view) {
        showErrorLog();
    }

    /* renamed from: lambda$onCreate$8$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$onCreate$8$iogithubdeltaconfigMainActivity(View view) {
        preview();
    }

    /* renamed from: lambda$onCreate$9$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$onCreate$9$iogithubdeltaconfigMainActivity(View view) {
        load();
    }

    /* renamed from: lambda$onCreate$10$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m12lambda$onCreate$10$iogithubdeltaconfigMainActivity(View view) {
        confirmRestore();
    }

    /* renamed from: lambda$onCreate$11$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m13lambda$onCreate$11$iogithubdeltaconfigMainActivity(View view) {
        confirmResetDefaults();
    }

    private boolean[] installedTargets() {
        GameTarget[] values = GameTarget.values();
        boolean[] zArr = new boolean[values.length];
        for (int i = 0; i < values.length; i += RISK_NOTICE_VERSION) {
            try {
                getPackageManager().getApplicationInfo(values[i].packageName, 0);
                zArr[i] = true;
            } catch (PackageManager.NameNotFoundException e) {
            } catch (SecurityException e2) {
            }
        }
        return zArr;
    }

    private String[] targetChoices() {
        GameTarget[] values = GameTarget.values();
        boolean[] installedTargets = installedTargets();
        String[] strArr = new String[values.length];
        for (int i = 0; i < values.length; i += RISK_NOTICE_VERSION) {
            strArr[i] = values[i].label + (installedTargets[i] ? " · 已安装" : " · 未安装");
        }
        return strArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int targetIndex(GameTarget gameTarget) {
        GameTarget[] values = GameTarget.values();
        for (int i = 0; i < values.length; i += RISK_NOTICE_VERSION) {
            if (values[i] == gameTarget) {
                return i;
            }
        }
        throw new IllegalArgumentException("游戏发行版不在选择列表中");
    }

    private void rememberTarget() {
        getSharedPreferences("game_target", 0).edit().putString("package", this.selectedTarget.packageName).apply();
    }

    private void refreshInstalledTargets() {
        if (this.busy) {
            return;
        }
        this.updatingTargets = true;
        this.gameTargetPicker.setAdapter((SpinnerAdapter) selectionAdapter(this.gameTargetPicker, targetChoices()));
        this.gameTargetPicker.setSelection(targetIndex(this.selectedTarget));
        this.updatingTargets = false;
        updateTargetStatus();
    }

    private void updateTargetStatus() {
        boolean z = installedTargets()[targetIndex(this.selectedTarget)];
        this.gameTargetStatus.setText(this.selectedTarget.label + (z ? " · 已安装" : " · 未安装") + "\n" + (this.selectedTarget.international ? "格式自动检测，实机待验证。认证与字段结构通过后才能修改。" : "读取时核对本发行版的配置、认证和字段结构。") + (z ? "" : "\n可保留此选择；读取时会提示当前发行版未安装。"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchTarget(GameTarget gameTarget) {
        if (this.busy || gameTarget == this.selectedTarget) {
            return;
        }
        if (this.pendingActionDialog != null) {
            this.pendingActionDialog.dismiss();
        }
        this.selectedTarget = gameTarget;
        this.repository = new GameRepository(getApplicationContext(), gameTarget.packageName);
        this.snapshot = null;
        rememberTarget();
        clearCurrentError();
        this.lastReport = targetReport(gameTarget, "尚无操作记录，请重新读取此发行版的配置。");
        this.currentLabel.setText("尚未读取 " + gameTarget.label + " 的配置。");
        this.initializing = true;
        this.quality.setSelection(0);
        this.fps.setSelection(0);
        this.ratio.setProgress(75);
        this.initializing = false;
        updateTargetStatus();
        setBusy(false, "已选择 " + gameTarget.label, "请点击“重新读取”。修改和还原仅针对当前所选发行版。");
        updateHints();
    }

    private boolean isCurrentTarget(GameRepository gameRepository, GameTarget gameTarget) {
        return !isDestroyed() && this.repository == gameRepository && this.selectedTarget == gameTarget;
    }

    private String targetReport(GameTarget gameTarget, String str) {
        return "发行版：" + gameTarget.label + (gameTarget.international ? "\n国际服格式自动检测，实机待验证。" : "") + "\n\n" + str;
    }

    private void showActionDialog(final AlertDialog alertDialog) {
        if (this.pendingActionDialog != null) {
            this.pendingActionDialog.dismiss();
        }
        this.pendingActionDialog = alertDialog;
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda29
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                MainActivity.this.m39lambda$showActionDialog$12$iogithubdeltaconfigMainActivity(alertDialog, dialogInterface);
            }
        });
        alertDialog.show();
    }

    /* renamed from: lambda$showActionDialog$12$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$showActionDialog$12$iogithubdeltaconfigMainActivity(AlertDialog alertDialog, DialogInterface dialogInterface) {
        if (this.pendingActionDialog == alertDialog) {
            this.pendingActionDialog = null;
        }
    }

    private void openProjectPage() {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(PROJECT_URL));
            intent.addCategory("android.intent.category.BROWSABLE");
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            new AlertDialog.Builder(this).setTitle("无法打开项目页面").setMessage("当前没有可用的浏览器，或系统阻止打开链接。可以复制项目地址后在浏览器中打开。").setNegativeButton("关闭", (DialogInterface.OnClickListener) null).setPositiveButton("复制链接", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda43
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m22lambda$openProjectPage$13$iogithubdeltaconfigMainActivity(dialogInterface, i);
                }
            }).show();
        }
    }

    /* renamed from: lambda$openProjectPage$13$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$openProjectPage$13$iogithubdeltaconfigMainActivity(DialogInterface dialogInterface, int i) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService("clipboard");
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("DeltaConfig 项目地址", PROJECT_URL));
        }
    }

    private boolean riskAcknowledged() {
        return getSharedPreferences("risk_notice", 0).getInt("acknowledged_version", 0) >= RISK_NOTICE_VERSION;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRootAuthorization() {
        if (this.busy) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new MainActivity$$ExternalSyntheticLambda32(this));
            return;
        }
        this.snapshot = null;
        final GameRepository gameRepository = this.repository;
        final GameTarget gameTarget = this.selectedTarget;
        this.currentLabel.setText("检查授权后，请重新读取游戏配置。");
        setBusy(true, "正在检查 root 授权", "正在请求 root 并核对进程身份；此次不会读取或修改游戏配置。");
        this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda33
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m5x1f2dcfe6(gameRepository, gameTarget);
            }
        });
    }

    /* renamed from: lambda$checkRootAuthorization$15$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m5x1f2dcfe6(final GameRepository gameRepository, final GameTarget gameTarget) {
        try {
            gameRepository.checkRootAuthorization();
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m4xaaee9787(gameRepository, gameTarget);
                }
            });
        } catch (Exception e) {
            fail(gameRepository, gameTarget, "root 授权检查失败", OperationFailure.wrap("检查 root 授权", "验证 root 身份", gameTarget.packageName, e).state("未写入游戏配置", "无需回滚", null));
        }
    }

    /* renamed from: lambda$checkRootAuthorization$14$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m4xaaee9787(GameRepository gameRepository, GameTarget gameTarget) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            clearCurrentError();
            this.lastReport = targetReport(gameTarget, "root 授权检查成功\n已确认 root 身份。游戏配置访问尚待单独验证。");
            setBusy(false, "root 已授权", "授权检查通过。请点击“重新读取”验证游戏配置。已记住授权时不会再次弹出授权框。");
        }
    }

    private void updateRootButton() {
        if (this.rootButton == null) {
            return;
        }
        this.rootButton.setText(this.repository.hasRootAuthorization() ? "重新授权 root" : "授权 root");
        this.rootButton.setEnabled(!this.busy);
    }

    private void requireRiskAcknowledgement(final Runnable runnable) {
        if (riskAcknowledged()) {
            runnable.run();
            return;
        }
        AlertDialog create = new AlertDialog.Builder(this).setTitle("使用前风险告知").setMessage("本软件需要 root 权限，会读取并修改《三角洲行动》的游戏配置文件。使用 root 或修改游戏配置可能触发反作弊检测，导致账号受限或封禁。请充分了解并接受上述风险后再使用。\n\n选择“退出”将关闭应用，不申请 root 权限，也不读取或修改游戏配置。").setCancelable(false).setNegativeButton("退出", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda25
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m29x9918cf7c(dialogInterface, i);
            }
        }).setPositiveButton("已知晓风险，继续", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda26
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m30xd5807db(runnable, dialogInterface, i);
            }
        }).create();
        create.setCanceledOnTouchOutside(false);
        create.show();
    }

    /* renamed from: lambda$requireRiskAcknowledgement$16$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m29x9918cf7c(DialogInterface dialogInterface, int i) {
        finish();
    }

    /* renamed from: lambda$requireRiskAcknowledgement$17$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m30xd5807db(Runnable runnable, DialogInterface dialogInterface, int i) {
        getSharedPreferences("risk_notice", 0).edit().putInt("acknowledged_version", RISK_NOTICE_VERSION).apply();
        runnable.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void load() {
        if (this.busy) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new MainActivity$$ExternalSyntheticLambda5(this));
            return;
        }
        final GameRepository gameRepository = this.repository;
        final GameTarget gameTarget = this.selectedTarget;
        setBusy(true, "正在检测 " + gameTarget.label, "正在检查 root、识别认证材料并校验配置；首次识别新库可能需要更久…");
        this.snapshot = null;
        final int displayShortEdge = displayShortEdge();
        this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda30
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m10lambda$load$19$iogithubdeltaconfigMainActivity(gameRepository, displayShortEdge, gameTarget);
            }
        });
    }

    /* renamed from: lambda$load$19$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m10lambda$load$19$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, int i, final GameTarget gameTarget) {
        try {
            final GameRepository.Snapshot load = gameRepository.load();
            byte[] deriveKey = ConfigCrypto.deriveKey(load.fixedMask, load.gameplay);
            try {
                String decodeIni = ConfigCrypto.decodeIni(load.engine, deriveKey);
                final int i2 = ConfigPatcher.getInt(decodeIni, "DFMPerfGearConfig", "DFMQualityLevel", -1);
                final int i3 = ConfigPatcher.getInt(decodeIni, "DFMPerfGearConfig", "DFMFPSLevel", -1);
                Arrays.fill(deriveKey, (byte) 0);
                final int readRatio = readRatio(load, i);
                runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m9lambda$load$18$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, load, i2, i3, readRatio);
                    }
                });
            } catch (Throwable th) {
                Arrays.fill(deriveKey, (byte) 0);
                throw th;
            }
        } catch (Exception e) {
            fail(gameRepository, gameTarget, "读取配置失败", OperationFailure.wrap("读取配置", "读取并验证本机游戏文件", gameTarget.packageName, e));
        }
    }

    /* renamed from: lambda$load$18$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m9lambda$load$18$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, GameRepository.Snapshot snapshot, int i, int i2, int i3) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = snapshot;
            this.initializing = true;
            selectValue(this.quality, ConfigPatcher.QUALITY_VALUES, i);
            selectValue(this.fps, ConfigPatcher.FPS_VALUES, i2);
            this.ratio.setProgress(i3 - 25);
            this.initializing = false;
            clearCurrentError();
            this.currentLabel.setText(currentSettings(gameTarget, i, i2, snapshot.versionName));
            setBusy(false, "配置兼容性检测通过", snapshot.compatibilityDescription + "\n可选择目标设置并确认修改。");
            this.lastReport = targetReport(gameTarget, "读取成功\n游戏版本：" + snapshot.versionName + " (" + snapshot.versionCode + ")\n兼容性检测：" + snapshot.compatibilityDescription + "\n配置认证与字段结构：通过");
            updateHints();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void preview() {
        if (this.busy || this.snapshot == null) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.preview();
                }
            });
            return;
        }
        try {
            final int parseInt = Integer.parseInt(this.shortEdge.getText().toString().trim());
            if (parseInt < 200 || parseInt > 8192) {
                throw new NumberFormatException();
            }
            final GameRepository gameRepository = this.repository;
            final GameTarget gameTarget = this.selectedTarget;
            final GameRepository.Snapshot snapshot = this.snapshot;
            if (targetSettingsSelected()) {
                final int i = ConfigPatcher.QUALITY_VALUES[this.quality.getSelectedItemPosition() - RISK_NOTICE_VERSION];
                final int i2 = ConfigPatcher.FPS_VALUES[this.fps.getSelectedItemPosition() - RISK_NOTICE_VERSION];
                final int progress = this.ratio.getProgress() + 25;
                setBusy(true, "正在准备预览", "检查修改范围并生成新的认证配置…");
                this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda28
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m25lambda$preview$22$iogithubdeltaconfigMainActivity(snapshot, i, i2, progress, parseInt, gameRepository, gameTarget);
                    }
                });
            }
        } catch (NumberFormatException e) {
            this.shortEdge.setError("请输入 200–8192 之间的短边像素值");
        }
    }

    /* renamed from: lambda$preview$22$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m25lambda$preview$22$iogithubdeltaconfigMainActivity(final GameRepository.Snapshot snapshot, int i, int i2, int i3, int i4, final GameRepository gameRepository, final GameTarget gameTarget) {
        try {
            final ConfigPatcher.Plan plan = ConfigPatcher.plan(snapshot.engine, snapshot.game, snapshot.gameplay, snapshot.fixedMask, i, i2, i3, i4);
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m24lambda$preview$21$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, plan, snapshot);
                }
            });
        } catch (Exception e) {
            fail(gameRepository, gameTarget, "生成修改预览失败", OperationFailure.wrap("生成修改预览", "解密、修改并重新封装配置", failurePath(snapshot, e), e).state("本次尚未写入游戏配置", "未执行回滚", null));
        }
    }

    /* renamed from: lambda$preview$21$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$preview$21$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, final GameTarget gameTarget, final ConfigPatcher.Plan plan, final GameRepository.Snapshot snapshot) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            clearCurrentError();
            setBusy(false, "修改预览已就绪", "尚未写入，请确认目标设置。");
            this.lastReport = targetReport(gameTarget, "修改预览已就绪\n\n" + plan.summary + "\n\n尚未写入游戏配置。");
            showActionDialog(new AlertDialog.Builder(this).setTitle("确认目标设置").setMessage(targetReport(gameTarget, plan.summary) + "\n\n实际帧率与渲染效果须在游戏中确认。\n\n风险告知\n" + RISK_NOTICE).setNegativeButton("返回", (DialogInterface.OnClickListener) null).setPositiveButton("知晓风险并写入", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda46
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m23lambda$preview$20$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, snapshot, plan, dialogInterface, i);
                }
            }).create());
        }
    }

    /* renamed from: lambda$preview$20$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m23lambda$preview$20$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, GameRepository.Snapshot snapshot, ConfigPatcher.Plan plan, DialogInterface dialogInterface, int i) {
        m0lambda$apply$23$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, snapshot, plan);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: apply, reason: merged with bridge method [inline-methods] */
    public void m0lambda$apply$23$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, final GameTarget gameTarget, final GameRepository.Snapshot snapshot, final ConfigPatcher.Plan plan) {
        if (this.busy || !isCurrentTarget(gameRepository, gameTarget) || this.snapshot != snapshot || !gameTarget.packageName.equals(snapshot.packageName)) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m0lambda$apply$23$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, snapshot, plan);
                }
            });
        } else {
            setBusy(true, "正在写入", "正在应用目标设置并验证结果…");
            this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m3lambda$apply$26$iogithubdeltaconfigMainActivity(gameRepository, snapshot, plan, gameTarget);
                }
            });
        }
    }

    /* renamed from: lambda$apply$26$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$apply$26$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, GameRepository.Snapshot snapshot, final ConfigPatcher.Plan plan, final GameTarget gameTarget) {
        try {
            final String apply = gameRepository.apply(snapshot, plan.engine, plan.game);
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda40
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m2lambda$apply$25$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, plan, apply);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m1lambda$apply$24$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
            fail(gameRepository, gameTarget, "修改配置时发生错误", OperationFailure.wrap("修改配置", "备份、提交及回读校验", configurationPaths(snapshot), e));
        }
    }

    /* renamed from: lambda$apply$24$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m1lambda$apply$24$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
        }
    }

    /* renamed from: lambda$apply$25$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m2lambda$apply$25$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, ConfigPatcher.Plan plan, String str) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            clearCurrentError();
            this.lastReport = targetReport(gameTarget, "配置写入完成\n\n" + plan.summary + "\n\n配置校验：通过\n实际游戏效果：尚未验证");
            refreshAfterCommit(gameRepository, gameTarget, "写入", str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmRestore() {
        if (this.busy) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.confirmRestore();
                }
            });
            return;
        }
        final GameRepository gameRepository = this.repository;
        final GameTarget gameTarget = this.selectedTarget;
        showActionDialog(new AlertDialog.Builder(this).setTitle("还原最近备份").setMessage(targetReport(gameTarget, "将恢复此发行版最近一次修改前保存的设置。") + "\n\n风险告知\n" + RISK_NOTICE).setNegativeButton("返回", (DialogInterface.OnClickListener) null).setPositiveButton("还原", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda19
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7lambda$confirmRestore$27$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, dialogInterface, i);
            }
        }).create());
    }

    /* renamed from: lambda$confirmRestore$27$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m7lambda$confirmRestore$27$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, DialogInterface dialogInterface, int i) {
        m35lambda$restore$28$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: restore, reason: merged with bridge method [inline-methods] */
    public void m35lambda$restore$28$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, final GameTarget gameTarget) {
        if (this.busy || !isCurrentTarget(gameRepository, gameTarget)) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m35lambda$restore$28$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
        } else {
            setBusy(true, "正在还原", "正在核对备份所属设备、版本和配置认证…");
            this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m38lambda$restore$31$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
        }
    }

    /* renamed from: lambda$restore$31$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m38lambda$restore$31$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, final GameTarget gameTarget) {
        try {
            final String restoreLatest = gameRepository.restoreLatest();
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda47
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m36lambda$restore$29$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, restoreLatest);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda48
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m37lambda$restore$30$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
            fail(gameRepository, gameTarget, "还原配置时发生错误", OperationFailure.wrap("还原配置", "核验备份并恢复配置", gameTarget.packageName, e));
        }
    }

    /* renamed from: lambda$restore$29$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m36lambda$restore$29$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, String str) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            clearCurrentError();
            this.lastReport = targetReport(gameTarget, "还原完成\n已恢复此发行版最近备份的设置并通过校验。\n实际游戏效果：尚未验证");
            refreshAfterCommit(gameRepository, gameTarget, "还原", str);
        }
    }

    /* renamed from: lambda$restore$30$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m37lambda$restore$30$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmResetDefaults() {
        if (this.busy || this.snapshot == null) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.confirmResetDefaults();
                }
            });
            return;
        }
        final GameRepository gameRepository = this.repository;
        final GameTarget gameTarget = this.selectedTarget;
        final GameRepository.Snapshot snapshot = this.snapshot;
        showActionDialog(new AlertDialog.Builder(this).setTitle("恢复默认画质设置").setMessage(targetReport(gameTarget, "将清除画质、帧率和渲染比例覆盖设置，进入游戏后由游戏重新生成默认设置。实际默认档位由游戏决定。不会清除账号、下载资源或其他游戏数据。") + "\n\n风险告知\n" + RISK_NOTICE).setNegativeButton("返回", (DialogInterface.OnClickListener) null).setPositiveButton("恢复默认", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda38
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m6x7850e6dd(gameRepository, gameTarget, snapshot, dialogInterface, i);
            }
        }).create());
    }

    /* renamed from: lambda$confirmResetDefaults$32$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m6x7850e6dd(GameRepository gameRepository, GameTarget gameTarget, GameRepository.Snapshot snapshot, DialogInterface dialogInterface, int i) {
        m31lambda$resetDefaults$33$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, snapshot);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: resetDefaults, reason: merged with bridge method [inline-methods] */
    public void m31lambda$resetDefaults$33$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, final GameTarget gameTarget, final GameRepository.Snapshot snapshot) {
        if (this.busy || snapshot == null || this.snapshot != snapshot || !isCurrentTarget(gameRepository, gameTarget) || !gameTarget.packageName.equals(snapshot.packageName)) {
            return;
        }
        if (!riskAcknowledged()) {
            requireRiskAcknowledgement(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda41
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m31lambda$resetDefaults$33$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, snapshot);
                }
            });
        } else {
            setBusy(true, "正在恢复默认设置", "正在重置画质配置并验证结果…");
            this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda42
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m34lambda$resetDefaults$36$iogithubdeltaconfigMainActivity(gameRepository, snapshot, gameTarget);
                }
            });
        }
    }

    /* renamed from: lambda$resetDefaults$36$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m34lambda$resetDefaults$36$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, GameRepository.Snapshot snapshot, final GameTarget gameTarget) {
        try {
            gameRepository.resetDefaults(snapshot);
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda35
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m33lambda$resetDefaults$35$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda34
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m32lambda$resetDefaults$34$iogithubdeltaconfigMainActivity(gameRepository, gameTarget);
                }
            });
            fail(gameRepository, gameTarget, "恢复默认画质设置失败", OperationFailure.wrap("恢复默认画质设置", "重置自定义设置并核验结果", configurationPaths(snapshot), e));
        }
    }

    /* renamed from: lambda$resetDefaults$34$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m32lambda$resetDefaults$34$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
        }
    }

    /* renamed from: lambda$resetDefaults$35$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m33lambda$resetDefaults$35$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
            clearCurrentError();
            this.currentLabel.setText("画质、帧率和渲染覆盖已清除，实际默认档位由游戏决定。");
            this.initializing = true;
            this.quality.setSelection(0);
            this.fps.setSelection(0);
            this.initializing = false;
            this.lastReport = targetReport(gameTarget, "恢复默认画质设置完成\n已清除画质、帧率和渲染覆盖；进入游戏后由游戏重新生成默认设置。实际默认档位由游戏决定。");
            setBusy(false, "恢复默认设置已完成", "已清除画质、帧率和渲染覆盖；进入游戏后由游戏重新生成默认设置。实际默认档位由游戏决定。");
            new AlertDialog.Builder(this).setTitle("恢复默认设置完成").setMessage("请进入游戏生成默认画质设置，正常退出后点击“重新读取”。需要撤销时可使用“还原最近备份”。").setPositiveButton("知道了", (DialogInterface.OnClickListener) null).show();
        }
    }

    private void refreshAfterCommit(final GameRepository gameRepository, final GameTarget gameTarget, final String str, final String str2) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
            setBusy(true, str + "已完成，正在刷新", "已完成配置" + str + "和文件校验，正在重新读取当前状态…");
            final int displayShortEdge = displayShortEdge();
            this.worker.execute(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda44
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m28lambda$refreshAfterCommit$39$iogithubdeltaconfigMainActivity(gameRepository, displayShortEdge, gameTarget, str, str2);
                }
            });
        }
    }

    /* renamed from: lambda$refreshAfterCommit$39$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m28lambda$refreshAfterCommit$39$iogithubdeltaconfigMainActivity(final GameRepository gameRepository, int i, final GameTarget gameTarget, final String str, String str2) {
        try {
            final GameRepository.Snapshot load = gameRepository.load();
            byte[] deriveKey = ConfigCrypto.deriveKey(load.fixedMask, load.gameplay);
            try {
                String decodeIni = ConfigCrypto.decodeIni(load.engine, deriveKey);
                final int i2 = ConfigPatcher.getInt(decodeIni, "DFMPerfGearConfig", "DFMQualityLevel", -1);
                final int i3 = ConfigPatcher.getInt(decodeIni, "DFMPerfGearConfig", "DFMFPSLevel", -1);
                Arrays.fill(deriveKey, (byte) 0);
                final int readRatio = readRatio(load, i);
                runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda20
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m26lambda$refreshAfterCommit$37$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, load, i2, i3, readRatio, str);
                    }
                });
            } catch (Throwable th) {
                Arrays.fill(deriveKey, (byte) 0);
                throw th;
            }
        } catch (Exception e) {
            appendErrorLog(targetReport(gameTarget, str + "已完成，状态暂未刷新\n\n" + OperationFailure.format(OperationFailure.wrap(str + "后刷新", "提交完成后重新读取配置", gameTarget.packageName, e).state("已完成" + str + "并通过文件校验", "提交已完成；未因本次刷新失败执行回滚", backupPath(str2)).hint("本次" + str + "已经成功，状态暂未刷新，可重新读取。"))));
            runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m27lambda$refreshAfterCommit$38$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, str);
                }
            });
        }
    }

    /* renamed from: lambda$refreshAfterCommit$37$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m26lambda$refreshAfterCommit$37$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, GameRepository.Snapshot snapshot, int i, int i2, int i3, String str) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = snapshot;
            this.initializing = true;
            selectValue(this.quality, ConfigPatcher.QUALITY_VALUES, i);
            selectValue(this.fps, ConfigPatcher.FPS_VALUES, i2);
            this.ratio.setProgress(i3 - 25);
            this.initializing = false;
            clearCurrentError();
            this.currentLabel.setText(currentSettings(gameTarget, i, i2, snapshot.versionName));
            setBusy(false, str + "已完成并校验", "当前状态已刷新。");
            updateHints();
            new AlertDialog.Builder(this).setTitle(str + "完成").setMessage(targetReport(gameTarget, "设置已" + str + "并通过校验。请重新进入游戏确认实际效果。")).setPositiveButton("知道了", (DialogInterface.OnClickListener) null).show();
        }
    }

    /* renamed from: lambda$refreshAfterCommit$38$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m27lambda$refreshAfterCommit$38$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, String str) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.snapshot = null;
            clearCurrentError();
            this.lastReport += "\n\n状态暂未刷新，可重新读取。";
            setBusy(false, str + "已完成", "设置已" + str + "并通过校验。状态暂未刷新，可重新读取。");
        }
    }

    private String configurationPaths(GameRepository.Snapshot snapshot) {
        return "Engine.ini: " + snapshot.enginePath + "\nGame.ini: " + snapshot.gamePath + "\nUserGameplay.ini: " + snapshot.gameplayPath;
    }

    private String failurePath(GameRepository.Snapshot snapshot, Exception exc) {
        if (exc instanceof OperationFailure) {
            String path = ((OperationFailure) exc).getPath();
            if ("Engine.ini".equals(path)) {
                return snapshot.enginePath;
            }
            if ("Game.ini".equals(path)) {
                return snapshot.gamePath;
            }
            if ("UserGameplay.ini".equals(path)) {
                return snapshot.gameplayPath;
            }
        }
        return configurationPaths(snapshot);
    }

    private String backupPath(String str) {
        Matcher matcher = Pattern.compile("/data/adb/deltaconfig/[^\\s；;]+").matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHints() {
        int i;
        if (this.ratio == null || this.shortEdge == null || this.initializing) {
            return;
        }
        int progress = this.ratio.getProgress() + 25;
        this.ratioLabel.setText("目标渲染比例  " + progress + "%" + (progress == 100 ? " · 原生目标" : ""));
        boolean z = false;
        try {
            i = Integer.parseInt(this.shortEdge.getText().toString().trim());
        } catch (NumberFormatException e) {
            i = 0;
        }
        this.targetLabel.setText((progress == 100 ? "所有档位使用 Surface 原尺寸目标；系统缩放仍可能影响最终尺寸。" : "场景短边请求：" + Math.round((i * progress) / 100.0f) + " 像素 · 比例基于上方校准值") + "\n此处为待写入目标，不代表游戏当前的实测渲染比例。");
        int selectedItemPosition = this.quality == null ? -1 : this.quality.getSelectedItemPosition() - RISK_NOTICE_VERSION;
        int selectedItemPosition2 = this.fps != null ? this.fps.getSelectedItemPosition() - RISK_NOTICE_VERSION : -1;
        int fpsForInternal = selectedItemPosition2 < 0 ? 0 : ConfigPatcher.fpsForInternal(ConfigPatcher.FPS_VALUES[selectedItemPosition2]);
        String str = selectedItemPosition == 5 ? "终极档有独立的资源表帧率上限；本应用不会修改该表。" : "游戏可能按设备支持状态限制实际帧率。";
        if (fpsForInternal >= 90) {
            str = str + " " + fpsForInternal + " 帧还要求系统向游戏报告至少 " + fpsForInternal + "Hz。";
        }
        this.limitLabel.setText(str);
        if (this.previewButton != null) {
            Button button = this.previewButton;
            if (!this.busy && riskAcknowledged() && this.snapshot != null && targetSettingsSelected()) {
                z = RISK_NOTICE_VERSION;
            }
            button.setEnabled(z);
        }
    }

    private int inferRatio(byte[] bArr, int i) {
        Matcher matcher = Pattern.compile("(?:UltraLowResolution|LowResolution|MidResolution|HighResolution|UltraHighResolution|SuperResolution)=(\\d+)").matcher(new String(bArr, StandardCharsets.UTF_8));
        int i2 = -1;
        int i3 = 0;
        while (matcher.find()) {
            int parseInt = Integer.parseInt(matcher.group(RISK_NOTICE_VERSION));
            if (i2 < 0) {
                i2 = parseInt;
            } else if (i2 != parseInt) {
                return 75;
            }
            i3 += RISK_NOTICE_VERSION;
        }
        if ((i3 != 90 || i2 < 8192) && i3 == 90 && i > 0) {
            return Math.max(25, Math.min(100, (int) Math.round((i2 * 100.0d) / i)));
        }
        return 100;
    }

    private int readRatio(GameRepository.Snapshot snapshot, int i) throws OperationFailure {
        try {
            return inferRatio(snapshot.game, i);
        } catch (RuntimeException e) {
            throw OperationFailure.wrap("读取渲染比例", "解析 Game.ini 的分辨率数值", snapshot.gamePath, e).hint("Game.ini 中的分辨率数值无法解析；请保留此报告和原文件，不要继续覆盖配置。");
        }
    }

    private void fail(final GameRepository gameRepository, final GameTarget gameTarget, final String str, Exception exc) {
        final String userReport = appendErrorLog(targetReport(gameTarget, new StringBuilder().append(str).append("\n\n").append(OperationFailure.format(exc)).toString())) ? OperationFailure.userReport(exc) : OperationFailure.userReport(exc).replace("详细诊断已保存至本机错误日志。", "本次错误日志未能保存；以上操作失败信息仍有效。");
        runOnUiThread(new Runnable() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m8lambda$fail$40$iogithubdeltaconfigMainActivity(gameRepository, gameTarget, str, userReport);
            }
        });
    }

    /* renamed from: lambda$fail$40$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$fail$40$iogithubdeltaconfigMainActivity(GameRepository gameRepository, GameTarget gameTarget, String str, String str2) {
        if (isCurrentTarget(gameRepository, gameTarget)) {
            this.lastErrorReport = targetReport(gameTarget, str + "\n\n" + str2);
            this.lastReport = this.lastErrorReport;
            this.errorButton.setVisibility(0);
            if (str2.length() > 1200) {
                str2 = str2.substring(0, 1200) + "\n\n点击“查看本次错误”阅读原因。";
            }
            setBusy(false, str, str2);
            showErrorDetails();
        }
    }

    private void clearCurrentError() {
        this.lastErrorReport = null;
        this.errorButton.setVisibility(8);
    }

    private synchronized byte[] readErrorLog() throws IOException {
        File file = new File(getFilesDir(), "error-history.log");
        if (!file.isFile()) {
            return new byte[0];
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        try {
            int min = (int) Math.min(randomAccessFile.length(), 524288L);
            byte[] bArr = new byte[min];
            randomAccessFile.seek(randomAccessFile.length() - min);
            randomAccessFile.readFully(bArr);
            randomAccessFile.close();
            return bArr;
        } finally {
        }
    }

    private synchronized boolean appendErrorLog(String str) {
        try {
            byte[] readErrorLog = readErrorLog();
            byte[] bytes = ("\n\n===== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date()) + " =====\n" + str + "\n").getBytes(StandardCharsets.UTF_8);
            int length = readErrorLog.length + bytes.length;
            byte[] bArr = new byte[length];
            System.arraycopy(readErrorLog, 0, bArr, 0, readErrorLog.length);
            System.arraycopy(bytes, 0, bArr, readErrorLog.length, bytes.length);
            int max = Math.max(0, length - ERROR_LOG_LIMIT);
            while (max < length && (bArr[max] & 192) == 128) {
                max += RISK_NOTICE_VERSION;
            }
            FileOutputStream openFileOutput = openFileOutput("error-history.log", 0);
            try {
                openFileOutput.write(bArr, max, length - max);
                if (openFileOutput != null) {
                    openFileOutput.close();
                }
            } finally {
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void migrateSavedError() {
        SharedPreferences sharedPreferences = getSharedPreferences("error_log", 0);
        if (sharedPreferences.getBoolean("last_error_migrated", false)) {
            return;
        }
        File file = new File(getFilesDir(), "last-error.txt");
        boolean isFile = file.isFile() ^ RISK_NOTICE_VERSION;
        if (!isFile) {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                try {
                    int min = (int) Math.min(randomAccessFile.length(), 262144L);
                    byte[] bArr = new byte[min];
                    randomAccessFile.seek(randomAccessFile.length() - min);
                    randomAccessFile.readFully(bArr);
                    isFile = appendErrorLog("旧版错误记录\n" + new String(bArr, StandardCharsets.UTF_8));
                    randomAccessFile.close();
                } finally {
                }
            } catch (IOException e) {
            }
        }
        if (isFile) {
            sharedPreferences.edit().putBoolean("last_error_migrated", true).apply();
            deleteFile("last-error.txt");
        }
    }

    private void showErrorLog() {
        try {
            byte[] readErrorLog = readErrorLog();
            final String str = readErrorLog.length == 0 ? "暂无错误日志。" : new String(readErrorLog, StandardCharsets.UTF_8);
            ScrollView scrollView = new ScrollView(this);
            TextView text = text(str, 13, INK, false);
            text.setTextIsSelectable(true);
            text.setPadding(dp(20), dp(12), dp(20), dp(16));
            scrollView.addView(text);
            new AlertDialog.Builder(this).setTitle("错误日志（历史记录）").setView(scrollView).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).setPositiveButton("复制日志", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda45
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m41lambda$showErrorLog$41$iogithubdeltaconfigMainActivity(str, dialogInterface, i);
                }
            }).show();
        } catch (IOException e) {
            new AlertDialog.Builder(this).setTitle("错误日志读取失败").setMessage("无法读取本应用保存的错误日志；不会影响游戏配置。").setPositiveButton("知道了", (DialogInterface.OnClickListener) null).show();
        }
    }

    /* renamed from: lambda$showErrorLog$41$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m41lambda$showErrorLog$41$iogithubdeltaconfigMainActivity(String str, DialogInterface dialogInterface, int i) {
        ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("鼠鼠工具箱错误日志", str));
    }

    private void showErrorDetails() {
        if (this.lastErrorReport == null || this.lastErrorReport.isEmpty()) {
            return;
        }
        final String str = this.lastErrorReport;
        ScrollView scrollView = new ScrollView(this);
        TextView text = text(str, 14, INK, false);
        text.setTextIsSelectable(true);
        text.setPadding(dp(20), dp(12), dp(20), dp(16));
        scrollView.addView(text);
        new AlertDialog.Builder(this).setTitle("本次错误").setView(scrollView).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).setPositiveButton("复制报错", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda17
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m40lambda$showErrorDetails$42$iogithubdeltaconfigMainActivity(str, dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$showErrorDetails$42$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$showErrorDetails$42$iogithubdeltaconfigMainActivity(String str, DialogInterface dialogInterface, int i) {
        ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("鼠鼠工具箱错误详情", str));
    }

    private void setBusy(boolean z, String str, String str2) {
        this.busy = z;
        boolean z2 = false;
        this.progress.setVisibility(z ? 0 : 8);
        this.statusTitle.setText(str);
        this.statusBody.setText(str2);
        boolean z3 = (z || !riskAcknowledged()) ? false : RISK_NOTICE_VERSION;
        this.readButton.setEnabled(z3);
        this.restoreButton.setEnabled(z3);
        this.previewButton.setEnabled((z3 && this.snapshot != null && targetSettingsSelected()) ? RISK_NOTICE_VERSION : false);
        this.resetButton.setEnabled((!z3 || this.snapshot == null) ? false : RISK_NOTICE_VERSION);
        this.quality.setEnabled((!z3 || this.snapshot == null) ? false : RISK_NOTICE_VERSION);
        this.fps.setEnabled((!z3 || this.snapshot == null) ? false : RISK_NOTICE_VERSION);
        this.ratio.setEnabled((!z3 || this.snapshot == null) ? false : RISK_NOTICE_VERSION);
        EditText editText = this.shortEdge;
        if (z3 && this.snapshot != null) {
            z2 = RISK_NOTICE_VERSION;
        }
        editText.setEnabled(z2);
        this.gameTargetPicker.setEnabled(!z);
        this.refreshTargetsButton.setEnabled(z ^ RISK_NOTICE_VERSION);
        updateRootButton();
    }

    private void showReport() {
        new AlertDialog.Builder(this).setTitle("最近操作记录").setMessage(this.lastReport).setNegativeButton("关闭", (DialogInterface.OnClickListener) null).setPositiveButton("复制记录", new DialogInterface.OnClickListener() { // from class: io.github.deltaconfig.MainActivity$$ExternalSyntheticLambda31
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m42lambda$showReport$43$iogithubdeltaconfigMainActivity(dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$showReport$43$io-github-deltaconfig-MainActivity, reason: not valid java name */
    /* synthetic */ void m42lambda$showReport$43$iogithubdeltaconfigMainActivity(DialogInterface dialogInterface, int i) {
        ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("鼠鼠工具箱操作记录", this.lastReport));
    }

    private String fpsLabel(int i) {
        for (int i2 = 0; i2 < ConfigPatcher.FPS_VALUES.length; i2 += RISK_NOTICE_VERSION) {
            if (ConfigPatcher.FPS_VALUES[i2] == i) {
                return ConfigPatcher.FPS_LABELS[i2];
            }
        }
        return "其他帧率档位（" + i + "）";
    }

    private void selectValue(Spinner spinner, int[] iArr, int i) {
        int i2 = 0;
        while (i2 < iArr.length) {
            int i3 = iArr[i2];
            i2 += RISK_NOTICE_VERSION;
            if (i3 == i) {
                spinner.setSelection(i2);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private boolean targetSettingsSelected() {
        return this.quality != null && this.fps != null && this.quality.getSelectedItemPosition() > 0 && this.fps.getSelectedItemPosition() > 0;
    }

    private String currentSettings(GameTarget gameTarget, int i, int i2, String str) {
        return "发行版：" + gameTarget.label + "\n当前：" + (i < 0 ? "画质由游戏生成" : ConfigPatcher.qualityLabel(i)) + " / " + (i2 < 0 ? "帧率由游戏生成" : fpsLabel(i2)) + "\n游戏版本：" + str;
    }

    private int displayShortEdge() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        return Math.max(200, Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels));
    }

    private Spinner spinner(String str, String[] strArr) {
        String[] strArr2 = new String[strArr.length + RISK_NOTICE_VERSION];
        strArr2[0] = str;
        System.arraycopy(strArr, 0, strArr2, RISK_NOTICE_VERSION, strArr.length);
        return selectionSpinner(strArr2);
    }

    private ArrayAdapter<String> selectionAdapter(final Spinner spinner, String[] strArr) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strArr) { // from class: io.github.deltaconfig.MainActivity.5
            @Override // android.widget.ArrayAdapter, android.widget.Adapter
            public View getView(int i, View view, ViewGroup viewGroup) {
                TextView textView = (TextView) super.getView(i, view, viewGroup);
                textView.setTextColor(MainActivity.this.controlColors(MainActivity.INK, MainActivity.MUTED));
                textView.setTextSize(16.0f);
                textView.setSingleLine(false);
                textView.setMaxLines(2);
                textView.setMinimumHeight(MainActivity.this.dp(48));
                textView.setGravity(16);
                textView.setPadding(MainActivity.this.dp(8), MainActivity.this.dp(6), MainActivity.this.dp(8), MainActivity.this.dp(6));
                return textView;
            }

            @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter, android.widget.SpinnerAdapter
            public View getDropDownView(int i, View view, ViewGroup viewGroup) {
                TextView textView = (TextView) super.getDropDownView(i, view, viewGroup);
                textView.setTextColor(MainActivity.INK);
                textView.setTextSize(16.0f);
                textView.setSingleLine(false);
                textView.setMaxLines(2);
                textView.setMinimumHeight(MainActivity.this.dp(48));
                textView.setPadding(MainActivity.this.dp(14), MainActivity.this.dp(10), MainActivity.this.dp(14), MainActivity.this.dp(10));
                StateListDrawable stateListDrawable = new StateListDrawable();
                int[] iArr = {android.R.attr.state_pressed};
                MainActivity mainActivity = MainActivity.this;
                int i2 = MainActivity.PINK;
                stateListDrawable.addState(iArr, mainActivity.shape(MainActivity.PINK, MainActivity.BORDER, 10));
                stateListDrawable.addState(new int[]{android.R.attr.state_focused}, MainActivity.this.shape(MainActivity.PINK, MainActivity.ACCENT, 10));
                stateListDrawable.addState(new int[]{android.R.attr.state_checked}, MainActivity.this.shape(MainActivity.PINK, MainActivity.BORDER, 10));
                int[] iArr2 = new int[0];
                MainActivity mainActivity2 = MainActivity.this;
                if (i != spinner.getSelectedItemPosition()) {
                    i2 = -1796;
                }
                stateListDrawable.addState(iArr2, mainActivity2.shape(i2, MainActivity.BORDER, 10));
                textView.setBackground(stateListDrawable);
                if (textView instanceof CheckedTextView) {
                    ((CheckedTextView) textView).setCheckMarkTintList(ColorStateList.valueOf(MainActivity.ACCENT));
                }
                return textView;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return arrayAdapter;
    }

    private Spinner selectionSpinner(String[] strArr) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter((SpinnerAdapter) selectionAdapter(spinner, strArr));
        spinner.setMinimumHeight(dp(48));
        spinner.setPadding(dp(4), 0, dp(4), 0);
        spinner.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-16842910}, new int[]{android.R.attr.state_focused}, new int[]{android.R.attr.state_pressed}, new int[0]}, new int[]{BORDER, ACCENT, ACCENT, ACCENT}));
        spinner.setPopupBackgroundDrawable(shape(-1796, BORDER, 14));
        return spinner;
    }

    private Button button(String str, boolean z) {
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        button.setTextSize(15.0f);
        button.setMinHeight(dp(48));
        button.setTextColor(new ColorStateList(new int[][]{new int[]{-16842910}, new int[]{android.R.attr.state_pressed}, new int[]{android.R.attr.state_focused}, new int[0]}, new int[]{DISABLED_INK, INK, INK, z ? INK : ACCENT}));
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{-16842910}, shape(DISABLED, BORDER, 13));
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, shape(z ? -615747 : PINK, ACCENT, 13));
        int[] iArr = {android.R.attr.state_focused};
        int i = ROSE;
        stateListDrawable.addState(iArr, shape(z ? ROSE : -4876, ACCENT, 13));
        int[] iArr2 = new int[0];
        if (!z) {
            i = -218106888;
        }
        stateListDrawable.addState(iArr2, shape(i, z ? -1665097 : LAVENDER, 13));
        button.setBackgroundTintList(null);
        button.setBackground(stateListDrawable);
        return button;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ColorStateList controlColors(int i, int i2) {
        return new ColorStateList(new int[][]{new int[]{-16842910}, new int[0]}, new int[]{i2, i});
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GradientDrawable shape(int i, int i2, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(i3));
        gradientDrawable.setStroke(dp(RISK_NOTICE_VERSION), i2);
        return gradientDrawable;
    }

    private LinearLayout column() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(RISK_NOTICE_VERSION);
        return linearLayout;
    }

    private LinearLayout card() {
        LinearLayout column = column();
        column.setPadding(dp(18), dp(18), dp(18), dp(18));
        column.setBackground(shape(CARD, BORDER, 18));
        return column;
    }

    private TextView text(String str, int i, int i2, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        textView.setLineSpacing(dp(3), 1.0f);
        if (z) {
            textView.setTypeface(Typeface.DEFAULT, RISK_NOTICE_VERSION);
        }
        return textView;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(BORDER);
        view.setMinimumHeight(dp(RISK_NOTICE_VERSION));
        return view;
    }

    private void add(LinearLayout linearLayout, View view, int i) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, view.getMinimumHeight() == dp(RISK_NOTICE_VERSION) ? dp(RISK_NOTICE_VERSION) : -2);
        layoutParams.topMargin = dp(i);
        linearLayout.addView(view, layoutParams);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.pendingActionDialog != null) {
            this.pendingActionDialog.dismiss();
        }
        super.onDestroy();
        this.worker.shutdown();
    }
}
