package io.github.deltaconfig.core;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.Random;

/** Uses the app's own ConfigCrypto (via reflection for the private encrypt) to build fixtures. */
public final class Gen {
    static Method enc;

    static byte[] encrypt(String s, byte[] key, int magic) throws Exception {
        return (byte[]) enc.invoke(null, s, key, magic);
    }

    static byte[] repeatXor(byte[] a, byte[] b) {
        byte[] r = new byte[32];
        for (int i = 0; i < 32; i++) r[i] = (byte) (a[i] ^ b[i % b.length]);
        return r;
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 255));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        enc = ConfigCrypto.class.getDeclaredMethod("encrypt", String.class, byte[].class, int.class);
        enc.setAccessible(true);

        String mode = args[0];
        Path dir = Paths.get(args[1]);

        if (mode.equals("gen")) {
            byte[] material = new byte[32];
            Random r = new Random(20261115L);
            r.nextBytes(material);

            // per-device 20-byte digest rendered as 40 hex chars (as the real client stores it)
            byte[] digest = new byte[20];
            r.nextBytes(digest);
            StringBuilder hexd = new StringBuilder();
            for (byte b : digest) hexd.append(String.format("%02x", b & 255));
            String digestHex = hexd.toString();

            int magic = 17666; // 0x4502
            byte[] hashField = encrypt(digestHex, material, magic);
            byte[] key = repeatXor(material, digest);

            String engine = "[DFMPerfGearConfig]\r\nDFMQualityLevel=3\r\nDFMFPSLevel=2\r\n"
                    + "[QualityFPSSetting]\r\nUnlimit=0\r\n"
                    + "[SystemSettings]\r\nPerfGear.SmallPadSize=100\r\nDFMDeviceFrame.White90fps=0\r\n"
                    + "Token = " + new String(encrypt("PLACEHOLDER", key, magic), StandardCharsets.US_ASCII) + "\r\n";
            // now legitimately replace the placeholder through the app's own public API
            byte[] engineBytes = ConfigCrypto.replaceIni(engine.getBytes(StandardCharsets.ISO_8859_1),
                    "[DFMPerfGearConfig]\r\nDFMQualityLevel=3\r\nDFMFPSLevel=2\r\n"
                            + "[QualityFPSSetting]\r\nUnlimit=0\r\n"
                            + "[SystemSettings]\r\nPerfGear.SmallPadSize=100\r\nDFMDeviceFrame.White90fps=0\r\n",
                    key);

            String game = "\ufeff[/Script/Engine.RendererSettings]\r\nr.MobileHDR=True\r\n"
                    + "[/Script/PerfGear.PerfGearAndroidPadSizeConfig]\r\n"
                    + "SmallPadConfig=(Level1Config=(BackBuffer=8192,UltraLowResolution=1080,LowResolution=1080,MidResolution=1080,HighResolution=1080,UltraHighResolution=1080,SuperResolution=1080),Level2Config=(BackBuffer=8192,UltraLowResolution=1080,LowResolution=1080,MidResolution=1080,HighResolution=1080,UltraHighResolution=1080,SuperResolution=1080),Level3Config=(BackBuffer=8192,UltraLowResolution=1080,LowResolution=1080,MidResolution=1080,HighResolution=1080,UltraHighResolution=1080,SuperResolution=1080),Level4Config=(BackBuffer=8192,UltraLowResolution=1080,LowResolution=1080,MidResolution=1080,HighResolution=1080,UltraHighResolution=1080,SuperResolution=1080),Level5Config=(BackBuffer=8192,UltraLowResolution=1080,LowResolution=1080,MidResolution=1080,HighResolution=1080,UltraHighResolution=1080,SuperResolution=1080))\r\n";

            String gameplay = "[/Script/Engine.GameUserSettings]\r\nHash = "
                    + new String(hashField, StandardCharsets.US_ASCII) + "\r\n";

            Files.createDirectories(dir);
            Files.write(dir.resolve("Engine.ini"), engineBytes);
            Files.write(dir.resolve("Game.ini"), game.getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("UserGameplay.ini"), gameplay.getBytes(StandardCharsets.US_ASCII));

            System.out.println("MATERIAL=" + hex(material));
            System.out.println("DIGEST=" + digestHex);
            System.out.println("KEY=" + hex(key));
            System.out.println("HASHFIELD=" + new String(hashField, StandardCharsets.US_ASCII));
            System.out.println("ENGINE_TOKEN=" + ConfigCrypto.getField(engineBytes, "Token").length);

            // emit an independent vector for cross-implementation testing
            String sample = "[DFMPerfGearConfig]\nDFMQualityLevel=5\nDFMFPSLevel=11\n";
            System.out.println("VEC_TEXT64=" + Base64.getEncoder().encodeToString(sample.getBytes(StandardCharsets.ISO_8859_1)));
            System.out.println("VEC_TOKEN=" + new String(encrypt(sample, key, magic), StandardCharsets.US_ASCII));
        } else if (mode.equals("verify")) {
            byte[] engine = Files.readAllBytes(dir.resolve("Engine.ini"));
            byte[] gameplay = Files.readAllBytes(dir.resolve("UserGameplay.ini"));
            byte[] game = Files.readAllBytes(dir.resolve("Game.ini"));
            byte[] material = hexToBytes(args[2]);
            byte[] key = ConfigCrypto.deriveKey(material, gameplay);
            String text = ConfigCrypto.decodeIni(engine, key);
            System.out.println("DECRYPT_OK len=" + text.length());
            System.out.println("QUALITY=" + find(text, "DFMQualityLevel"));
            System.out.println("FPS=" + find(text, "DFMFPSLevel"));
            System.out.println("UNLIMIT=" + find(text, "Unlimit"));
            System.out.println("PADSIZE=" + find(text, "PerfGear.SmallPadSize"));
            System.out.println("WHITE185=" + find(text, "DFMDeviceFrame.White185fps"));
            System.out.println("TOKEN_REWRITE_CONSISTENT=" + (ConfigCrypto.decodeIni(engine, key).equals(text)));
            System.out.println("GAME_BYTES=" + game.length);
        }
    }

    static String find(String text, String k) {
        for (String line : text.split("\r?\n")) {
            String t = line.trim();
            if (t.startsWith(k + "=")) return t.substring(k.length() + 1);
        }
        return "<absent>";
    }

    static byte[] hexToBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return b;
    }
}
