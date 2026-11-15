package io.github.dfmperf.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * 读取 / 校验 / 写入游戏配置。
 *
 * 不保留备份目录：写入前把原字节留在内存里，任何一步失败就用它回滚；
 * 成功后直接丢弃，磁盘上不留多余文件。
 */
public final class Repo {
    public final File engine;
    public final File game;
    public final File gameplay;
    public final File lib;
    public final File dataDir;

    public Repo(File engine, File game, File gameplay, File lib, File dataDir) {
        this.engine = engine;
        this.game = game;
        this.gameplay = gameplay;
        this.lib = lib;
        this.dataDir = dataDir;
    }

    public static Repo forGame(File dataDir, String pkg, File nativeLibDir, File externalFilesDir) {
        File cfg = new File(dataDir, "files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android");
        File engine = new File(cfg, "Engine.ini");
        File game = new File(cfg, "Game.ini");
        File gameplay = null;
        if (externalFilesDir != null) {
            gameplay = new File(externalFilesDir,
                    "UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/UserGameplay.ini");
        }
        if (gameplay == null || !gameplay.isFile()) {
            File alt = new File("/data/media/0/Android/data/" + pkg
                    + "/files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/UserGameplay.ini");
            if (alt.isFile()) gameplay = alt;
        }
        File lib = new File(nativeLibDir, "libUE4.so");
        return new Repo(engine, game, gameplay, lib, dataDir);
    }

    /** 写入目标档位。失败会用内存里的原始字节回滚。 */
    public String apply(Material m, int quality, int fpsInternal, int ratio, int shortEdge) throws Exception {
        byte[] eng = readFile(engine, ConfigCrypto.MAX_INI_BYTES);
        byte[] gam = readFile(game, ConfigCrypto.MAX_INI_BYTES);
        byte[] gp = readFile(gameplay, ConfigCrypto.MAX_INI_BYTES);

        String tuple = ConfigPlan.padTuple(ConfigPlan.padPixels(ratio, shortEdge));
        boolean same = ConfigPlan.engineMatches(eng, gp, m.material, quality, fpsInternal)
                && tuple.equals(Ini.get(new String(gam, "UTF-8"), ConfigPlan.PAD_SECTION, "SmallPadConfig"));
        if (same) return "已是目标档位，未写入";

        ConfigPlan plan = ConfigPlan.plan(eng, gam, gp, m.material, quality, fpsInternal, ratio, shortEdge);
        try {
            writeAtomic(engine, plan.engine);
            writeAtomic(game, plan.game);
            verify(plan, m);
            return "已写入并回读校验：" + plan.summary;
        } catch (Exception e) {
            try {
                writeAtomic(engine, eng);
                writeAtomic(game, gam);
            } catch (Exception ignored) {
                throw new IOException("写入失败且回滚失败，请重新启动游戏让游戏重建配置: " + e.getMessage(), e);
            }
            throw new IOException("写入失败，已回滚到原配置: " + e.getMessage(), e);
        }
    }

    /** 删除本工具写入的覆盖项（画质/帧率/渲染）。 */
    public String removeOverrides(Material m) throws Exception {
        byte[] eng = readFile(engine, ConfigCrypto.MAX_INI_BYTES);
        byte[] gam = readFile(game, ConfigCrypto.MAX_INI_BYTES);
        byte[] gp = readFile(gameplay, ConfigCrypto.MAX_INI_BYTES);

        ConfigPlan plan = ConfigPlan.removeOverrides(eng, gam, gp, m.material);
        if (Arrays.equals(plan.engine, eng) && Arrays.equals(plan.game, gam)) {
            return "当前没有本工具写入的覆盖项";
        }
        try {
            writeAtomic(engine, plan.engine);
            writeAtomic(game, plan.game);
            byte[] re = readFile(engine, ConfigCrypto.MAX_INI_BYTES);
            if (!Arrays.equals(re, plan.engine)) throw new IOException("重置后回读不一致");
            ConfigCrypto.decodeIni(re, m.key);
            return "已清除覆盖项，默认档位由游戏下次启动重建";
        } catch (Exception e) {
            try {
                writeAtomic(engine, eng);
                writeAtomic(game, gam);
            } catch (Exception ignored) {
                // 保留原始错误
            }
            throw new IOException("重置失败，已回滚: " + e.getMessage(), e);
        }
    }

    private void verify(ConfigPlan plan, Material m) throws Exception {
        byte[] re = readFile(engine, ConfigCrypto.MAX_INI_BYTES);
        byte[] rg = readFile(game, ConfigCrypto.MAX_INI_BYTES);
        if (!Arrays.equals(re, plan.engine) || !Arrays.equals(rg, plan.game)) {
            throw new IOException("写入后回读字节不一致");
        }
        ConfigCrypto.decodeIni(re, m.key); // 认证不通过会抛异常
    }

    /** 同目录临时文件 + fsync + 回读 + rename，尽量缩小半写入窗口。 */
    public static void writeAtomic(File target, byte[] data) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".dfmperf.tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(data);
            out.getFD().sync();
        } finally {
            out.close();
        }
        byte[] back = readFile(tmp, ConfigCrypto.MAX_INI_BYTES);
        if (!Arrays.equals(back, data)) {
            tmp.delete();
            throw new IOException("暂存文件回读不一致: " + tmp);
        }
        if (target.exists() && !target.delete()) {
            tmp.delete();
            throw new IOException("无法替换目标文件: " + target);
        }
        if (!tmp.renameTo(target)) {
            tmp.delete();
            throw new IOException("重命名失败: " + target);
        }
    }

    public static byte[] readFile(File f, int max) throws IOException {
        if (!f.isFile()) throw new IOException("文件不存在: " + f);
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            long len = raf.length();
            if (len <= 0 || len > max) throw new IOException("文件大小异常: " + f + " (" + len + ")");
            byte[] b = new byte[(int) len];
            raf.readFully(b);
            return b;
        }
    }
}
