package io.github.deltaconfig.core;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class ConfigPatcher {
    public static final String PAD_SECTION = "/Script/PerfGear.PerfGearAndroidPadSizeConfig";
    public static final String[] QUALITY_LABELS = {"流畅", "标准", "精致", "高清", "极致", "终极"};
    public static final int[] QUALITY_VALUES = {0, 1, 2, 3, 4, 5};
    public static final String[] FPS_LABELS = {"60 帧", "90 帧", "120 帧", "144 帧", "165 帧", "185 帧"};
    public static final int[] FPS_VALUES = {2, 7, 8, 9, 10, 11};
    private static final Pattern HEADER = Pattern.compile("^[ \\t]*\\[([^\\]\\r\\n]+)\\][ \\t]*(?:[;#].*)?$");

    private ConfigPatcher() {
    }

    public static final class Plan {
        public final byte[] engine;
        public final byte[] game;
        public final String summary;
        public final int targetShortEdge;

        private Plan(byte[] bArr, byte[] bArr2, String str, int i) {
            this.engine = bArr;
            this.game = bArr2;
            this.summary = str;
            this.targetShortEdge = i;
        }
    }

    public static Plan plan(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, int i, int i2, int i3, int i4) throws Exception {
        char c;
        try {
            if (i < 0 || i > 5) {
                throw new IllegalArgumentException("画质档位不受支持");
            }
            if (fpsForInternal(i2) < 0) {
                throw new IllegalArgumentException("帧率档位映射未经验证");
            }
            if (i3 < 25 || i3 > 100) {
                throw new IllegalArgumentException("渲染比例须为 25% 至 100%");
            }
            if (i4 < 1 || i4 > 8192) {
                throw new IllegalArgumentException("屏幕短边无效");
            }
            int fpsForInternal = fpsForInternal(i2);
            if (bArr2 == null || bArr2.length == 0 || bArr2.length > 2097152) {
                throw new OperationFailure("生成修改配置", "检查 Game.ini 输入大小", "Game.ini", "Game.ini 为空或超过 2 MiB", null);
            }
            try {
                byte[] deriveKey = ConfigCrypto.deriveKey(bArr4, bArr3);
                try {
                    try {
                        try {
                            String field = setField(setField(setField(ConfigCrypto.decodeIni(bArr, deriveKey), "DFMPerfGearConfig", "DFMQualityLevel", "" + i), "DFMPerfGearConfig", "DFMFPSLevel", "" + i2), "QualityFPSSetting", "Unlimit", "1");
                            if (fpsForInternal >= 90) {
                                field = setField(field, "SystemSettings", "DFMDeviceFrame.White" + fpsForInternal + "fps", "1");
                            }
                            try {
                                byte[] replaceIni = ConfigCrypto.replaceIni(bArr, setField(field, "SystemSettings", "PerfGear.SmallPadSize", "0"), deriveKey);
                                boolean z = bArr2.length >= 3 && (bArr2[0] & 255) == 239 && (bArr2[1] & 255) == 187 && (bArr2[2] & 255) == 191;
                                try {
                                    String charBuffer = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bArr2, z ? 3 : 0, bArr2.length - (z ? 3 : 0))).toString();
                                    int max = i3 == 100 ? 8192 : Math.max(1, (int) Math.round(i4 * (i3 / 100.0d)));
                                    String[] strArr = {"UltraLowResolution", "LowResolution", "MidResolution", "HighResolution", "UltraHighResolution", "SuperResolution"};
                                    StringBuilder sb = new StringBuilder("BackBuffer=8192");
                                    int i5 = 0;
                                    while (true) {
                                        c = ',';
                                        if (i5 >= 6) {
                                            break;
                                        }
                                        sb.append(',').append(strArr[i5]).append('=').append(max);
                                        i5++;
                                    }
                                    StringBuilder sb2 = new StringBuilder("(");
                                    int i6 = 1;
                                    while (i6 <= 5) {
                                        if (i6 != 1) {
                                            sb2.append(c);
                                        }
                                        sb2.append("Level").append(i6).append("Config=(").append((CharSequence) sb).append(')');
                                        i6++;
                                        c = ',';
                                    }
                                    sb2.append(')');
                                    try {
                                        String[] strArr2 = {"Small", "Mid", "Big"};
                                        String str = charBuffer;
                                        for (int i7 = 0; i7 < 3; i7++) {
                                            str = setField(str, PAD_SECTION, strArr2[i7] + "PadConfig", sb2.toString());
                                        }
                                        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                                        byte[] bArr5 = z ? new byte[bytes.length + 3] : bytes;
                                        if (z) {
                                            System.arraycopy(bArr2, 0, bArr5, 0, 3);
                                            System.arraycopy(bytes, 0, bArr5, 3, bytes.length);
                                        }
                                        if (bArr5.length > 2097152) {
                                            throw new OperationFailure("生成修改配置", "校验 Game.ini 重新编码大小", "Game.ini", "修改后的 Game.ini 超过 2 MiB", null);
                                        }
                                        Plan plan = new Plan(replaceIni, bArr5, qualityLabel(i) + " / " + fpsForInternal + " 帧请求 / 渲染 " + i3 + "%\n三组设备配置的全部画质档位使用同一渲染目标；安装包与 UserGameplay.ini 不修改。\n实际帧率与分辨率受游戏运行时、显示模式和设备性能约束。" + (i == 5 ? "\n终极画质的资源表独立帧率上限未被本工具覆盖。" : ""), max);
                                        Arrays.fill(deriveKey, (byte) 0);
                                        return plan;
                                    } catch (Exception e) {
                                        throw OperationFailure.wrap("生成修改配置", "更新 Game.ini 的各档渲染分辨率字段", "Game.ini", e);
                                    }
                                } catch (Exception e2) {
                                    throw OperationFailure.wrap("生成修改配置", "按 UTF-8 解码 Game.ini", "Game.ini", e2);
                                }
                            } catch (Exception e3) {
                                throw OperationFailure.wrap("生成修改配置", "重新加密、认证并回读 Engine.ini", "Engine.ini", e3);
                            }
                        } catch (Exception e4) {
                            throw OperationFailure.wrap("生成修改配置", "更新 Engine.ini 的画质、帧率及设备字段", "Engine.ini", e4);
                        }
                    } catch (Exception e5) {
                        throw OperationFailure.wrap("生成修改配置", "解密并校验 Engine.ini", "Engine.ini", e5);
                    }
                } catch (Throwable th) {
                    Arrays.fill(deriveKey, (byte) 0);
                    throw th;
                }
            } catch (Exception e6) {
                throw OperationFailure.wrap("生成修改配置", "校验 UserGameplay.ini 并派生本机认证密钥", "UserGameplay.ini", e6);
            }
        } catch (Exception e7) {
            throw OperationFailure.wrap("生成修改配置", "检查画质、帧率及渲染比例参数", "Engine.ini / Game.ini", e7);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:23:0x006a, code lost:
    
        if ((r11[2] & 255) == 191) goto L65;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static Plan resetDefaults(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) throws Exception {
        boolean z;
        if (bArr2 == null || bArr2.length == 0 || bArr2.length > 2097152) {
            throw new OperationFailure("恢复默认画质设置", "检查渲染配置输入大小", "Game.ini", "配置为空或超过安全大小上限", null);
        }
        try {
            byte[] deriveKey = ConfigCrypto.deriveKey(bArr4, bArr3);
            try {
                try {
                    try {
                        byte[] replaceIni = ConfigCrypto.replaceIni(bArr, removeFields(removeFields(removeFields(ConfigCrypto.decodeIni(bArr, deriveKey), "DFMPerfGearConfig", "DFMQualityLevel", "DFMFPSLevel"), "QualityFPSSetting", "Unlimit"), "SystemSettings", "PerfGear.SmallPadSize", "DFMDeviceFrame.White90fps", "DFMDeviceFrame.White120fps", "DFMDeviceFrame.White144fps", "DFMDeviceFrame.White165fps", "DFMDeviceFrame.White185fps"), deriveKey);
                        if (bArr2.length >= 3 && (bArr2[0] & 255) == 239) {
                            z = true;
                            if ((bArr2[1] & 255) == 187) {
                            }
                        }
                        z = false;
                        try {
                            String charBuffer = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bArr2, z ? 3 : 0, bArr2.length - (z ? 3 : 0))).toString();
                            if (charBuffer.indexOf(0) >= 0 || !Pattern.compile("(?m)^[ \\t]*\\[[^\\]\\r\\n]+\\]").matcher(charBuffer).find()) {
                                throw new IllegalArgumentException("渲染配置不是受支持的 INI 格式");
                            }
                            byte[] bytes = removeFields(charBuffer, PAD_SECTION, "SmallPadConfig", "MidPadConfig", "BigPadConfig").getBytes(StandardCharsets.UTF_8);
                            byte[] bArr5 = z ? new byte[bytes.length + 3] : bytes;
                            if (z) {
                                System.arraycopy(bArr2, 0, bArr5, 0, 3);
                                System.arraycopy(bytes, 0, bArr5, 3, bytes.length);
                            }
                            Plan plan = new Plan(replaceIni, bArr5, "清除本工具涉及的画质、帧率和渲染覆盖项。\n保留其余配置与本机认证信息；默认档位由游戏下次启动时自行生成。", 0);
                            Arrays.fill(deriveKey, (byte) 0);
                            return plan;
                        } catch (Exception e) {
                            throw OperationFailure.wrap("恢复默认画质设置", "解码并验证渲染配置", "Game.ini", e);
                        }
                    } catch (Exception e2) {
                        throw OperationFailure.wrap("恢复默认画质设置", "重新封装并认证主配置", "Engine.ini", e2);
                    }
                } catch (Exception e3) {
                    throw OperationFailure.wrap("恢复默认画质设置", "解密并认证主配置", "Engine.ini", e3);
                }
            } catch (Throwable th) {
                Arrays.fill(deriveKey, (byte) 0);
                throw th;
            }
        } catch (Exception e4) {
            throw OperationFailure.wrap("恢复默认画质设置", "验证本机认证信息", "UserGameplay.ini", e4);
        }
    }

    private static String removeFields(String str, String str2, String... strArr) {
        StringBuilder sb = new StringBuilder(str.length());
        boolean z = false;
        for (String str3 : lines(str)) {
            String withoutEnding = withoutEnding(str3);
            Matcher matcher = HEADER.matcher(withoutEnding);
            boolean z2 = true;
            if (matcher.matches()) {
                z = matcher.group(1).equalsIgnoreCase(str2);
                sb.append(str3);
            } else {
                if (z) {
                    for (String str4 : strArr) {
                        if (Pattern.compile("^[ \\t]*" + Pattern.quote(str4) + "[ \\t]*=.*$", 2).matcher(withoutEnding).matches()) {
                            break;
                        }
                    }
                }
                z2 = false;
                if (!z2) {
                    sb.append(str3);
                }
            }
        }
        return sb.toString();
    }

    public static int fpsForInternal(int i) {
        switch (i) {
            case 2:
                return 60;
            case 3:
            case 4:
            case 5:
            case 6:
            default:
                return -1;
            case 7:
                return 90;
            case 8:
                return 120;
            case 9:
                return 144;
            case 10:
                return 165;
            case 11:
                return 185;
        }
    }

    public static String qualityLabel(int i) {
        return (i < 0 || i >= QUALITY_LABELS.length) ? "未知档位" : QUALITY_LABELS[i];
    }

    public static int getInt(String str, String str2, String str3, int i) {
        try {
            Pattern compile = Pattern.compile("^[ \\t]*" + Pattern.quote(str3) + "[ \\t]*=[ \\t]*([+-]?[0-9]+)[ \\t]*(?:[;#].*)?$", 2);
            Iterator<String> it = lines(str).iterator();
            boolean z = false;
            while (it.hasNext()) {
                String withoutEnding = withoutEnding(it.next());
                Matcher matcher = HEADER.matcher(withoutEnding);
                if (matcher.matches()) {
                    z = matcher.group(1).equalsIgnoreCase(str2);
                } else {
                    Matcher matcher2 = compile.matcher(withoutEnding);
                    if (z && matcher2.matches()) {
                        return Integer.parseInt(matcher2.group(1));
                    }
                }
            }
        } catch (RuntimeException e) {
        }
        return i;
    }

    private static String setField(String str, String str2, String str3, String str4) {
        String str5;
        String str6 = "\r\n";
        if (!str.contains("\r\n")) {
            str6 = (!str.contains("\n") && str.contains("\r")) ? "\r" : "\n";
        }
        List<String> lines = lines(str);
        int size = lines.size();
        int i = -1;
        int i2 = -1;
        for (int i3 = 0; i3 < lines.size(); i3++) {
            Matcher matcher = HEADER.matcher(withoutEnding(lines.get(i3)));
            if (matcher.matches()) {
                if (i2 >= 0 && size == lines.size()) {
                    size = i3;
                }
                if (!matcher.group(1).equalsIgnoreCase(str2)) {
                    continue;
                } else {
                    if (i2 >= 0) {
                        throw new IllegalArgumentException("目标配置节重复: " + str2);
                    }
                    i2 = i3 + 1;
                }
            }
        }
        if (i2 < 0) {
            if (str.isEmpty() || str.endsWith(str6 + str6)) {
                str5 = "";
            } else {
                str5 = (str.endsWith("\n") || str.endsWith("\r")) ? str6 : str6 + str6;
            }
            return str + str5 + '[' + str2 + ']' + str6 + str3 + '=' + str4 + str6;
        }
        Pattern compile = Pattern.compile("^([ \\t]*" + Pattern.quote(str3) + "[ \\t]*=[ \\t]*)([^\\r\\n]*)$", 2);
        while (i2 < size) {
            Matcher matcher2 = compile.matcher(withoutEnding(lines.get(i2)));
            if (matcher2.matches()) {
                if (i >= 0) {
                    throw new IllegalArgumentException("目标字段重复: " + str2 + '.' + str3);
                }
                String str7 = lines.get(i2);
                lines.set(i2, matcher2.group(1) + str4 + str7.substring(withoutEnding(str7).length()));
                i = i2;
            }
            i2++;
        }
        if (i < 0) {
            if (size > 0) {
                int i4 = size - 1;
                if (!lines.get(i4).endsWith("\n") && !lines.get(i4).endsWith("\r")) {
                    lines.set(i4, lines.get(i4) + str6);
                }
            }
            lines.add(size, str3 + '=' + str4 + str6);
        }
        StringBuilder sb = new StringBuilder(str.length() + str4.length() + 128);
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
        }
        return sb.toString();
    }

    private static List<String> lines(String str) {
        int i;
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        int i3 = 0;
        while (i2 < str.length()) {
            char charAt = str.charAt(i2);
            if (charAt == '\r' || charAt == '\n') {
                if (charAt == '\r' && (i = i2 + 1) < str.length() && str.charAt(i) == '\n') {
                    i2 = i;
                }
                int i4 = i2 + 1;
                arrayList.add(str.substring(i3, i4));
                i3 = i4;
            }
            i2++;
        }
        if (i3 < str.length()) {
            arrayList.add(str.substring(i3));
        }
        return arrayList;
    }

    private static String withoutEnding(String str) {
        int length = str.length();
        while (length > 0) {
            int i = length - 1;
            if (str.charAt(i) != '\r' && str.charAt(i) != '\n') {
                break;
            }
            length--;
        }
        return str.substring(0, length);
    }
}
