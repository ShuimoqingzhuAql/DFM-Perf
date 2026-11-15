package io.github.deltaconfig.core;

import android.R;
import io.github.deltaconfig.core.ConfigCrypto;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/* loaded from: classes.dex */
public final class LibraryMaskResolver {
    private static final long LEGACY_FIRST = 24240560;
    private static final long LEGACY_SECOND = 24385264;
    private static final int MAX_CANDIDATES = 64;
    private static final long MAX_SCAN_BYTES = 536870912;
    private static final int MAX_TABLE_ENTRIES = 1024;
    public static final int READ_BLOCK_BYTES = 1048576;

    public interface Source {
        long length() throws Exception;

        byte[] read(long j, int i) throws Exception;

        default boolean efficientRandomAccess() {
            return false;
        }
    }

    public static final class Result {
        public final int codeEncoding;
        public final String description;
        public final long firstOffset;
        public final byte[] mask;
        public final int materialEncoding;
        public final long secondOffset;

        private Result(byte[] bArr, String str, long j, long j2, int i, int i2) {
            this.mask = (byte[]) bArr.clone();
            this.description = str;
            this.firstOffset = j;
            this.secondOffset = j2;
            this.materialEncoding = i;
            this.codeEncoding = i2;
        }
    }

    private LibraryMaskResolver() {
    }

    public static Result resolve(Source source, byte[] bArr, byte[] bArr2) throws Exception {
        return resolve(source, bArr, bArr2, true);
    }

    public static Result resolveByCode(Source source, byte[] bArr, byte[] bArr2) throws Exception {
        return resolve(source, bArr, bArr2, false);
    }

    private static Result resolve(Source source, byte[] bArr, byte[] bArr2, boolean z) throws Exception {
        if (source == null || bArr == null) {
            throw new IllegalArgumentException("缺少游戏库或认证配置");
        }
        byte[] field = ConfigCrypto.getField(bArr, "Hash");
        if (bArr2 != null) {
            try {
                Arrays.fill(ConfigCrypto.getField(bArr2, "Token"), (byte) 0);
            } finally {
                Arrays.fill(field, (byte) 0);
            }
        }
        return resolveWithHash(source, field, bArr2, z);
    }

    /* JADX WARN: Unreachable blocks removed: 2, instructions: 2 */
    private static Result resolveWithHash(Source source, byte[] bArr, byte[] bArr2, boolean z) throws Exception {
        Result result;
        byte[] bArr3;
        AuthenticationBudget authenticationBudget = new AuthenticationBudget(bArr.length);
        Reader reader = new Reader(source);
        Elf parse = Elf.parse(reader);
        if (z && range(LEGACY_FIRST, 16L, reader.length) && range(LEGACY_SECOND, 16L, reader.length)) {
            byte[] join = join(reader.read(LEGACY_FIRST, 16), reader.read(LEGACY_SECOND, 16));
            try {
                Result authenticate = authenticate(join, bArr, bArr2, LEGACY_FIRST, LEGACY_SECOND, "已认证的兼容区段", -1, authenticationBudget);
                if (authenticate != null) {
                    Arrays.fill(join, (byte) 0);
                    return authenticate;
                }
                Arrays.fill(join, (byte) 0);
            } catch (Throwable th) {
                Arrays.fill(join, (byte) 0);
                throw th;
            }
        }
        List<Candidate> discover = discover(reader, parse);
        if (discover.isEmpty()) {
            throw new GeneralSecurityException("认证算法或游戏库布局未识别：未找到受支持的 ARM64 材料装载结构");
        }
        TreeMap treeMap = new TreeMap();
        Iterator<Candidate> it = discover.iterator();
        while (true) {
            result = null;
            if (it.hasNext()) {
                Candidate next = it.next();
                treeMap.put(Long.valueOf(next.first), null);
                treeMap.put(Long.valueOf(next.second), null);
            } else {
                try {
                    break;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        for (Map.Entry entry : treeMap.entrySet()) {
            entry.setValue(reader.read(((Long) entry.getKey()).longValue(), 16));
        }
        Result result2 = null;
        for (Candidate candidate : discover) {
            try {
                byte[] join2 = join((byte[]) treeMap.get(Long.valueOf(candidate.first)), (byte[]) treeMap.get(Long.valueOf(candidate.second)));
                try {
                    bArr3 = join2;
                    try {
                        Result authenticate2 = authenticate(join2, bArr, bArr2, candidate.first, candidate.second, "ELF ARM64 指令重定位（" + encodingDescription(candidate.xor & 255) + "）", candidate.xor & 255, authenticationBudget);
                        if (authenticate2 != null) {
                            if (result2 != null) {
                                boolean isEqual = MessageDigest.isEqual(result2.mask, authenticate2.mask);
                                Arrays.fill(authenticate2.mask, (byte) 0);
                                if (!isEqual) {
                                    Arrays.fill(result2.mask, (byte) 0);
                                    throw new GeneralSecurityException("认证材料存在歧义：多个不同候选通过认证，已停止操作");
                                }
                            } else {
                                result2 = authenticate2;
                            }
                            Arrays.fill(bArr3, (byte) 0);
                        } else {
                            Arrays.fill(bArr3, (byte) 0);
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        Arrays.fill(bArr3, (byte) 0);
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    bArr3 = join2;
                }
            } catch (Throwable th5) {
                th = th5;
                result = result2;
                for (byte[] bArr4 : treeMap.values()) {
                    if (bArr4 != null) {
                        Arrays.fill(bArr4, (byte) 0);
                    }
                }
                if (result != null) {
                    Arrays.fill(result.mask, (byte) 0);
                }
                throw th;
            }
        }
        if (result2 == null) {
            throw new GeneralSecurityException("已定位 ARM64 候选，但配置 HMAC 未匹配；请核对同设备配套配置及游戏库");
        }
        for (byte[] bArr5 : treeMap.values()) {
            if (bArr5 != null) {
                Arrays.fill(bArr5, (byte) 0);
            }
        }
        return result2;
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x002d, code lost:
    
        if (r19 == null) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0061, code lost:
    
        r2 = new java.lang.StringBuilder().append(r24).append("；材料").append(encodingDescription(r14)).append("；");
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0082, code lost:
    
        if (r19 != null) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0084, code lost:
    
        r1 = "Hash 认证通过";
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0089, code lost:
    
        r9 = r2.append(r1).toString();
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0091, code lost:
    
        r1 = r15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x009c, code lost:
    
        r0 = new io.github.deltaconfig.core.LibraryMaskResolver.Result(r15, r9, r20, r22, r14, r25, null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x009f, code lost:
    
        java.util.Arrays.fill(r1, (byte) 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x00a2, code lost:
    
        if (r6 == null) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x00a4, code lost:
    
        java.util.Arrays.fill(r6, (byte) 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x00a7, code lost:
    
        return r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00a8, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00ac, code lost:
    
        r9 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0087, code lost:
    
        r1 = "Hash 与 Engine 双重认证通过";
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x002f, code lost:
    
        io.github.deltaconfig.core.ConfigCrypto.decodeIni(r19, r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0042, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0060, code lost:
    
        throw new io.github.deltaconfig.core.ConfigCrypto.AuthenticatedFormatException("Engine 认证通过，但配置协议发生变化：" + r0.getMessage(), r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0037, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0041, code lost:
    
        throw new java.security.GeneralSecurityException("UserGameplay 认证已匹配，但 Engine HMAC 不匹配；请核对本机配套配置", r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0039, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00e2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static Result authenticate(byte[] bArr, byte[] bArr2, byte[] bArr3, long j, long j2, String str, int i, AuthenticationBudget authenticationBudget) throws Exception {
        byte[] bArr4;
        byte[] bArr5;
        int[] materialEncodingCandidates = ConfigCrypto.materialEncodingCandidates();
        int length = materialEncodingCandidates.length;
        int i2 = 0;
        while (true) {
            byte[] bArr6 = null;
            if (i2 >= length) {
                return null;
            }
            int i3 = materialEncodingCandidates[i2];
            authenticationBudget.consume();
            bArr4 = (byte[]) bArr.clone();
            for (int i4 = 0; i4 < bArr4.length; i4++) {
                bArr4[i4] = (byte) (bArr4[i4] ^ ((byte) i3));
            }
            try {
                try {
                    bArr6 = ConfigCrypto.deriveKeyFromHash(bArr4, bArr2);
                    try {
                        break;
                    } catch (Throwable th) {
                        th = th;
                        bArr5 = bArr4;
                        Arrays.fill(bArr5, (byte) 0);
                        if (bArr6 != null) {
                            Arrays.fill(bArr6, (byte) 0);
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    byte[] bArr7 = bArr4;
                }
            } catch (ConfigCrypto.AuthenticatedFormatException e) {
                bArr5 = bArr4;
                try {
                    throw new ConfigCrypto.AuthenticatedFormatException("UserGameplay Hash 认证通过，但配置协议发生变化：" + e.getMessage(), e);
                } catch (Throwable th3) {
                    th = th3;
                    Arrays.fill(bArr5, (byte) 0);
                    if (bArr6 != null) {
                    }
                    throw th;
                }
            } catch (IllegalArgumentException e2) {
            } catch (GeneralSecurityException e3) {
            }
            Arrays.fill(bArr4, (byte) 0);
            i2++;
        }
    }

    private static String encodingDescription(int i) {
        return i == 0 ? "原始编码" : "单字节 XOR 0x" + Integer.toHexString(i);
    }

    private static final class AuthenticationBudget {
        final int fieldBytes;
        long remaining = ConfigCrypto.MAX_MATERIAL_AUTH_BYTES;

        AuthenticationBudget(int i) {
            this.fieldBytes = i;
        }

        void consume() throws IOException {
            if (this.fieldBytes > this.remaining) {
                throw new ConfigCrypto.MaterialAuthenticationBudgetException("配置认证候选计算超过 64 MiB 预算，已停止自动识别");
            }
            this.remaining -= this.fieldBytes;
        }
    }

    private static List<Candidate> discover(Reader reader, Elf elf) throws Exception {
        long j;
        byte[] bArr;
        int i;
        int i2;
        int i3;
        elf.prepareCode(reader);
        long j2 = 0;
        for (Region region : elf.code) {
            if (region.size > MAX_SCAN_BYTES - j2) {
                throw new IOException("游戏库可执行区域超过自动识别的 512 MiB 扫描上限");
            }
            j2 += region.size;
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Region region2 : elf.code) {
            int i4 = 0;
            byte[] bArr2 = new byte[0];
            long j3 = 0;
            while (j3 < region2.size) {
                int min = (int) Math.min(1048576L, region2.size - j3);
                byte[] read = reader.read(region2.offset + j3, min);
                int length = bArr2.length + read.length;
                byte[] bArr3 = new byte[length];
                System.arraycopy(bArr2, i4, bArr3, i4, bArr2.length);
                System.arraycopy(read, i4, bArr3, bArr2.length, read.length);
                long length2 = j3 - bArr2.length;
                int i5 = (int) ((4 - ((region2.address + length2) & 3)) & 3);
                while (i5 + 32 <= length) {
                    int int32 = int32(bArr3, i5 + 16);
                    int i6 = R.attr.cacheColorHint * ((int32 >>> 24) ^ 82);
                    if (((int32 ^ i6) & (-32)) != 1384120352) {
                        i = i5;
                        i2 = length;
                        j = j3;
                        bArr = bArr3;
                        i3 = min;
                    } else {
                        j = j3;
                        bArr = bArr3;
                        i = i5;
                        i2 = length;
                        i3 = min;
                        Candidate candidate = candidate(bArr3, i5, i5 + region2.address + length2, i6, elf);
                        if (candidate != null) {
                            linkedHashMap.put(candidate.first + ":" + candidate.second, candidate);
                            if (linkedHashMap.size() > MAX_CANDIDATES) {
                                throw new IOException("游戏库结构候选超过 64 个，布局存在歧义；已停止自动识别");
                            }
                        } else {
                            continue;
                        }
                    }
                    i5 = i + 4;
                    length = i2;
                    min = i3;
                    bArr3 = bArr;
                    j3 = j;
                }
                int i7 = length;
                bArr2 = Arrays.copyOfRange(bArr3, i7 - Math.min(28, i7), i7);
                j3 += min;
                i4 = 0;
            }
        }
        return new ArrayList(linkedHashMap.values());
    }

    private static Candidate candidate(byte[] bArr, int i, long j, int i2, Elf elf) throws IOException {
        long j2;
        int int32 = int32(bArr, i) ^ i2;
        int int322 = int32(bArr, i + 4) ^ i2;
        int int323 = int32(bArr, i + 8) ^ i2;
        int int324 = int32(bArr, i + 12) ^ i2;
        int int325 = int32(bArr, i + 16) ^ i2;
        int int326 = int32(bArr, i + 20) ^ i2;
        int int327 = int32(bArr, i + 24) ^ i2;
        int int328 = int32(bArr, i + 28) ^ i2;
        if (adrp(int32) && adrp(int322) && loadQ(int323) && loadQ(int324)) {
            j2 = 4 + j;
            int323 = int322;
            int322 = int323;
        } else {
            if (!adrp(int32) || !loadQ(int322) || !adrp(int323) || !loadQ(int324)) {
                return null;
            }
            j2 = 8 + j;
        }
        if (((int322 >>> 5) & 31) != (int32 & 31) || ((int324 >>> 5) & 31) != (int323 & 31) || (int325 & (-32)) != 1384120352 || (int326 & (-4194304)) != 956301312 || (int326 & 31) != (int325 & 31) || (int327 & (-4194304)) != -1392508928 || ((-67108864) & int328) != 335544320) {
            return null;
        }
        int i3 = int322 & 31;
        int i4 = int324 & 31;
        int i5 = int327 & 31;
        int i6 = (int327 >>> 10) & 31;
        if (i3 == i4 || !((i5 == i3 && i6 == i4) || (i5 == i4 && i6 == i3))) {
            return null;
        }
        long signExtend = signExtend(int328 & 67108863, 26) << 2;
        if (signExtend >= 0 || signExtend < -4096) {
            return null;
        }
        long add = add(adprAddress(int32, j), ((int322 >>> 10) & 4095) * 16);
        long add2 = add(adprAddress(int323, j2), ((int324 >>> 10) & 4095) * 16);
        long dataOffset = elf.dataOffset(add, 16);
        long dataOffset2 = elf.dataOffset(add2, 16);
        if (dataOffset < 0 || dataOffset2 < 0) {
            return null;
        }
        return i5 == i3 ? new Candidate(dataOffset, dataOffset2, i2) : new Candidate(dataOffset2, dataOffset, i2);
    }

    private static boolean adrp(int i) {
        return (i & (-1627389952)) == -1879048192;
    }

    private static boolean loadQ(int i) {
        return (i & (-4194304)) == 1035993088;
    }

    private static long adprAddress(int i, long j) throws IOException {
        return add(j & (-4096), signExtend(((i >>> 29) & 3) | (((i >>> 5) & 524287) << 2), 21) << 12);
    }

    private static long signExtend(long j, int i) {
        int i2 = 64 - i;
        return (j << i2) >> i2;
    }

    private static byte[] join(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[32];
        System.arraycopy(bArr, 0, bArr3, 0, 16);
        System.arraycopy(bArr2, 0, bArr3, 16, 16);
        return bArr3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean range(long j, long j2, long j3) {
        return j >= 0 && j2 >= 0 && j <= j3 && j2 <= j3 - j;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long add(long j, long j2) throws IOException {
        try {
            long addExact = Math.addExact(j, j2);
            if (addExact >= 0) {
                return addExact;
            }
            throw new ArithmeticException();
        } catch (ArithmeticException e) {
            throw new IOException("ELF 地址计算越界", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int int32(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (bArr[i] & 255) | ((bArr[i + 1] & 255) << 8) | ((bArr[i + 2] & 255) << 16);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long u32(byte[] bArr, int i) {
        return int32(bArr, i) & 4294967295L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int u16(byte[] bArr, int i) {
        return ((bArr[i + 1] & 255) << 8) | (bArr[i] & 255);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static long u64(byte[] bArr, int i) throws IOException {
        long u32 = (u32(bArr, i + 4) << 32) | u32(bArr, i);
        if (u32 < 0) {
            throw new IOException("ELF 使用了超出支持范围的 64 位地址");
        }
        return u32;
    }

    private static final class Reader {
        final long length;
        final Source source;

        Reader(Source source) throws Exception {
            this.source = source;
            this.length = source.length();
            if (this.length < 64 || this.length > 8589934592L) {
                throw new IOException("游戏库长度超出 64 字节至 8 GiB 支持范围");
            }
        }

        byte[] read(long j, int i) throws Exception {
            if (i > 1048576 || !LibraryMaskResolver.range(j, i, this.length)) {
                throw new IOException("ELF 读取范围越界");
            }
            byte[] read = this.source.read(j, i);
            if (read == null || read.length != i) {
                throw new IOException("游戏库读取被截断或返回长度不一致");
            }
            return read;
        }
    }

    private static final class Candidate {
        final long first;
        final long second;
        final int xor;

        Candidate(long j, long j2, int i) {
            this.first = j;
            this.second = j2;
            this.xor = i;
        }
    }

    private static final class Region {
        final long address;
        final int flags;
        final long offset;
        final long size;

        Region(long j, long j2, long j3, int i) {
            this.offset = j;
            this.address = j2;
            this.size = j3;
            this.flags = i;
        }
    }

    private static final class Elf {
        int sectionCount;
        long sectionOffset;
        int sectionSize;
        final List<Region> loads = new ArrayList();
        final List<Region> code = new ArrayList();

        private Elf() {
        }

        /* JADX WARN: Code restructure failed: missing block: B:86:0x015d, code lost:
        
            if (io.github.deltaconfig.core.LibraryMaskResolver.range(r11.sectionOffset, r11.sectionCount * r11.sectionSize, r28.length) != false) goto L65;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        static Elf parse(Reader reader) throws Exception {
            boolean z;
            int i;
            byte[] read = reader.read(0L, LibraryMaskResolver.MAX_CANDIDATES);
            if (read[0] == Byte.MAX_VALUE && read[1] == 69 && read[2] == 76 && read[3] == 70 && read[4] == 2 && read[5] == 1 && read[6] == 1 && LibraryMaskResolver.u16(read, 18) == 183 && (LibraryMaskResolver.u16(read, 16) == 3 || LibraryMaskResolver.u16(read, 16) == 2)) {
                long j = 1;
                if (LibraryMaskResolver.u32(read, 20) == 1 && LibraryMaskResolver.u16(read, 52) >= LibraryMaskResolver.MAX_CANDIDATES) {
                    long u64 = LibraryMaskResolver.u64(read, 32);
                    int u16 = LibraryMaskResolver.u16(read, 54);
                    int u162 = LibraryMaskResolver.u16(read, 56);
                    if (u162 != 0 && u162 <= LibraryMaskResolver.MAX_TABLE_ENTRIES && u16 >= 56 && u16 <= 256) {
                        int i2 = u162;
                        if (LibraryMaskResolver.range(u64, u162 * u16, reader.length)) {
                            Elf elf = new Elf();
                            byte[] read2 = reader.read(u64, u16 * i2);
                            int i3 = 0;
                            while (i3 < i2) {
                                int i4 = i3 * u16;
                                if (LibraryMaskResolver.u32(read2, i4) != j) {
                                    i = i2;
                                } else {
                                    long u642 = LibraryMaskResolver.u64(read2, i4 + 8);
                                    long u643 = LibraryMaskResolver.u64(read2, i4 + 16);
                                    long u644 = LibraryMaskResolver.u64(read2, i4 + 32);
                                    i = i2;
                                    long u645 = LibraryMaskResolver.u64(read2, i4 + 40);
                                    if (u644 <= u645 && LibraryMaskResolver.range(u642, u644, reader.length)) {
                                        LibraryMaskResolver.add(u643, u645);
                                        elf.loads.add(new Region(u642, u643, u644, LibraryMaskResolver.int32(read2, i4 + 4)));
                                    } else {
                                        throw new IOException("ELF LOAD 区域无效或越界");
                                    }
                                }
                                i3++;
                                i2 = i;
                                j = 1;
                            }
                            if (!elf.loads.isEmpty()) {
                                elf.sectionOffset = LibraryMaskResolver.u64(read, 40);
                                elf.sectionSize = LibraryMaskResolver.u16(read, 58);
                                elf.sectionCount = LibraryMaskResolver.u16(read, 60);
                                if (elf.sectionOffset != 0 || elf.sectionCount != 0) {
                                    if (elf.sectionCount != 0 && elf.sectionCount <= LibraryMaskResolver.MAX_TABLE_ENTRIES && elf.sectionSize >= LibraryMaskResolver.MAX_CANDIDATES && elf.sectionSize <= 256) {
                                    }
                                    throw new IOException("ELF 节表无效或越界");
                                }
                                boolean z2 = false;
                                for (Region region : elf.loads) {
                                    if ((region.flags & 1) != 0 && region.size > 0) {
                                        z = true;
                                        z2 |= z;
                                    }
                                    z = false;
                                    z2 |= z;
                                }
                                if (!z2) {
                                    throw new IOException("ELF 没有可执行区域");
                                }
                                return elf;
                            }
                            throw new IOException("ELF 没有可读取的 LOAD 区域");
                        }
                    }
                    throw new IOException("ELF 程序头无效或越界");
                }
            }
            throw new IOException("游戏库不是受支持的 little-endian ELF64 ARM64 文件");
        }

        void prepareCode(Reader reader) throws Exception {
            if (this.code.isEmpty()) {
                if (reader.source.efficientRandomAccess() && this.sectionCount != 0) {
                    byte[] read = reader.read(this.sectionOffset, this.sectionSize * this.sectionCount);
                    for (int i = 0; i < this.sectionCount; i++) {
                        int i2 = this.sectionSize * i;
                        if (LibraryMaskResolver.u32(read, i2 + 4) == 1 && (LibraryMaskResolver.u64(read, i2 + 8) & 4) != 0) {
                            long u64 = LibraryMaskResolver.u64(read, i2 + 16);
                            long u642 = LibraryMaskResolver.u64(read, i2 + 24);
                            long u643 = LibraryMaskResolver.u64(read, i2 + 32);
                            if (!LibraryMaskResolver.range(u642, u643, reader.length) || !executableMapping(u642, u64, u643)) {
                                throw new IOException("ELF 可执行节与 LOAD 映射不一致");
                            }
                            if (u643 > 0) {
                                this.code.add(new Region(u642, u64, u643, 5));
                            }
                        }
                    }
                }
                if (this.code.isEmpty()) {
                    for (Region region : this.loads) {
                        if ((region.flags & 1) != 0 && region.size > 0) {
                            this.code.add(region);
                        }
                    }
                }
                if (!this.code.isEmpty()) {
                    Collections.sort(this.code, new Comparator<Region>() { // from class: io.github.deltaconfig.core.LibraryMaskResolver.Elf.1
                        @Override // java.util.Comparator
                        public int compare(Region region2, Region region3) {
                            return Long.compare(region2.offset, region3.offset);
                        }
                    });
                    for (int i3 = 1; i3 < this.code.size(); i3++) {
                        int i4 = i3 - 1;
                        if (this.code.get(i3).offset < LibraryMaskResolver.add(this.code.get(i4).offset, this.code.get(i4).size)) {
                            throw new IOException("ELF 可执行扫描范围重叠，已停止识别");
                        }
                    }
                    return;
                }
                throw new IOException("ELF 没有可执行区域");
            }
        }

        boolean executableMapping(long j, long j2, long j3) throws IOException {
            for (Region region : this.loads) {
                if ((region.flags & 1) != 0 && j2 >= region.address && LibraryMaskResolver.range(j2 - region.address, j3, region.size) && LibraryMaskResolver.add(region.offset, j2 - region.address) == j) {
                    return true;
                }
            }
            return false;
        }

        long dataOffset(long j, int i) throws IOException {
            long j2 = -1;
            for (Region region : this.loads) {
                if (j >= region.address && LibraryMaskResolver.range(j - region.address, i, region.size)) {
                    long add = LibraryMaskResolver.add(region.offset, j - region.address);
                    if (j2 >= 0 && j2 != add) {
                        throw new IOException("ELF 虚拟地址存在歧义映射");
                    }
                    if ((region.flags & 2) != 0) {
                        return -1L;
                    }
                    j2 = add;
                }
            }
            return j2;
        }
    }
}
