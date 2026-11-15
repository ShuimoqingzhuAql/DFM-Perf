package io.github.deltaconfig.core;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class ConfigCompatibility {
    private static final int MAX_LINE = 65536;
    private static final int MAX_LINES = 100000;
    private static final String PAD = "/script/perfgear.perfgearandroidpadsizeconfig";
    private static final String GEAR = "dfmperfgearconfig";
    private static final String FPS = "qualityfpssetting";
    private static final String SYSTEM = "systemsettings";
    private static final String[] ENGINE_SECTIONS = {GEAR, FPS, SYSTEM};
    private static final String QUALITY = "dfmqualitylevel";
    private static final String FRAME = "dfmfpslevel";
    private static final Set<String> GEAR_KEYS = names(QUALITY, FRAME);
    private static final Set<String> SYSTEM_KEYS = names("perfgear.smallpadsize", "dfmdeviceframe.white90fps", "dfmdeviceframe.white120fps", "dfmdeviceframe.white144fps", "dfmdeviceframe.white165fps", "dfmdeviceframe.white185fps");
    private static final Set<String> PAD_KEYS = names("smallpadconfig", "midpadconfig", "bigpadconfig");
    private static final Set<String> RESOLUTION_KEYS = names("backbuffer", "ultralowresolution", "lowresolution", "midresolution", "highresolution", "ultrahighresolution", "superresolution");
    private static final Pattern HEADER = Pattern.compile("^\\[([^\\]\\r\\n]+)\\][ \\t]*(?:[;#].*)?$");
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]{1,10}");

    private ConfigCompatibility() {
    }

    public static void validate(String str, byte[] bArr) throws Exception {
        Ini parse = parse(str, "主配置", true);
        Map<String, String> map = parse.values.get(GEAR);
        if (map == null || (!map.containsKey(QUALITY) && !map.containsKey(FRAME))) {
            throw incompatible("主配置尚无画质与帧率字段；请先启动游戏保存画质后再读取");
        }
        if (!map.containsKey(QUALITY) || !map.containsKey(FRAME)) {
            throw incompatible("主配置的画质或帧率字段缺失；请先启动游戏保存画质后再读取");
        }
        int integer = integer(map.get(QUALITY), "画质档位");
        if (integer < 0 || integer > 5) {
            throw incompatible("当前画质枚举超出已识别范围");
        }
        if (ConfigPatcher.fpsForInternal(integer(map.get(FRAME), "帧率档位")) < 0) {
            throw incompatible("当前帧率枚举未识别；请先在游戏中保存已支持的帧率档位后再读取");
        }
        Map<String, String> map2 = parse.values.get(FPS);
        if (map2 != null && map2.containsKey("unlimit")) {
            bool(map2.get("unlimit"), "帧率覆盖开关");
        }
        Map<String, String> map3 = parse.values.get(SYSTEM);
        if (map3 != null) {
            for (Map.Entry<String, String> entry : map3.entrySet()) {
                if (entry.getKey().equals("perfgear.smallpadsize")) {
                    String value = entry.getValue();
                    if (!value.matches("[+]?(?:[0-9]{1,3})(?:\\.[0-9]{1,6})?")) {
                        throw incompatible("设备尺寸覆盖字段格式已变化");
                    }
                    if (Double.parseDouble(value) > 100.0d) {
                        throw incompatible("设备尺寸覆盖字段超出已识别范围");
                    }
                } else {
                    bool(entry.getValue(), "设备帧率开关");
                }
            }
        }
        if (bArr == null || bArr.length == 0 || bArr.length > 2097152) {
            throw incompatible("渲染配置为空或超过大小上限");
        }
        Map<String, String> map4 = parse(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bArr)).toString(), "渲染配置", false).values.get(PAD);
        if (map4 != null) {
            Iterator<String> it = map4.values().iterator();
            while (it.hasNext()) {
                new PadReader(it.next()).validate();
            }
        }
    }

    private static Set<String> names(String... strArr) {
        return new HashSet(Arrays.asList(strArr));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static GeneralSecurityException incompatible(String str) {
        return new GeneralSecurityException("配置结构兼容性检查未通过：" + str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int integer(String str, String str2) throws GeneralSecurityException {
        if (!INTEGER.matcher(str).matches()) {
            throw incompatible(str2 + "不是已识别的整数格式");
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw incompatible(str2 + "超出整数范围");
        }
    }

    private static void bool(String str, String str2) throws GeneralSecurityException {
        if (!str.equals("0") && !str.equals("1") && !str.equalsIgnoreCase("true") && !str.equalsIgnoreCase("false")) {
            throw incompatible(str2 + "格式已变化");
        }
    }

    private static final class Ini {
        final Map<String, Map<String, String>> values;

        private Ini() {
            this.values = new HashMap();
        }
    }

    private static Ini parse(String str, String str2, boolean z) throws GeneralSecurityException {
        Set<String> set;
        String str3 = str;
        if (str3 == null || str.length() == 0 || str.length() > 2097152) {
            throw incompatible(str2 + "为空或超过大小上限");
        }
        int i = 1;
        if (str3.charAt(0) == 65279) {
            str3 = str3.substring(1);
        }
        Ini ini = new Ini();
        Set<String> names = z ? names(ENGINE_SECTIONS) : names(PAD);
        HashSet hashSet = new HashSet();
        String str4 = "";
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i2 < str3.length()) {
            int i5 = i2;
            while (i5 < str3.length() && str3.charAt(i5) != '\r' && str3.charAt(i5) != '\n') {
                char charAt = str3.charAt(i5);
                if (charAt == 0 || (charAt < ' ' && charAt != '\t')) {
                    throw incompatible(str2 + "含控制字符");
                }
                i5++;
                if (i5 - i2 > MAX_LINE) {
                    throw incompatible(str2 + "单行过长");
                }
            }
            i4 += i;
            if (i4 > MAX_LINES) {
                throw incompatible(str2 + "行数超过上限");
            }
            String trim = str3.substring(i2, i5).trim();
            if (i5 < str3.length() && str3.charAt(i5) == '\r') {
                i5++;
            }
            if (i5 < str3.length() && str3.charAt(i5) == '\n') {
                i5++;
            }
            if (!trim.isEmpty() && !trim.startsWith(";") && !trim.startsWith("#")) {
                if (trim.startsWith("[")) {
                    Matcher matcher = HEADER.matcher(trim);
                    if (!matcher.matches() || matcher.group(i).trim().isEmpty()) {
                        throw incompatible(str2 + "节标题格式已变化");
                    }
                    String lowerCase = matcher.group(i).trim().toLowerCase(Locale.ROOT);
                    if (names.contains(lowerCase)) {
                        if (!matcher.group(i).equals(matcher.group(i).trim())) {
                            throw incompatible(str2 + "目标配置节名称含未支持的首尾空白");
                        }
                        if (!hashSet.add(lowerCase)) {
                            throw incompatible(str2 + "存在重复的目标配置节");
                        }
                        ini.values.put(lowerCase, new HashMap());
                    }
                    i3 = i;
                    str4 = lowerCase;
                    i2 = i5;
                } else {
                    int indexOf = trim.indexOf(61);
                    if (i3 == 0 || indexOf < i) {
                        throw incompatible(str2 + "不是已识别的 INI 格式");
                    }
                    String lowerCase2 = trim.substring(0, indexOf).trim().toLowerCase(Locale.ROOT);
                    if (lowerCase2.isEmpty()) {
                        throw incompatible(str2 + "字段名为空");
                    }
                    if (names.contains(str4)) {
                        if (!z) {
                            set = PAD_KEYS;
                        } else if (str4.equals(GEAR)) {
                            set = GEAR_KEYS;
                        } else {
                            set = str4.equals(FPS) ? names("unlimit") : SYSTEM_KEYS;
                        }
                        if (lowerCase2.length() > i && "+-!.".indexOf(lowerCase2.charAt(0)) >= 0 && set.contains(lowerCase2.substring(1))) {
                            throw incompatible(str2 + "目标字段出现未支持的数组操作符");
                        }
                        if (set.contains(lowerCase2)) {
                            String trim2 = trim.substring(indexOf + 1).trim();
                            int length = trim2.length();
                            for (int i6 = 0; i6 < trim2.length(); i6++) {
                                if (trim2.charAt(i6) == ';' || trim2.charAt(i6) == '#') {
                                    length = i6;
                                    break;
                                }
                            }
                            if (ini.values.get(str4).put(lowerCase2, trim2.substring(0, length).trim()) != null) {
                                throw incompatible(str2 + "存在重复的目标字段");
                            }
                        }
                    }
                }
            }
            i2 = i5;
            i = 1;
        }
        if (i3 == 0) {
            throw incompatible(str2 + "缺少 INI 节标题");
        }
        return ini;
    }

    private static final class PadReader {
        private int offset;
        private final String text;

        PadReader(String str) throws GeneralSecurityException {
            if (str.length() > 16384) {
                throw ConfigCompatibility.incompatible("渲染覆盖组过长");
            }
            this.text = str;
        }

        void validate() throws GeneralSecurityException {
            symbol('(');
            HashSet hashSet = new HashSet();
            do {
                String name = name();
                if (!name.matches("level[1-5]config") || !hashSet.add(name)) {
                    throw ConfigCompatibility.incompatible("渲染覆盖的档位结构已变化或重复");
                }
                symbol('=');
                symbol('(');
                HashSet hashSet2 = new HashSet();
                do {
                    String name2 = name();
                    if (!ConfigCompatibility.RESOLUTION_KEYS.contains(name2) || !hashSet2.add(name2)) {
                        throw ConfigCompatibility.incompatible("渲染覆盖的分辨率字段已变化或重复");
                    }
                    symbol('=');
                    int number = number();
                    if (number < 1 || number > 8192) {
                        throw ConfigCompatibility.incompatible("渲染覆盖尺寸超出已识别范围");
                    }
                } while (take(','));
                symbol(')');
                if (hashSet2.size() != ConfigCompatibility.RESOLUTION_KEYS.size()) {
                    throw ConfigCompatibility.incompatible("渲染覆盖组缺少已知分辨率字段");
                }
            } while (take(','));
            symbol(')');
            space();
            if (hashSet.size() != 5 || this.offset != this.text.length()) {
                throw ConfigCompatibility.incompatible("渲染覆盖组的数量或结尾格式已变化");
            }
        }

        private void space() {
            while (this.offset < this.text.length() && Character.isWhitespace(this.text.charAt(this.offset))) {
                this.offset++;
            }
        }

        private String name() throws GeneralSecurityException {
            char charAt;
            space();
            int i = this.offset;
            while (this.offset < this.text.length() && (((charAt = this.text.charAt(this.offset)) >= 'A' && charAt <= 'Z') || ((charAt >= 'a' && charAt <= 'z') || ((charAt >= '0' && charAt <= '9') || charAt == '_')))) {
                this.offset++;
            }
            if (i == this.offset || this.offset - i > 64) {
                throw ConfigCompatibility.incompatible("渲染覆盖字段名格式已变化");
            }
            return this.text.substring(i, this.offset).toLowerCase(Locale.ROOT);
        }

        private int number() throws GeneralSecurityException {
            space();
            int i = this.offset;
            if (this.offset < this.text.length() && this.text.charAt(this.offset) == '+') {
                this.offset++;
            }
            while (this.offset < this.text.length() && this.text.charAt(this.offset) >= '0' && this.text.charAt(this.offset) <= '9') {
                this.offset++;
            }
            return ConfigCompatibility.integer(this.text.substring(i, this.offset), "渲染覆盖尺寸");
        }

        private boolean take(char c) {
            space();
            if (this.offset >= this.text.length() || this.text.charAt(this.offset) != c) {
                return false;
            }
            this.offset++;
            return true;
        }

        private void symbol(char c) throws GeneralSecurityException {
            if (!take(c)) {
                throw ConfigCompatibility.incompatible("渲染覆盖括号或分隔符结构已变化");
            }
        }
    }
}
