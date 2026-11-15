package io.github.deltaconfig.core;

/* loaded from: classes.dex */
public final class ConfigPatcher {
    public static final java.lang.String[] FPS_LABELS = null;
    public static final int[] FPS_VALUES = null;
    private static final java.util.regex.Pattern HEADER = null;
    public static final java.lang.String PAD_SECTION = "/Script/PerfGear.PerfGearAndroidPadSizeConfig";
    public static final java.lang.String[] QUALITY_LABELS = null;
    public static final int[] QUALITY_VALUES = null;

    /* renamed from: io.github.deltaconfig.core.ConfigPatcher$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
    }

    public static final class Plan {
        public final byte[] engine;
        public final byte[] game;
        public final java.lang.String summary;
        public final int targetShortEdge;

        private Plan(byte[] r1, byte[] r2, java.lang.String r3, int r4) {
                r0 = this;
                r0.<init>()
                r0.engine = r1
                r0.game = r2
                r0.summary = r3
                r0.targetShortEdge = r4
                return
        }

        /* synthetic */ Plan(byte[] r1, byte[] r2, java.lang.String r3, int r4, io.github.deltaconfig.core.ConfigPatcher.AnonymousClass1 r5) {
                r0 = this;
                r0.<init>(r1, r2, r3, r4)
                return
        }
    }

    static {
            java.lang.String r4 = "极致"
            java.lang.String r5 = "终极"
            java.lang.String r0 = "流畅"
            java.lang.String r1 = "标准"
            java.lang.String r2 = "精致"
            java.lang.String r3 = "高清"
            java.lang.String[] r0 = new java.lang.String[]{r0, r1, r2, r3, r4, r5}
            io.github.deltaconfig.core.ConfigPatcher.QUALITY_LABELS = r0
            r0 = 6
            int[] r1 = new int[r0]
            r1 = {x003c: FILL_ARRAY_DATA , data: [0, 1, 2, 3, 4, 5} // fill-array
            io.github.deltaconfig.core.ConfigPatcher.QUALITY_VALUES = r1
            java.lang.String r6 = "165 帧"
            java.lang.String r7 = "185 帧"
            java.lang.String r2 = "60 帧"
            java.lang.String r3 = "90 帧"
            java.lang.String r4 = "120 帧"
            java.lang.String r5 = "144 帧"
            java.lang.String[] r1 = new java.lang.String[]{r2, r3, r4, r5, r6, r7}
            io.github.deltaconfig.core.ConfigPatcher.FPS_LABELS = r1
            int[] r0 = new int[r0]
            r0 = {x004c: FILL_ARRAY_DATA , data: [2, 7, 8, 9, 10, 11} // fill-array
            io.github.deltaconfig.core.ConfigPatcher.FPS_VALUES = r0
            java.lang.String r0 = "^[ \\t]*\\[([^\\]\\r\\n]+)\\][ \\t]*(?:[;#].*)?$"
            java.util.regex.Pattern r0 = java.util.regex.Pattern.compile(r0)
            io.github.deltaconfig.core.ConfigPatcher.HEADER = r0
            return
    }

    private ConfigPatcher() {
            r0 = this;
            r0.<init>()
            return
    }

    public static int fpsForInternal(int r0) {
            switch(r0) {
                case 2: goto L14;
                case 3: goto L3;
                case 4: goto L3;
                case 5: goto L3;
                case 6: goto L3;
                case 7: goto L11;
                case 8: goto Le;
                case 9: goto Lb;
                case 10: goto L8;
                case 11: goto L5;
                default: goto L3;
            }
        L3:
            r0 = -1
            return r0
        L5:
            r0 = 185(0xb9, float:2.59E-43)
            return r0
        L8:
            r0 = 165(0xa5, float:2.31E-43)
            return r0
        Lb:
            r0 = 144(0x90, float:2.02E-43)
            return r0
        Le:
            r0 = 120(0x78, float:1.68E-43)
            return r0
        L11:
            r0 = 90
            return r0
        L14:
            r0 = 60
            return r0
    }

    public static int getInt(java.lang.String r5, java.lang.String r6, java.lang.String r7, int r8) {
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.RuntimeException -> L68
            r0.<init>()     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r1 = "^[ \\t]*"
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r7 = java.util.regex.Pattern.quote(r7)     // Catch: java.lang.RuntimeException -> L68
            java.lang.StringBuilder r7 = r0.append(r7)     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r0 = "[ \\t]*=[ \\t]*([+-]?[0-9]+)[ \\t]*(?:[;#].*)?$"
            java.lang.StringBuilder r7 = r7.append(r0)     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r7 = r7.toString()     // Catch: java.lang.RuntimeException -> L68
            r0 = 2
            java.util.regex.Pattern r7 = java.util.regex.Pattern.compile(r7, r0)     // Catch: java.lang.RuntimeException -> L68
            java.util.List r5 = lines(r5)     // Catch: java.lang.RuntimeException -> L68
            java.util.Iterator r5 = r5.iterator()     // Catch: java.lang.RuntimeException -> L68
            r0 = 0
        L2c:
            boolean r1 = r5.hasNext()     // Catch: java.lang.RuntimeException -> L68
            if (r1 == 0) goto L69
            java.lang.Object r1 = r5.next()     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r1 = (java.lang.String) r1     // Catch: java.lang.RuntimeException -> L68
            java.lang.String r1 = withoutEnding(r1)     // Catch: java.lang.RuntimeException -> L68
            java.util.regex.Pattern r2 = io.github.deltaconfig.core.ConfigPatcher.HEADER     // Catch: java.lang.RuntimeException -> L68
            java.util.regex.Matcher r2 = r2.matcher(r1)     // Catch: java.lang.RuntimeException -> L68
            boolean r3 = r2.matches()     // Catch: java.lang.RuntimeException -> L68
            r4 = 1
            if (r3 == 0) goto L52
            java.lang.String r0 = r2.group(r4)     // Catch: java.lang.RuntimeException -> L68
            boolean r0 = r0.equalsIgnoreCase(r6)     // Catch: java.lang.RuntimeException -> L68
            goto L2c
        L52:
            java.util.regex.Matcher r1 = r7.matcher(r1)     // Catch: java.lang.RuntimeException -> L68
            if (r0 == 0) goto L67
            boolean r2 = r1.matches()     // Catch: java.lang.RuntimeException -> L68
            if (r2 == 0) goto L67
            java.lang.String r5 = r1.group(r4)     // Catch: java.lang.RuntimeException -> L68
            int r5 = java.lang.Integer.parseInt(r5)     // Catch: java.lang.RuntimeException -> L68
            return r5
        L67:
            goto L2c
        L68:
            r5 = move-exception
        L69:
            return r8
    }

    private static java.util.List<java.lang.String> lines(java.lang.String r6) {
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r1 = 0
            r2 = r1
        L8:
            int r3 = r6.length()
            if (r1 >= r3) goto L39
            char r3 = r6.charAt(r1)
            r4 = 10
            r5 = 13
            if (r3 == r5) goto L1b
            if (r3 == r4) goto L1b
            goto L36
        L1b:
            if (r3 != r5) goto L2c
            int r3 = r1 + 1
            int r5 = r6.length()
            if (r3 >= r5) goto L2c
            char r5 = r6.charAt(r3)
            if (r5 != r4) goto L2c
            r1 = r3
        L2c:
            int r3 = r1 + 1
            java.lang.String r2 = r6.substring(r2, r3)
            r0.add(r2)
            r2 = r3
        L36:
            int r1 = r1 + 1
            goto L8
        L39:
            int r1 = r6.length()
            if (r2 >= r1) goto L46
            java.lang.String r6 = r6.substring(r2)
            r0.add(r6)
        L46:
            return r0
    }

    public static io.github.deltaconfig.core.ConfigPatcher.Plan plan(byte[] r25, byte[] r26, byte[] r27, byte[] r28, int r29, int r30, int r31, int r32) throws java.lang.Exception {
            r0 = r25
            r1 = r26
            r2 = r29
            r3 = r31
            r4 = r32
            java.lang.String r5 = "Game.ini"
            java.lang.String r6 = "1"
            java.lang.String r7 = "DFMPerfGearConfig"
            java.lang.String r8 = ""
            java.lang.String r9 = "Engine.ini"
            java.lang.String r10 = "生成修改配置"
            if (r2 < 0) goto L29c
            r11 = 5
            if (r2 > r11) goto L29c
            int r12 = fpsForInternal(r30)     // Catch: java.lang.Exception -> L2a4
            if (r12 < 0) goto L294
            r12 = 25
            if (r3 < r12) goto L28c
            r12 = 100
            if (r3 > r12) goto L28c
            r13 = 1
            if (r4 < r13) goto L284
            r14 = 8192(0x2000, float:1.148E-41)
            if (r4 > r14) goto L284
        L31:
            int r15 = fpsForInternal(r30)
            if (r1 == 0) goto L269
            int r14 = r1.length
            if (r14 == 0) goto L269
            int r14 = r1.length
            r11 = 2097152(0x200000, float:2.938736E-39)
            if (r14 > r11) goto L269
            r14 = r27
            r11 = r28
            byte[] r11 = io.github.deltaconfig.core.ConfigCrypto.deriveKey(r11, r14)     // Catch: java.lang.Exception -> L25e
            java.lang.String r12 = io.github.deltaconfig.core.ConfigCrypto.decodeIni(r0, r11)     // Catch: java.lang.Throwable -> L24e java.lang.Exception -> L250
            java.lang.String r13 = "DFMQualityLevel"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            r14.<init>()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.StringBuilder r14 = r14.append(r8)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.StringBuilder r14 = r14.append(r2)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r14 = r14.toString()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r12 = setField(r12, r7, r13, r14)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r13 = "DFMFPSLevel"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            r14.<init>()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.StringBuilder r14 = r14.append(r8)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            r16 = r8
            r8 = r30
            java.lang.StringBuilder r8 = r14.append(r8)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r8 = r8.toString()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r7 = setField(r12, r7, r13, r8)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r8 = "QualityFPSSetting"
            java.lang.String r12 = "Unlimit"
            java.lang.String r7 = setField(r7, r8, r12, r6)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            r8 = 90
            java.lang.String r12 = "SystemSettings"
            if (r15 < r8) goto Laa
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            r8.<init>()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r13 = "DFMDeviceFrame.White"
            java.lang.StringBuilder r8 = r8.append(r13)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.StringBuilder r8 = r8.append(r15)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r13 = "fps"
            java.lang.StringBuilder r8 = r8.append(r13)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r8 = r8.toString()     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            java.lang.String r7 = setField(r7, r12, r8, r6)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
        Laa:
            java.lang.String r6 = "PerfGear.SmallPadSize"
            java.lang.String r8 = "0"
            java.lang.String r6 = setField(r7, r12, r6, r8)     // Catch: java.lang.Exception -> L246 java.lang.Throwable -> L24e
            byte[] r18 = io.github.deltaconfig.core.ConfigCrypto.replaceIni(r0, r6, r11)     // Catch: java.lang.Exception -> L23d java.lang.Throwable -> L24e
            int r0 = r1.length     // Catch: java.lang.Throwable -> L24e
            r6 = 3
            if (r0 < r6) goto Ld9
            r7 = 0
            r0 = r1[r7]     // Catch: java.lang.Throwable -> L24e
            r0 = r0 & 255(0xff, float:3.57E-43)
            r7 = 239(0xef, float:3.35E-43)
            if (r0 != r7) goto Ld9
            r0 = 1
            r7 = r1[r0]     // Catch: java.lang.Throwable -> L24e
            r0 = r7 & 255(0xff, float:3.57E-43)
            r7 = 187(0xbb, float:2.62E-43)
            if (r0 != r7) goto Ld9
            r0 = 2
            r0 = r1[r0]     // Catch: java.lang.Throwable -> L24e
            r0 = r0 & 255(0xff, float:3.57E-43)
            r7 = 191(0xbf, float:2.68E-43)
            if (r0 != r7) goto Ld9
            r0 = 1
            goto Lda
        Ld9:
            r0 = 0
        Lda:
            java.nio.charset.Charset r7 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.charset.CharsetDecoder r7 = r7.newDecoder()     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.charset.CodingErrorAction r8 = java.nio.charset.CodingErrorAction.REPORT     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.charset.CharsetDecoder r7 = r7.onMalformedInput(r8)     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.charset.CodingErrorAction r8 = java.nio.charset.CodingErrorAction.REPORT     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.charset.CharsetDecoder r7 = r7.onUnmappableCharacter(r8)     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            if (r0 == 0) goto Lf0
            r8 = r6
            goto Lf1
        Lf0:
            r8 = 0
        Lf1:
            int r9 = r1.length     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            if (r0 == 0) goto Lf6
            r12 = r6
            goto Lf7
        Lf6:
            r12 = 0
        Lf7:
            int r9 = r9 - r12
            java.nio.ByteBuffer r8 = java.nio.ByteBuffer.wrap(r1, r8, r9)     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.nio.CharBuffer r7 = r7.decode(r8)     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Exception -> L235 java.lang.Throwable -> L24e
            r8 = 100
            if (r3 != r8) goto L10c
            r14 = 8192(0x2000, float:1.148E-41)
            goto L11d
        L10c:
            double r8 = (double) r4
            double r12 = (double) r3
            r19 = 4636737291354636288(0x4059000000000000, double:100.0)
            double r12 = r12 / r19
            double r8 = r8 * r12
            long r8 = java.lang.Math.round(r8)     // Catch: java.lang.Throwable -> L24e
            int r4 = (int) r8     // Catch: java.lang.Throwable -> L24e
            r8 = 1
            int r14 = java.lang.Math.max(r8, r4)     // Catch: java.lang.Throwable -> L24e
        L11d:
            java.lang.String r19 = "UltraLowResolution"
            java.lang.String r20 = "LowResolution"
            java.lang.String r21 = "MidResolution"
            java.lang.String r22 = "HighResolution"
            java.lang.String r23 = "UltraHighResolution"
            java.lang.String r24 = "SuperResolution"
            java.lang.String[] r4 = new java.lang.String[]{r19, r20, r21, r22, r23, r24}     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L24e
            java.lang.String r9 = "BackBuffer=8192"
            r8.<init>(r9)     // Catch: java.lang.Throwable -> L24e
            r9 = 0
        L135:
            r12 = 44
            r13 = 6
            if (r9 >= r13) goto L150
            r13 = r4[r9]     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r12 = r8.append(r12)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r12 = r12.append(r13)     // Catch: java.lang.Throwable -> L24e
            r13 = 61
            java.lang.StringBuilder r12 = r12.append(r13)     // Catch: java.lang.Throwable -> L24e
            r12.append(r14)     // Catch: java.lang.Throwable -> L24e
            int r9 = r9 + 1
            goto L135
        L150:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L24e
            java.lang.String r9 = "("
            r4.<init>(r9)     // Catch: java.lang.Throwable -> L24e
            r9 = 1
        L158:
            r13 = 41
            r6 = 5
            if (r9 > r6) goto L180
            r6 = 1
            if (r9 == r6) goto L163
            r4.append(r12)     // Catch: java.lang.Throwable -> L24e
        L163:
            java.lang.String r6 = "Level"
            java.lang.StringBuilder r6 = r4.append(r6)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r6 = r6.append(r9)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r12 = "Config=("
            java.lang.StringBuilder r6 = r6.append(r12)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r6 = r6.append(r8)     // Catch: java.lang.Throwable -> L24e
            r6.append(r13)     // Catch: java.lang.Throwable -> L24e
            int r9 = r9 + 1
            r6 = 3
            r12 = 44
            goto L158
        L180:
            r4.append(r13)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r6 = "Small"
            java.lang.String r8 = "Mid"
            java.lang.String r9 = "Big"
            java.lang.String[] r6 = new java.lang.String[]{r6, r8, r9}     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            r8 = r7
            r7 = 0
        L18f:
            r9 = 3
            if (r7 >= r9) goto L1b4
            r9 = r6[r7]     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.String r12 = "/Script/PerfGear.PerfGearAndroidPadSizeConfig"
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            r13.<init>()     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.StringBuilder r9 = r13.append(r9)     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.String r13 = "PadConfig"
            java.lang.StringBuilder r9 = r9.append(r13)     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.String r9 = r9.toString()     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.String r13 = r4.toString()     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            java.lang.String r8 = setField(r8, r12, r9, r13)     // Catch: java.lang.Exception -> L22d java.lang.Throwable -> L24e
            int r7 = r7 + 1
            goto L18f
        L1b4:
            java.nio.charset.Charset r4 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L24e
            byte[] r4 = r8.getBytes(r4)     // Catch: java.lang.Throwable -> L24e
            if (r0 == 0) goto L1c3
            int r5 = r4.length     // Catch: java.lang.Throwable -> L24e
            r6 = 3
            int r5 = r5 + r6
            byte[] r5 = new byte[r5]     // Catch: java.lang.Throwable -> L24e
            goto L1c4
        L1c3:
            r5 = r4
        L1c4:
            if (r0 == 0) goto L1cf
            r0 = 3
            r6 = 0
            java.lang.System.arraycopy(r1, r6, r5, r6, r0)     // Catch: java.lang.Throwable -> L24e
            int r1 = r4.length     // Catch: java.lang.Throwable -> L24e
            java.lang.System.arraycopy(r4, r6, r5, r0, r1)     // Catch: java.lang.Throwable -> L24e
        L1cf:
            int r0 = r5.length     // Catch: java.lang.Throwable -> L24e
            r1 = 2097152(0x200000, float:2.938736E-39)
            if (r0 > r1) goto L21d
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L24e
            r0.<init>()     // Catch: java.lang.Throwable -> L24e
            java.lang.String r1 = qualityLabel(r29)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r1 = " / "
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r0 = r0.append(r15)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r1 = " 帧请求 / 渲染 "
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L24e
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r1 = "%\n三组设备配置的全部画质档位使用同一渲染目标；安装包与 UserGameplay.ini 不修改。\n实际帧率与分辨率受游戏运行时、显示模式和设备性能约束。"
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L24e
            r1 = 5
            if (r2 != r1) goto L201
            java.lang.String r8 = "\n终极画质的资源表独立帧率上限未被本工具覆盖。"
            goto L203
        L201:
            r8 = r16
        L203:
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> L24e
            java.lang.String r20 = r0.toString()     // Catch: java.lang.Throwable -> L24e
            io.github.deltaconfig.core.ConfigPatcher$Plan r0 = new io.github.deltaconfig.core.ConfigPatcher$Plan     // Catch: java.lang.Throwable -> L24e
            r22 = 0
            r17 = r0
            r19 = r5
            r21 = r14
            r17.<init>(r18, r19, r20, r21, r22)     // Catch: java.lang.Throwable -> L24e
            r1 = 0
            java.util.Arrays.fill(r11, r1)
            return r0
        L21d:
            io.github.deltaconfig.core.OperationFailure r0 = new io.github.deltaconfig.core.OperationFailure     // Catch: java.lang.Throwable -> L24e
            java.lang.String r3 = "生成修改配置"
            java.lang.String r4 = "校验 Game.ini 重新编码大小"
            java.lang.String r5 = "Game.ini"
            java.lang.String r6 = "修改后的 Game.ini 超过 2 MiB"
            r7 = 0
            r2 = r0
            r2.<init>(r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L22d:
            r0 = move-exception
            java.lang.String r1 = "更新 Game.ini 的各档渲染分辨率字段"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r1, r5, r0)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L235:
            r0 = move-exception
            java.lang.String r1 = "按 UTF-8 解码 Game.ini"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r1, r5, r0)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L23d:
            r0 = move-exception
            r1 = r0
            java.lang.String r0 = "重新加密、认证并回读 Engine.ini"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r0, r9, r1)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L246:
            r0 = move-exception
            java.lang.String r1 = "更新 Engine.ini 的画质、帧率及设备字段"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r1, r9, r0)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L24e:
            r0 = move-exception
            goto L259
        L250:
            r0 = move-exception
            r1 = r0
            java.lang.String r0 = "解密并校验 Engine.ini"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r0, r9, r1)     // Catch: java.lang.Throwable -> L24e
            throw r0     // Catch: java.lang.Throwable -> L24e
        L259:
            r1 = 0
            java.util.Arrays.fill(r11, r1)
            throw r0
        L25e:
            r0 = move-exception
            r1 = r0
            java.lang.String r0 = "校验 UserGameplay.ini 并派生本机认证密钥"
            java.lang.String r2 = "UserGameplay.ini"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r0, r2, r1)
            throw r0
        L269:
            io.github.deltaconfig.core.OperationFailure r0 = new io.github.deltaconfig.core.OperationFailure
            java.lang.String r1 = "Game.ini 为空或超过 2 MiB"
            r2 = 0
            java.lang.String r3 = "生成修改配置"
            java.lang.String r4 = "检查 Game.ini 输入大小"
            java.lang.String r5 = "Game.ini"
            r25 = r0
            r26 = r3
            r27 = r4
            r28 = r5
            r29 = r1
            r30 = r2
            r25.<init>(r26, r27, r28, r29, r30)
            throw r0
        L284:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> L2a4
            java.lang.String r1 = "屏幕短边无效"
            r0.<init>(r1)     // Catch: java.lang.Exception -> L2a4
            throw r0     // Catch: java.lang.Exception -> L2a4
        L28c:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> L2a4
            java.lang.String r1 = "渲染比例须为 25% 至 100%"
            r0.<init>(r1)     // Catch: java.lang.Exception -> L2a4
            throw r0     // Catch: java.lang.Exception -> L2a4
        L294:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> L2a4
            java.lang.String r1 = "帧率档位映射未经验证"
            r0.<init>(r1)     // Catch: java.lang.Exception -> L2a4
            throw r0     // Catch: java.lang.Exception -> L2a4
        L29c:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> L2a4
            java.lang.String r1 = "画质档位不受支持"
            r0.<init>(r1)     // Catch: java.lang.Exception -> L2a4
            throw r0     // Catch: java.lang.Exception -> L2a4
        L2a4:
            r0 = move-exception
            java.lang.String r1 = "检查画质、帧率及渲染比例参数"
            java.lang.String r2 = "Engine.ini / Game.ini"
            io.github.deltaconfig.core.OperationFailure r0 = io.github.deltaconfig.core.OperationFailure.wrap(r10, r1, r2, r0)
            throw r0
    }

    public static java.lang.String qualityLabel(int r1) {
            if (r1 < 0) goto Lc
            java.lang.String[] r0 = io.github.deltaconfig.core.ConfigPatcher.QUALITY_LABELS
            int r0 = r0.length
            if (r1 >= r0) goto Lc
            java.lang.String[] r0 = io.github.deltaconfig.core.ConfigPatcher.QUALITY_LABELS
            r1 = r0[r1]
            goto Le
        Lc:
            java.lang.String r1 = "未知档位"
        Le:
            return r1
    }

    private static java.lang.String removeFields(java.lang.String r11, java.lang.String r12, java.lang.String... r13) {
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            int r1 = r11.length()
            r0.<init>(r1)
            java.util.List r11 = lines(r11)
            java.util.Iterator r11 = r11.iterator()
            r1 = 0
            r2 = r1
        L14:
            boolean r3 = r11.hasNext()
            if (r3 == 0) goto L7d
            java.lang.Object r3 = r11.next()
            java.lang.String r3 = (java.lang.String) r3
            java.lang.String r4 = withoutEnding(r3)
            java.util.regex.Pattern r5 = io.github.deltaconfig.core.ConfigPatcher.HEADER
            java.util.regex.Matcher r5 = r5.matcher(r4)
            boolean r6 = r5.matches()
            r7 = 1
            if (r6 == 0) goto L3d
            java.lang.String r2 = r5.group(r7)
            boolean r2 = r2.equalsIgnoreCase(r12)
            r0.append(r3)
            goto L14
        L3d:
            if (r2 == 0) goto L76
            int r5 = r13.length
            r6 = r1
        L42:
            if (r6 >= r5) goto L76
            r8 = r13[r6]
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r10 = "^[ \\t]*"
            java.lang.StringBuilder r9 = r9.append(r10)
            java.lang.String r8 = java.util.regex.Pattern.quote(r8)
            java.lang.StringBuilder r8 = r9.append(r8)
            java.lang.String r9 = "[ \\t]*=.*$"
            java.lang.StringBuilder r8 = r8.append(r9)
            java.lang.String r8 = r8.toString()
            r9 = 2
            java.util.regex.Pattern r8 = java.util.regex.Pattern.compile(r8, r9)
            java.util.regex.Matcher r8 = r8.matcher(r4)
            boolean r8 = r8.matches()
            if (r8 == 0) goto L73
            goto L77
        L73:
            int r6 = r6 + 1
            goto L42
        L76:
            r7 = r1
        L77:
            if (r7 != 0) goto L7c
            r0.append(r3)
        L7c:
            goto L14
        L7d:
            java.lang.String r11 = r0.toString()
            return r11
    }

    public static io.github.deltaconfig.core.ConfigPatcher.Plan resetDefaults(byte[] r10, byte[] r11, byte[] r12, byte[] r13) throws java.lang.Exception {
            java.lang.String r0 = "Engine.ini"
            java.lang.String r1 = "恢复默认画质设置"
            if (r11 == 0) goto L117
            int r2 = r11.length
            if (r2 == 0) goto L117
            int r2 = r11.length
            r3 = 2097152(0x200000, float:2.938736E-39)
            if (r2 > r3) goto L117
            byte[] r12 = io.github.deltaconfig.core.ConfigCrypto.deriveKey(r13, r12)     // Catch: java.lang.Exception -> L10d
            r13 = 0
            java.lang.String r2 = io.github.deltaconfig.core.ConfigCrypto.decodeIni(r10, r12)     // Catch: java.lang.Throwable -> Lff java.lang.Exception -> L101
            java.lang.String r3 = "DFMPerfGearConfig"
            java.lang.String r4 = "DFMQualityLevel"
            java.lang.String r5 = "DFMFPSLevel"
            java.lang.String[] r4 = new java.lang.String[]{r4, r5}     // Catch: java.lang.Throwable -> Lff
            java.lang.String r2 = removeFields(r2, r3, r4)     // Catch: java.lang.Throwable -> Lff
            java.lang.String r3 = "QualityFPSSetting"
            java.lang.String r4 = "Unlimit"
            java.lang.String[] r4 = new java.lang.String[]{r4}     // Catch: java.lang.Throwable -> Lff
            java.lang.String r2 = removeFields(r2, r3, r4)     // Catch: java.lang.Throwable -> Lff
            java.lang.String r3 = "SystemSettings"
            java.lang.String r4 = "PerfGear.SmallPadSize"
            java.lang.String r5 = "DFMDeviceFrame.White90fps"
            java.lang.String r6 = "DFMDeviceFrame.White120fps"
            java.lang.String r7 = "DFMDeviceFrame.White144fps"
            java.lang.String r8 = "DFMDeviceFrame.White165fps"
            java.lang.String r9 = "DFMDeviceFrame.White185fps"
            java.lang.String[] r4 = new java.lang.String[]{r4, r5, r6, r7, r8, r9}     // Catch: java.lang.Throwable -> Lff
            java.lang.String r2 = removeFields(r2, r3, r4)     // Catch: java.lang.Throwable -> Lff
            byte[] r4 = io.github.deltaconfig.core.ConfigCrypto.replaceIni(r10, r2, r12)     // Catch: java.lang.Exception -> Lf7 java.lang.Throwable -> Lff
            int r10 = r11.length     // Catch: java.lang.Throwable -> Lff
            r0 = 3
            if (r10 < r0) goto L6d
            r10 = r11[r13]     // Catch: java.lang.Throwable -> Lff
            r10 = r10 & 255(0xff, float:3.57E-43)
            r2 = 239(0xef, float:3.35E-43)
            if (r10 != r2) goto L6d
            r10 = 1
            r2 = r11[r10]     // Catch: java.lang.Throwable -> Lff
            r2 = r2 & 255(0xff, float:3.57E-43)
            r3 = 187(0xbb, float:2.62E-43)
            if (r2 != r3) goto L6d
            r2 = 2
            r2 = r11[r2]     // Catch: java.lang.Throwable -> Lff
            r2 = r2 & 255(0xff, float:3.57E-43)
            r3 = 191(0xbf, float:2.68E-43)
            if (r2 != r3) goto L6d
            goto L6e
        L6d:
            r10 = r13
        L6e:
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.charset.CharsetDecoder r2 = r2.newDecoder()     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.charset.CodingErrorAction r3 = java.nio.charset.CodingErrorAction.REPORT     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.charset.CharsetDecoder r2 = r2.onMalformedInput(r3)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.charset.CodingErrorAction r3 = java.nio.charset.CodingErrorAction.REPORT     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.charset.CharsetDecoder r2 = r2.onUnmappableCharacter(r3)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            if (r10 == 0) goto L84
            r3 = r0
            goto L85
        L84:
            r3 = r13
        L85:
            int r5 = r11.length     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            if (r10 == 0) goto L8a
            r6 = r0
            goto L8b
        L8a:
            r6 = r13
        L8b:
            int r5 = r5 - r6
            java.nio.ByteBuffer r3 = java.nio.ByteBuffer.wrap(r11, r3, r5)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.nio.CharBuffer r2 = r2.decode(r3)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            int r3 = r2.indexOf(r13)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            if (r3 >= 0) goto Le5
            java.lang.String r3 = "(?m)^[ \\t]*\\[[^\\]\\r\\n]+\\]"
            java.util.regex.Pattern r3 = java.util.regex.Pattern.compile(r3)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.util.regex.Matcher r3 = r3.matcher(r2)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            boolean r3 = r3.find()     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            if (r3 == 0) goto Le5
        Laf:
            java.lang.String r1 = "/Script/PerfGear.PerfGearAndroidPadSizeConfig"
            java.lang.String r3 = "SmallPadConfig"
            java.lang.String r5 = "MidPadConfig"
            java.lang.String r6 = "BigPadConfig"
            java.lang.String[] r3 = new java.lang.String[]{r3, r5, r6}     // Catch: java.lang.Throwable -> Lff
            java.lang.String r1 = removeFields(r2, r1, r3)     // Catch: java.lang.Throwable -> Lff
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> Lff
            byte[] r1 = r1.getBytes(r2)     // Catch: java.lang.Throwable -> Lff
            if (r10 == 0) goto Lcd
            int r2 = r1.length     // Catch: java.lang.Throwable -> Lff
            int r2 = r2 + r0
            byte[] r2 = new byte[r2]     // Catch: java.lang.Throwable -> Lff
            r5 = r2
            goto Lce
        Lcd:
            r5 = r1
        Lce:
            if (r10 == 0) goto Ld7
            java.lang.System.arraycopy(r11, r13, r5, r13, r0)     // Catch: java.lang.Throwable -> Lff
            int r10 = r1.length     // Catch: java.lang.Throwable -> Lff
            java.lang.System.arraycopy(r1, r13, r5, r0, r10)     // Catch: java.lang.Throwable -> Lff
        Ld7:
            io.github.deltaconfig.core.ConfigPatcher$Plan r10 = new io.github.deltaconfig.core.ConfigPatcher$Plan     // Catch: java.lang.Throwable -> Lff
            java.lang.String r6 = "清除本工具涉及的画质、帧率和渲染覆盖项。\n保留其余配置与本机认证信息；默认档位由游戏下次启动时自行生成。"
            r7 = 0
            r8 = 0
            r3 = r10
            r3.<init>(r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> Lff
            java.util.Arrays.fill(r12, r13)
            return r10
        Le5:
            java.lang.IllegalArgumentException r10 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            java.lang.String r11 = "渲染配置不是受支持的 INI 格式"
            r10.<init>(r11)     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
            throw r10     // Catch: java.lang.Exception -> Led java.lang.Throwable -> Lff
        Led:
            r10 = move-exception
            java.lang.String r11 = "解码并验证渲染配置"
            java.lang.String r0 = "Game.ini"
            io.github.deltaconfig.core.OperationFailure r10 = io.github.deltaconfig.core.OperationFailure.wrap(r1, r11, r0, r10)     // Catch: java.lang.Throwable -> Lff
            throw r10     // Catch: java.lang.Throwable -> Lff
        Lf7:
            r10 = move-exception
            java.lang.String r11 = "重新封装并认证主配置"
            io.github.deltaconfig.core.OperationFailure r10 = io.github.deltaconfig.core.OperationFailure.wrap(r1, r11, r0, r10)     // Catch: java.lang.Throwable -> Lff
            throw r10     // Catch: java.lang.Throwable -> Lff
        Lff:
            r10 = move-exception
            goto L109
        L101:
            r10 = move-exception
            java.lang.String r11 = "解密并认证主配置"
            io.github.deltaconfig.core.OperationFailure r10 = io.github.deltaconfig.core.OperationFailure.wrap(r1, r11, r0, r10)     // Catch: java.lang.Throwable -> Lff
            throw r10     // Catch: java.lang.Throwable -> Lff
        L109:
            java.util.Arrays.fill(r12, r13)
            throw r10
        L10d:
            r10 = move-exception
            java.lang.String r11 = "验证本机认证信息"
            java.lang.String r12 = "UserGameplay.ini"
            io.github.deltaconfig.core.OperationFailure r10 = io.github.deltaconfig.core.OperationFailure.wrap(r1, r11, r12, r10)
            throw r10
        L117:
            io.github.deltaconfig.core.OperationFailure r10 = new io.github.deltaconfig.core.OperationFailure
            java.lang.String r4 = "配置为空或超过安全大小上限"
            r5 = 0
            java.lang.String r1 = "恢复默认画质设置"
            java.lang.String r2 = "检查渲染配置输入大小"
            java.lang.String r3 = "Game.ini"
            r0 = r10
            r0.<init>(r1, r2, r3, r4, r5)
            throw r10
    }

    private static java.lang.String setField(java.lang.String r12, java.lang.String r13, java.lang.String r14, java.lang.String r15) {
            java.lang.String r0 = "\r\n"
            boolean r1 = r12.contains(r0)
            java.lang.String r2 = "\r"
            java.lang.String r3 = "\n"
            if (r1 == 0) goto Ld
            goto L1c
        Ld:
            boolean r0 = r12.contains(r3)
            if (r0 == 0) goto L15
        L13:
            r0 = r3
            goto L1c
        L15:
            boolean r0 = r12.contains(r2)
            if (r0 == 0) goto L13
            r0 = r2
        L1c:
            java.util.List r1 = lines(r12)
            int r4 = r1.size()
            r5 = -1
            r6 = 0
            r7 = r5
        L27:
            int r8 = r1.size()
            r9 = 1
            if (r6 >= r8) goto L79
            java.util.regex.Pattern r8 = io.github.deltaconfig.core.ConfigPatcher.HEADER
            java.lang.Object r10 = r1.get(r6)
            java.lang.String r10 = (java.lang.String) r10
            java.lang.String r10 = withoutEnding(r10)
            java.util.regex.Matcher r8 = r8.matcher(r10)
            boolean r10 = r8.matches()
            if (r10 != 0) goto L45
            goto L76
        L45:
            if (r7 < 0) goto L4e
            int r10 = r1.size()
            if (r4 != r10) goto L4e
            r4 = r6
        L4e:
            java.lang.String r8 = r8.group(r9)
            boolean r8 = r8.equalsIgnoreCase(r13)
            if (r8 == 0) goto L76
            if (r7 >= 0) goto L5d
            int r7 = r6 + 1
            goto L76
        L5d:
            java.lang.IllegalArgumentException r12 = new java.lang.IllegalArgumentException
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            java.lang.String r15 = "目标配置节重复: "
            java.lang.StringBuilder r14 = r14.append(r15)
            java.lang.StringBuilder r13 = r14.append(r13)
            java.lang.String r13 = r13.toString()
            r12.<init>(r13)
            throw r12
        L76:
            int r6 = r6 + 1
            goto L27
        L79:
            r6 = 61
            if (r7 >= 0) goto Lf4
            boolean r1 = r12.isEmpty()
            if (r1 != 0) goto Lbc
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r1 = r1.toString()
            boolean r1 = r12.endsWith(r1)
            if (r1 == 0) goto L9b
            goto Lbc
        L9b:
            boolean r1 = r12.endsWith(r3)
            if (r1 != 0) goto Lba
            boolean r1 = r12.endsWith(r2)
            if (r1 == 0) goto La8
            goto Lba
        La8:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r1 = r1.toString()
            goto Lbe
        Lba:
            r1 = r0
            goto Lbe
        Lbc:
            java.lang.String r1 = ""
        Lbe:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r12 = r2.append(r12)
            java.lang.StringBuilder r12 = r12.append(r1)
            r1 = 91
            java.lang.StringBuilder r12 = r12.append(r1)
            java.lang.StringBuilder r12 = r12.append(r13)
            r13 = 93
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.StringBuilder r12 = r12.append(r14)
            java.lang.StringBuilder r12 = r12.append(r6)
            java.lang.StringBuilder r12 = r12.append(r15)
            java.lang.StringBuilder r12 = r12.append(r0)
            java.lang.String r12 = r12.toString()
            return r12
        Lf4:
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r10 = "^([ \\t]*"
            java.lang.StringBuilder r8 = r8.append(r10)
            java.lang.String r10 = java.util.regex.Pattern.quote(r14)
            java.lang.StringBuilder r8 = r8.append(r10)
            java.lang.String r10 = "[ \\t]*=[ \\t]*)([^\\r\\n]*)$"
            java.lang.StringBuilder r8 = r8.append(r10)
            java.lang.String r8 = r8.toString()
            r10 = 2
            java.util.regex.Pattern r8 = java.util.regex.Pattern.compile(r8, r10)
        L118:
            if (r7 >= r4) goto L187
            java.lang.Object r10 = r1.get(r7)
            java.lang.String r10 = (java.lang.String) r10
            java.lang.String r10 = withoutEnding(r10)
            java.util.regex.Matcher r10 = r8.matcher(r10)
            boolean r11 = r10.matches()
            if (r11 != 0) goto L12f
            goto L161
        L12f:
            if (r5 >= 0) goto L164
        L132:
            java.lang.Object r5 = r1.get(r7)
            java.lang.String r5 = (java.lang.String) r5
            java.lang.String r11 = withoutEnding(r5)
            int r11 = r11.length()
            java.lang.String r5 = r5.substring(r11)
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r10 = r10.group(r9)
            java.lang.StringBuilder r10 = r11.append(r10)
            java.lang.StringBuilder r10 = r10.append(r15)
            java.lang.StringBuilder r5 = r10.append(r5)
            java.lang.String r5 = r5.toString()
            r1.set(r7, r5)
            r5 = r7
        L161:
            int r7 = r7 + 1
            goto L118
        L164:
            java.lang.IllegalArgumentException r12 = new java.lang.IllegalArgumentException
            java.lang.StringBuilder r15 = new java.lang.StringBuilder
            r15.<init>()
            java.lang.String r0 = "目标字段重复: "
            java.lang.StringBuilder r15 = r15.append(r0)
            java.lang.StringBuilder r13 = r15.append(r13)
            r15 = 46
            java.lang.StringBuilder r13 = r13.append(r15)
            java.lang.StringBuilder r13 = r13.append(r14)
            java.lang.String r13 = r13.toString()
            r12.<init>(r13)
            throw r12
        L187:
            if (r5 >= 0) goto L1db
            if (r4 <= 0) goto L1bf
            int r13 = r4 + (-1)
            java.lang.Object r5 = r1.get(r13)
            java.lang.String r5 = (java.lang.String) r5
            boolean r3 = r5.endsWith(r3)
            if (r3 != 0) goto L1bf
            java.lang.Object r3 = r1.get(r13)
            java.lang.String r3 = (java.lang.String) r3
            boolean r2 = r3.endsWith(r2)
            if (r2 != 0) goto L1bf
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.Object r3 = r1.get(r13)
            java.lang.String r3 = (java.lang.String) r3
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r0)
            java.lang.String r2 = r2.toString()
            r1.set(r13, r2)
        L1bf:
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.StringBuilder r13 = r13.append(r14)
            java.lang.StringBuilder r13 = r13.append(r6)
            java.lang.StringBuilder r13 = r13.append(r15)
            java.lang.StringBuilder r13 = r13.append(r0)
            java.lang.String r13 = r13.toString()
            r1.add(r4, r13)
        L1db:
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            int r12 = r12.length()
            int r14 = r15.length()
            int r12 = r12 + r14
            int r12 = r12 + 128
            r13.<init>(r12)
            java.util.Iterator r12 = r1.iterator()
        L1ef:
            boolean r14 = r12.hasNext()
            if (r14 == 0) goto L1ff
            java.lang.Object r14 = r12.next()
            java.lang.String r14 = (java.lang.String) r14
            r13.append(r14)
            goto L1ef
        L1ff:
            java.lang.String r12 = r13.toString()
            return r12
    }

    private static java.lang.String withoutEnding(java.lang.String r4) {
            int r0 = r4.length()
        L4:
            if (r0 <= 0) goto L1b
            int r1 = r0 + (-1)
            char r2 = r4.charAt(r1)
            r3 = 13
            if (r2 == r3) goto L18
            char r1 = r4.charAt(r1)
            r2 = 10
            if (r1 != r2) goto L1b
        L18:
            int r0 = r0 + (-1)
            goto L4
        L1b:
            r1 = 0
            java.lang.String r4 = r4.substring(r1, r0)
            return r4
    }
}
