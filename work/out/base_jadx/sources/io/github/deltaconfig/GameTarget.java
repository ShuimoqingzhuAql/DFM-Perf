package io.github.deltaconfig;

/* loaded from: classes.dex */
public final class GameTarget {
    public final boolean international;
    public final String label;
    public final String packageName;
    public static final GameTarget CN = new GameTarget(GameRepository.PACKAGE, "国服（腾讯）", false);
    public static final GameTarget GLOBAL = new GameTarget("com.proxima.dfm", "国际服（Level Infinite）", true);
    public static final GameTarget GARENA = new GameTarget("com.garena.game.df", "国际服（Garena）", true);
    private static final GameTarget[] TARGETS = {CN, GLOBAL, GARENA};

    private GameTarget(String str, String str2, boolean z) {
        this.packageName = str;
        this.label = str2;
        this.international = z;
    }

    public static GameTarget[] values() {
        return (GameTarget[]) TARGETS.clone();
    }

    public static GameTarget lookup(String str) {
        for (GameTarget gameTarget : TARGETS) {
            if (gameTarget.packageName.equals(str)) {
                return gameTarget;
            }
        }
        throw new IllegalArgumentException("未识别的游戏发行版；仅支持列表中的国服、Level Infinite 与 Garena 版本");
    }

    public static GameTarget initialSelection(String str, boolean[] zArr) {
        if (zArr == null || zArr.length != TARGETS.length) {
            throw new IllegalArgumentException("已安装发行版状态与支持列表不一致");
        }
        if (str != null) {
            try {
                return lookup(str);
            } catch (IllegalArgumentException e) {
            }
        }
        GameTarget gameTarget = null;
        for (int i = 0; i < TARGETS.length; i++) {
            if (zArr[i]) {
                if (gameTarget != null) {
                    return CN;
                }
                gameTarget = TARGETS[i];
            }
        }
        return gameTarget == null ? CN : gameTarget;
    }
}
