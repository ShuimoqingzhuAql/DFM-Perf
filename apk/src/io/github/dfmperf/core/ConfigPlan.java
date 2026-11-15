package io.github.dfmperf.core;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/** 生成目标配置（与 DeltaConfig 的 ConfigPatcher.plan 等价）。 */
public final class ConfigPlan {
    public static final String PAD_SECTION = "/Script/PerfGear.PerfGearAndroidPadSizeConfig";
    public static final String[] QUALITY_LABELS = {"流畅", "标准", "精致", "高清", "极致", "终极"};
    public static final String[] FPS_LABELS = {"60 帧", "90 帧", "120 帧", "144 帧", "165 帧", "185 帧"};
    public static final int[] FPS_VALUES = {2, 7, 8, 9, 10, 11};

    private static final String[] RES_KEYS = {
            "UltraLowResolution", "LowResolution", "MidResolution",
            "HighResolution", "UltraHighResolution", "SuperResolution"
    };

    public final byte[] engine;
    public final byte[] game;
    public final String summary;

    private ConfigPlan(byte[] engine, byte[] game, String summary) {
        this.engine = engine;
        this.game = game;
        this.summary = summary;
    }

    public static int fpsForInternal(int internal) {
        switch (internal) {
            case 2: return 60;
            case 7: return 90;
            case 8: return 120;
            case 9: return 144;
            case 10: return 165;
            case 11: return 185;
            default: return -1;
        }
    }

    public static String qualityLabel(int q) {
        return (q < 0 || q >= QUALITY_LABELS.length) ? "未知" : QUALITY_LABELS[q];
    }

    public static String fpsLabel(int internal) {
        for (int i = 0; i < FPS_VALUES.length; i++) if (FPS_VALUES[i] == internal) return FPS_LABELS[i];
        return "未识别";
    }

    public static int padPixels(int ratio, int shortEdge) {
        if (ratio >= 100) return 8192;
        int px = (int) Math.round(shortEdge * (ratio / 100.0d));
        if (px < 1) px = 1;
        if (px > 8192) px = 8192;
        return px;
    }

    public static String padTuple(int px) {
        StringBuilder sb = new StringBuilder("(Level1Config=(BackBuffer=8192");
        for (String k : RES_KEYS) sb.append(',').append(k).append('=').append(px);
        sb.append(')');
        for (int lvl = 2; lvl <= 5; lvl++) {
            sb.append(",Level").append(lvl).append("Config=(BackBuffer=8192");
            for (String k : RES_KEYS) sb.append(',').append(k).append('=').append(px);
            sb.append(')');
        }
        return sb.append(')').toString();
    }

    public static ConfigPlan plan(byte[] engineBytes, byte[] gameBytes, byte[] gameplayBytes, byte[] material32,
                                  int quality, int fpsInternal, int ratio, int shortEdge) throws Exception {
        if (quality < 0 || quality > 5) throw new IllegalArgumentException("画质档位不受支持");
        int fps = fpsForInternal(fpsInternal);
        if (fps < 0) throw new IllegalArgumentException("帧率档位映射未经验证");
        if (ratio < 0 || ratio > 100) throw new IllegalArgumentException("渲染比例须为 0% 至 100%");
        if (shortEdge < 200 || shortEdge > 8192) throw new IllegalArgumentException("短边像素须为 200-8192");

        byte[] key = ConfigCrypto.deriveKey(material32, gameplayBytes);
        try {
            String engineText = ConfigCrypto.decodeIni(engineBytes, key);
            engineText = Ini.set(engineText, "DFMPerfGearConfig", "DFMQualityLevel", Integer.toString(quality));
            engineText = Ini.set(engineText, "DFMPerfGearConfig", "DFMFPSLevel", Integer.toString(fpsInternal));
            engineText = Ini.set(engineText, "QualityFPSSetting", "Unlimit", "1");
            engineText = Ini.set(engineText, "SystemSettings", "PerfGear.SmallPadSize", "0");
            if (fps >= 90) {
                engineText = Ini.set(engineText, "SystemSettings", "DFMDeviceFrame.White" + fps + "fps", "1");
            }
            byte[] newEngine = ConfigCrypto.replaceIni(engineBytes, engineText, key);

            boolean bom = gameBytes.length >= 3 && (gameBytes[0] & 255) == 0xEF
                    && (gameBytes[1] & 255) == 0xBB && (gameBytes[2] & 255) == 0xBF;
            String gameText = decodeUtf8(gameBytes, bom ? 3 : 0, gameBytes.length - (bom ? 3 : 0));
            String tuple = padTuple(padPixels(ratio, shortEdge));
            gameText = Ini.set(gameText, PAD_SECTION, "SmallPadConfig", tuple);
            gameText = Ini.set(gameText, PAD_SECTION, "MidPadConfig", tuple);
            gameText = Ini.set(gameText, PAD_SECTION, "BigPadConfig", tuple);
            byte[] body = gameText.getBytes(StandardCharsets.UTF_8);
            byte[] newGame;
            if (bom) {
                newGame = new byte[body.length + 3];
                System.arraycopy(gameBytes, 0, newGame, 0, 3);
                System.arraycopy(body, 0, newGame, 3, body.length);
            } else {
                newGame = body;
            }
            String summary = qualityLabel(quality) + " / " + fps + " 帧 / 渲染 " + ratio + "%";
            return new ConfigPlan(newEngine, newGame, summary);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public static ConfigPlan removeOverrides(byte[] engineBytes, byte[] gameBytes, byte[] gameplayBytes,
                                             byte[] material32) throws Exception {
        byte[] key = ConfigCrypto.deriveKey(material32, gameplayBytes);
        try {
            String engineText = ConfigCrypto.decodeIni(engineBytes, key);
            engineText = Ini.remove(engineText, "DFMPerfGearConfig", "DFMQualityLevel", "DFMFPSLevel");
            engineText = Ini.remove(engineText, "QualityFPSSetting", "Unlimit");
            engineText = Ini.remove(engineText, "SystemSettings", "PerfGear.SmallPadSize",
                    "DFMDeviceFrame.White90fps", "DFMDeviceFrame.White120fps", "DFMDeviceFrame.White144fps",
                    "DFMDeviceFrame.White165fps", "DFMDeviceFrame.White185fps");
            byte[] newEngine = ConfigCrypto.replaceIni(engineBytes, engineText, key);

            boolean bom = gameBytes.length >= 3 && (gameBytes[0] & 255) == 0xEF
                    && (gameBytes[1] & 255) == 0xBB && (gameBytes[2] & 255) == 0xBF;
            String gameText = decodeUtf8(gameBytes, bom ? 3 : 0, gameBytes.length - (bom ? 3 : 0));
            if (gameText.indexOf('\0') >= 0 || !gameText.contains("[")) {
                throw new IllegalArgumentException("渲染配置不是受支持的 INI 格式");
            }
            gameText = Ini.remove(gameText, PAD_SECTION, "SmallPadConfig", "MidPadConfig", "BigPadConfig");
            byte[] body = gameText.getBytes(StandardCharsets.UTF_8);
            byte[] newGame;
            if (bom) {
                newGame = new byte[body.length + 3];
                System.arraycopy(gameBytes, 0, newGame, 0, 3);
                System.arraycopy(body, 0, newGame, 3, body.length);
            } else {
                newGame = body;
            }
            return new ConfigPlan(newEngine, newGame, "已清除画质/帧率/渲染覆盖项");
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public static boolean engineMatches(byte[] engineBytes, byte[] gameplayBytes, byte[] material32,
                                        int quality, int fpsInternal) throws Exception {
        byte[] key = ConfigCrypto.deriveKey(material32, gameplayBytes);
        try {
            String text = ConfigCrypto.decodeIni(engineBytes, key);
            if (!Integer.toString(quality).equals(Ini.get(text, "DFMPerfGearConfig", "DFMQualityLevel"))) return false;
            if (!Integer.toString(fpsInternal).equals(Ini.get(text, "DFMPerfGearConfig", "DFMFPSLevel"))) return false;
            if (!"1".equals(Ini.get(text, "QualityFPSSetting", "Unlimit"))) return false;
            int fps = fpsForInternal(fpsInternal);
            if (fps >= 90 && !"1".equals(Ini.get(text, "SystemSettings", "DFMDeviceFrame.White" + fps + "fps"))) {
                return false;
            }
            return true;
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private static String decodeUtf8(byte[] b, int off, int len) throws GeneralSecurityException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b, off, len))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new GeneralSecurityException("Game.ini 不是有效的 UTF-8", e);
        }
    }
}
