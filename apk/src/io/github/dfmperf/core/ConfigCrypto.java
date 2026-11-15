package io.github.dfmperf.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 配置容器格式（与游戏/DeltaConfig 一致）：
 *   字段值 = Base64( HMAC-SHA1(block)[20] || AES-256-ECB(block) )
 *   block  = magic:u16LE | length:u32LE | payload(每个字节 = 字符-1) | 0 填充
 *   AES key = key32[i] XOR tag[i % 20]
 *
 * 本文件不做任何混淆/加密，方便阅读与二次分析。
 */
public final class ConfigCrypto {
    public static final int MAX_INI_BYTES = 2 * 1024 * 1024;
    private static final Pattern FIELD = Pattern.compile("^%s[ \\t]*=[ \\t]*([^\\r\\n]+)", Pattern.MULTILINE);

    private ConfigCrypto() {}

    public static final class Decoded {
        public final int magic;
        public final int length;
        public final byte[] plaintext;
        Decoded(int magic, int length, byte[] plaintext) {
            this.magic = magic;
            this.length = length;
            this.plaintext = plaintext;
        }
    }

    public static byte[] getField(byte[] ini, String name) {
        checkSize(ini);
        Matcher m = Pattern.compile(String.format(FIELD.pattern(), Pattern.quote(name)), Pattern.MULTILINE).matcher(
                new String(ini, StandardCharsets.ISO_8859_1));
        if (!m.find()) throw new IllegalArgumentException("缺少配置字段: " + name);
        String v = m.group(1).trim();
        if (m.find()) throw new IllegalArgumentException("配置字段重复: " + name);
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        if (v.isEmpty()) throw new IllegalArgumentException("配置字段为空: " + name);
        for (int i = 0; i < v.length(); i++) {
            if (v.charAt(i) > 127) throw new IllegalArgumentException("认证字段包含非 ASCII 字符");
        }
        return v.getBytes(StandardCharsets.US_ASCII);
    }

    public static Decoded decrypt(byte[] fieldValue, byte[] key32) throws GeneralSecurityException {
        requireKey(key32);
        checkSize(fieldValue);
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(fieldValue);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("配置认证封装不是有效的 Base64", e);
        }
        if (raw.length <= 20 || (raw.length - 20) % 16 != 0) {
            throw new GeneralSecurityException("配置密文块长度不正确");
        }
        byte[] tag = Arrays.copyOf(raw, 20);
        byte[] aesKey = repeatXor(key32, tag);
        byte[] plain;
        try {
            Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"));
            plain = c.doFinal(raw, 20, raw.length - 20);
        } finally {
            Arrays.fill(aesKey, (byte) 0);
            Arrays.fill(raw, (byte) 0);
        }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key32, "HmacSHA1"));
        if (!MessageDigest.isEqual(tag, mac.doFinal(plain))) {
            Arrays.fill(plain, (byte) 0);
            throw new GeneralSecurityException("配置认证失败：设备、游戏版本或文件不匹配");
        }
        int magic = (plain[0] & 255) | ((plain[1] & 255) << 8);
        long len = 0;
        for (int i = 0; i < 4; i++) len |= (long) (plain[i + 2] & 255) << (i * 8);
        if ((magic != 17665 && magic != 17666) || len > plain.length - 6) {
            throw new GeneralSecurityException("配置已通过 HMAC，但认证头的版本或长度不受支持");
        }
        return new Decoded(magic, (int) len, plain);
    }

    public static String payloadText(Decoded d) {
        char[] out = new char[d.length];
        for (int i = 0; i < d.length; i++) out[i] = (char) ((d.plaintext[i + 6] & 255) + 1);
        return new String(out);
    }

    public static String decodeIni(byte[] engineIni, byte[] key32) throws GeneralSecurityException {
        Decoded d = decrypt(getField(engineIni, "Token"), key32);
        try {
            return payloadText(d);
        } finally {
            Arrays.fill(d.plaintext, (byte) 0);
        }
    }

    public static byte[] encrypt(String text, byte[] key32, int magic) throws GeneralSecurityException {
        requireKey(key32);
        int len = text.length();
        byte[] block = new byte[(len + 6 + 15) & ~15];
        block[0] = (byte) magic;
        block[1] = (byte) (magic >>> 8);
        for (int i = 0; i < 4; i++) block[i + 2] = (byte) (len >>> (i * 8));
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (ch < 1 || ch > 256) {
                Arrays.fill(block, (byte) 0);
                throw new IllegalArgumentException("配置包含该游戏编码无法表示的字符");
            }
            block[i + 6] = (byte) (ch - 1);
        }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key32, "HmacSHA1"));
        byte[] tag = mac.doFinal(block);
        byte[] aesKey = repeatXor(key32, tag);
        Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"));
        byte[] ct = c.doFinal(block);
        byte[] out = new byte[20 + ct.length];
        System.arraycopy(tag, 0, out, 0, 20);
        System.arraycopy(ct, 0, out, 20, ct.length);
        Arrays.fill(block, (byte) 0);
        Arrays.fill(aesKey, (byte) 0);
        return Base64.getEncoder().encode(out);
    }

    /** 用新明文替换 Engine.ini 中的 Token 字段（保留其余字节与行尾）。 */
    public static byte[] replaceIni(byte[] engineIni, String newText, byte[] key32) throws GeneralSecurityException {
        checkSize(engineIni);
        Decoded d = decrypt(getField(engineIni, "Token"), key32);
        int magic;
        String old;
        try {
            old = payloadText(d);
            magic = d.magic;
        } finally {
            Arrays.fill(d.plaintext, (byte) 0);
        }
        if (old.equals(newText)) return engineIni.clone();

        String b64 = new String(encrypt(newText, key32, magic), StandardCharsets.US_ASCII);
        byte[] field = getField(engineIni, "Token");
        boolean padded = field.length > 0 && field[field.length - 1] == '=';
        if (!padded) b64 = b64.replaceAll("=+$", "");

        String text = new String(engineIni, StandardCharsets.ISO_8859_1);
        Matcher m = Pattern.compile(String.format(FIELD.pattern(), "Token"), Pattern.MULTILINE).matcher(text);
        if (!m.find()) throw new IllegalArgumentException("缺少 Token 字段");
        int start = m.start(1), end = m.end(1);
        while (start < end && isSpace(text.charAt(start))) start++;
        while (end > start && isSpace(text.charAt(end - 1))) end--;
        if (end > start + 1 && text.charAt(start) == '"' && text.charAt(end - 1) == '"') {
            start++;
            end--;
        }
        byte[] out = (text.substring(0, start) + b64 + text.substring(end)).getBytes(StandardCharsets.ISO_8859_1);
        checkSize(out);
        if (!decodeIni(out, key32).equals(newText)) {
            throw new GeneralSecurityException("配置重新编码回读不一致");
        }
        return out;
    }

    /** key32 = material XOR (40位hex摘要解码后的20字节，循环) */
    public static byte[] deriveKey(byte[] material32, byte[] gameplayIni) throws GeneralSecurityException {
        byte[] field = getField(gameplayIni, "Hash");
        Decoded d = decrypt(field, material32);
        String hex;
        try {
            hex = payloadText(d);
        } finally {
            Arrays.fill(d.plaintext, (byte) 0);
        }
        if (!hex.matches("[0-9a-fA-F]{40}")) {
            throw new GeneralSecurityException("UserGameplay Hash 认证通过，但摘要不是 40 位十六进制");
        }
        byte[] digest = new byte[20];
        for (int i = 0; i < 20; i++) {
            digest[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return repeatXor(material32, digest);
    }

    public static byte[] repeatXor(byte[] key, byte[] mask) {
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) out[i] = (byte) (key[i] ^ mask[i % mask.length]);
        return out;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    private static void requireKey(byte[] key) {
        if (key == null || key.length != 32) throw new IllegalArgumentException("认证材料必须为 32 字节");
    }

    private static void checkSize(byte[] b) {
        if (b == null || b.length == 0 || b.length > MAX_INI_BYTES) {
            throw new IllegalArgumentException("配置文件为空或超过 2 MiB 限制");
        }
    }

    public static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    public static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 255));
        return sb.toString();
    }

    public static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
