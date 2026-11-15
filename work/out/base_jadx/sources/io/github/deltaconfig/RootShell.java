package io.github.deltaconfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public final class RootShell {
    private RootShell() {
    }

    public static final class Access {
        private final String initNamespace;
        private final String namespace;
        private final Route route;

        private Access(Route route, Probe probe) {
            this.route = route;
            this.namespace = probe.namespace;
            this.initNamespace = probe.initNamespace;
        }

        public String description() {
            return this.route.name() + "; self=" + this.namespace + "; init=" + this.initNamespace;
        }

        public Result run(String str, byte[] bArr, int i, long j) throws IOException, InterruptedException {
            return RootShell.execute(this.route.arguments(RootShell.guard(this.namespace) + str), bArr, i, j);
        }
    }

    public static final class SelectionFailure extends IOException {
        public final String diagnostic;
        public final Integer exitCode;

        private SelectionFailure(String str, Integer num, Throwable th) {
            super("所有 root 访问路径均未能读取目标普通文件；" + str, th);
            this.diagnostic = str;
            this.exitCode = num;
        }

        public String getDiagnostic() {
            return this.diagnostic;
        }

        public Integer getExitCode() {
            return this.exitCode;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static Access selectForFile(String str, long j) throws IOException, InterruptedException {
        String str2;
        if (str == null || !str.startsWith("/") || str.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Invalid root probe path");
        }
        ArrayList arrayList = new ArrayList();
        ArrayList<Route> arrayList2 = new ArrayList();
        Throwable th = null;
        Route route = new Route(null, null);
        arrayList2.add(route);
        Access probe = probe(route, str, j, arrayList);
        if (probe != null) {
            return probe;
        }
        try {
            Result execute = execute(new String[]{"su", "--help"}, null, 65536, Math.min(j, 15000L));
            String advertisedMountOption = advertisedMountOption(execute.text() + "\n" + execute.stderr);
            if (advertisedMountOption != null) {
                Route route2 = new Route(advertisedMountOption, null);
                arrayList2.add(route2);
                Access probe2 = probe(route2, str, j, arrayList);
                if (probe2 != null) {
                    return probe2;
                }
            } else {
                arrayList.add("su-help: 未声明 --mount-master/-M，未盲目传入全局挂载参数");
            }
        } catch (IOException e) {
            arrayList.add("su-help: " + concise(e));
        }
        for (Route route3 : arrayList2) {
            try {
                Result execute2 = execute(route3.arguments("if command -v nsenter >/dev/null 2>&1 && nsenter --help >/dev/null 2>&1; then printf 'NSENTER\\n'; fi; if [ -x /system/bin/toybox ] && /system/bin/toybox nsenter --help >/dev/null 2>&1; then printf 'TOYBOX\\n'; fi"), null, 4096, j);
                if (execute2.exitCode != 0) {
                    arrayList.add(route3.name() + " nsenter 探测 exit=" + execute2.exitCode + ": " + safe(execute2.stderr));
                } else {
                    boolean z = false;
                    for (String str3 : execute2.text().trim().split("\\r?\\n")) {
                        if ("NSENTER".equals(str3)) {
                            str2 = "nsenter";
                        } else if ("TOYBOX".equals(str3)) {
                            str2 = "/system/bin/toybox nsenter";
                        } else {
                            continue;
                        }
                        Access probe3 = probe(new Route(route3.mountOption, str2), str, j, arrayList);
                        if (probe3 != null) {
                            return probe3;
                        }
                        z = true;
                    }
                    if (!z) {
                        arrayList.add(route3.name() + ": 系统未提供可用 nsenter");
                    }
                }
            } catch (IOException e2) {
                arrayList.add(route3.name() + " nsenter 探测: " + concise(e2));
            }
        }
        throw new SelectionFailure("目标=" + str + "\n" + String.join("\n", arrayList), 51, th);
    }

    private static Access probe(Route route, String str, long j, List<String> list) throws InterruptedException {
        try {
            Result execute = execute(route.arguments(probeCommand(str)), null, 8192, j);
            try {
                Probe parseProbe = parseProbe(execute.text(), route.namespaceTool != null);
                String str2 = route.name() + "; self=" + parseProbe.namespace + "; init=" + parseProbe.initNamespace + "; path=" + str + "; exit=" + execute.exitCode + "; file=" + parseProbe.fileState;
                if (execute.exitCode == 0 && "OK".equals(parseProbe.fileState)) {
                    Access access = new Access(route, parseProbe);
                    Result run = access.run("true", null, 1024, j);
                    if (run.exitCode != 0) {
                        list.add(str2 + "; 重复验证失败 exit=" + run.exitCode + "; " + safe(run.stderr));
                        return null;
                    }
                    return access;
                }
                list.add(str2 + "; " + safe(execute.stderr));
                return null;
            } catch (IOException e) {
                list.add(route.name() + "; path=" + str + "; exit=" + execute.exitCode + "; probe=" + safe(execute.text()) + "; " + concise(e) + "; " + safe(execute.stderr));
                return null;
            }
        } catch (IOException e2) {
            list.add(route.name() + "; path=" + str + "; " + concise(e2));
            return null;
        }
    }

    private static String advertisedMountOption(String str) {
        if (str.matches("(?s).*(?<![A-Za-z0-9_-])--mount-master(?![A-Za-z0-9_-]).*")) {
            return "--mount-master";
        }
        if (str.matches("(?s).*(?<![A-Za-z0-9_-])-M(?=[\\s,]|$).*")) {
            return "-M";
        }
        return null;
    }

    private static String probeCommand(String str) {
        String quote = quote(str);
        return "id -u; readlink /proc/$$/ns/mnt; readlink /proc/1/ns/mnt || printf 'unavailable\\n'; if [ -L " + quote + " ]; then printf 'SYMLINK\\n'; exit 47; elif [ ! -e " + quote + " ]; then printf 'MISSING\\n'; exit 44; elif [ ! -f " + quote + " ]; then printf 'NOT_REGULAR\\n'; exit 45; elif [ ! -r " + quote + " ]; then printf 'UNREADABLE\\n'; exit 46; elif ! ( : < " + quote + " ) 2>/dev/null; then printf 'UNREADABLE\\n'; exit 46; else printf 'OK\\n'; fi";
    }

    private static Probe parseProbe(String str, boolean z) throws IOException {
        String[] split = str.replace("\r\n", "\n").trim().split("\n", -1);
        if (split.length != 4 || !"0".equals(split[0]) || !split[1].matches("mnt:\\[[0-9]+\\]") || ((!split[2].matches("mnt:\\[[0-9]+\\]") && !"unavailable".equals(split[2])) || !split[3].matches("OK|MISSING|NOT_REGULAR|UNREADABLE|SYMLINK"))) {
            throw new IOException("root UID/命名空间/文件状态探测格式无效（须 UID 0）");
        }
        if (z && !split[1].equals(split[2])) {
            throw new IOException("nsenter 未进入 PID 1 挂载命名空间；self=" + split[1] + "; init=" + split[2]);
        }
        return new Probe(split[1], split[2], split[3]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String guard(String str) {
        return "[ \"$(id -u)\" = 0 ] || { echo 'root UID 已变化，需要 UID 0' >&2; exit 49; }; [ \"$(readlink /proc/$$/ns/mnt)\" = " + quote(str) + " ] || { echo 'root 挂载命名空间已变化，已停止当前操作；请重新读取配置' >&2; exit 50; }; ";
    }

    private static final class Probe {
        final String fileState;
        final String initNamespace;
        final String namespace;

        Probe(String str, String str2, String str3) {
            this.namespace = str;
            this.initNamespace = str2;
            this.fileState = str3;
        }
    }

    private static final class Route {
        final String mountOption;
        final String namespaceTool;

        Route(String str, String str2) {
            this.mountOption = str;
            this.namespaceTool = str2;
        }

        String name() {
            return (this.mountOption == null ? "su-default" : "su " + this.mountOption) + (this.namespaceTool == null ? "" : " + " + this.namespaceTool + " PID1-mount");
        }

        String[] arguments(String str) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("su");
            if (this.mountOption != null) {
                arrayList.add(this.mountOption);
            }
            arrayList.add("-c");
            if (this.namespaceTool != null) {
                str = this.namespaceTool + " --mount=/proc/1/ns/mnt -- /system/bin/sh -c " + RootShell.quote(str);
            }
            arrayList.add(str);
            return (String[]) arrayList.toArray(new String[0]);
        }
    }

    private static String concise(IOException iOException) {
        return safe(iOException.getClass().getSimpleName() + ": " + iOException.getMessage());
    }

    private static String safe(String str) {
        if (str == null) {
            return "";
        }
        String replaceAll = str.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("(?i)\\b(token|hash|password|passwd|secret|key)\\b(?:\\s*[:=]|\\s+).*", "$1=[REDACTED]").replaceAll("[A-Za-z0-9+/=_-]{64,}", "[REDACTED]");
        return replaceAll.length() > 500 ? replaceAll.substring(0, 500) : replaceAll;
    }

    public static final class Result {
        public final int exitCode;
        public final String stderr;
        public final byte[] stdout;

        private Result(int i, byte[] bArr, byte[] bArr2) {
            this.exitCode = i;
            this.stdout = bArr;
            this.stderr = new String(bArr2, StandardCharsets.UTF_8);
        }

        public String text() {
            return new String(this.stdout, StandardCharsets.UTF_8);
        }
    }

    public static final class TransportFailure extends IOException {
        public final Integer exitCode;
        public final String stage;
        public final String stderr;

        private TransportFailure(String str, String str2, Integer num, String str3, Throwable th) {
            super(str2, th);
            this.stage = str;
            this.exitCode = num;
            this.stderr = str3 == null ? "" : str3;
        }

        public Integer getExitCode() {
            return this.exitCode;
        }

        public String getDiagnostic() {
            return this.stderr;
        }
    }

    public static String quote(String str) {
        if (str == null || str.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Invalid shell argument");
        }
        return "'" + str.replace("'", "'\\''") + "'";
    }

    public static Result run(String str, byte[] bArr, int i, long j) throws IOException, InterruptedException {
        return execute(new String[]{"su", "-c", str}, bArr, i, j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Result execute(String[] strArr, final byte[] bArr, int i, long j) throws IOException, InterruptedException {
        if (i < 0 || i > 67108864 || j <= 0) {
            throw new IllegalArgumentException("Invalid process limits");
        }
        try {
            final Process start = new ProcessBuilder(strArr).start();
            Capture capture = new Capture(start.getInputStream(), i);
            Capture capture2 = new Capture(start.getErrorStream(), 32768);
            Thread daemon = daemon("root-stdout", capture);
            Thread daemon2 = daemon("root-stderr", capture2);
            final IOException[] iOExceptionArr = new IOException[1];
            Thread daemon3 = daemon("root-stdin", new Runnable() { // from class: io.github.deltaconfig.RootShell$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RootShell.lambda$execute$0(start, bArr, iOExceptionArr);
                }
            });
            try {
                if (!start.waitFor(j, TimeUnit.MILLISECONDS)) {
                    start.destroy();
                    if (!start.waitFor(500L, TimeUnit.MILLISECONDS)) {
                        start.destroyForcibly();
                    }
                    daemon2.join(500L);
                    TransportFailure transport = transport("等待 su 命令结束", "root 命令超过 " + j + " 毫秒仍未结束；请检查 root 授权窗口和存储状态", start, capture2, null);
                    addFailures(transport, capture, capture2, iOExceptionArr[0]);
                    throw transport;
                }
                daemon.join(2000L);
                daemon2.join(2000L);
                daemon3.join(2000L);
                if (!daemon.isAlive() && !daemon2.isAlive() && !daemon3.isAlive()) {
                    if (!capture.tooLarge && !capture2.tooLarge) {
                        if (capture.failure != null) {
                            TransportFailure transport2 = transport("读取 root stdout", "读取 root 标准输出通道失败", start, capture2, capture.failure);
                            addFailures(transport2, capture, capture2, iOExceptionArr[0]);
                            throw transport2;
                        }
                        if (capture2.failure != null) {
                            TransportFailure transport3 = transport("读取 root stderr", "读取 root 错误输出通道失败", start, capture2, capture2.failure);
                            addFailures(transport3, capture, capture2, iOExceptionArr[0]);
                            throw transport3;
                        }
                        if (iOExceptionArr[0] != null) {
                            TransportFailure transport4 = transport("写入 root stdin", "向 root 标准输入通道写入或关闭失败，输入可能未完整传输", start, capture2, iOExceptionArr[0]);
                            addFailures(transport4, capture, capture2, iOExceptionArr[0]);
                            throw transport4;
                        }
                        Result result = new Result(start.exitValue(), capture.bytes.toByteArray(), capture2.bytes.toByteArray());
                        start.destroy();
                        try {
                            start.getOutputStream().close();
                        } catch (IOException e) {
                        }
                        try {
                            start.getInputStream().close();
                        } catch (IOException e2) {
                        }
                        try {
                            start.getErrorStream().close();
                        } catch (IOException e3) {
                        }
                        return result;
                    }
                    TransportFailure transport5 = transport("读取 root 输出", "root 输出超过安全限制：" + ((capture.tooLarge ? "stdout 超过 " + i + " 字节" : "") + ((capture.tooLarge && capture2.tooLarge) ? "；" : "") + (capture2.tooLarge ? "stderr 超过 32768 字节" : "")), start, capture2, null);
                    addFailures(transport5, capture, capture2, iOExceptionArr[0]);
                    throw transport5;
                }
                TransportFailure transport6 = transport("关闭 root 通道", "su 已退出，但通道未正常结束：" + ((daemon3.isAlive() ? "stdin " : "") + (daemon.isAlive() ? "stdout " : "") + (daemon2.isAlive() ? "stderr" : "")).trim(), start, capture2, null);
                addFailures(transport6, capture, capture2, iOExceptionArr[0]);
                throw transport6;
            } finally {
            }
        } catch (IOException e4) {
            throw new TransportFailure("启动 su 进程", "无法启动 su；请确认手机已 root，且 root 管理器提供 su", null, "", e4);
        }
    }

    static /* synthetic */ void lambda$execute$0(Process process, byte[] bArr, IOException[] iOExceptionArr) {
        try {
            OutputStream outputStream = process.getOutputStream();
            if (bArr != null) {
                try {
                    outputStream.write(bArr);
                } finally {
                }
            }
            outputStream.flush();
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            iOExceptionArr[0] = e;
        }
    }

    private static TransportFailure transport(String str, String str2, Process process, Capture capture, Throwable th) {
        Integer num;
        try {
            num = Integer.valueOf(process.exitValue());
        } catch (IllegalThreadStateException e) {
            num = null;
        }
        return new TransportFailure(str, str2, num, new String(capture.bytes.toByteArray(), StandardCharsets.UTF_8), th);
    }

    private static void addFailures(Throwable th, Capture capture, Capture capture2, IOException iOException) {
        if (capture.failure != null) {
            th.addSuppressed(capture.failure);
        }
        if (capture2.failure != null) {
            th.addSuppressed(capture2.failure);
        }
        if (iOException != null) {
            th.addSuppressed(iOException);
        }
    }

    private static Thread daemon(String str, Runnable runnable) {
        Thread thread = new Thread(runnable, str);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static final class Capture implements Runnable {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        volatile IOException failure;
        private final int limit;
        private final InputStream stream;
        volatile boolean tooLarge;

        Capture(InputStream inputStream, int i) {
            this.stream = inputStream;
            this.limit = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            byte[] bArr = new byte[16384];
            while (true) {
                try {
                    int read = this.stream.read(bArr);
                    if (read != -1) {
                        int size = this.limit - this.bytes.size();
                        if (read > size) {
                            this.tooLarge = true;
                        }
                        if (size > 0) {
                            this.bytes.write(bArr, 0, Math.min(read, size));
                        }
                    } else {
                        return;
                    }
                } catch (IOException e) {
                    this.failure = e;
                    return;
                }
            }
        }
    }
}
