package io.github.dfmperf.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在 libUE4.so 中定位 32 字节认证材料。
 * 1) 先用已知版本的固定偏移（按库的 SHA-256 查表）；
 * 2) 未知版本则扫描可执行段，识别 ARM64 的 ADRP/LDR q 常量装载桩；
 * 3) 任何候选都必须通过 UserGameplay.Hash 的 HMAC 门禁，否则视为无效。
 */
public final class Material {
    /** 已知 libUE4.so SHA-256 -> {第一段偏移, 第二段偏移} */
    private static final Map<String, long[]> KNOWN = new HashMap<>();
    static {
        KNOWN.put("686577e4acf4bdd45b113b579d72dd581e9beffb6f6d04d07871b0341ff470c1",
                new long[]{0x1559300L, 0x157d9c0L});
    }

    public final byte[] material;   // 32 字节
    public final int encoding;      // 材料整体 XOR 的单字节编码
    public final byte[] key;        // 由 UserGameplay 派生的配置密钥
    public final String source;     // 说明：已知偏移 / 扫描偏移

    private Material(byte[] material, int encoding, byte[] key, String source) {
        this.material = material;
        this.encoding = encoding;
        this.key = key;
        this.source = source;
    }

    public static Material resolve(File libUE4, byte[] gameplayIni) throws Exception {
        if (!libUE4.isFile()) throw new IOException("找不到 libUE4.so: " + libUE4);
        String sha = sha256(libUE4);
        long[] off = KNOWN.get(sha);
        List<long[]> candidates = new ArrayList<>();
        if (off != null) candidates.add(off);
        for (long[] c : candidates) {
            Material m = tryOffsets(libUE4, c[0], c[1], gameplayIni, "已知偏移 " + Long.toHexString(c[0]) + "/" + Long.toHexString(c[1]));
            if (m != null) return m;
        }
        // 固定偏移不可用：扫描
        List<long[]> found = scan(libUE4);
        Material result = null;
        for (long[] c : found) {
            Material m = tryOffsets(libUE4, c[0], c[1], gameplayIni, "扫描偏移 " + Long.toHexString(c[0]) + "/" + Long.toHexString(c[1]));
            if (m == null) continue;
            if (result != null && !MessageDigest.isEqual(result.material, m.material)) {
                throw new GeneralSecurityException("认证材料存在歧义：多个候选通过认证，已停止");
            }
            if (result == null) result = m;
        }
        if (result == null) {
            throw new GeneralSecurityException("已定位 ARM64 候选，但配置 HMAC 未匹配；请核对同设备配置与游戏库");
        }
        return result;
    }

    private static final int[] ENCODINGS = buildEncodings();

    private static int[] buildEncodings() {
        int[] e = new int[256];
        e[0] = 226;
        e[1] = 0;
        e[2] = 16;
        int i = 3;
        for (int v = 0; v < 256; v++) {
            if (v != 226 && v != 0 && v != 16) e[i++] = v;
        }
        return e;
    }

    private static Material tryOffsets(File lib, long o1, long o2, byte[] gameplay, String source) throws Exception {
        byte[] raw = new byte[32];
        if (!readAt(lib, o1, raw, 0, 16)) return null;
        if (!readAt(lib, o2, raw, 16, 16)) return null;
        for (int enc : ENCODINGS) {
            byte[] m = raw.clone();
            if (enc != 0) for (int i = 0; i < 32; i++) m[i] ^= (byte) enc;
            try {
                byte[] key = ConfigCrypto.deriveKey(m, gameplay);
                return new Material(m, enc, key, source);
            } catch (Exception ignored) {
                // 该编码不匹配，继续尝试
            }
        }
        return null;
    }

    private static boolean readAt(File f, long off, byte[] dst, int dstOff, int len) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            if (off < 0 || off + len > raf.length()) return false;
            raf.seek(off);
            raf.readFully(dst, dstOff, len);
            return true;
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[1 << 20];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return ConfigCrypto.bytesToHex(md.digest());
    }

    // ---------------------------------------------------------------- ELF 扫描
    private static final class Seg {
        long off, va, filesz;
        int flags;
    }

    /** 返回 {first, second} 文件偏移列表（尚未认证）。 */
    public static List<long[]> scan(File lib) throws Exception {
        List<Seg> exec = new ArrayList<>();
        List<Seg> all = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(lib, "r")) {
            byte[] hdr = new byte[64];
            raf.readFully(hdr);
            if (!(hdr[0] == 0x7f && hdr[1] == 'E' && hdr[2] == 'L' && hdr[3] == 'F' && hdr[4] == 2 && hdr[5] == 1)) {
                throw new IOException("游戏库不是 ELF64 小端文件");
            }
            if (u16(hdr, 18) != 183) throw new IOException("游戏库不是 ARM64");
            long phoff = u64(hdr, 32);
            int phentsize = u16(hdr, 54);
            int phnum = u16(hdr, 56);
            byte[] ph = new byte[phentsize * phnum];
            raf.seek(phoff);
            raf.readFully(ph);
            for (int i = 0; i < phnum; i++) {
                int o = i * phentsize;
                if (u32(ph, o) != 1) continue;
                Seg s = new Seg();
                s.flags = u32(ph, o + 4);
                s.off = u64(ph, o + 8);
                s.va = u64(ph, o + 16);
                s.filesz = u64(ph, o + 32);
                all.add(s);
                if ((s.flags & 1) != 0 && s.filesz > 0) exec.add(s);
            }
        }
        List<long[]> out = new ArrayList<>();
        for (Seg s : exec) scanSegment(lib, s, all, out);
        return out;
    }

    private static void scanSegment(File lib, Seg seg, List<Seg> all, List<long[]> out) throws IOException {
        final int chunk = 1 << 20;
        try (RandomAccessFile raf = new RandomAccessFile(lib, "r")) {
            byte[] carry = new byte[0];
            long done = 0;
            while (done < seg.filesz) {
                int want = (int) Math.min(chunk, seg.filesz - done);
                byte[] buf = new byte[want];
                raf.seek(seg.off + done);
                raf.readFully(buf);
                byte[] data = new byte[carry.length + buf.length];
                System.arraycopy(carry, 0, data, 0, carry.length);
                System.arraycopy(buf, 0, data, carry.length, buf.length);
                long dataAddr = seg.va + done - carry.length;
                int start = (int) ((4 - (dataAddr & 3)) & 3);
                for (int i = start; i + 32 <= data.length; i += 4) {
                    long pc = dataAddr + i;
                    long[] va = match(data, i, pc);
                    if (va == null) continue;
                    long f1 = vaToOffset(all, va[0], 16);
                    long f2 = vaToOffset(all, va[1], 16);
                    if (f1 < 0 || f2 < 0) continue;
                    boolean dup = false;
                    for (long[] e : out) if (e[0] == f1 && e[1] == f2) { dup = true; break; }
                    if (!dup) out.add(new long[]{f1, f2});
                    if (out.size() > 64) throw new IOException("候选超过 64 个，布局存在歧义");
                }
                int keep = Math.min(28, data.length);
                carry = new byte[keep];
                System.arraycopy(data, data.length - keep, carry, 0, keep);
                done += want;
            }
        }
    }

    /** 识别 32 字节窗口；成功返回两个常量地址。 */
    private static long[] match(byte[] d, int i, long pc) {
        int w4 = i32(d, i + 16);
        int key = 0x01010101 * (((w4 >>> 24) ^ 0x52) & 0xFF);
        if (((w4 ^ key) & 0xFFFFFFE0) != 0x52800020) return null;
        int[] w = new int[8];
        for (int k = 0; k < 8; k++) w[k] = i32(d, i + k * 4) ^ key;

        int adrp1, ldr1, adrp2, ldr2;
        long pc2;
        if (adrp(w[0]) && adrp(w[1]) && loadQ(w[2]) && loadQ(w[3])) {
            adrp1 = w[0]; ldr1 = w[2]; adrp2 = w[1]; ldr2 = w[3];
            pc2 = pc + 4;
        } else if (adrp(w[0]) && loadQ(w[1]) && adrp(w[2]) && loadQ(w[3])) {
            adrp1 = w[0]; ldr1 = w[1]; adrp2 = w[2]; ldr2 = w[3];
            pc2 = pc + 8;
        } else {
            return null;
        }
        if (((ldr1 >>> 5) & 31) != (adrp1 & 31)) return null;
        if (((ldr2 >>> 5) & 31) != (adrp2 & 31)) return null;
        if ((w[5] & 0xFFC00000) != 0x39000000) return null;          // STRB
        if ((w[5] & 31) != (w[4] & 31)) return null;
        if ((w[6] & 0xFFC00000) != 0xAD000000) return null;          // STP q
        if ((w[7] & 0xFC000000) != 0x14000000) return null;          // B
        int i3 = ldr1 & 31, i4 = ldr2 & 31;
        int i5 = w[6] & 31, i6 = (w[6] >>> 10) & 31;
        if (i3 == i4) return null;
        if (!((i5 == i3 && i6 == i4) || (i5 == i4 && i6 == i3))) return null;
        int disp = (int) (signExtend(w[7] & 0x3FFFFFF, 26) << 2);
        if (disp >= 0 || disp < -4096) return null;

        long a1 = adrpAddress(adrp1, pc) + ((long) ((ldr1 >>> 10) & 0xFFF)) * 16;
        long a2 = adrpAddress(adrp2, pc2) + ((long) ((ldr2 >>> 10) & 0xFFF)) * 16;
        return (i5 == i3) ? new long[]{a1, a2} : new long[]{a2, a1};
    }

    /** 虚拟地址 -> 文件偏移（唯一、非可写映射）。 */
    private static long vaToOffset(List<Seg> all, long va, int len) {
        long found = -1;
        for (Seg s : all) {
            if (va < s.va) continue;
            long delta = va - s.va;
            if (delta + len > s.filesz) continue;
            if ((s.flags & 2) != 0) return -1;
            long off = s.off + delta;
            if (found >= 0 && found != off) return -1;
            found = off;
        }
        return found;
    }

    private static boolean adrp(int w) { return (w & 0x9F000000) == 0x90000000; }
    private static boolean loadQ(int w) { return (w & 0xFFC00000) == 0x3DC00000; }

    private static long adrpAddress(int w, long pc) {
        long imm = ((w >>> 29) & 3) | (((w >>> 5) & 0x7FFFF) << 2);
        imm = signExtend(imm, 21) << 12;
        return (pc & ~0xFFFL) + imm;
    }

    private static long signExtend(long v, int bits) {
        long m = 1L << (bits - 1);
        v &= (1L << bits) - 1;
        return (v ^ m) - m;
    }

    private static int u16(byte[] b, int o) { return (b[o] & 255) | ((b[o + 1] & 255) << 8); }

    private static int u32(byte[] b, int o) {
        return (b[o] & 255) | ((b[o + 1] & 255) << 8) | ((b[o + 2] & 255) << 16) | ((b[o + 3] & 255) << 24);
    }

    private static long u64(byte[] b, int o) {
        return (u32(b, o) & 0xFFFFFFFFL) | ((long) u32(b, o + 4) << 32);
    }

    private static int i32(byte[] b, int o) { return u32(b, o); }
}
