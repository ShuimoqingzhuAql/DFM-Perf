package io.github.deltaconfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import io.github.deltaconfig.RootShell;
import io.github.deltaconfig.core.ConfigCompatibility;
import io.github.deltaconfig.core.ConfigCrypto;
import io.github.deltaconfig.core.ConfigPatcher;
import io.github.deltaconfig.core.LibraryMaskResolver;
import io.github.deltaconfig.core.OperationFailure;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class GameRepository {
    private static final long APK_INFLATE_LIMIT = 1073741824;
    public static final int BASELINE_CODE = 2207;
    public static final String BASELINE_VERSION = "1.201.37117.67";
    private static final int LIBRARY_READ_LIMIT = 1048576;
    private static final long LIBRARY_SIZE_LIMIT = 8589934592L;
    private static final int LIMIT = 4194304;
    public static final String PACKAGE = "com.tencent.tmgp.dfm";
    private static final String RELATIVE = "/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/";
    private static final String ROOT = "/data/adb/deltaconfig";
    private static final long TIMEOUT = 120000;
    private String backupPath;
    private final String base;
    private final Context context;
    private String operation;
    private boolean originalRestored;
    private final String packageName;
    private String rollbackStatus;
    private RootShell.Access rootAccess;
    private volatile boolean rootAuthorized;
    private final GameTarget target;
    private final int userId;
    private String writeStatus;

    public GameRepository(Context context) {
        this(context, PACKAGE);
    }

    public GameRepository(Context context, String str) {
        this.operation = "读取配置";
        this.writeStatus = "未写入游戏配置";
        this.rollbackStatus = "无需回滚";
        this.target = GameTarget.lookup(str);
        this.packageName = this.target.packageName;
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext != null ? applicationContext : context;
        this.userId = Process.myUid() / 100000;
        this.base = backupBase(str, this.userId);
    }

    public String getPackageName() {
        return this.packageName;
    }

    public static final class Snapshot {
        public final String compatibilityDescription;
        public final byte[] engine;
        private final String engineHash;
        public final String enginePath;
        public final byte[] fixedMask;
        public final byte[] game;
        private final String gameHash;
        public final String gamePath;
        public final byte[] gameplay;
        private final String gameplayHash;
        public final String gameplayPath;
        private final Installation installation;
        public final String librarySource;
        public final String packageName;
        public final String rootAccessDescription;
        private final int uid;
        public final long versionCode;
        public final String versionName;

        private Snapshot(Paths paths, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, String str, String str2, String str3) throws Exception {
            this.engine = (byte[]) bArr.clone();
            this.game = (byte[]) bArr2.clone();
            this.gameplay = (byte[]) bArr3.clone();
            this.installation = paths.installation;
            this.packageName = this.installation.packageName;
            this.fixedMask = (byte[]) bArr4.clone();
            this.versionName = this.installation.versionName;
            this.versionCode = this.installation.versionCode;
            this.enginePath = paths.engine;
            this.gamePath = paths.game;
            this.gameplayPath = paths.gameplay;
            this.librarySource = str;
            this.uid = paths.info.uid;
            this.rootAccessDescription = str2;
            this.compatibilityDescription = str3;
            this.engineHash = GameRepository.hash(bArr);
            this.gameHash = GameRepository.hash(bArr2);
            this.gameplayHash = GameRepository.hash(bArr3);
        }
    }

    public synchronized Snapshot load() throws Exception {
        Paths paths;
        begin("读取配置");
        try {
            requireRoot();
            paths = paths();
            recover(paths);
        } catch (Exception e) {
            throw diagnosed(e);
        }
        return capture(paths, true);
    }

    public boolean hasRootAuthorization() {
        return this.rootAuthorized;
    }

    public synchronized String checkRootAuthorization() throws Exception {
        begin("检查 root 授权");
        try {
            requireRoot();
        } catch (Exception e) {
            throw diagnosed(e);
        }
        return "root 授权已确认（UID 0）；配置可读性将在读取时另行检查。";
    }

    /* JADX WARN: Unreachable blocks removed: 2, instructions: 3 */
    private Snapshot capture(Paths paths, boolean z) throws Exception {
        String str;
        byte[] bArr;
        Paths paths2;
        String str2;
        String str3;
        Exception exc;
        ResolvedMask resolveApkMask;
        byte[] bArr2;
        byte[] bArr3;
        Paths paths3;
        byte[] bArr4;
        ResolvedMask resolveNativeMask;
        byte[] bArr5;
        checkInstallation(paths, "读取配置前核对游戏安装身份");
        byte[] read = read(paths.engine, LIMIT, "读取 Engine.ini 字节");
        byte[] read2 = read(paths.game, LIMIT, "读取 Game.ini 字节");
        byte[] read3 = read(paths.gameplay, LIMIT, "读取 UserGameplay.ini 字节");
        String str4 = paths.info.nativeLibraryDir + "/libUE4.so";
        try {
            resolveNativeMask = resolveNativeMask(paths, str4, read3, z ? read : null);
            bArr5 = resolveNativeMask.mask;
            if (z) {
                authenticate(bArr5, read3, read, read2, paths, "本机库认证");
            } else {
                verifyGameplay(bArr5, read3, paths.gameplay, "还原前验证当前设备 Hash 派生");
            }
            if (z) {
                validateCompatibility(bArr5, read3, read, read2, paths, "本机库配置结构兼容检查");
            }
            checkInstallation(paths, "读取配置后核对游戏安装身份");
            bArr3 = read;
            str = str4;
            bArr = read3;
            paths2 = paths;
        } catch (Exception e) {
            e = e;
            str = str4;
            bArr = read3;
            paths2 = paths;
        }
        try {
            return new Snapshot(paths, bArr3, read2, read3, bArr5, str, this.rootAccess.description(), resolveNativeMask.description);
        } catch (Exception e2) {
            e = e2;
            Exception exc2 = e;
            if (stopLibraryFallback(exc2)) {
                throw exc2;
            }
            String str5 = paths2.info.sourceDir + "!lib/arm64-v8a/libUE4.so";
            try {
                resolveApkMask = resolveApkMask(paths2, bArr, z ? read : null);
                bArr2 = resolveApkMask.mask;
            } catch (Exception e3) {
                e = e3;
                str2 = str;
            }
            try {
                if (z) {
                    paths3 = paths2;
                    bArr4 = bArr;
                    bArr3 = str;
                    authenticate(bArr2, bArr, read, read2, paths, "APK 库回退认证");
                } else {
                    paths3 = paths2;
                    bArr4 = bArr;
                    bArr3 = str;
                    verifyGameplay(bArr2, bArr4, paths3.gameplay, "APK 库回退验证当前设备 Hash 派生");
                }
                if (z) {
                    validateCompatibility(bArr2, bArr4, read, read2, paths, "APK 库配置结构兼容检查");
                }
                checkInstallation(paths3, "APK 库读取后核对游戏安装身份");
                str2 = bArr3;
                str3 = str5;
                exc = exc2;
            } catch (Exception e4) {
                e = e4;
                str2 = bArr3;
                str3 = str5;
                exc = exc2;
                OperationFailure problem = problem("本机库与 APK 库两路读取/认证均失败", str2 + "\n" + str3, "两个库来源都未能完成配置认证；下方分别列出失败的文件与步骤", e);
                problem.addSuppressed(exc);
                throw problem;
            }
            try {
                return new Snapshot(paths, read, read2, bArr4, bArr2, str3, this.rootAccess.description(), resolveApkMask.description);
            } catch (Exception e5) {
                e = e5;
                OperationFailure problem2 = problem("本机库与 APK 库两路读取/认证均失败", str2 + "\n" + str3, "两个库来源都未能完成配置认证；下方分别列出失败的文件与步骤", e);
                problem2.addSuppressed(exc);
                throw problem2;
            }
        }
    }

    public synchronized String apply(Snapshot snapshot, byte[] bArr, byte[] bArr2) throws Exception {
        Paths paths;
        begin("修改配置");
        try {
            if (snapshot == null) {
                throw problem("检查配置快照", null, "尚无配置快照，请先读取配置", null);
            }
            if (bArr == null || bArr2 == null || bArr.length == 0 || bArr2.length == 0 || bArr.length > LIMIT || bArr2.length > LIMIT) {
                throw problem("检查待写入配置大小", snapshot.enginePath + "\n" + snapshot.gamePath, "Engine.ini 或 Game.ini 为空，或超过 4194304 字节安全上限", null);
            }
            requireRoot();
            paths = paths();
            recover(paths);
            checkSnapshot(paths, snapshot);
            authenticate(snapshot.fixedMask, snapshot.gameplay, snapshot.engine, snapshot.game, paths, "验证原配置");
            authenticate(snapshot.fixedMask, snapshot.gameplay, bArr, bArr2, paths, "验证待写入配置");
            validateCompatibility(snapshot.fixedMask, snapshot.gameplay, snapshot.engine, snapshot.game, paths, "修改前核对原配置结构");
            validateCompatibility(snapshot.fixedMask, snapshot.gameplay, bArr, bArr2, paths, "修改前核对目标配置结构");
        } catch (Exception e) {
            throw diagnosed(e);
        }
        return transact(paths, snapshot, (byte[]) bArr.clone(), (byte[]) bArr2.clone(), true);
    }

    public synchronized String resetDefaults(Snapshot snapshot) throws Exception {
        begin("重置为游戏默认配置");
        try {
            if (snapshot == null) {
                throw problem("检查重置前配置快照", null, "尚无配置快照，请先读取配置", null);
            }
            requireRoot();
            Paths paths = paths();
            recover(paths);
            checkSnapshot(paths, snapshot);
            authenticate(snapshot.fixedMask, snapshot.gameplay, snapshot.engine, snapshot.game, paths, "重置前验证当前配置");
            try {
                ConfigPatcher.Plan resetDefaults = ConfigPatcher.resetDefaults(snapshot.engine, snapshot.game, snapshot.gameplay, snapshot.fixedMask);
                if (resetDefaults == null || resetDefaults.engine == null || resetDefaults.game == null || resetDefaults.engine.length == 0 || resetDefaults.game.length == 0 || resetDefaults.engine.length > LIMIT || resetDefaults.game.length > LIMIT) {
                    throw problem("检查默认配置重建方案大小", paths.engine + "\n" + paths.game, "重置方案的 Engine.ini 或 Game.ini 为空，或超过 4194304 字节安全上限", null);
                }
                authenticate(snapshot.fixedMask, snapshot.gameplay, resetDefaults.engine, resetDefaults.game, paths, "验证删除覆盖项后的待写入配置");
                if (Arrays.equals(snapshot.engine, resetDefaults.engine) && Arrays.equals(snapshot.game, resetDefaults.game)) {
                    checkSnapshot(paths, snapshot);
                    return "当前配置无需删除本工具覆盖项，未写入游戏文件；默认档位仍由游戏自行决定。";
                }
                transact(paths, snapshot, (byte[]) resetDefaults.engine.clone(), (byte[]) resetDefaults.game.clone(), true);
                return "已删除本工具的画质、帧率及渲染覆盖项，并通过回读认证；默认值由游戏下次启动时重建。\n本次重置前配置备份：" + this.backupPath;
            } catch (Exception e) {
                throw wrap("生成删除本工具覆盖项的配置", paths.engine + "\n" + paths.game, e);
            }
        } catch (Exception e2) {
            throw diagnosed(e2);
        }
    }

    public synchronized String restoreLatest() throws Exception {
        String str;
        begin("还原配置");
        try {
            requireRoot();
            Paths paths = paths();
            recover(paths);
            Snapshot capture = capture(paths, false);
            String pointer = pointer("latest");
            if (pointer == null) {
                throw problem("查找最近成功备份", this.base + "/latest", "当前 Android 用户没有可恢复的备份索引", null);
            }
            str = this.base + "/" + pointer;
            JSONObject manifest = manifest(pointer, paths);
            byte[] read = read(str + "/Engine.ini", LIMIT, "读取待恢复的 Engine.ini 备份");
            byte[] read2 = read(str + "/Game.ini", LIMIT, "读取待恢复的 Game.ini 备份");
            verifyBackup(manifest, read, read2, read(str + "/UserGameplay.ini", LIMIT, "读取设备认证上下文备份"), str);
            verifyContext(capture.gameplay, manifest.getString("gameplayHash"), paths.gameplay, "核对备份设备认证上下文");
            authenticate(capture.fixedMask, capture.gameplay, read, read2, paths, "验证待恢复配置");
            transact(paths, capture, read, read2, false);
        } catch (Exception e) {
            throw diagnosed(e);
        }
        return "已恢复原配置；备份仍保留在 " + str;
    }

    public synchronized String latestBackupInfo() throws Exception {
        String pointer;
        begin("查询备份位置");
        try {
            requireRoot();
            paths();
            pointer = pointer("latest");
        } catch (Exception e) {
            throw diagnosed(e);
        }
        return pointer == null ? "尚无备份" : this.base + "/" + pointer;
    }

    private String transact(Paths paths, Snapshot snapshot, byte[] bArr, byte[] bArr2, boolean z) throws Exception {
        CommitBoundary commitBoundary;
        checkSnapshot(paths, snapshot);
        String str = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date()) + "-" + UUID.randomUUID().toString();
        String str2 = this.base + "/" + str;
        ensureStorage();
        checked("创建原配置备份目录", str2, "umask 077; mkdir " + q(str2), null, 8192);
        this.backupPath = str2;
        Metadata metadata = metadata(paths.engine, "备份前读取 Engine.ini 权限");
        Metadata metadata2 = metadata(paths.game, "备份前读取 Game.ini 权限");
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("format", 2).put("package", this.packageName).put("versionName", paths.installation.versionName).put("versionCode", paths.installation.versionCode).put("lastUpdateTime", paths.installation.lastUpdateTime).put("sourceDir", paths.installation.sourceDir).put("nativeLibraryDir", paths.installation.nativeLibraryDir).put("userId", this.userId).put("uid", paths.info.uid).put("device", deviceIdentity()).put("enginePath", paths.engine).put("gamePath", paths.game).put("gameplayPath", paths.gameplay).put("engineHash", snapshot.engineHash).put("gameHash", snapshot.gameHash).put("gameplayHash", snapshot.gameplayHash).put("updateLatest", z).put("engineMetadata", metadata.json()).put("gameMetadata", metadata2.json());
        backupOne(paths.engine, str2 + "/Engine.ini");
        backupOne(paths.game, str2 + "/Game.ini");
        backupOne(paths.gameplay, str2 + "/UserGameplay.ini");
        writeRoot(str2 + "/manifest.json", jSONObject.toString().getBytes(StandardCharsets.UTF_8), "保存备份清单");
        verifyBackup(jSONObject, read(str2 + "/Engine.ini", LIMIT, "回读 Engine.ini 原件备份"), read(str2 + "/Game.ini", LIMIT, "回读 Game.ini 原件备份"), read(str2 + "/UserGameplay.ini", LIMIT, "回读 UserGameplay.ini 原件备份"), str2);
        prepare(paths.engine, bArr, metadata, str);
        prepare(paths.game, bArr2, metadata2, str);
        checkSnapshot(paths, snapshot);
        writeRoot(str2 + "/state", "PREPARED\n".getBytes(StandardCharsets.US_ASCII), "保存准备完成事务状态");
        writePointer("pending", str);
        CommitBoundary commitBoundary2 = new CommitBoundary();
        this.writeStatus = "已开始替换游戏配置，可能已部分写入";
        this.rollbackStatus = "失败时将尝试回滚";
        try {
            checkInstallation(paths, "提交前再次核对游戏安装身份");
            try {
                commitBoundary = commitBoundary2;
            } catch (Exception e) {
                e = e;
                commitBoundary = commitBoundary2;
            }
        } catch (Exception e2) {
            e = e2;
            commitBoundary = commitBoundary2;
        }
        try {
            checked("替换 Engine.ini（重命名暂存文件）", paths.engine, "mv -f " + q(temporary(paths.engine, str, ".new")) + " " + q(paths.engine), null, 8192);
            checkInstallation(paths, "替换 Game.ini 前核对游戏安装身份");
            checked("替换 Game.ini（重命名暂存文件）", paths.game, "mv -f " + q(temporary(paths.game, str, ".new")) + " " + q(paths.game), null, 8192);
            checked("同步已写入配置到存储", paths.engine + "\n" + paths.game, "sync", null, 8192);
            byte[] read = read(paths.engine, LIMIT, "写入后回读 Engine.ini");
            byte[] read2 = read(paths.game, LIMIT, "写入后回读 Game.ini");
            byte[] read3 = read(paths.gameplay, LIMIT, "写入后回读 UserGameplay.ini（确认未被改动）");
            checkInstallation(paths, "写入后核对游戏安装身份");
            verifyBytes(read, hash(bArr), paths.engine, "写入后校验 Engine.ini 字节");
            verifyBytes(read2, hash(bArr2), paths.game, "写入后校验 Game.ini 字节");
            verifyBytes(read3, snapshot.gameplayHash, paths.gameplay, "写入后校验 UserGameplay.ini 未变化");
            authenticate(snapshot.fixedMask, read3, read, read2, paths, "写入后认证回读配置");
            verifyMetadata(paths.engine, metadata, "写入后验证 Engine.ini 权限");
            verifyMetadata(paths.game, metadata2, "写入后验证 Game.ini 权限");
            String str3 = str2 + "/state";
            String str4 = str3 + ".commit-" + UUID.randomUUID().toString();
            writeRoot(str4, "COMMITTED\n".getBytes(StandardCharsets.US_ASCII), "准备并同步已提交事务标记");
            commitBoundary.markerAttempted();
            checked("提交 COMMITTED 事务标记：重命名", str3, "mv -f " + q(str4) + " " + q(str3), null, 8192);
            commitBoundary.markerConfirmed();
            this.writeStatus = "配置已写入并校验通过；COMMITTED 标记已替换，等待存储同步";
            this.rollbackStatus = "未回滚：已发布提交标记";
            checked("提交 COMMITTED 事务标记：同步到存储", str3, "sync", null, 8192);
            commitBoundary.markerSynced();
            this.writeStatus = "配置已写入并通过回读、认证及权限校验";
            this.rollbackStatus = "未回滚：配置提交已成功";
            if (z) {
                writePointer("latest", str);
            }
            checked("删除已完成事务的 pending 索引", this.base + "/pending", "rm -f " + q(this.base + "/pending"), null, 8192);
            cleanup(paths, str);
            return "配置已写入并回读验证；原配置备份：" + str2;
        } catch (Exception e3) {
            e = e3;
            Exception exc = e;
            if (commitBoundary.canRollback()) {
                try {
                    rollback(paths, str, jSONObject);
                    this.writeStatus = "写入失败，原配置已恢复并校验通过";
                    this.rollbackStatus = "自动回滚成功";
                    throw exc;
                } catch (Exception e4) {
                    if (this.originalRestored) {
                        this.writeStatus = "原配置已恢复并校验通过；回滚记录或临时文件清理未完成";
                        this.rollbackStatus = "已还原游戏配置，但同步或收尾失败；请保留备份并查看具体失败步骤";
                    } else {
                        this.writeStatus = "配置可能部分写入；未能完成回滚";
                        this.rollbackStatus = "需要人工恢复；请勿启动游戏，请保留下面的原配置备份目录";
                    }
                    OperationFailure problem = problem("写入失败后的自动回滚", str2, this.originalRestored ? "原配置已还原，但回滚收尾失败；原始失败与回滚失败均保留在下方" : "原始写入失败，自动回滚也失败；两个错误均保留在下方", exc);
                    problem.addSuppressed(e4);
                    throw problem;
                }
            }
            if (!commitBoundary.isMarkerConfirmed()) {
                this.writeStatus = "配置已写入并校验通过；COMMITTED 标记重命名的结果尚未确认";
                this.rollbackStatus = "未自动回滚：提交标记可能已生效；pending 索引已保留，下次读取将按实际事务状态恢复或完成提交";
                throw exc;
            }
            if (!commitBoundary.isMarkerSynced()) {
                this.writeStatus = "配置已写入并校验通过；COMMITTED 标记已替换，但持久化同步未确认";
                this.rollbackStatus = "未回滚已发布的提交；pending 索引已保留，下次读取将按实际事务状态检查";
                throw exc;
            }
            this.writeStatus = "配置已写入并校验通过；备份索引或临时文件清理失败";
            this.rollbackStatus = "未回滚：配置提交已成功；请按具体失败步骤检查备份索引或临时文件";
            throw exc;
        }
    }

    private void backupOne(String str, String str2) throws Exception {
        checked("复制原配置到备份（来源 → 目标）", str + "\n→ " + str2, "umask 077; cp -p " + q(str) + " " + q(str2), null, 8192);
        checked("设置备份文件所有者", str2, "chown 0:0 " + q(str2), null, 8192);
        checked("设置备份文件权限", str2, "chmod 600 " + q(str2), null, 8192);
    }

    private void prepare(String str, byte[] bArr, Metadata metadata, String str2) throws Exception {
        regular(str, "准备替换文件：检查原配置");
        String temporary = temporary(str, str2, ".old");
        String temporary2 = temporary(str, str2, ".new");
        absent(temporary, "检查原件旁路副本路径");
        absent(temporary2, "检查新配置暂存路径");
        checked("创建原件旁路副本（来源 → 目标）", str + "\n→ " + temporary, "cp -p " + q(str) + " " + q(temporary), null, 8192);
        checked("创建新配置暂存文件（来源 → 目标）", str + "\n→ " + temporary2, "cp -p " + q(str) + " " + q(temporary2), null, 8192);
        setMetadata(temporary, metadata, "设置原件旁路副本权限");
        checked("写入新配置暂存字节", temporary2, "cat > " + q(temporary2), bArr, 8192);
        setMetadata(temporary2, metadata, "设置新配置暂存文件权限");
        verifyBytes(read(temporary2, LIMIT, "回读新配置暂存字节"), hash(bArr), temporary2, "校验新配置暂存文件字节");
    }

    private void recover(Paths paths) throws Exception {
        String pointer = pointer("pending");
        if (pointer == null) {
            return;
        }
        String str = this.base + "/" + pointer;
        this.backupPath = str;
        this.writeStatus = "发现未完成事务，游戏配置可能已部分写入";
        this.rollbackStatus = "正在检查上一次事务状态";
        JSONObject manifest = manifest(pointer, paths);
        String str2 = str + "/state";
        String trim = new String(read(str2, 128, "读取未完成事务状态"), StandardCharsets.US_ASCII).trim();
        if ("COMMITTED".equals(trim)) {
            this.writeStatus = "上次配置已提交；备份索引和临时文件清理尚未全部完成";
            this.rollbackStatus = "无需回滚已提交配置";
            if (restoreLatestFlag(manifest, str)) {
                writePointer("latest", pointer);
            }
            checked("恢复事务：删除已提交 pending 索引", this.base + "/pending", "rm -f " + q(this.base + "/pending"), null, 8192);
            cleanup(paths, pointer);
            this.writeStatus = "已完成上次已提交配置的索引和清理；本次尚未写入新配置";
            return;
        }
        if ("PREPARED".equals(trim) || "ROLLED_BACK".equals(trim)) {
            this.rollbackStatus = "尚未开始回滚，正在核对原件备份与当前认证上下文";
            try {
                rollback(paths, pointer, manifest);
                this.writeStatus = "本次读取前已自动恢复原配置，并通过回读校验";
                this.rollbackStatus = "未完成事务已成功回滚";
                return;
            } catch (Exception e) {
                if (this.originalRestored) {
                    this.writeStatus = "原配置已恢复并校验通过；事务恢复收尾未完成";
                    this.rollbackStatus = "游戏配置已还原，回滚记录或临时文件清理失败";
                } else {
                    this.writeStatus = "上次配置可能部分写入，自动恢复失败";
                    this.rollbackStatus = "回滚未完成，需要人工恢复；请保留原配置备份，不要启动游戏";
                }
                throw e;
            }
        }
        throw problem("识别未完成事务状态", str2, "事务状态不是 PREPARED、COMMITTED 或 ROLLED_BACK，为保护原件已停止操作", null);
    }

    private void rollback(Paths paths, String str, JSONObject jSONObject) throws Exception {
        String str2 = this.base + "/" + str;
        this.originalRestored = false;
        this.rollbackStatus = "正在从原件备份回滚";
        byte[] read = read(str2 + "/Engine.ini", LIMIT, "回滚：读取 Engine.ini 原件备份");
        byte[] read2 = read(str2 + "/Game.ini", LIMIT, "回滚：读取 Game.ini 原件备份");
        verifyBackup(jSONObject, read, read2, read(str2 + "/UserGameplay.ini", LIMIT, "回滚：读取认证上下文备份"), str2);
        verifyContext(read(paths.gameplay, LIMIT, "回滚：读取当前 UserGameplay.ini"), jSONObject.getString("gameplayHash"), paths.gameplay, "回滚：核对设备认证上下文");
        checkInstallation(paths, "回滚前核对游戏安装身份");
        restoreOne(paths, paths.engine, read, backupMetadata(jSONObject, "engineMetadata", str2), str);
        restoreOne(paths, paths.game, read2, backupMetadata(jSONObject, "gameMetadata", str2), str);
        checkInstallation(paths, "回滚后核对游戏安装身份");
        verifyBytes(read(paths.engine, LIMIT, "回滚后回读 Engine.ini"), jSONObject.getString("engineHash"), paths.engine, "回滚后校验 Engine.ini 字节");
        verifyBytes(read(paths.game, LIMIT, "回滚后回读 Game.ini"), jSONObject.getString("gameHash"), paths.game, "回滚后校验 Game.ini 字节");
        this.originalRestored = true;
        this.writeStatus = "原配置已恢复并校验通过";
        this.rollbackStatus = "游戏配置已还原，正在保存回滚状态并清理临时文件";
        writeRoot(str2 + "/state", "ROLLED_BACK\n".getBytes(StandardCharsets.US_ASCII), "保存已回滚事务状态");
        checked("回滚：同步还原配置到存储", paths.engine + "\n" + paths.game, "sync", null, 8192);
        checked("回滚：删除 pending 索引", this.base + "/pending", "rm -f " + q(this.base + "/pending"), null, 8192);
        cleanup(paths, str);
        this.rollbackStatus = "原配置回滚及收尾均已完成";
    }

    private Metadata backupMetadata(JSONObject jSONObject, String str, String str2) throws Exception {
        try {
            return Metadata.from(jSONObject.getJSONObject(str));
        } catch (Exception e) {
            throw problem("解析备份清单中的 " + str + " 权限记录", str2 + "/manifest.json", "备份权限记录缺失或 UID/GID/权限/SELinux 标签格式无效", e);
        }
    }

    private boolean restoreLatestFlag(JSONObject jSONObject, String str) throws Exception {
        try {
            return jSONObject.getBoolean("updateLatest");
        } catch (Exception e) {
            throw wrap("读取备份清单的 updateLatest 恢复标记", str + "/manifest.json", e);
        }
    }

    private void restoreOne(Paths paths, String str, byte[] bArr, Metadata metadata, String str2) throws Exception {
        regular(str, "回滚：检查目标原配置");
        String temporary = temporary(str, str2, ".rollback");
        noLink(temporary, "回滚：检查暂存文件路径");
        checked("回滚：创建暂存副本（来源 → 目标）", str + "\n→ " + temporary, "cp -p " + q(str) + " " + q(temporary), null, 8192);
        checked("回滚：写入原配置暂存字节", temporary, "cat > " + q(temporary), bArr, 8192);
        setMetadata(temporary, metadata, "回滚：设置暂存文件权限");
        verifyBytes(read(temporary, LIMIT, "回滚：回读暂存字节"), hash(bArr), temporary, "回滚：校验暂存文件字节");
        checkInstallation(paths, "回滚：替换原配置前再次核对游戏安装身份");
        checked("回滚：替换目标文件", str, "mv -f " + q(temporary) + " " + q(str), null, 8192);
        verifyMetadata(str, metadata, "回滚：验证还原文件权限");
    }

    private void cleanup(Paths paths, String str) throws Exception {
        String[] strArr = {paths.engine, paths.game};
        for (int i = 0; i < 2; i++) {
            String str2 = strArr[i];
            String[] strArr2 = {".new", ".old", ".rollback"};
            for (int i2 = 0; i2 < 3; i2++) {
                String temporary = temporary(str2, str, strArr2[i2]);
                checked("清理事务临时文件", temporary, "rm -f " + q(temporary), null, 8192);
            }
        }
    }

    private JSONObject manifest(String str, Paths paths) throws Exception {
        String str2 = this.base + "/" + str + "/manifest.json";
        try {
            JSONObject jSONObject = new JSONObject(new String(read(str2, 32768, "读取备份清单"), StandardCharsets.UTF_8));
            int i = jSONObject.getInt("format");
            if ((i != 1 && i != 2) || !this.packageName.equals(jSONObject.getString("package")) || !sameGameVersion(paths.installation.versionName, paths.installation.versionCode, jSONObject.getString("versionName"), jSONObject.getLong("versionCode")) || ((i == 2 && (jSONObject.getLong("lastUpdateTime") != paths.installation.lastUpdateTime || !paths.installation.sourceDir.equals(jSONObject.getString("sourceDir")) || !paths.installation.nativeLibraryDir.equals(jSONObject.getString("nativeLibraryDir")))) || jSONObject.getInt("userId") != this.userId || jSONObject.getInt("uid") != paths.info.uid || !deviceIdentity().equals(jSONObject.getString("device")) || !paths.engine.equals(jSONObject.getString("enginePath")) || !paths.game.equals(jSONObject.getString("gamePath")) || !paths.gameplay.equals(jSONObject.getString("gameplayPath")))) {
                throw problem("核对备份清单与当前环境", str2, "备份的设备标识、Android 用户、游戏版本、UID 或文件路径与当前环境不匹配", null);
            }
            return jSONObject;
        } catch (Exception e) {
            throw wrap("读取并核对备份清单", str2, e);
        }
    }

    private void verifyBackup(JSONObject jSONObject, byte[] bArr, byte[] bArr2, byte[] bArr3, String str) throws Exception {
        try {
            verifyBytes(bArr, jSONObject.getString("engineHash"), str + "/Engine.ini", "校验 Engine.ini 原件备份");
            verifyBytes(bArr2, jSONObject.getString("gameHash"), str + "/Game.ini", "校验 Game.ini 原件备份");
            verifyBytes(bArr3, jSONObject.getString("gameplayHash"), str + "/UserGameplay.ini", "校验 UserGameplay.ini 原件备份");
        } catch (Exception e) {
            throw wrap("校验备份清单记录的文件摘要", str + "/manifest.json", e);
        }
    }

    private void verifyBytes(byte[] bArr, String str, String str2, String str3) throws Exception {
        if (!hash(bArr).equals(str)) {
            throw problem(str3, str2, "文件内容与预期的 SHA-256 摘要不一致；文件可能被改动、写入不完整或备份损坏", null);
        }
    }

    private static void verifyGameplayContext(byte[] bArr, String str) throws Exception {
        if (!hash(bArr).equals(str)) {
            throw new IOException("UserGameplay 已变化，旧备份的认证上下文不再匹配；已停止恢复并保留事务记录及原件");
        }
    }

    private void verifyContext(byte[] bArr, String str, String str2, String str3) throws Exception {
        try {
            verifyGameplayContext(bArr, str);
        } catch (Exception e) {
            throw wrap(str3, str2, e);
        }
    }

    private void checkSnapshot(Paths paths, Snapshot snapshot) throws Exception {
        try {
            verifySnapshotTarget(this.packageName, snapshot.packageName);
            if (!snapshot.installation.equals(paths.installation)) {
                throw problem("核对读取快照的游戏版本与安装身份", "package:" + this.packageName, "游戏版本或安装来源自读取后发生变化；请重新读取，新版本不会复用旧配置快照", null);
            }
            checkInstallation(paths, "提交配置前核对游戏未升级或重新安装");
            if (this.rootAccess != null && this.rootAccess.description().equals(snapshot.rootAccessDescription)) {
                if (snapshot.uid == paths.info.uid && snapshot.enginePath.equals(paths.engine) && snapshot.gamePath.equals(paths.game) && snapshot.gameplayPath.equals(paths.gameplay)) {
                    verifyBytes(snapshot.engine, snapshot.engineHash, snapshot.enginePath, "核对内存 Engine.ini 快照未变化");
                    verifyBytes(snapshot.game, snapshot.gameHash, snapshot.gamePath, "核对内存 Game.ini 快照未变化");
                    verifyBytes(snapshot.gameplay, snapshot.gameplayHash, snapshot.gameplayPath, "核对内存 UserGameplay.ini 快照未变化");
                    verifyBytes(read(paths.engine, LIMIT, "重新读取 Engine.ini 检查并发变化"), snapshot.engineHash, paths.engine, "核对 Engine.ini 自读取后未变化");
                    verifyBytes(read(paths.game, LIMIT, "重新读取 Game.ini 检查并发变化"), snapshot.gameHash, paths.game, "核对 Game.ini 自读取后未变化");
                    verifyBytes(read(paths.gameplay, LIMIT, "重新读取 UserGameplay.ini 检查并发变化"), snapshot.gameplayHash, paths.gameplay, "核对 UserGameplay.ini 自读取后未变化");
                    return;
                }
                throw problem("核对读取快照的用户与路径", paths.engine + "\n" + paths.game + "\n" + paths.gameplay, "当前应用 UID 或配置路径与读取快照不一致，请重新读取", null);
            }
            throw problem("核对读取快照的 root 访问上下文", paths.engine, "读取快照与当前操作的 root 路径或挂载命名空间不一致；为避免在不同上下文写入，请重新读取配置", null);
        } catch (Exception e) {
            throw wrap("核对读取快照的游戏目标", "package:" + this.packageName, e);
        }
    }

    private static void verifySnapshotTarget(String str, String str2) throws IOException {
        GameTarget.lookup(str);
        if (!str.equals(str2)) {
            throw new IOException("读取快照属于其他游戏目标；国服与国际服配置不能交叉写入，请重新读取当前目标配置");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void verifyInstalledTarget(String str, String str2, String str3) throws IOException {
        GameTarget.lookup(str);
        if (!str.equals(str2) || !str.equals(str3)) {
            throw new IOException("查询到的游戏包名与当前所选目标不匹配，已停止读取");
        }
    }

    private static String backupBase(String str, int i) {
        GameTarget.lookup(str);
        if (i >= 0) {
            return "/data/adb/deltaconfig/backups/" + (PACKAGE.equals(str) ? "" : str + "/") + i;
        }
        throw new IllegalArgumentException("Android 用户编号无效");
    }

    private static String[] backupDirectories(String str, int i) {
        String backupBase = backupBase(str, i);
        if (PACKAGE.equals(str)) {
            return new String[]{ROOT, "/data/adb/deltaconfig/backups", backupBase};
        }
        return new String[]{ROOT, "/data/adb/deltaconfig/backups", "/data/adb/deltaconfig/backups/" + str, backupBase};
    }

    private static String privateConfigDirectory(String str, String str2) throws IOException {
        GameTarget.lookup(str);
        if (str2 == null || !str2.startsWith("/") || str2.indexOf(0) >= 0) {
            throw new IOException("所选游戏的应用私有目录无效");
        }
        return str2 + "/files" + RELATIVE;
    }

    private static String gameplayPath(String str, int i, boolean z) {
        GameTarget.lookup(str);
        if (i >= 0) {
            return (z ? "/data/media/" : "/storage/emulated/") + i + "/Android/data/" + str + "/files" + RELATIVE + "UserGameplay.ini";
        }
        throw new IllegalArgumentException("Android 用户编号无效");
    }

    private Paths paths() throws Exception {
        String canonical;
        try {
            PackageInfo packageInfo = this.context.getPackageManager().getPackageInfo(this.packageName, 0);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (applicationInfo == null || applicationInfo.uid / 100000 != this.userId) {
                throw problem("核对游戏所属 Android 用户", "package:" + this.packageName, "当前工具用户：" + this.userId + "；游戏用户：" + (applicationInfo == null ? "未知（缺少 ApplicationInfo）" : Integer.valueOf(applicationInfo.uid / 100000)) + "；拒绝跨用户读取", null);
            }
            try {
                Installation from = Installation.from(packageInfo, this.packageName);
                String privateConfigDirectory = privateConfigDirectory(this.packageName, applicationInfo.dataDir);
                try {
                    this.rootAccess = RootShell.selectForFile(privateConfigDirectory + "Engine.ini", TIMEOUT);
                    ensureStorage();
                    String canonical2 = canonical(privateConfigDirectory + "Engine.ini");
                    String canonical3 = canonical(privateConfigDirectory + "Game.ini");
                    String gameplayPath = gameplayPath(this.packageName, this.userId, false);
                    String gameplayPath2 = gameplayPath(this.packageName, this.userId, true);
                    try {
                        canonical = canonical(gameplayPath);
                    } catch (Exception e) {
                        try {
                            canonical = canonical(gameplayPath2);
                        } catch (Exception e2) {
                            OperationFailure problem = problem("定位 UserGameplay.ini 的两条外部存储路径", gameplayPath + "\n" + gameplayPath2, "外部存储路径与底层 media 路径均不可用；请先启动游戏生成配置，并检查存储挂载与 root 访问权限", e2);
                            problem.addSuppressed(e);
                            throw problem;
                        }
                    }
                    return new Paths(applicationInfo, from, canonical2, canonical3, canonical);
                } catch (RootShell.SelectionFailure e3) {
                    OperationFailure problem2 = problem("选择 root 访问上下文：逐路验证 Engine.ini 可见性", privateConfigDirectory + "Engine.ini", e3.getMessage(), e3);
                    if (e3.exitCode != null) {
                        problem2.exitCode(e3.exitCode.intValue());
                        throw problem2;
                    }
                    throw problem2;
                } catch (InterruptedException e4) {
                    Thread.currentThread().interrupt();
                    throw problem("选择 root 访问上下文：等待探测", privateConfigDirectory + "Engine.ini", "root 上下文探测被中断，尚未写入游戏配置", e4);
                } catch (Exception e5) {
                    throw wrap("选择 root 访问上下文", privateConfigDirectory + "Engine.ini", e5);
                }
            } catch (Exception e6) {
                throw wrap("读取实际游戏版本与安装身份", "package:" + this.packageName, e6);
            }
        } catch (PackageManager.NameNotFoundException e7) {
            throw problem("查询已安装游戏", "package:" + this.packageName, "当前 Android 用户未安装所选的" + this.target.label + "（" + this.packageName + "），或系统未向本应用提供该包信息；请安装并启动所选游戏生成配置", e7);
        } catch (Exception e8) {
            throw wrap("查询已安装游戏信息", "package:" + this.packageName, e8);
        }
    }

    private String canonical(String str) throws Exception {
        regular(str, "定位配置：检查原始路径");
        String trim = checked("解析配置真实路径", str, "readlink -f " + q(str), null, 8192).text().trim();
        if (!trim.startsWith("/") || trim.indexOf(10) >= 0 || trim.indexOf(13) >= 0) {
            throw problem("检查规范化后的配置路径", str, "readlink 未返回一个完整且唯一的绝对路径", null);
        }
        regular(trim, "定位配置：检查解析后路径");
        return trim;
    }

    private void regular(String str, String str2) throws Exception {
        checked(str2, str, "if [ -L " + q(str) + " ]; then echo '拒绝符号链接' >&2; exit 47; fi; if [ ! -e " + q(str) + " ]; then echo '文件不存在或在当前 root 命名空间不可见' >&2; exit 44; fi; if [ ! -f " + q(str) + " ]; then echo '路径不是普通文件' >&2; exit 45; fi; if [ ! -r " + q(str) + " ]; then echo '文件不可读' >&2; exit 46; fi", null, 8192);
    }

    private void noLink(String str, String str2) throws Exception {
        checked(str2, str, "if [ -L " + q(str) + " ]; then echo '拒绝符号链接' >&2; exit 47; fi", null, 8192);
    }

    private void absent(String str, String str2) throws Exception {
        noLink(str, str2);
        checked(str2, str, "if [ -e " + q(str) + " ]; then echo '临时目标已存在，拒绝覆盖' >&2; exit 48; fi", null, 8192);
    }

    private byte[] read(String str, int i, String str2) throws Exception {
        regular(str, str2 + "：检查文件");
        return checked(str2 + "：读取字节", str, "cat " + q(str), null, i).stdout;
    }

    private ResolvedMask resolveNativeMask(Paths paths, final String str, byte[] bArr, byte[] bArr2) throws Exception {
        regular(str, "本机库：检查 libUE4.so");
        try {
            final long parseLong = Long.parseLong(checked("本机库：读取 ELF 文件长度", str, "stat -c %s " + q(str), null, 8192).text().trim());
            validateLibraryLength(parseLong);
            try {
                return resolveMask(paths, new LibraryMaskResolver.Source() { // from class: io.github.deltaconfig.GameRepository.1
                    @Override // io.github.deltaconfig.core.LibraryMaskResolver.Source
                    public long length() {
                        return parseLong;
                    }

                    @Override // io.github.deltaconfig.core.LibraryMaskResolver.Source
                    public boolean efficientRandomAccess() {
                        return true;
                    }

                    @Override // io.github.deltaconfig.core.LibraryMaskResolver.Source
                    public byte[] read(long j, int i) throws Exception {
                        GameRepository.validateLibraryRange(parseLong, j, i);
                        if (i == 0) {
                            return new byte[0];
                        }
                        long j2 = j % 65536;
                        long j3 = i + j2;
                        long j4 = ((j3 + 65536) - 1) / 65536;
                        byte[] bArr3 = GameRepository.this.checked("本机库：按范围读取 ELF 兼容性材料", str, "dd if=" + GameRepository.q(str) + " bs=65536 skip=" + (j / 65536) + " count=" + j4, null, (int) (65536 * j4)).stdout;
                        if (j3 > bArr3.length) {
                            throw new IOException("游戏库范围读取被截断");
                        }
                        int i2 = (int) j2;
                        return Arrays.copyOfRange(bArr3, i2, i2 + i);
                    }
                }, str, bArr, bArr2);
            } catch (Exception e) {
                throw wrap("本机库：ELF ARM64 兼容识别与配置认证", str, e);
            }
        } catch (Exception e2) {
            throw wrap("本机库：验证库长度", str, e2);
        }
    }

    private ResolvedMask resolveApkMask(Paths paths, byte[] bArr, byte[] bArr2) throws Exception {
        String str = paths.info.sourceDir;
        try {
            ZipFile zipFile = new ZipFile(str);
            String str2 = str + "!lib/arm64-v8a/libUE4.so";
            try {
                try {
                    ZipEntry entry = zipFile.getEntry("lib/arm64-v8a/libUE4.so");
                    if (entry == null) {
                        throw problem("APK 库回退：定位 ARM64 库条目", str2, "APK 内没有 lib/arm64-v8a/libUE4.so；需要 ARM64 ELF 库才能检查当前配置兼容性", null);
                    }
                    ZipLibrarySource zipLibrarySource = new ZipLibrarySource(zipFile, entry);
                    try {
                        ResolvedMask resolveMask = resolveMask(paths, zipLibrarySource, str2 + ":" + entry.getCrc(), bArr, bArr2);
                        zipLibrarySource.close();
                        zipFile.close();
                        return resolveMask;
                    } finally {
                    }
                } finally {
                }
            } catch (Exception e) {
                throw wrap("APK 库回退：ELF ARM64 兼容识别与配置认证", str2, e);
            }
        } catch (Exception e2) {
            throw wrap("APK 库回退：打开游戏 APK", str, e2);
        }
    }

    private ResolvedMask resolveMask(Paths paths, LibraryMaskResolver.Source source, String str, byte[] bArr, byte[] bArr2) throws Exception {
        SharedPreferences sharedPreferences;
        CachedMaterial cachedMaterial;
        validateArm64Header(source.read(0L, 64), source.length());
        String hash = hash((paths.installation.cacheIdentity() + "|" + str + "|" + source.length()).getBytes(StandardCharsets.UTF_8));
        try {
            sharedPreferences = this.context.getSharedPreferences("verified-library-offsets", 0);
        } catch (Exception e) {
            sharedPreferences = null;
        }
        if (sharedPreferences != null) {
            try {
                long j = sharedPreferences.getLong(hash + ".a", -1L);
                long j2 = sharedPreferences.getLong(hash + ".b", -1L);
                if (j >= 0 && j2 >= 0) {
                    byte[] bArr3 = new byte[32];
                    System.arraycopy(source.read(j, 16), 0, bArr3, 0, 16);
                    System.arraycopy(source.read(j2, 16), 0, bArr3, 16, 16);
                    int i = sharedPreferences.getInt(hash + ".e", -1);
                    try {
                        cachedMaterial = authenticateCachedMask(bArr3, bArr, bArr2, i);
                        if (cachedMaterial == null) {
                            Arrays.fill(bArr3, (byte) 0);
                            if (cachedMaterial != null) {
                                Arrays.fill(cachedMaterial.mask, (byte) 0);
                            }
                        } else {
                            if (bArr2 != null) {
                                try {
                                    if (cachedMaterial.encoding != i) {
                                        try {
                                            sharedPreferences.edit().putInt(hash + ".e", cachedMaterial.encoding).apply();
                                        } catch (Exception e2) {
                                        }
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    Arrays.fill(bArr3, (byte) 0);
                                    if (cachedMaterial != null) {
                                        Arrays.fill(cachedMaterial.mask, (byte) 0);
                                    }
                                    throw th;
                                }
                            }
                            ResolvedMask resolvedMask = new ResolvedMask(cachedMaterial.mask, bArr2 == null ? "已验证偏移缓存；当前 Hash 认证通过" : "已验证偏移缓存；Hash 与 Engine HMAC 均通过");
                            Arrays.fill(bArr3, (byte) 0);
                            if (cachedMaterial != null) {
                                Arrays.fill(cachedMaterial.mask, (byte) 0);
                            }
                            return resolvedMask;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        cachedMaterial = null;
                    }
                }
            } catch (ConfigCrypto.AuthenticatedFormatException e3) {
                throw e3;
            } catch (ConfigCrypto.MaterialAuthenticationBudgetException e4) {
                throw e4;
            } catch (Exception e5) {
            }
        }
        LibraryMaskResolver.Result resolve = LibraryMaskResolver.resolve(source, bArr, bArr2);
        if (sharedPreferences != null && bArr2 != null) {
            try {
                sharedPreferences.edit().putLong(hash + ".a", resolve.firstOffset).putLong(hash + ".b", resolve.secondOffset).putInt(hash + ".e", resolve.materialEncoding).apply();
            } catch (Exception e6) {
            }
        }
        try {
            return new ResolvedMask(resolve.mask, resolve.description);
        } finally {
            Arrays.fill(resolve.mask, (byte) 0);
        }
    }

    private static final class CachedMaterial {
        final int encoding;
        final byte[] mask;

        CachedMaterial(byte[] bArr, int i) {
            this.mask = (byte[]) bArr.clone();
            this.encoding = i;
        }
    }

    private static boolean stopLibraryFallback(Throwable th) {
        int i = 0;
        while (th != null && i < 96) {
            if (!(th instanceof ConfigCrypto.AuthenticatedFormatException) && !(th instanceof ConfigCrypto.MaterialAuthenticationBudgetException)) {
                i++;
                th = th.getCause();
            } else {
                return true;
            }
        }
        return false;
    }

    private static CachedMaterial authenticateCachedMask(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) throws ConfigCrypto.MaterialAuthenticationBudgetException, ConfigCrypto.AuthenticatedFormatException {
        if (bArr == null || bArr.length != 32) {
            return null;
        }
        byte[] field = ConfigCrypto.getField(bArr2, "Hash");
        long length = field.length;
        int[] materialEncodingCandidates = ConfigCrypto.materialEncodingCandidates();
        long j = ConfigCrypto.MAX_MATERIAL_AUTH_BYTES;
        int i2 = -1;
        while (true) {
            try {
                if (i2 >= materialEncodingCandidates.length) {
                    return null;
                }
                int i3 = i2 < 0 ? i : materialEncodingCandidates[i2];
                if (i3 >= 0 && i3 <= 255) {
                    if (i2 >= 0 && i3 == i) {
                    }
                    if (length > j) {
                        throw new ConfigCrypto.MaterialAuthenticationBudgetException("认证材料编码探测超过 64 MiB 校验预算；已停止读取并保留原配置");
                    }
                    j -= length;
                    byte[] bArr4 = (byte[]) bArr.clone();
                    for (int i4 = 0; i4 < bArr4.length; i4++) {
                        bArr4[i4] = (byte) (bArr4[i4] ^ ((byte) i3));
                    }
                    try {
                        if (candidateMatches(bArr4, field, bArr3)) {
                            return new CachedMaterial(bArr4, i3);
                        }
                        Arrays.fill(bArr4, (byte) 0);
                    } finally {
                        Arrays.fill(bArr4, (byte) 0);
                    }
                }
                i2++;
            } finally {
                Arrays.fill(field, (byte) 0);
            }
        }
    }

    private static boolean candidateMatches(byte[] bArr, byte[] bArr2, byte[] bArr3) throws ConfigCrypto.AuthenticatedFormatException {
        byte[] bArr4 = null;
        try {
            try {
                bArr4 = ConfigCrypto.deriveKeyFromHash(bArr, bArr2);
                if (bArr3 != null) {
                    ConfigCrypto.decodeIni(bArr3, bArr4);
                }
                if (bArr4 == null) {
                    return true;
                }
                Arrays.fill(bArr4, (byte) 0);
                return true;
            } catch (ConfigCrypto.AuthenticatedFormatException e) {
                throw e;
            } catch (Exception e2) {
                if (bArr4 != null) {
                    Arrays.fill(bArr4, (byte) 0);
                }
                return false;
            }
        } catch (Throwable th) {
            if (bArr4 != null) {
                Arrays.fill(bArr4, (byte) 0);
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void validateLibraryLength(long j) throws IOException {
        if (j < 64 || j > LIBRARY_SIZE_LIMIT) {
            throw new IOException("游戏库长度不在 ELF 兼容检测范围内");
        }
    }

    private static void validateArm64Header(byte[] bArr, long j) throws IOException {
        validateLibraryLength(j);
        if (bArr == null || bArr.length != 64 || bArr[0] != Byte.MAX_VALUE || bArr[1] != 69 || bArr[2] != 76 || bArr[3] != 70 || bArr[4] != 2 || bArr[5] != 1 || bArr[6] != 1 || (bArr[16] & 255) != 3 || bArr[17] != 0 || (bArr[18] & 255) != 183 || bArr[19] != 0 || (bArr[52] & 255) != 64 || bArr[53] != 0) {
            throw new IOException("游戏库不是支持的 ELF64 小端 ARM64 共享库；已停止配置操作");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void validateLibraryRange(long j, long j2, int i) throws IOException {
        validateLibraryLength(j);
        if (j2 < 0 || i < 0 || i > 1048576 || j2 > j || i > j - j2) {
            throw new IOException("ELF 读取范围越界或单次读取超过 1 MiB");
        }
    }

    private static final class ResolvedMask {
        final String description;
        final byte[] mask;

        ResolvedMask(byte[] bArr, String str) {
            this.mask = (byte[]) bArr.clone();
            this.description = str;
        }
    }

    private static final class ZipLibrarySource implements LibraryMaskResolver.Source, AutoCloseable {
        private final ZipEntry entry;
        private long inflated;
        private long position;
        private InputStream stream;
        private final ZipFile zip;

        ZipLibrarySource(ZipFile zipFile, ZipEntry zipEntry) throws Exception {
            this.zip = zipFile;
            this.entry = zipEntry;
            GameRepository.validateLibraryLength(zipEntry.getSize());
            if (zipEntry.getMethod() != 0 && zipEntry.getMethod() != 8) {
                throw new IOException("APK 游戏库压缩方法不受支持");
            }
        }

        @Override // io.github.deltaconfig.core.LibraryMaskResolver.Source
        public long length() {
            return this.entry.getSize();
        }

        @Override // io.github.deltaconfig.core.LibraryMaskResolver.Source
        public byte[] read(long j, int i) throws Exception {
            GameRepository.validateLibraryRange(length(), j, i);
            if (this.stream == null || j < this.position) {
                if (this.stream != null) {
                    this.stream.close();
                }
                this.stream = this.zip.getInputStream(this.entry);
                this.position = 0L;
            }
            long j2 = j - this.position;
            if (this.entry.getMethod() == 8) {
                consumeBudget(j2);
            }
            while (this.position < j) {
                long skip = this.stream.skip(j - this.position);
                if (skip > 0) {
                    this.position += skip;
                } else {
                    if (this.stream.read() < 0) {
                        throw new IOException("APK 游戏库在跳转期间被截断");
                    }
                    this.position++;
                }
            }
            consumeBudget(i);
            byte[] exact = GameRepository.exact(this.stream, i);
            this.position += exact.length;
            return exact;
        }

        private void consumeBudget(long j) throws IOException {
            if (j < 0 || j > GameRepository.APK_INFLATE_LIMIT - this.inflated) {
                throw new IOException("APK 游戏库兼容识别读取量超过 1 GiB；已停止探测并保留原配置");
            }
            this.inflated += j;
        }

        @Override // java.lang.AutoCloseable
        public void close() throws IOException {
            if (this.stream != null) {
                this.stream.close();
            }
        }
    }

    private static void skip(InputStream inputStream, long j) throws IOException {
        while (j > 0) {
            long skip = inputStream.skip(j);
            if (skip > 0) {
                j -= skip;
            } else {
                if (inputStream.read() == -1) {
                    throw new IOException("游戏库被截断，尚需跳过 " + j + " 字节");
                }
                j--;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] exact(InputStream inputStream, int i) throws IOException {
        byte[] bArr = new byte[i];
        int i2 = 0;
        while (i2 < i) {
            int read = inputStream.read(bArr, i2, i - i2);
            if (read < 0) {
                throw new IOException("游戏库被截断：需要 " + i + " 字节，仅读到 " + i2 + " 字节");
            }
            i2 += read;
        }
        return bArr;
    }

    private static byte[] mask(byte[] bArr, byte[] bArr2) throws IOException {
        if (bArr.length != 16 || bArr2.length != 16) {
            throw new IOException("游戏库适配区段长度不正确：第一段 " + bArr.length + " 字节，第二段 " + bArr2.length + " 字节；应各为 16 字节");
        }
        byte[] bArr3 = new byte[32];
        for (int i = 0; i < 16; i++) {
            bArr3[i] = (byte) (bArr[i] ^ 226);
            bArr3[i + 16] = (byte) (bArr2[i] ^ 226);
        }
        return bArr3;
    }

    private void authenticate(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, Paths paths, String str) throws Exception {
        try {
            byte[] deriveKey = ConfigCrypto.deriveKey(bArr, bArr2);
            try {
                try {
                    ConfigCrypto.decodeIni(bArr3, deriveKey);
                    try {
                        validateGame(bArr4);
                        Arrays.fill(deriveKey, (byte) 0);
                    } catch (Exception e) {
                        throw wrap(str + "：验证 Game.ini 明文格式", paths.game, e);
                    }
                } catch (Exception e2) {
                    throw wrap(str + "：解析 Engine Token、解密并验证 HMAC", paths.engine, e2);
                }
            } catch (Throwable th) {
                Arrays.fill(deriveKey, (byte) 0);
                throw th;
            }
        } catch (Exception e3) {
            throw wrap(str + "：解析 UserGameplay Hash 并派生本机配置密钥", paths.gameplay, e3);
        }
    }

    private void validateCompatibility(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, Paths paths, String str) throws Exception {
        byte[] bArr5 = null;
        try {
            try {
                bArr5 = ConfigCrypto.deriveKey(bArr, bArr2);
                ConfigCompatibility.validate(ConfigCrypto.decodeIni(bArr3, bArr5), bArr4);
            } catch (Exception e) {
                throw wrap(str, paths.engine + "\n" + paths.game, e);
            }
        } finally {
            if (bArr5 != null) {
                Arrays.fill(bArr5, (byte) 0);
            }
        }
    }

    private void checkInstallation(Paths paths, String str) throws Exception {
        try {
            if (!paths.installation.equals(Installation.from(this.context.getPackageManager().getPackageInfo(this.packageName, 0), this.packageName))) {
                throw new IOException("游戏版本或安装身份在操作期间发生变化；已停止使用旧配置快照，请重新读取");
            }
        } catch (Exception e) {
            throw wrap(str, "package:" + this.packageName, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean sameGameVersion(String str, long j, String str2, long j2) {
        return str != null && str.equals(str2) && j == j2;
    }

    private void verifyGameplay(byte[] bArr, byte[] bArr2, String str, String str2) throws Exception {
        try {
            Arrays.fill(ConfigCrypto.deriveKey(bArr, bArr2), (byte) 0);
        } catch (Exception e) {
            throw wrap(str2, str, e);
        }
    }

    private static void validateGame(byte[] bArr) throws Exception {
        if (bArr.length == 0 || bArr.length > 2097152) {
            throw new IOException("Game.ini 大小无效");
        }
        String charBuffer = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bArr)).toString();
        if (charBuffer.startsWith("\ufeff")) {
            charBuffer = charBuffer.substring(1);
        }
        if (charBuffer.indexOf(0) >= 0 || !Pattern.compile("(?m)^\\s*\\[[^\\]\\r\\n]+\\]\\s*$").matcher(charBuffer).find()) {
            throw new IOException("Game.ini 不是支持的明文 INI 格式");
        }
    }

    private void requireRoot() throws Exception {
        this.rootAuthorized = false;
        String trim = checked("获取 root 权限并检查 UID", "su / 当前 Android 用户 " + this.userId, "id -u", null, 128).text().trim();
        if (!"0".equals(trim)) {
            throw problem("验证 root UID", "su", "su 返回的 UID 为 " + trim + "，需要 UID 0；请在 root 管理器中授权本应用", null);
        }
        this.rootAuthorized = true;
    }

    private void ensureStorage() throws Exception {
        String[] backupDirectories = backupDirectories(this.packageName, this.userId);
        for (String str : backupDirectories) {
            noLink(str, "检查备份目录不是符号链接");
        }
        checked("创建本机备份目录", this.base, "umask 077; mkdir -p " + q(this.base), null, 8192);
        for (String str2 : backupDirectories) {
            checked("设置备份目录所有者", str2, "chown 0:0 " + q(str2), null, 8192);
            checked("设置备份目录访问权限", str2, "chmod 700 " + q(str2), null, 8192);
        }
    }

    private String pointer(String str) throws Exception {
        String str2 = this.base + "/" + str;
        noLink(str2, "检查 " + str + " 备份索引路径");
        String trim = checked("读取 " + str + " 备份索引", str2, "if [ -e " + q(str2) + " ]; then if [ ! -f " + q(str2) + " ]; then echo '索引不是普通文件' >&2; exit 45; fi; cat " + q(str2) + "; fi", null, 256).text().trim();
        if (trim.isEmpty()) {
            return null;
        }
        if (!trim.matches("[0-9]{8}_[0-9]{6}_[0-9]{3}-[a-f0-9-]{36}")) {
            throw problem("解析 " + str + " 备份索引", str2, "索引内容不是受支持的事务 ID 格式", null);
        }
        return trim;
    }

    private void writePointer(String str, String str2) throws Exception {
        String str3 = this.base + "/" + str;
        String str4 = str3 + ".tmp";
        writeRoot(str4, (str2 + "\n").getBytes(StandardCharsets.US_ASCII), "保存 " + str + " 备份索引暂存内容");
        checked("提交 " + str + " 备份索引", str3, "mv -f " + q(str4) + " " + q(str3), null, 8192);
        checked("同步 " + str + " 备份索引", str3, "sync", null, 8192);
    }

    private void writeRoot(String str, byte[] bArr, String str2) throws Exception {
        String str3 = str + ".write-" + UUID.randomUUID().toString();
        noLink(str, str2 + "：检查目标路径");
        absent(str3, str2 + "：检查临时文件路径");
        checked(str2 + "：创建临时文件并写入字节", str3, "umask 077; cat > " + q(str3), bArr, 8192);
        checked(str2 + "：设置临时文件所有者", str3, "chown 0:0 " + q(str3), null, 8192);
        checked(str2 + "：设置临时文件权限", str3, "chmod 600 " + q(str3), null, 8192);
        verifyBytes(read(str3, Math.max(bArr.length, 128), str2 + "：回读临时文件"), hash(bArr), str3, str2 + "：校验临时文件字节");
        checked(str2 + "：重命名提交", str, "mv -f " + q(str3) + " " + q(str), null, 8192);
        checked(str2 + "：同步到存储", str, "sync", null, 8192);
    }

    private Metadata metadata(String str, String str2) throws Exception {
        regular(str, str2 + "：检查文件");
        String[] split = checked(str2 + "：读取 UID/GID/权限位", str, "stat -c '%u %g %a' " + q(str), null, 1024).text().trim().split("\\s+");
        if (split.length != 3) {
            throw problem(str2 + "：解析 UID/GID/权限位", str, "stat 未返回恰好三项 UID、GID 和八进制权限位", null);
        }
        String[] split2 = checked(str2 + "：读取 SELinux 标签", str, "ls -Zd " + q(str), null, 8192).text().trim().split("\\s+");
        if (split2.length == 0 || split2[0].isEmpty()) {
            throw problem(str2 + "：解析 SELinux 标签", str, "ls -Z 没有返回 SELinux 标签", null);
        }
        try {
            return new Metadata(split[0], split[1], split[2], split2[0]);
        } catch (Exception e) {
            throw wrap(str2 + "：验证 UID/GID/权限/SELinux 标签格式", str, e);
        }
    }

    private void setMetadata(String str, Metadata metadata, String str2) throws Exception {
        checked(str2 + "：chown 设置 UID/GID", str, "chown " + q(metadata.uid + ":" + metadata.gid) + " " + q(str), null, 8192);
        checked(str2 + "：chmod 设置权限位", str, "chmod " + q(metadata.mode) + " " + q(str), null, 8192);
        try {
            checked(str2 + "：chcon 设置 SELinux 标签", str, "chcon " + q(metadata.label) + " " + q(str), null, 8192);
            e = null;
        } catch (Exception e) {
            e = e;
            try {
                checked(str2 + "：chcon 失败后尝试 restorecon", str, "restorecon " + q(str), null, 8192);
            } catch (Exception e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
        try {
            verifyMetadata(str, metadata, str2 + "：回读验证权限和 SELinux 标签");
        } catch (Exception e3) {
            if (e != null) {
                e3.addSuppressed(e);
            }
            throw e3;
        }
    }

    private void verifyMetadata(String str, Metadata metadata, String str2) throws Exception {
        Metadata metadata2 = metadata(str, str2);
        if (!metadata2.equals(metadata)) {
            throw problem(str2, str, "权限或 SELinux 标签与原件不一致；预期 UID:GID=" + metadata.uid + ":" + metadata.gid + "，权限=" + metadata.mode + "，标签=" + metadata.label + "；实际 UID:GID=" + metadata2.uid + ":" + metadata2.gid + "，权限=" + metadata2.mode + "，标签=" + metadata2.label, null);
        }
    }

    private String deviceIdentity() throws Exception {
        try {
            String string = Settings.Secure.getString(this.context.getContentResolver(), "android_id");
            if (string == null || string.isEmpty()) {
                throw problem("检查本机备份标识", "Settings.Secure.ANDROID_ID", "系统未返回 Android ID，无法保证备份属于本设备", null);
            }
            return hash((string + "|" + this.userId).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw wrap("读取用于本机备份匹配的 Android ID", "Settings.Secure.ANDROID_ID", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RootShell.Result checked(String str, String str2, String str3, byte[] bArr, int i) throws Exception {
        String str4;
        try {
            RootShell.Result run = this.rootAccess == null ? RootShell.run(str3, bArr, i, TIMEOUT) : this.rootAccess.run(str3, bArr, i, TIMEOUT);
            if (run.exitCode != 0) {
                String sanitize = OperationFailure.sanitize(run.stderr);
                switch (run.exitCode) {
                    case 44:
                        str4 = "文件不存在或在当前 root 命名空间不可见";
                        break;
                    case 45:
                        str4 = "目标不是普通文件";
                        break;
                    case 46:
                        str4 = "文件不可读";
                        break;
                    case 47:
                        str4 = "目标是符号链接，为避免误改已拒绝操作";
                        break;
                    case 48:
                        str4 = "临时目标已存在，拒绝覆盖";
                        break;
                    case 49:
                        str4 = "当前 root 进程不再具有 UID 0，已停止操作";
                        break;
                    case 50:
                        str4 = "root 挂载命名空间与已验证上下文不一致，已停止操作；请重新读取配置";
                        break;
                    default:
                        str4 = "root 命令返回失败";
                        break;
                }
                throw problem(str, str2, str4 + (sanitize.isEmpty() ? "；root 未返回错误文本" : "；root stderr：" + sanitize), null).exitCode(run.exitCode);
            }
            return run;
        } catch (RootShell.TransportFailure e) {
            String sanitize2 = OperationFailure.sanitize(e.stderr);
            OperationFailure problem = problem(str + " / " + e.stage, str2, e.getMessage() + (sanitize2.isEmpty() ? "" : "；root stderr：" + sanitize2), e);
            if (e.exitCode != null) {
                problem.exitCode(e.exitCode.intValue());
                throw problem;
            }
            throw problem;
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
            throw problem(str + " / 等待 root 通道", str2, "当前操作线程被中断，需根据事务状态判断是否已写入", e2);
        } catch (Exception e3) {
            throw wrap(str + " / 执行 root 命令", str2, e3);
        }
    }

    private void begin(String str) {
        this.operation = str;
        this.writeStatus = "未写入游戏配置";
        this.rollbackStatus = "无需回滚";
        this.backupPath = null;
        this.originalRestored = false;
        this.rootAccess = null;
    }

    private OperationFailure problem(String str, String str2, String str3, Throwable th) {
        return new OperationFailure(this.operation, str, str2, str3, th);
    }

    private OperationFailure wrap(String str, String str2, Throwable th) {
        return OperationFailure.wrap(this.operation, str, str2, th);
    }

    private OperationFailure diagnosed(Throwable th) {
        return wrap("执行操作", null, th).state(this.writeStatus, this.rollbackStatus, this.backupPath);
    }

    private static final class CommitBoundary {
        private int phase;

        private CommitBoundary() {
        }

        void markerAttempted() {
            if (this.phase != 0) {
                throw new IllegalStateException("提交标记重命名只能尝试一次");
            }
            this.phase = 1;
        }

        void markerConfirmed() {
            if (this.phase != 1) {
                throw new IllegalStateException("尚未尝试提交标记重命名");
            }
            this.phase = 2;
        }

        void markerSynced() {
            if (this.phase != 2) {
                throw new IllegalStateException("提交标记尚未确认替换，不能确认同步");
            }
            this.phase = 3;
        }

        boolean canRollback() {
            return this.phase == 0;
        }

        boolean isMarkerConfirmed() {
            return this.phase >= 2;
        }

        boolean isMarkerSynced() {
            return this.phase == 3;
        }
    }

    private static String safeDiagnostic(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        String replaceAll = str.split("[\\r\\n]", 2)[0].replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("(?i)\\b(token|hash|password|passwd|secret|key)\\b(?:\\s*[:=]|\\s+).*", "$1=[REDACTED]").replaceAll("[A-Za-z0-9+/=_-]{64,}", "[REDACTED]");
        return replaceAll.length() > 300 ? replaceAll.substring(0, 300) : replaceAll;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String q(String str) {
        return RootShell.quote(str);
    }

    private static String temporary(String str, String str2, String str3) {
        return str + ".deltaconfig-" + str2 + str3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String hash(byte[] bArr) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bArr);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) {
            sb.append(String.format(Locale.US, "%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    private static final class Paths {
        final String engine;
        final String game;
        final String gameplay;
        final ApplicationInfo info;
        final Installation installation;

        Paths(ApplicationInfo applicationInfo, Installation installation, String str, String str2, String str3) {
            this.info = applicationInfo;
            this.installation = installation;
            this.engine = str;
            this.game = str2;
            this.gameplay = str3;
        }
    }

    private static final class Installation {
        final long lastUpdateTime;
        final String nativeLibraryDir;
        final String packageName;
        final String sourceDir;
        final int uid;
        final long versionCode;
        final String versionName;

        Installation(String str, String str2, long j, long j2, String str3, String str4, int i) throws IOException {
            GameTarget.lookup(str);
            if (str2 == null || str2.trim().isEmpty() || j < 0 || j2 < 0 || str3 == null || !str3.startsWith("/") || str4 == null || !str4.startsWith("/")) {
                throw new IOException("游戏的实际版本号或安装路径无效，无法绑定配置与备份");
            }
            this.packageName = str;
            this.versionName = str2;
            this.versionCode = j;
            this.lastUpdateTime = j2;
            this.sourceDir = str3;
            this.nativeLibraryDir = str4;
            this.uid = i;
        }

        static Installation from(PackageInfo packageInfo, String str) throws IOException {
            if (packageInfo != null && packageInfo.applicationInfo != null) {
                GameRepository.verifyInstalledTarget(str, packageInfo.packageName, packageInfo.applicationInfo.packageName);
                return new Installation(str, packageInfo.versionName, Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode, packageInfo.lastUpdateTime, packageInfo.applicationInfo.sourceDir, packageInfo.applicationInfo.nativeLibraryDir, packageInfo.applicationInfo.uid);
            }
            throw new IOException("缺少游戏安装信息");
        }

        String cacheIdentity() {
            return this.packageName + "|" + this.versionName + "|" + this.versionCode + "|" + this.lastUpdateTime + "|" + this.uid + "|" + this.sourceDir + "|" + this.nativeLibraryDir;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Installation)) {
                return false;
            }
            Installation installation = (Installation) obj;
            return this.packageName.equals(installation.packageName) && GameRepository.sameGameVersion(this.versionName, this.versionCode, installation.versionName, installation.versionCode) && this.lastUpdateTime == installation.lastUpdateTime && this.uid == installation.uid && this.sourceDir.equals(installation.sourceDir) && this.nativeLibraryDir.equals(installation.nativeLibraryDir);
        }

        public int hashCode() {
            return cacheIdentity().hashCode();
        }
    }

    private static final class Metadata {
        final String gid;
        final String label;
        final String mode;
        final String uid;

        Metadata(String str, String str2, String str3, String str4) throws IOException {
            if (!str.matches("[0-9]{1,10}") || !str2.matches("[0-9]{1,10}") || !str3.matches("[0-7]{3,4}") || !str4.matches("[a-zA-Z0-9_]+:[a-zA-Z0-9_]+:[a-zA-Z0-9_]+:[a-zA-Z0-9_:,.-]+")) {
                throw new IOException("文件权限或 SELinux 标签格式不受支持");
            }
            this.uid = str;
            this.gid = str2;
            this.mode = str3;
            this.label = str4;
        }

        JSONObject json() throws Exception {
            return new JSONObject().put("uid", this.uid).put("gid", this.gid).put("mode", this.mode).put("label", this.label);
        }

        static Metadata from(JSONObject jSONObject) throws Exception {
            return new Metadata(jSONObject.getString("uid"), jSONObject.getString("gid"), jSONObject.getString("mode"), jSONObject.getString("label"));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Metadata)) {
                return false;
            }
            Metadata metadata = (Metadata) obj;
            return this.uid.equals(metadata.uid) && this.gid.equals(metadata.gid) && this.mode.equals(metadata.mode) && this.label.equals(metadata.label);
        }

        public int hashCode() {
            return (this.uid + this.gid + this.mode + this.label).hashCode();
        }
    }
}
