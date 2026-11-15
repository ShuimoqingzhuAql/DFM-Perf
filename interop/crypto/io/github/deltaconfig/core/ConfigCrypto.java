package io.github.deltaconfig.core;

import java.io.IOException;
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

/* loaded from: classes.dex */
public final class ConfigCrypto {
    public static final int MAX_INI_BYTES = 2097152;
    public static final long MAX_MATERIAL_AUTH_BYTES = 67108864;

    private ConfigCrypto() {
    }

    public static final class AuthenticatedFormatException extends GeneralSecurityException {
        public AuthenticatedFormatException(String str) {
            super(str);
        }

        public AuthenticatedFormatException(String str, Throwable th) {
            super(str, th);
        }
    }

    public static final class MaterialAuthenticationBudgetException extends IOException {
        public MaterialAuthenticationBudgetException(String str) {
            super(str);
        }
    }

    public static int[] materialEncodingCandidates() {
        int[] iArr = new int[256];
        iArr[0] = 226;
        iArr[1] = 0;
        iArr[2] = 16;
        int i = 3;
        for (int i2 = 0; i2 < 256; i2++) {
            if (i2 != 226 && i2 != 0 && i2 != 16) {
                iArr[i] = i2;
                i++;
            }
        }
        return iArr;
    }

    public static byte[] decodeFixedMask(byte[] bArr) {
        requireKey(bArr);
        byte[] bArr2 = (byte[]) bArr.clone();
        for (int i = 0; i < bArr2.length; i++) {
            bArr2[i] = (byte) (bArr2[i] ^ (-30));
        }
        return bArr2;
    }

    public static byte[] deriveKey(byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        byte[] field = getField(bArr2, "Hash");
        try {
            return deriveKeyFromHash(bArr, field);
        } finally {
            Arrays.fill(field, (byte) 0);
        }
    }

    public static byte[] deriveKeyFromHash(byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        requireKey(bArr);
        Decoded decrypt = decrypt(bArr2, bArr);
        String payloadText = payloadText(decrypt.plaintext, decrypt.length);
        try {
            if (!payloadText.matches("[0-9a-fA-F]{40}")) {
                throw new AuthenticatedFormatException("UserGameplay Hash 已通过 HMAC，但认证摘要不是受支持的 40 位十六进制格式");
            }
            byte[] bArr3 = new byte[20];
            for (int i = 0; i < 20; i++) {
                int i2 = i * 2;
                bArr3[i] = (byte) Integer.parseInt(payloadText.substring(i2, i2 + 2), 16);
            }
            byte[] repeatXor = repeatXor(bArr, bArr3);
            Arrays.fill(bArr3, (byte) 0);
            return repeatXor;
        } finally {
            Arrays.fill(decrypt.plaintext, (byte) 0);
        }
    }

    public static String decodeIni(byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        Decoded decrypt = decrypt(getField(bArr, "Token"), bArr2);
        try {
            return payloadText(decrypt.plaintext, decrypt.length);
        } finally {
            Arrays.fill(decrypt.plaintext, (byte) 0);
        }
    }

    public static byte[] replaceIni(byte[] bArr, String str, byte[] bArr2) throws GeneralSecurityException {
        checkSize(bArr);
        if (str == null || str.length() > 2097120) {
            throw new IllegalArgumentException("配置内容过大或为空");
        }
        byte[] field = getField(bArr, "Token");
        Decoded decrypt = decrypt(field, bArr2);
        try {
            if (payloadText(decrypt.plaintext, decrypt.length).equals(str)) {
                return (byte[]) bArr.clone();
            }
            byte[] encrypt = encrypt(str, bArr2, decrypt.magic);
            Arrays.fill(decrypt.plaintext, (byte) 0);
            String str2 = new String(encrypt, StandardCharsets.US_ASCII);
            if (field.length == 0 || field[field.length - 1] != 61) {
                str2 = str2.replaceAll("=+$", "");
            }
            String str3 = new String(bArr, StandardCharsets.ISO_8859_1);
            Matcher matcher = fieldPattern("Token").matcher(str3);
            if (!matcher.find()) {
                throw new IllegalArgumentException("缺少 Token 字段");
            }
            int start = matcher.start(1);
            int end = matcher.end(1);
            while (start < end && isSpace(str3.charAt(start))) {
                start++;
            }
            while (end > start && isSpace(str3.charAt(end - 1))) {
                end--;
            }
            int i = start + 1;
            if (end > i && str3.charAt(start) == '\"' && str3.charAt(end - 1) == '\"') {
                end--;
                start = i;
            }
            byte[] bytes = (str3.substring(0, start) + str2 + str3.substring(end)).getBytes(StandardCharsets.ISO_8859_1);
            checkSize(bytes);
            if (!decodeIni(bytes, bArr2).equals(str)) {
                throw new GeneralSecurityException("配置重新编码回读不一致");
            }
            return bytes;
        } finally {
            Arrays.fill(decrypt.plaintext, (byte) 0);
        }
    }

    public static byte[] getField(byte[] bArr, String str) {
        checkSize(bArr);
        if (str == null || !str.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("无效的配置字段名");
        }
        Matcher matcher = fieldPattern(str).matcher(new String(bArr, StandardCharsets.ISO_8859_1));
        if (!matcher.find()) {
            throw new IllegalArgumentException("缺少配置字段: " + str);
        }
        String trim = matcher.group(1).trim();
        if (matcher.find()) {
            throw new IllegalArgumentException("配置字段重复: " + str);
        }
        if (trim.length() >= 2 && trim.startsWith("\"") && trim.endsWith("\"")) {
            trim = trim.substring(1, trim.length() - 1);
        }
        if (trim.isEmpty()) {
            throw new IllegalArgumentException("配置字段为空: " + str);
        }
        for (int i = 0; i < trim.length(); i++) {
            if (trim.charAt(i) > 127) {
                throw new IllegalArgumentException("认证字段包含非 ASCII 字符");
            }
        }
        return trim.getBytes(StandardCharsets.US_ASCII);
    }

    private static Pattern fieldPattern(String str) {
        return Pattern.compile("^" + Pattern.quote(str) + "[ \\t]*=[ \\t]*([^\\r\\n]+)", 10);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    private static final class Decoded {
        final int length;
        final int magic;
        final byte[] plaintext;

        Decoded(int i, int i2, byte[] bArr) {
            this.magic = i;
            this.length = i2;
            this.plaintext = bArr;
        }
    }

    private static Decoded decrypt(byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        boolean z;
        requireKey(bArr2);
        checkSize(bArr);
        try {
            byte[] decode = Base64.getDecoder().decode(bArr);
            if (decode.length <= 20 || (decode.length - 20) % 16 != 0) {
                throw new GeneralSecurityException("配置密文块长度不正确");
            }
            byte[] copyOf = Arrays.copyOf(decode, 20);
            byte[] repeatXor = repeatXor(bArr2, copyOf);
            byte[] bArr3 = null;
            try {
                Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(2, new SecretKeySpec(repeatXor, "AES"));
                bArr3 = cipher.doFinal(decode, 20, decode.length - 20);
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(new SecretKeySpec(bArr2, "HmacSHA1"));
                if (!MessageDigest.isEqual(copyOf, mac.doFinal(bArr3))) {
                    throw new GeneralSecurityException("配置认证失败：设备、游戏版本或文件不匹配");
                }
                z = true;
                int i = (bArr3[0] & 255) | ((bArr3[1] & 255) << 8);
                long j = 0;
                for (int i2 = 0; i2 < 4; i2++) {
                    j |= (bArr3[i2 + 2] & 255) << (i2 * 8);
                }
                if ((i != 17665 && i != 17666) || j > bArr3.length - 6) {
                    throw new AuthenticatedFormatException("配置已通过 HMAC，但认证头的版本或长度不受支持");
                }
                try {
                    Decoded decoded = new Decoded(i, (int) j, bArr3);
                    Arrays.fill(repeatXor, (byte) 0);
                    Arrays.fill(decode, (byte) 0);
                    return decoded;
                } catch (Throwable th) {
                    th = th;
                    Arrays.fill(repeatXor, (byte) 0);
                    Arrays.fill(decode, (byte) 0);
                    if (!z && bArr3 != null) {
                        Arrays.fill(bArr3, (byte) 0);
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                if (th2 instanceof GeneralSecurityException) {
                    throw (GeneralSecurityException) th2;
                }
                throw new GeneralSecurityException("配置解密失败", th2);
            }
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("配置认证封装不是有效的 Base64", e);
        }
    }

    private static byte[] encrypt(String str, byte[] bArr, int i) throws GeneralSecurityException {
        requireKey(bArr);
        int length = str.length();
        byte[] bArr2 = new byte[(length + 6 + 15) & (-16)];
        bArr2[0] = (byte) i;
        bArr2[1] = (byte) (i >>> 8);
        for (int i2 = 0; i2 < 4; i2++) {
            bArr2[i2 + 2] = (byte) (length >>> (i2 * 8));
        }
        for (int i3 = 0; i3 < length; i3++) {
            try {
                char charAt = str.charAt(i3);
                if (charAt < 1 || charAt > 256) {
                    throw new IllegalArgumentException("配置包含该游戏编码无法表示的字符");
                }
                bArr2[i3 + 6] = (byte) (charAt - 1);
            } catch (Throwable th) {
                Arrays.fill(bArr2, (byte) 0);
                if (0 != 0) {
                    Arrays.fill((byte[]) null, (byte) 0);
                }
                throw th;
            }
        }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(bArr, "HmacSHA1"));
        byte[] doFinal = mac.doFinal(bArr2);
        byte[] repeatXor = repeatXor(bArr, doFinal);
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(1, new SecretKeySpec(repeatXor, "AES"));
        byte[] doFinal2 = cipher.doFinal(bArr2);
        byte[] bArr3 = new byte[doFinal2.length + 20];
        System.arraycopy(doFinal, 0, bArr3, 0, 20);
        System.arraycopy(doFinal2, 0, bArr3, 20, doFinal2.length);
        byte[] encode = Base64.getEncoder().encode(bArr3);
        Arrays.fill(bArr2, (byte) 0);
        if (repeatXor != null) {
            Arrays.fill(repeatXor, (byte) 0);
        }
        return encode;
    }

    private static String payloadText(byte[] bArr, int i) {
        char[] cArr = new char[i];
        for (int i2 = 0; i2 < i; i2++) {
            cArr[i2] = (char) ((bArr[i2 + 6] & 255) + 1);
        }
        return new String(cArr);
    }

    private static byte[] repeatXor(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[32];
        for (int i = 0; i < 32; i++) {
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i % bArr2.length]);
        }
        return bArr3;
    }

    private static void requireKey(byte[] bArr) {
        if (bArr == null || bArr.length != 32) {
            throw new IllegalArgumentException("认证材料必须为 32 字节");
        }
    }

    private static void checkSize(byte[] bArr) {
        if (bArr == null || bArr.length == 0 || bArr.length > 2097152) {
            throw new IllegalArgumentException("配置文件为空或超过 2 MiB 限制");
        }
    }
}
