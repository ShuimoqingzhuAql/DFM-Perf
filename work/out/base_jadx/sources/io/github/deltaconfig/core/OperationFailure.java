package io.github.deltaconfig.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class OperationFailure extends IOException {
    private static final int MAX_REPORT = 12000;
    private String backupPath;
    private final List<String[]> contexts;
    public final String operation;
    public final String path;
    private Integer processExitCode;
    public final String reason;
    private String rollbackStatus;
    public final String stage;
    private String suggestion;
    private String writeStatus;
    private static final Pattern ASSIGNMENT = Pattern.compile("(?i)((?:token|hash|aes[_ -]?key|key|fixed[_ -]?mask|mask|device[_ -]?id|android[_ -]?id|serial(?:no|number)?|uin|account|phone|mobile|oaid|imei|imsi|meid|password|passwd|pwd|passphrase|手机号|电话号码|账号|密码|密钥|固定掩码|设备\\s*(?:ID|标识|序列号)|认证摘要)[\"']?\\s*[:=：]\\s*)[^\\r\\n]*");
    private static final Pattern PATH = Pattern.compile("(?<![A-Za-z0-9+])(?:[A-Za-z]:[\\\\/]|/(?:data|storage|sdcard|mnt|system|vendor|apex|proc|dev|cache|product|odm|metadata|tmp|apps)/)[^\\s\\\"'<>|,;]*");
    private static final Pattern COMMAND_TEXT = Pattern.compile("(?im)(?:^[ \\t]*(?:shell[ \\t]+command|command|cmd|完整命令|执行命令)[ \\t]*[:=][^\\r\\n]*|\\b(?:su|sh|bash)[ \\t]+-c[ \\t]+[^\\r\\n]*)");
    private static final Pattern KNOWN_DIAGNOSTIC = Pattern.compile("(?i)Permission denied|No such file or directory|Read-only file system|No space left on device|Operation not permitted|SELinux|avc:[ \\t]*denied|root[ \\t]*授权被拒绝");
    private static final Pattern LONG_VALUE = Pattern.compile("(?<![A-Za-z0-9+/])[A-Za-z0-9+/]{40,}={0,2}(?![A-Za-z0-9+/=])");

    public OperationFailure(String str, String str2, String str3, String str4, Throwable th) {
        super(sanitize((str4 == null || str4.trim().isEmpty()) ? "未提供详细原因" : str4), th);
        this.contexts = new ArrayList();
        this.operation = safe(str, "未标明操作");
        this.stage = safe(str2, "未标明阶段");
        this.path = safe(str3, "未定位到具体文件");
        this.reason = getMessage();
    }

    public static OperationFailure wrap(String str, String str2, String str3, Throwable th) {
        if (th instanceof OperationFailure) {
            OperationFailure operationFailure = (OperationFailure) th;
            operationFailure.contexts.add(new String[]{safe(str, "未标明操作"), safe(str2, "未标明阶段"), safe(str3, "未定位到具体文件")});
            return operationFailure;
        }
        return new OperationFailure(str, str2, str3, message(th), th);
    }

    public OperationFailure state(String str, String str2, String str3) {
        if (str != null && !str.trim().isEmpty()) {
            this.writeStatus = sanitize(str);
        }
        if (str2 != null && !str2.trim().isEmpty()) {
            this.rollbackStatus = sanitize(str2);
        }
        if (str3 != null && !str3.trim().isEmpty()) {
            this.backupPath = sanitize(str3);
        }
        return this;
    }

    public OperationFailure exitCode(int i) {
        this.processExitCode = Integer.valueOf(i);
        return this;
    }

    public OperationFailure hint(String str) {
        this.suggestion = sanitize(str);
        return this;
    }

    public String getOperation() {
        return this.operation;
    }

    public String getStage() {
        return this.stage;
    }

    public String getPath() {
        return this.path;
    }

    public static String sanitize(String str) {
        if (str == null) {
            return "";
        }
        Matcher matcher = COMMAND_TEXT.matcher(str.replace((char) 0, ' ').replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " "));
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            StringBuilder sb = new StringBuilder("[命令已省略]");
            Matcher matcher2 = KNOWN_DIAGNOSTIC.matcher(matcher.group());
            while (matcher2.find()) {
                sb.append("；").append(matcher2.group());
            }
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(sb.toString()));
        }
        matcher.appendTail(stringBuffer);
        String replaceAll = ASSIGNMENT.matcher(stringBuffer.toString()).replaceAll("$1[已隐藏]");
        ArrayList arrayList = new ArrayList();
        Matcher matcher3 = PATH.matcher(replaceAll);
        StringBuffer stringBuffer2 = new StringBuffer();
        while (matcher3.find()) {
            arrayList.add(matcher3.group());
            matcher3.appendReplacement(stringBuffer2, Matcher.quoteReplacement("【路径" + (arrayList.size() - 1) + "】"));
        }
        matcher3.appendTail(stringBuffer2);
        String replaceAll2 = LONG_VALUE.matcher(stringBuffer2.toString()).replaceAll("[长认证值已隐藏]").replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])", "[手机号已隐藏]").replaceAll("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", "[标识已隐藏]");
        for (int i = 0; i < arrayList.size(); i++) {
            replaceAll2 = replaceAll2.replace("【路径" + i + "】", (CharSequence) arrayList.get(i));
        }
        return replaceAll2;
    }

    private static String safe(String str, String str2) {
        String sanitize = sanitize(str);
        return sanitize.trim().isEmpty() ? str2 : sanitize;
    }

    private static String message(Throwable th) {
        if (th == null) {
            return "没有异常详情，无法确认失败原因";
        }
        String message = th.getMessage();
        return (message == null || message.trim().isEmpty()) ? th.getClass().getSimpleName() : message;
    }

    private static final class Entry {
        final Throwable error;
        final String relation;

        Entry(Throwable th, String str) {
            this.error = th;
            this.relation = str;
        }
    }

    private static void collect(Throwable th, String str, List<Entry> list, Set<Throwable> set) {
        if (th == null || !set.add(th) || set.size() > 96) {
            return;
        }
        collect(th.getCause(), "原因", list, set);
        list.add(new Entry(th, str));
        for (Throwable th2 : th.getSuppressed()) {
            collect(th2, "其他尝试或回滚错误", list, set);
        }
    }

    private static OperationFailure firstFailure(Throwable th) {
        Set newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        while (th != null && newSetFromMap.add(th)) {
            if (th instanceof OperationFailure) {
                return (OperationFailure) th;
            }
            th = th.getCause();
        }
        return null;
    }

    private static OperationFailure deepestFailure(Throwable th) {
        Set newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        OperationFailure operationFailure = null;
        while (th != null && newSetFromMap.add(th)) {
            if (th instanceof OperationFailure) {
                operationFailure = (OperationFailure) th;
            }
            th = th.getCause();
        }
        return operationFailure;
    }

    private static boolean absolute(String str) {
        return str.startsWith("/") || str.matches("^[A-Za-z]:[\\\\/].*");
    }

    private static String bestPath(OperationFailure operationFailure, List<Entry> list) {
        if (operationFailure == null) {
            return "未定位到具体文件";
        }
        if (absolute(operationFailure.path)) {
            return operationFailure.path;
        }
        String str = operationFailure.path;
        for (Entry entry : list) {
            if (entry.error instanceof OperationFailure) {
                OperationFailure operationFailure2 = (OperationFailure) entry.error;
                if (absolute(operationFailure2.path) && operationFailure2.path.replace('\\', '/').endsWith("/" + str)) {
                    return operationFailure2.path;
                }
                for (String[] strArr : operationFailure2.contexts) {
                    if (absolute(strArr[2]) && strArr[2].replace('\\', '/').endsWith("/" + str)) {
                        return strArr[2];
                    }
                }
            }
        }
        return operationFailure.path;
    }

    private static String clip(String str, int i) {
        if (str.length() <= i) {
            return str;
        }
        if (i < 60) {
            return str.substring(0, Math.max(0, i));
        }
        int i2 = i - 30;
        int i3 = (i2 * 2) / 3;
        return str.substring(0, i3) + "\n[较长内容已缩略，保留首尾]\n" + str.substring(str.length() - (i2 - i3));
    }

    private static String fieldState(Throwable th, int i) {
        String str;
        Set newSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        while (th != null && newSetFromMap.add(th)) {
            if (th instanceof OperationFailure) {
                OperationFailure operationFailure = (OperationFailure) th;
                if (i == 0) {
                    str = operationFailure.writeStatus;
                } else {
                    str = i == 1 ? operationFailure.rollbackStatus : operationFailure.backupPath;
                }
                if (str != null && !str.isEmpty()) {
                    return str;
                }
            }
            th = th.getCause();
        }
        return i == 0 ? "未知；尚未确认是否写入" : i == 1 ? "未知；尚未确认是否执行或完成回滚" : "未记录";
    }

    private static Integer exitCode(Throwable th) {
        if (th instanceof OperationFailure) {
            OperationFailure operationFailure = (OperationFailure) th;
            if (operationFailure.processExitCode != null) {
                return operationFailure.processExitCode;
            }
        }
        try {
            Object invoke = th.getClass().getMethod("getExitCode", new Class[0]).invoke(th, new Object[0]);
            if (invoke instanceof Number) {
                return Integer.valueOf(((Number) invoke).intValue());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String advice(Throwable th, List<Entry> list) {
        OperationFailure firstFailure = firstFailure(th);
        if (firstFailure != null && firstFailure.suggestion != null && !firstFailure.suggestion.isEmpty()) {
            return firstFailure.suggestion;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (Entry entry : list) {
            sb.append(entry.error.getClass().getSimpleName()).append(' ').append(message(entry.error)).append(' ');
            if (entry.error instanceof OperationFailure) {
                sb2.append(((OperationFailure) entry.error).stage).append(' ');
            }
        }
        String lowerCase = sb.toString().toLowerCase(Locale.ROOT);
        String str = lowerCase + sb2.toString().toLowerCase(Locale.ROOT);
        if (lowerCase.contains("no space left") || lowerCase.contains("enospc") || lowerCase.contains("空间不足")) {
            return "释放手机内部存储及备份目录空间后重试；先核对上方写入与回滚状态。";
        }
        if (lowerCase.contains("read-only file system") || lowerCase.contains("erofs") || lowerCase.contains("只读")) {
            return "目标文件系统为只读；确认配置位于可写数据目录，不要强行改写系统分区。";
        }
        if (lowerCase.contains("selinux") || lowerCase.contains("avc: denied")) {
            return "检查对应文件的 SELinux 拒绝日志、权限和路径；不要通过关闭 SELinux 来掩盖问题。";
        }
        if ((str.contains("root 授权") || str.contains("root permission") || str.contains("superuser") || str.contains("超级用户") || str.contains("请求 root") || str.contains("获取 root") || str.contains("su: denied")) && (lowerCase.contains("denied") || lowerCase.contains("拒绝") || lowerCase.contains("未授权") || lowerCase.contains("未获得"))) {
            return "在 root 管理器中允许本应用获得超级用户权限，再重新读取；检查是否误选了拒绝。";
        }
        if (lowerCase.contains("permission denied") || lowerCase.contains("权限")) {
            return "检查本应用 root 授权、对应文件权限与访问路径；如有 SELinux 拒绝日志一并核对。";
        }
        if (lowerCase.contains("no such file") || lowerCase.contains("enoent") || lowerCase.contains("不存在") || lowerCase.contains("缺少文件") || lowerCase.contains("找不到文件") || lowerCase.contains("filenotfound")) {
            return "确认游戏已安装、至少启动一次并正常退出；核对当前 Android 用户及目标文件完整路径。";
        }
        if (lowerCase.contains("漂移") || lowerCase.contains("发生变化") || lowerCase.contains("已变化") || lowerCase.contains("快照") || lowerCase.contains("changed since")) {
            return "游戏或其他程序已改变文件；关闭游戏，重新读取当前配置后再操作，不要覆盖旧快照。";
        }
        if (lowerCase.contains("兼容性") || lowerCase.contains("未识别") || lowerCase.contains("扫描上限") || lowerCase.contains("扫描预算")) {
            return "先在游戏中保存画质设置并正常退出，再重新检测；仍失败时，保留游戏版本和本机错误日志以适配当前格式。";
        }
        if (lowerCase.contains("hmac") || lowerCase.contains("认证失败") || lowerCase.contains("认证不匹配")) {
            return "确认 Engine.ini、UserGameplay.ini 和游戏库来自本机同一安装版本；重新读取后重试，勿混用其他设备配置。";
        }
        if (lowerCase.contains("libue4") || lowerCase.contains("库不匹配") || lowerCase.contains("认证材料") || lowerCase.contains("elf") || lowerCase.contains("不支持的游戏版本")) {
            return "确认安装的是 ARM64 游戏且配置与本机安装配套；库布局或认证规则未识别时，保留游戏版本和错误日志进行适配。";
        }
        if (lowerCase.contains("utf") || lowerCase.contains("malformedinput") || lowerCase.contains("unmappable") || lowerCase.contains("格式") || lowerCase.contains("不受支持") || lowerCase.contains("base64") || lowerCase.contains("密文块") || lowerCase.contains("字段重复")) {
            return "配置编码或格式与当前适配不一致；保留原文件及此报告，勿反复强制写入。";
        }
        return "保留此报告；核对目标文件、root 授权及上方写入/回滚状态，再重新读取配置。";
    }

    public static String userReport(Throwable th) {
        ArrayList arrayList = new ArrayList();
        collect(th, "失败上下文", arrayList, Collections.newSetFromMap(new IdentityHashMap()));
        OperationFailure deepestFailure = deepestFailure(th);
        String message = arrayList.isEmpty() ? message(th) : message(((Entry) arrayList.get(0)).error);
        StringBuilder sb = new StringBuilder("配置操作失败\n");
        sb.append("操作：").append(clip(uiText(deepestFailure == null ? "未标明操作" : deepestFailure.operation, arrayList), 180)).append('\n');
        sb.append("阶段：").append(clip(uiText(deepestFailure == null ? "未标明阶段" : deepestFailure.stage, arrayList), 360)).append('\n');
        sb.append("原因：").append(clip(uiText(message, arrayList), 700)).append('\n');
        Iterator it = arrayList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Integer exitCode = exitCode(((Entry) it.next()).error);
            if (exitCode != null) {
                sb.append("退出码：").append(exitCode).append('\n');
                break;
            }
        }
        sb.append("写入状态：").append(clip(uiText(fieldState(th, 0), arrayList), 280)).append('\n');
        sb.append("回滚状态：").append(clip(uiText(fieldState(th, 1), arrayList), 280)).append('\n');
        sb.append("建议：").append(clip(uiText(advice(th, arrayList), arrayList), 400)).append('\n');
        sb.append("详细诊断已保存至本机错误日志。");
        return clip(sb.toString(), 2400);
    }

    private static String uiText(String str, List<Entry> list) {
        String sanitize = sanitize(str);
        for (Entry entry : list) {
            if (entry.error instanceof OperationFailure) {
                OperationFailure operationFailure = (OperationFailure) entry.error;
                if (!operationFailure.path.isEmpty() && !operationFailure.path.equals("未定位到具体文件")) {
                    sanitize = sanitize.replace(operationFailure.path, "相关配置");
                }
                if (operationFailure.backupPath != null && !operationFailure.backupPath.isEmpty()) {
                    sanitize = sanitize.replace(operationFailure.backupPath, "本机备份");
                }
                for (String[] strArr : operationFailure.contexts) {
                    if (strArr[2] != null && !strArr[2].isEmpty() && !strArr[2].equals("未定位到具体文件")) {
                        sanitize = sanitize.replace(strArr[2], "相关配置");
                    }
                }
            }
        }
        return PATH.matcher(sanitize).replaceAll("相关位置").replaceAll("(?i)(?<![A-Za-z0-9_])(?:[A-Za-z]:[\\\\/]|/)[^\\s\\\"'<>|,;，；]*", "相关位置").replaceAll("(?im)^.*(?:目标文件|备份位置)[：:][^\\r\\n]*(?:\\r?\\n|$)", "").replaceAll("(?i)UserGameplay\\.ini", "认证配置").replaceAll("(?i)Engine\\.ini", "主配置").replaceAll("(?i)Game\\.ini", "渲染配置").replaceAll("(?i)libUE4\\.so", "游戏库").replaceAll("(?i)(?<![A-Za-z0-9_])[A-Za-z0-9_.-]+\\.(?:ini|so|apk|json)(?![A-Za-z0-9_])", "相关配置").trim();
    }

    public static String format(Throwable th) {
        String str;
        boolean z;
        char c;
        char c2;
        ArrayList<Entry> arrayList = new ArrayList();
        collect(th, "失败上下文", arrayList, Collections.newSetFromMap(new IdentityHashMap()));
        OperationFailure deepestFailure = deepestFailure(th);
        StringBuilder sb = new StringBuilder("配置操作失败报告\n");
        String str2 = "操作：";
        char c3 = '\n';
        sb.append("操作：").append(deepestFailure == null ? "未标明操作" : deepestFailure.operation).append('\n');
        sb.append("阶段：").append(deepestFailure == null ? "未标明阶段" : deepestFailure.stage).append('\n');
        sb.append("目标文件：").append(clip(bestPath(deepestFailure, arrayList), 1800)).append('\n');
        char c4 = 0;
        sb.append("原因：").append(clip(sanitize(arrayList.isEmpty() ? message(th) : message(((Entry) arrayList.get(0)).error)), 2500)).append('\n');
        Iterator it = arrayList.iterator();
        Integer num = null;
        while (it.hasNext() && (num = exitCode(((Entry) it.next()).error)) == null) {
        }
        if (num != null) {
            sb.append("退出码：").append(num).append('\n');
        }
        sb.append("写入状态：").append(clip(fieldState(th, 0), 600)).append('\n');
        boolean z2 = true;
        sb.append("回滚状态：").append(clip(fieldState(th, 1), 600)).append('\n');
        char c5 = 2;
        sb.append("备份位置：").append(clip(fieldState(th, 2), 1000)).append('\n');
        sb.append("建议：").append(clip(advice(th, arrayList), 800)).append('\n');
        sb.append("\n详细原因链（含其他尝试或回滚错误）：\n");
        int max = Math.max(120, Math.min(8000, Math.max(0, (12000 - sb.length()) - 100) / Math.max(1, arrayList.size())));
        int i = 0;
        for (Entry entry : arrayList) {
            StringBuilder sb2 = new StringBuilder();
            i++;
            sb2.append('[').append(i).append("] ").append(entry.relation).append(" · ").append(entry.error.getClass().getSimpleName()).append(c3);
            if (!(entry.error instanceof OperationFailure)) {
                str = str2;
                z = z2;
                c = c5;
            } else {
                OperationFailure operationFailure = (OperationFailure) entry.error;
                sb2.append(str2).append(operationFailure.operation).append("；阶段：").append(operationFailure.stage).append("\n目标文件：").append(operationFailure.path).append(c3);
                for (String[] strArr : operationFailure.contexts) {
                    sb2.append("外层上下文：").append(strArr[c4]).append(" / ").append(strArr[1]).append("\n目标文件：").append(strArr[2]).append('\n');
                    str2 = str2;
                    c4 = 0;
                }
                str = str2;
                c = 2;
                z = true;
                if (operationFailure.writeStatus != null || operationFailure.rollbackStatus != null) {
                    sb2.append("该失败状态：写入=").append(operationFailure.writeStatus == null ? "未知" : operationFailure.writeStatus).append("；回滚=").append(operationFailure.rollbackStatus != null ? operationFailure.rollbackStatus : "未知").append('\n');
                }
            }
            Integer exitCode = exitCode(entry.error);
            if (exitCode != null) {
                c2 = '\n';
                sb2.append("退出码：").append(exitCode).append('\n');
            } else {
                c2 = '\n';
            }
            sb2.append("原因：").append(sanitize(message(entry.error))).append(c2);
            sb.append(clip(sb2.toString(), Math.min(max, Math.max(0, (12000 - sb.length()) - 60)))).append('\n');
            if (12000 - sb.length() < 120) {
                break;
            }
            str2 = str;
            z2 = z;
            c3 = '\n';
            c5 = c;
            c4 = 0;
        }
        if (i < arrayList.size()) {
            sb.append("[报告过长，其余 ").append(arrayList.size() - i).append(" 项已省略]\n");
        } else if (arrayList.size() >= 96) {
            sb.append("[异常层级过多，部分深层内容已省略]\n");
        }
        return clip(sanitize(sb.toString()), MAX_REPORT);
    }
}
