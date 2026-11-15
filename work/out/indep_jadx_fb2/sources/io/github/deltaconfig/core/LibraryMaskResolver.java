package io.github.deltaconfig.core;

/* loaded from: classes.dex */
public final class LibraryMaskResolver {
    private static final long LEGACY_FIRST = 24240560;
    private static final long LEGACY_SECOND = 24385264;
    private static final int MAX_CANDIDATES = 64;
    private static final long MAX_SCAN_BYTES = 536870912;
    private static final int MAX_TABLE_ENTRIES = 1024;
    public static final int READ_BLOCK_BYTES = 1048576;

    /* renamed from: io.github.deltaconfig.core.LibraryMaskResolver$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
    }

    private static final class AuthenticationBudget {
        final int fieldBytes;
        long remaining;

        AuthenticationBudget(int r3) {
                r2 = this;
                r2.<init>()
                r0 = 67108864(0x4000000, double:3.3156184E-316)
                r2.remaining = r0
                r2.fieldBytes = r3
                return
        }

        void consume() throws java.io.IOException {
                r4 = this;
                int r0 = r4.fieldBytes
                long r0 = (long) r0
                long r2 = r4.remaining
                int r0 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
                if (r0 > 0) goto L12
                long r0 = r4.remaining
                int r2 = r4.fieldBytes
                long r2 = (long) r2
                long r0 = r0 - r2
                r4.remaining = r0
                return
            L12:
                io.github.deltaconfig.core.ConfigCrypto$MaterialAuthenticationBudgetException r0 = new io.github.deltaconfig.core.ConfigCrypto$MaterialAuthenticationBudgetException
                java.lang.String r1 = "配置认证候选计算超过 64 MiB 预算，已停止自动识别"
                r0.<init>(r1)
                throw r0
        }
    }

    private static final class Candidate {
        final long first;
        final long second;
        final int xor;

        Candidate(long r1, long r3, int r5) {
                r0 = this;
                r0.<init>()
                r0.first = r1
                r0.second = r3
                r0.xor = r5
                return
        }
    }

    private static final class Elf {
        final java.util.List<io.github.deltaconfig.core.LibraryMaskResolver.Region> code;
        final java.util.List<io.github.deltaconfig.core.LibraryMaskResolver.Region> loads;
        int sectionCount;
        long sectionOffset;
        int sectionSize;


        private Elf() {
                r1 = this;
                r1.<init>()
                java.util.ArrayList r0 = new java.util.ArrayList
                r0.<init>()
                r1.loads = r0
                java.util.ArrayList r0 = new java.util.ArrayList
                r0.<init>()
                r1.code = r0
                return
        }

        static io.github.deltaconfig.core.LibraryMaskResolver.Elf parse(io.github.deltaconfig.core.LibraryMaskResolver.Reader r28) throws java.lang.Exception {
                r0 = r28
                r1 = 0
                r3 = 64
                byte[] r4 = r0.read(r1, r3)
                r5 = 0
                r6 = r4[r5]
                r7 = 127(0x7f, float:1.78E-43)
                if (r6 != r7) goto L1ab
                r6 = 1
                r7 = r4[r6]
                r8 = 69
                if (r7 != r8) goto L1ab
                r7 = 2
                r8 = r4[r7]
                r9 = 76
                if (r8 != r9) goto L1ab
                r8 = 3
                r9 = r4[r8]
                r10 = 70
                if (r9 != r10) goto L1ab
                r9 = 4
                r9 = r4[r9]
                if (r9 != r7) goto L1ab
                r9 = 5
                r9 = r4[r9]
                if (r9 != r6) goto L1ab
                r9 = 6
                r9 = r4[r9]
                if (r9 != r6) goto L1ab
                r9 = 18
                int r9 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r9)
                r10 = 183(0xb7, float:2.56E-43)
                if (r9 != r10) goto L1ab
                r9 = 16
                int r10 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r9)
                if (r10 == r8) goto L4d
                int r8 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r9)
                if (r8 != r7) goto L1ab
            L4d:
                r7 = 20
                long r7 = io.github.deltaconfig.core.LibraryMaskResolver.access$300(r4, r7)
                r9 = 1
                int r7 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1))
                if (r7 != 0) goto L1ab
                r7 = 52
                int r7 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r7)
                if (r7 < r3) goto L1ab
                r7 = 32
                long r7 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r4, r7)
                r11 = 54
                int r15 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r11)
                r11 = 56
                int r13 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r11)
                if (r13 == 0) goto L1a3
                r14 = 1024(0x400, float:1.435E-42)
                if (r13 > r14) goto L1a3
                if (r15 < r11) goto L1a3
                r11 = 256(0x100, float:3.59E-43)
                if (r15 > r11) goto L1a3
                long r11 = (long) r15
                r17 = r15
                long r14 = (long) r13
                long r14 = r14 * r11
                long r11 = r0.length
                r18 = r11
                r5 = 256(0x100, float:3.59E-43)
                r11 = r7
                r6 = r13
                r5 = 1024(0x400, float:1.435E-42)
                r13 = r14
                r15 = r18
                boolean r11 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r11, r13, r15)
                if (r11 == 0) goto L1a3
                io.github.deltaconfig.core.LibraryMaskResolver$Elf r11 = new io.github.deltaconfig.core.LibraryMaskResolver$Elf
                r11.<init>()
                int r15 = r17 * r6
                byte[] r7 = r0.read(r7, r15)
                r8 = 0
            La3:
                if (r8 >= r6) goto L10e
                int r15 = r8 * r17
                long r12 = io.github.deltaconfig.core.LibraryMaskResolver.access$300(r7, r15)
                int r12 = (r12 > r9 ? 1 : (r12 == r9 ? 0 : -1))
                if (r12 == 0) goto Lb2
                r16 = r6
                goto Lfb
            Lb2:
                int r12 = r15 + 8
                long r12 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r7, r12)
                int r14 = r15 + 16
                long r9 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r7, r14)
                int r14 = r15 + 32
                long r26 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r7, r14)
                int r14 = r15 + 40
                r16 = r6
                long r5 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r7, r14)
                int r14 = (r26 > r5 ? 1 : (r26 == r5 ? 0 : -1))
                if (r14 > 0) goto L106
                long r1 = r0.length
                r20 = r12
                r22 = r26
                r24 = r1
                boolean r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r20, r22, r24)
                if (r1 == 0) goto L106
                io.github.deltaconfig.core.LibraryMaskResolver.access$500(r9, r5)
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r11.loads
                io.github.deltaconfig.core.LibraryMaskResolver$Region r2 = new io.github.deltaconfig.core.LibraryMaskResolver$Region
                int r15 = r15 + 4
                int r5 = io.github.deltaconfig.core.LibraryMaskResolver.access$600(r7, r15)
                r20 = r2
                r21 = r12
                r23 = r9
                r25 = r26
                r27 = r5
                r20.<init>(r21, r23, r25, r27)
                r1.add(r2)
            Lfb:
                int r8 = r8 + 1
                r6 = r16
                r1 = 0
                r5 = 1024(0x400, float:1.435E-42)
                r9 = 1
                goto La3
            L106:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF LOAD 区域无效或越界"
                r0.<init>(r1)
                throw r0
            L10e:
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r11.loads
                boolean r1 = r1.isEmpty()
                if (r1 != 0) goto L19b
                r1 = 40
                long r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r4, r1)
                r11.sectionOffset = r1
                r1 = 58
                int r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r1)
                r11.sectionSize = r1
                r1 = 60
                int r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$200(r4, r1)
                r11.sectionCount = r1
                long r1 = r11.sectionOffset
                r4 = 0
                int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
                if (r1 != 0) goto L13a
                int r1 = r11.sectionCount
                if (r1 == 0) goto L15f
            L13a:
                int r1 = r11.sectionCount
                if (r1 == 0) goto L193
                int r1 = r11.sectionCount
                r2 = 1024(0x400, float:1.435E-42)
                if (r1 > r2) goto L193
                int r1 = r11.sectionSize
                if (r1 < r3) goto L193
                int r1 = r11.sectionSize
                r2 = 256(0x100, float:3.59E-43)
                if (r1 > r2) goto L193
                long r3 = r11.sectionOffset
                int r1 = r11.sectionSize
                long r1 = (long) r1
                int r5 = r11.sectionCount
                long r5 = (long) r5
                long r5 = r5 * r1
                long r7 = r0.length
                boolean r0 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r3, r5, r7)
                if (r0 == 0) goto L193
            L15f:
            L160:
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r11.loads
                java.util.Iterator r0 = r0.iterator()
                r1 = 0
            L167:
                boolean r2 = r0.hasNext()
                if (r2 == 0) goto L188
                java.lang.Object r2 = r0.next()
                io.github.deltaconfig.core.LibraryMaskResolver$Region r2 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r2
                int r3 = r2.flags
                r4 = 1
                r3 = r3 & r4
                if (r3 == 0) goto L183
                long r2 = r2.size
                r5 = 0
                int r2 = (r2 > r5 ? 1 : (r2 == r5 ? 0 : -1))
                if (r2 <= 0) goto L185
                r2 = r4
                goto L186
            L183:
                r5 = 0
            L185:
                r2 = 0
            L186:
                r1 = r1 | r2
                goto L167
            L188:
                if (r1 == 0) goto L18b
                return r11
            L18b:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 没有可执行区域"
                r0.<init>(r1)
                throw r0
            L193:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 节表无效或越界"
                r0.<init>(r1)
                throw r0
            L19b:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 没有可读取的 LOAD 区域"
                r0.<init>(r1)
                throw r0
            L1a3:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 程序头无效或越界"
                r0.<init>(r1)
                throw r0
            L1ab:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "游戏库不是受支持的 little-endian ELF64 ARM64 文件"
                r0.<init>(r1)
                throw r0
        }

        long dataOffset(long r16, int r18) throws java.io.IOException {
                r15 = this;
                r0 = r15
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r0.loads
                java.util.Iterator r1 = r1.iterator()
                r2 = -1
                r4 = r2
            Lb:
                boolean r6 = r1.hasNext()
                if (r6 == 0) goto L57
                java.lang.Object r6 = r1.next()
                io.github.deltaconfig.core.LibraryMaskResolver$Region r6 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r6
                long r7 = r6.address
                int r7 = (r16 > r7 ? 1 : (r16 == r7 ? 0 : -1))
                if (r7 < 0) goto L54
                long r7 = r6.address
                long r9 = r16 - r7
                r7 = r18
                long r11 = (long) r7
                long r13 = r6.size
                boolean r8 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r9, r11, r13)
                if (r8 != 0) goto L2d
                goto Lb
            L2d:
                long r8 = r6.offset
                long r10 = r6.address
                long r10 = r16 - r10
                long r8 = io.github.deltaconfig.core.LibraryMaskResolver.access$500(r8, r10)
                r10 = 0
                int r10 = (r4 > r10 ? 1 : (r4 == r10 ? 0 : -1))
                if (r10 < 0) goto L4a
                int r4 = (r4 > r8 ? 1 : (r4 == r8 ? 0 : -1))
                if (r4 != 0) goto L42
                goto L4a
            L42:
                java.io.IOException r1 = new java.io.IOException
                java.lang.String r2 = "ELF 虚拟地址存在歧义映射"
                r1.<init>(r2)
                throw r1
            L4a:
                int r4 = r6.flags
                r4 = r4 & 2
                if (r4 == 0) goto L51
                return r2
            L51:
                r4 = r8
                goto Lb
            L54:
                r7 = r18
                goto Lb
            L57:
                return r4
        }

        boolean executableMapping(long r14, long r16, long r18) throws java.io.IOException {
                r13 = this;
                r0 = r13
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r0.loads
                java.util.Iterator r1 = r1.iterator()
            L7:
                boolean r2 = r1.hasNext()
                if (r2 == 0) goto L3d
                java.lang.Object r2 = r1.next()
                io.github.deltaconfig.core.LibraryMaskResolver$Region r2 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r2
                int r3 = r2.flags
                r4 = 1
                r3 = r3 & r4
                if (r3 == 0) goto L3c
                long r5 = r2.address
                int r3 = (r16 > r5 ? 1 : (r16 == r5 ? 0 : -1))
                if (r3 < 0) goto L3c
                long r5 = r2.address
                long r7 = r16 - r5
                long r11 = r2.size
                r9 = r18
                boolean r3 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r7, r9, r11)
                if (r3 == 0) goto L3c
                long r5 = r2.offset
                long r2 = r2.address
                long r2 = r16 - r2
                long r2 = io.github.deltaconfig.core.LibraryMaskResolver.access$500(r5, r2)
                int r2 = (r2 > r14 ? 1 : (r2 == r14 ? 0 : -1))
                if (r2 != 0) goto L3c
                return r4
            L3c:
                goto L7
            L3d:
                r1 = 0
                return r1
        }

        void prepareCode(io.github.deltaconfig.core.LibraryMaskResolver.Reader r27) throws java.lang.Exception {
                r26 = this;
                r7 = r26
                r8 = r27
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                boolean r0 = r0.isEmpty()
                if (r0 != 0) goto Ld
                return
            Ld:
                io.github.deltaconfig.core.LibraryMaskResolver$Source r0 = r8.source
                boolean r0 = r0.efficientRandomAccess()
                r9 = 0
                if (r0 == 0) goto L95
                int r0 = r7.sectionCount
                if (r0 == 0) goto L95
                long r0 = r7.sectionOffset
                int r2 = r7.sectionSize
                int r3 = r7.sectionCount
                int r2 = r2 * r3
                byte[] r11 = r8.read(r0, r2)
                r0 = 0
                r12 = r0
            L28:
                int r0 = r7.sectionCount
                if (r12 >= r0) goto L95
                int r0 = r7.sectionSize
                int r0 = r0 * r12
                int r1 = r0 + 4
                long r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$300(r11, r1)
                r3 = 1
                int r1 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
                if (r1 != 0) goto L92
                int r1 = r0 + 8
                long r1 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r11, r1)
                r3 = 4
                long r1 = r1 & r3
                int r1 = (r1 > r9 ? 1 : (r1 == r9 ? 0 : -1))
                if (r1 != 0) goto L49
                goto L92
            L49:
                int r1 = r0 + 16
                long r16 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r11, r1)
                int r1 = r0 + 24
                long r14 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r11, r1)
                int r0 = r0 + 32
                long r24 = io.github.deltaconfig.core.LibraryMaskResolver.access$400(r11, r0)
                long r0 = r8.length
                r18 = r14
                r20 = r24
                r22 = r0
                boolean r0 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r18, r20, r22)
                if (r0 == 0) goto L8a
                r0 = r26
                r1 = r14
                r3 = r16
                r5 = r24
                boolean r0 = r0.executableMapping(r1, r3, r5)
                if (r0 == 0) goto L8a
                int r0 = (r24 > r9 ? 1 : (r24 == r9 ? 0 : -1))
                if (r0 <= 0) goto L92
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                io.github.deltaconfig.core.LibraryMaskResolver$Region r1 = new io.github.deltaconfig.core.LibraryMaskResolver$Region
                r20 = 5
                r13 = r1
                r18 = r24
                r13.<init>(r14, r16, r18, r20)
                r0.add(r1)
                goto L92
            L8a:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 可执行节与 LOAD 映射不一致"
                r0.<init>(r1)
                throw r0
            L92:
                int r12 = r12 + 1
                goto L28
            L95:
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                boolean r0 = r0.isEmpty()
                r1 = 1
                if (r0 == 0) goto Lc1
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.loads
                java.util.Iterator r0 = r0.iterator()
            La4:
                boolean r2 = r0.hasNext()
                if (r2 == 0) goto Lc1
                java.lang.Object r2 = r0.next()
                io.github.deltaconfig.core.LibraryMaskResolver$Region r2 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r2
                int r3 = r2.flags
                r3 = r3 & r1
                if (r3 == 0) goto La4
                long r3 = r2.size
                int r3 = (r3 > r9 ? 1 : (r3 == r9 ? 0 : -1))
                if (r3 <= 0) goto La4
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r3 = r7.code
                r3.add(r2)
                goto La4
            Lc1:
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                boolean r0 = r0.isEmpty()
                if (r0 != 0) goto L110
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                io.github.deltaconfig.core.LibraryMaskResolver$Elf$1 r2 = new io.github.deltaconfig.core.LibraryMaskResolver$Elf$1
                r2.<init>(r7)
                java.util.Collections.sort(r0, r2)
            Ld4:
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                int r0 = r0.size()
                if (r1 >= r0) goto L10f
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                java.lang.Object r0 = r0.get(r1)
                io.github.deltaconfig.core.LibraryMaskResolver$Region r0 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r0
                long r2 = r0.offset
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                int r4 = r1 + (-1)
                java.lang.Object r0 = r0.get(r4)
                io.github.deltaconfig.core.LibraryMaskResolver$Region r0 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r0
                long r5 = r0.offset
                java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r0 = r7.code
                java.lang.Object r0 = r0.get(r4)
                io.github.deltaconfig.core.LibraryMaskResolver$Region r0 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r0
                long r8 = r0.size
                long r4 = io.github.deltaconfig.core.LibraryMaskResolver.access$500(r5, r8)
                int r0 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1))
                if (r0 < 0) goto L107
                int r1 = r1 + 1
                goto Ld4
            L107:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 可执行扫描范围重叠，已停止识别"
                r0.<init>(r1)
                throw r0
            L10f:
                return
            L110:
                java.io.IOException r0 = new java.io.IOException
                java.lang.String r1 = "ELF 没有可执行区域"
                r0.<init>(r1)
                throw r0
        }
    }

    private static final class Reader {
        final long length;
        final io.github.deltaconfig.core.LibraryMaskResolver.Source source;

        Reader(io.github.deltaconfig.core.LibraryMaskResolver.Source r5) throws java.lang.Exception {
                r4 = this;
                r4.<init>()
                r4.source = r5
                long r0 = r5.length()
                r4.length = r0
                long r0 = r4.length
                r2 = 64
                int r5 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
                if (r5 < 0) goto L1f
                long r0 = r4.length
                r2 = 8589934592(0x200000000, double:4.243991582E-314)
                int r5 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
                if (r5 > 0) goto L1f
                return
            L1f:
                java.io.IOException r5 = new java.io.IOException
                java.lang.String r0 = "游戏库长度超出 64 字节至 8 GiB 支持范围"
                r5.<init>(r0)
                throw r5
        }

        byte[] read(long r8, int r10) throws java.lang.Exception {
                r7 = this;
                r0 = 1048576(0x100000, float:1.469368E-39)
                if (r10 > r0) goto L22
                long r3 = (long) r10
                long r5 = r7.length
                r1 = r8
                boolean r0 = io.github.deltaconfig.core.LibraryMaskResolver.access$100(r1, r3, r5)
                if (r0 == 0) goto L22
                io.github.deltaconfig.core.LibraryMaskResolver$Source r0 = r7.source
                byte[] r8 = r0.read(r8, r10)
                if (r8 == 0) goto L1a
                int r9 = r8.length
                if (r9 != r10) goto L1a
                return r8
            L1a:
                java.io.IOException r8 = new java.io.IOException
                java.lang.String r9 = "游戏库读取被截断或返回长度不一致"
                r8.<init>(r9)
                throw r8
            L22:
                java.io.IOException r8 = new java.io.IOException
                java.lang.String r9 = "ELF 读取范围越界"
                r8.<init>(r9)
                throw r8
        }
    }

    private static final class Region {
        final long address;
        final int flags;
        final long offset;
        final long size;

        Region(long r1, long r3, long r5, int r7) {
                r0 = this;
                r0.<init>()
                r0.offset = r1
                r0.address = r3
                r0.size = r5
                r0.flags = r7
                return
        }
    }

    public static final class Result {
        public final int codeEncoding;
        public final java.lang.String description;
        public final long firstOffset;
        public final byte[] mask;
        public final int materialEncoding;
        public final long secondOffset;

        private Result(byte[] r1, java.lang.String r2, long r3, long r5, int r7, int r8) {
                r0 = this;
                r0.<init>()
                java.lang.Object r1 = r1.clone()
                byte[] r1 = (byte[]) r1
                r0.mask = r1
                r0.description = r2
                r0.firstOffset = r3
                r0.secondOffset = r5
                r0.materialEncoding = r7
                r0.codeEncoding = r8
                return
        }

        /* synthetic */ Result(byte[] r1, java.lang.String r2, long r3, long r5, int r7, int r8, io.github.deltaconfig.core.LibraryMaskResolver.AnonymousClass1 r9) {
                r0 = this;
                r0.<init>(r1, r2, r3, r5, r7, r8)
                return
        }
    }

    public interface Source {
        default boolean efficientRandomAccess() {
                r1 = this;
                r0 = 0
                return r0
        }

        long length() throws java.lang.Exception;

        byte[] read(long r1, int r3) throws java.lang.Exception;
    }

    private LibraryMaskResolver() {
            r0 = this;
            r0.<init>()
            return
    }

    static /* synthetic */ boolean access$100(long r0, long r2, long r4) {
            boolean r0 = range(r0, r2, r4)
            return r0
    }

    static /* synthetic */ int access$200(byte[] r0, int r1) {
            int r0 = u16(r0, r1)
            return r0
    }

    static /* synthetic */ long access$300(byte[] r0, int r1) {
            long r0 = u32(r0, r1)
            return r0
    }

    static /* synthetic */ long access$400(byte[] r0, int r1) throws java.io.IOException {
            long r0 = u64(r0, r1)
            return r0
    }

    static /* synthetic */ long access$500(long r0, long r2) throws java.io.IOException {
            long r0 = add(r0, r2)
            return r0
    }

    static /* synthetic */ int access$600(byte[] r0, int r1) {
            int r0 = int32(r0, r1)
            return r0
    }

    private static long add(long r0, long r2) throws java.io.IOException {
            long r0 = java.lang.Math.addExact(r0, r2)     // Catch: java.lang.ArithmeticException -> L11
            r2 = 0
            int r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r2 < 0) goto Lb
            return r0
        Lb:
            java.lang.ArithmeticException r0 = new java.lang.ArithmeticException     // Catch: java.lang.ArithmeticException -> L11
            r0.<init>()     // Catch: java.lang.ArithmeticException -> L11
            throw r0     // Catch: java.lang.ArithmeticException -> L11
        L11:
            r0 = move-exception
            java.io.IOException r1 = new java.io.IOException
            java.lang.String r2 = "ELF 地址计算越界"
            r1.<init>(r2, r0)
            throw r1
    }

    private static long adprAddress(int r6, long r7) throws java.io.IOException {
            int r0 = r6 >>> 29
            r0 = r0 & 3
            long r0 = (long) r0
            int r6 = r6 >>> 5
            long r2 = (long) r6
            r4 = 524287(0x7ffff, double:2.59032E-318)
            long r2 = r2 & r4
            r6 = 2
            long r2 = r2 << r6
            long r0 = r0 | r2
            r2 = -4096(0xfffffffffffff000, double:NaN)
            long r6 = r7 & r2
            r8 = 21
            long r0 = signExtend(r0, r8)
            r8 = 12
            long r0 = r0 << r8
            long r6 = add(r6, r0)
            return r6
    }

    private static boolean adrp(int r1) {
            r0 = -1627389952(0xffffffff9f000000, float:-2.7105054E-20)
            r1 = r1 & r0
            r0 = -1879048192(0xffffffff90000000, float:-2.524355E-29)
            if (r1 != r0) goto L9
            r1 = 1
            goto La
        L9:
            r1 = 0
        La:
            return r1
    }

    private static io.github.deltaconfig.core.LibraryMaskResolver.Result authenticate(byte[] r17, byte[] r18, byte[] r19, long r20, long r22, java.lang.String r24, int r25, io.github.deltaconfig.core.LibraryMaskResolver.AuthenticationBudget r26) throws java.lang.Exception {
            r1 = r19
            int[] r2 = io.github.deltaconfig.core.ConfigCrypto.materialEncodingCandidates()
            int r3 = r2.length
            r4 = 0
            r5 = r4
        L9:
            r6 = 0
            if (r5 >= r3) goto Le6
            r14 = r2[r5]
            r26.consume()
            java.lang.Object r0 = r17.clone()
            r15 = r0
            byte[] r15 = (byte[]) r15
            r0 = r4
        L19:
            int r7 = r15.length
            if (r0 >= r7) goto L26
            r7 = r15[r0]
            byte r8 = (byte) r14
            r7 = r7 ^ r8
            byte r7 = (byte) r7
            r15[r0] = r7
            int r0 = r0 + 1
            goto L19
        L26:
            r7 = r18
            byte[] r6 = io.github.deltaconfig.core.ConfigCrypto.deriveKeyFromHash(r15, r7)     // Catch: java.lang.Throwable -> Laa java.lang.IllegalArgumentException -> Lae java.security.GeneralSecurityException -> Lb0 io.github.deltaconfig.core.ConfigCrypto.AuthenticatedFormatException -> Lbc
            if (r1 == 0) goto L61
            io.github.deltaconfig.core.ConfigCrypto.decodeIni(r1, r6)     // Catch: java.lang.Throwable -> L33 java.lang.IllegalArgumentException -> L37 java.security.GeneralSecurityException -> L39 io.github.deltaconfig.core.ConfigCrypto.AuthenticatedFormatException -> L42
            goto L61
        L33:
            r0 = move-exception
            r9 = r15
            goto Ldd
        L37:
            r0 = move-exception
            goto L3a
        L39:
            r0 = move-exception
        L3a:
            java.security.GeneralSecurityException r1 = new java.security.GeneralSecurityException     // Catch: java.lang.Throwable -> L33
            java.lang.String r2 = "UserGameplay 认证已匹配，但 Engine HMAC 不匹配；请核对本机配套配置"
            r1.<init>(r2, r0)     // Catch: java.lang.Throwable -> L33
            throw r1     // Catch: java.lang.Throwable -> L33
        L42:
            r0 = move-exception
            r1 = r0
            io.github.deltaconfig.core.ConfigCrypto$AuthenticatedFormatException r0 = new io.github.deltaconfig.core.ConfigCrypto$AuthenticatedFormatException     // Catch: java.lang.Throwable -> L33
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L33
            r2.<init>()     // Catch: java.lang.Throwable -> L33
            java.lang.String r3 = "Engine 认证通过，但配置协议发生变化："
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L33
            java.lang.String r3 = r1.getMessage()     // Catch: java.lang.Throwable -> L33
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L33
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L33
            r0.<init>(r2, r1)     // Catch: java.lang.Throwable -> L33
            throw r0     // Catch: java.lang.Throwable -> L33
        L61:
            io.github.deltaconfig.core.LibraryMaskResolver$Result r0 = new io.github.deltaconfig.core.LibraryMaskResolver$Result     // Catch: java.lang.Throwable -> Laa
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Laa
            r2.<init>()     // Catch: java.lang.Throwable -> Laa
            r8 = r24
            java.lang.StringBuilder r2 = r2.append(r8)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = "；材料"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = encodingDescription(r14)     // Catch: java.lang.Throwable -> Laa
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = "；"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            if (r1 != 0) goto L87
            java.lang.String r1 = "Hash 认证通过"
            goto L89
        L87:
            java.lang.String r1 = "Hash 与 Engine 双重认证通过"
        L89:
            java.lang.StringBuilder r1 = r2.append(r1)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r9 = r1.toString()     // Catch: java.lang.Throwable -> Laa
            r16 = 0
            r7 = r0
            r8 = r15
            r10 = r20
            r12 = r22
            r1 = r15
            r15 = r25
            r7.<init>(r8, r9, r10, r12, r14, r15, r16)     // Catch: java.lang.Throwable -> La8
            java.util.Arrays.fill(r1, r4)
            if (r6 == 0) goto La7
            java.util.Arrays.fill(r6, r4)
        La7:
            return r0
        La8:
            r0 = move-exception
            goto Lac
        Laa:
            r0 = move-exception
            r1 = r15
        Lac:
            r9 = r1
            goto Ldd
        Lae:
            r0 = move-exception
            goto Lb1
        Lb0:
            r0 = move-exception
        Lb1:
            r8 = r24
            r9 = r15
            java.util.Arrays.fill(r9, r4)
            int r5 = r5 + 1
            goto L9
        Lbc:
            r0 = move-exception
            r9 = r15
            r1 = r0
            io.github.deltaconfig.core.ConfigCrypto$AuthenticatedFormatException r0 = new io.github.deltaconfig.core.ConfigCrypto$AuthenticatedFormatException     // Catch: java.lang.Throwable -> Ldc
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Ldc
            r2.<init>()     // Catch: java.lang.Throwable -> Ldc
            java.lang.String r3 = "UserGameplay Hash 认证通过，但配置协议发生变化："
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Ldc
            java.lang.String r3 = r1.getMessage()     // Catch: java.lang.Throwable -> Ldc
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Ldc
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Ldc
            r0.<init>(r2, r1)     // Catch: java.lang.Throwable -> Ldc
            throw r0     // Catch: java.lang.Throwable -> Ldc
        Ldc:
            r0 = move-exception
        Ldd:
            java.util.Arrays.fill(r9, r4)
            if (r6 == 0) goto Le5
            java.util.Arrays.fill(r6, r4)
        Le5:
            throw r0
        Le6:
            return r6
    }

    private static io.github.deltaconfig.core.LibraryMaskResolver.Candidate candidate(byte[] r20, int r21, long r22, int r24, io.github.deltaconfig.core.LibraryMaskResolver.Elf r25) throws java.io.IOException {
            r0 = r20
            r1 = r22
            r3 = r25
            int r4 = int32(r20, r21)
            r4 = r4 ^ r24
            int r5 = r21 + 4
            int r5 = int32(r0, r5)
            r5 = r5 ^ r24
            int r6 = r21 + 8
            int r6 = int32(r0, r6)
            r6 = r6 ^ r24
            int r7 = r21 + 12
            int r7 = int32(r0, r7)
            r7 = r7 ^ r24
            int r8 = r21 + 16
            int r8 = int32(r0, r8)
            r8 = r8 ^ r24
            int r9 = r21 + 20
            int r9 = int32(r0, r9)
            r9 = r9 ^ r24
            int r10 = r21 + 24
            int r10 = int32(r0, r10)
            r10 = r10 ^ r24
            int r11 = r21 + 28
            int r0 = int32(r0, r11)
            r0 = r0 ^ r24
            boolean r11 = adrp(r4)
            if (r11 == 0) goto L65
            boolean r11 = adrp(r5)
            if (r11 == 0) goto L65
            boolean r11 = loadQ(r6)
            if (r11 == 0) goto L65
            boolean r11 = loadQ(r7)
            if (r11 == 0) goto L65
            r13 = 4
            long r13 = r13 + r1
            r19 = r6
            r6 = r5
            r5 = r19
            goto L80
        L65:
            boolean r11 = adrp(r4)
            if (r11 == 0) goto L141
            boolean r11 = loadQ(r5)
            if (r11 == 0) goto L141
            boolean r11 = adrp(r6)
            if (r11 == 0) goto L141
            boolean r11 = loadQ(r7)
            if (r11 == 0) goto L141
            r13 = 8
            long r13 = r13 + r1
        L80:
            int r11 = r5 >>> 5
            r11 = r11 & 31
            r15 = r4 & 31
            if (r11 != r15) goto L13f
            int r11 = r7 >>> 5
            r11 = r11 & 31
            r15 = r6 & 31
            if (r11 == r15) goto L93
            r0 = 0
            goto L140
        L93:
            r11 = r8 & (-32)
            r15 = 1384120352(0x52800020, float:2.7487896E11)
            if (r11 != r15) goto L13d
            r11 = -4194304(0xffffffffffc00000, float:NaN)
            r15 = r9 & r11
            r12 = 956301312(0x39000000, float:1.2207031E-4)
            if (r15 != r12) goto L13d
            r9 = r9 & 31
            r8 = r8 & 31
            if (r9 == r8) goto Lab
            r0 = 0
            goto L13e
        Lab:
            r8 = r10 & r11
            r9 = -1392508928(0xffffffffad000000, float:-7.275958E-12)
            if (r8 != r9) goto L13b
            r8 = -67108864(0xfffffffffc000000, float:-2.658456E36)
            r8 = r8 & r0
            r9 = 335544320(0x14000000, float:6.4623485E-27)
            if (r8 == r9) goto Lbb
            r0 = 0
            goto L13c
        Lbb:
            r8 = r5 & 31
            r9 = r7 & 31
            r11 = r10 & 31
            int r10 = r10 >>> 10
            r10 = r10 & 31
            if (r8 == r9) goto L139
            if (r11 != r8) goto Lcb
            if (r10 == r9) goto Ld2
        Lcb:
            if (r11 != r9) goto L139
            if (r10 == r8) goto Ld2
            r0 = 0
            goto L13a
        Ld2:
            long r9 = (long) r0
            r15 = 67108863(0x3ffffff, double:3.31561837E-316)
            long r9 = r9 & r15
            r0 = 26
            long r9 = signExtend(r9, r0)
            r0 = 2
            long r9 = r9 << r0
            r15 = 0
            int r0 = (r9 > r15 ? 1 : (r9 == r15 ? 0 : -1))
            if (r0 >= 0) goto L137
            r17 = -4096(0xfffffffffffff000, double:NaN)
            int r0 = (r9 > r17 ? 1 : (r9 == r17 ? 0 : -1))
            if (r0 >= 0) goto Led
            r0 = 0
            goto L138
        Led:
            long r0 = adprAddress(r4, r1)
            int r2 = r5 >>> 10
            r2 = r2 & 4095(0xfff, float:5.738E-42)
            long r4 = (long) r2
            r9 = 16
            long r4 = r4 * r9
            long r0 = add(r0, r4)
            long r4 = adprAddress(r6, r13)
            int r2 = r7 >>> 10
            r2 = r2 & 4095(0xfff, float:5.738E-42)
            long r6 = (long) r2
            long r6 = r6 * r9
            long r4 = add(r4, r6)
            r2 = 16
            long r6 = r3.dataOffset(r0, r2)
            long r3 = r3.dataOffset(r4, r2)
            int r0 = (r6 > r15 ? 1 : (r6 == r15 ? 0 : -1))
            if (r0 < 0) goto L135
            int r0 = (r3 > r15 ? 1 : (r3 == r15 ? 0 : -1))
            if (r0 >= 0) goto L11e
            goto L135
        L11e:
            if (r11 != r8) goto L12a
            io.github.deltaconfig.core.LibraryMaskResolver$Candidate r8 = new io.github.deltaconfig.core.LibraryMaskResolver$Candidate
            r0 = r8
            r1 = r6
            r5 = r24
            r0.<init>(r1, r3, r5)
            goto L134
        L12a:
            io.github.deltaconfig.core.LibraryMaskResolver$Candidate r8 = new io.github.deltaconfig.core.LibraryMaskResolver$Candidate
            r0 = r8
            r1 = r3
            r3 = r6
            r5 = r24
            r0.<init>(r1, r3, r5)
        L134:
            return r8
        L135:
            r0 = 0
            return r0
        L137:
            r0 = 0
        L138:
            return r0
        L139:
            r0 = 0
        L13a:
            return r0
        L13b:
            r0 = 0
        L13c:
            return r0
        L13d:
            r0 = 0
        L13e:
            return r0
        L13f:
            r0 = 0
        L140:
            return r0
        L141:
            r0 = 0
            return r0
    }

    private static java.util.List<io.github.deltaconfig.core.LibraryMaskResolver.Candidate> discover(io.github.deltaconfig.core.LibraryMaskResolver.Reader r21, io.github.deltaconfig.core.LibraryMaskResolver.Elf r22) throws java.lang.Exception {
            r0 = r21
            r7 = r22
            r7.prepareCode(r0)
            java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r7.code
            java.util.Iterator r1 = r1.iterator()
            r2 = 0
        L10:
            boolean r4 = r1.hasNext()
            if (r4 == 0) goto L32
            java.lang.Object r4 = r1.next()
            io.github.deltaconfig.core.LibraryMaskResolver$Region r4 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r4
            long r5 = r4.size
            r10 = 536870912(0x20000000, double:2.65249474E-315)
            long r10 = r10 - r2
            int r5 = (r5 > r10 ? 1 : (r5 == r10 ? 0 : -1))
            if (r5 > 0) goto L2a
            long r4 = r4.size
            long r2 = r2 + r4
            goto L10
        L2a:
            java.io.IOException r0 = new java.io.IOException
            java.lang.String r1 = "游戏库可执行区域超过自动识别的 512 MiB 扫描上限"
            r0.<init>(r1)
            throw r0
        L32:
            java.util.LinkedHashMap r10 = new java.util.LinkedHashMap
            r10.<init>()
            java.util.List<io.github.deltaconfig.core.LibraryMaskResolver$Region> r1 = r7.code
            java.util.Iterator r11 = r1.iterator()
        L3d:
            boolean r1 = r11.hasNext()
            if (r1 == 0) goto L117
            java.lang.Object r1 = r11.next()
            r12 = r1
            io.github.deltaconfig.core.LibraryMaskResolver$Region r12 = (io.github.deltaconfig.core.LibraryMaskResolver.Region) r12
            r13 = 0
            byte[] r1 = new byte[r13]
            r14 = 0
        L4f:
            long r2 = r12.size
            int r2 = (r14 > r2 ? 1 : (r14 == r2 ? 0 : -1))
            if (r2 >= 0) goto L115
            long r2 = r12.size
            long r2 = r2 - r14
            r4 = 1048576(0x100000, double:5.180654E-318)
            long r2 = java.lang.Math.min(r4, r2)
            int r6 = (int) r2
            long r2 = r12.offset
            long r2 = r2 + r14
            byte[] r2 = r0.read(r2, r6)
            int r3 = r1.length
            int r4 = r2.length
            int r5 = r3 + r4
            byte[] r3 = new byte[r5]
            int r4 = r1.length
            java.lang.System.arraycopy(r1, r13, r3, r13, r4)
            int r4 = r1.length
            int r8 = r2.length
            java.lang.System.arraycopy(r2, r13, r3, r4, r8)
            int r1 = r1.length
            long r1 = (long) r1
            long r8 = r14 - r1
            long r1 = r12.address
            long r1 = r1 + r8
            r16 = 3
            long r1 = r1 & r16
            r18 = 4
            long r18 = r18 - r1
            long r1 = r18 & r16
            int r1 = (int) r1
            r4 = r1
        L89:
            int r1 = r4 + 32
            if (r1 > r5) goto Lfe
            int r1 = r4 + 16
            int r1 = int32(r3, r1)
            int r2 = r1 >>> 24
            r2 = r2 ^ 82
            r16 = 16843009(0x1010101, float:2.3694278E-38)
            int r16 = r16 * r2
            r1 = r1 ^ r16
            r1 = r1 & (-32)
            r2 = 1384120352(0x52800020, float:2.7487896E11)
            if (r1 == r2) goto Lad
            r20 = r4
            r13 = r5
            r18 = r14
            r15 = r3
            r14 = r6
            goto Lf5
        Lad:
            long r1 = r12.address
            long r1 = r1 + r8
            r18 = r14
            long r13 = (long) r4
            long r13 = r13 + r1
            r1 = r3
            r2 = r4
            r15 = r3
            r20 = r4
            r3 = r13
            r13 = r5
            r5 = r16
            r14 = r6
            r6 = r22
            io.github.deltaconfig.core.LibraryMaskResolver$Candidate r1 = candidate(r1, r2, r3, r5, r6)
            if (r1 == 0) goto Lf5
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            long r3 = r1.first
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r3 = ":"
            java.lang.StringBuilder r2 = r2.append(r3)
            long r3 = r1.second
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            r10.put(r2, r1)
            int r1 = r10.size()
            r2 = 64
            if (r1 > r2) goto Led
            goto Lf5
        Led:
            java.io.IOException r0 = new java.io.IOException
            java.lang.String r1 = "游戏库结构候选超过 64 个，布局存在歧义；已停止自动识别"
            r0.<init>(r1)
            throw r0
        Lf5:
            int r4 = r20 + 4
            r5 = r13
            r6 = r14
            r3 = r15
            r14 = r18
            r13 = 0
            goto L89
        Lfe:
            r13 = r5
            r18 = r14
            r15 = r3
            r14 = r6
            r1 = 28
            int r1 = java.lang.Math.min(r1, r13)
            int r5 = r13 - r1
            byte[] r1 = java.util.Arrays.copyOfRange(r15, r5, r13)
            long r2 = (long) r14
            long r14 = r18 + r2
            r13 = 0
            goto L4f
        L115:
            goto L3d
        L117:
            java.util.ArrayList r0 = new java.util.ArrayList
            java.util.Collection r1 = r10.values()
            r0.<init>(r1)
            return r0
    }

    private static java.lang.String encodingDescription(int r2) {
            if (r2 != 0) goto L5
            java.lang.String r2 = "原始编码"
            goto L1c
        L5:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "单字节 XOR 0x"
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r2 = java.lang.Integer.toHexString(r2)
            java.lang.StringBuilder r2 = r0.append(r2)
            java.lang.String r2 = r2.toString()
        L1c:
            return r2
    }

    private static int int32(byte[] r2, int r3) {
            r0 = r2[r3]
            r0 = r0 & 255(0xff, float:3.57E-43)
            int r1 = r3 + 1
            r1 = r2[r1]
            r1 = r1 & 255(0xff, float:3.57E-43)
            int r1 = r1 << 8
            r0 = r0 | r1
            int r1 = r3 + 2
            r1 = r2[r1]
            r1 = r1 & 255(0xff, float:3.57E-43)
            int r1 = r1 << 16
            r0 = r0 | r1
            int r3 = r3 + 3
            r2 = r2[r3]
            r2 = r2 & 255(0xff, float:3.57E-43)
            int r2 = r2 << 24
            r2 = r2 | r0
            return r2
    }

    private static byte[] join(byte[] r3, byte[] r4) {
            r0 = 32
            byte[] r0 = new byte[r0]
            r1 = 0
            r2 = 16
            java.lang.System.arraycopy(r3, r1, r0, r1, r2)
            java.lang.System.arraycopy(r4, r1, r0, r2, r2)
            return r0
    }

    private static boolean loadQ(int r1) {
            r0 = -4194304(0xffffffffffc00000, float:NaN)
            r1 = r1 & r0
            r0 = 1035993088(0x3dc00000, float:0.09375)
            if (r1 != r0) goto L9
            r1 = 1
            goto La
        L9:
            r1 = 0
        La:
            return r1
    }

    private static boolean range(long r3, long r5, long r7) {
            r0 = 0
            int r2 = (r3 > r0 ? 1 : (r3 == r0 ? 0 : -1))
            if (r2 < 0) goto L15
            int r0 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r0 < 0) goto L15
            int r0 = (r3 > r7 ? 1 : (r3 == r7 ? 0 : -1))
            if (r0 > 0) goto L15
            long r7 = r7 - r3
            int r3 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1))
            if (r3 > 0) goto L15
            r3 = 1
            goto L16
        L15:
            r3 = 0
        L16:
            return r3
    }

    public static io.github.deltaconfig.core.LibraryMaskResolver.Result resolve(io.github.deltaconfig.core.LibraryMaskResolver.Source r1, byte[] r2, byte[] r3) throws java.lang.Exception {
            r0 = 1
            io.github.deltaconfig.core.LibraryMaskResolver$Result r1 = resolve(r1, r2, r3, r0)
            return r1
    }

    private static io.github.deltaconfig.core.LibraryMaskResolver.Result resolve(io.github.deltaconfig.core.LibraryMaskResolver.Source r2, byte[] r3, byte[] r4, boolean r5) throws java.lang.Exception {
            if (r2 == 0) goto L23
            if (r3 == 0) goto L23
            java.lang.String r0 = "Hash"
            byte[] r3 = io.github.deltaconfig.core.ConfigCrypto.getField(r3, r0)
            r0 = 0
            if (r4 == 0) goto L16
            java.lang.String r1 = "Token"
            byte[] r1 = io.github.deltaconfig.core.ConfigCrypto.getField(r4, r1)     // Catch: java.lang.Throwable -> L1e
            java.util.Arrays.fill(r1, r0)     // Catch: java.lang.Throwable -> L1e
        L16:
            io.github.deltaconfig.core.LibraryMaskResolver$Result r2 = resolveWithHash(r2, r3, r4, r5)     // Catch: java.lang.Throwable -> L1e
            java.util.Arrays.fill(r3, r0)
            return r2
        L1e:
            r2 = move-exception
            java.util.Arrays.fill(r3, r0)
            throw r2
        L23:
            java.lang.IllegalArgumentException r2 = new java.lang.IllegalArgumentException
            java.lang.String r3 = "缺少游戏库或认证配置"
            r2.<init>(r3)
            throw r2
    }

    public static io.github.deltaconfig.core.LibraryMaskResolver.Result resolveByCode(io.github.deltaconfig.core.LibraryMaskResolver.Source r1, byte[] r2, byte[] r3) throws java.lang.Exception {
            r0 = 0
            io.github.deltaconfig.core.LibraryMaskResolver$Result r1 = resolve(r1, r2, r3, r0)
            return r1
    }

    private static io.github.deltaconfig.core.LibraryMaskResolver.Result resolveWithHash(io.github.deltaconfig.core.LibraryMaskResolver.Source r16, byte[] r17, byte[] r18, boolean r19) throws java.lang.Exception {
            io.github.deltaconfig.core.LibraryMaskResolver$AuthenticationBudget r0 = new io.github.deltaconfig.core.LibraryMaskResolver$AuthenticationBudget
            r11 = r17
            int r1 = r11.length
            r0.<init>(r1)
            io.github.deltaconfig.core.LibraryMaskResolver$Reader r12 = new io.github.deltaconfig.core.LibraryMaskResolver$Reader
            r1 = r16
            r12.<init>(r1)
            io.github.deltaconfig.core.LibraryMaskResolver$Elf r13 = io.github.deltaconfig.core.LibraryMaskResolver.Elf.parse(r12)
            r14 = 16
            r15 = 0
            if (r19 == 0) goto L6a
            r3 = 16
            long r5 = r12.length
            r1 = 24240560(0x171e1b0, double:1.1976428E-316)
            boolean r1 = range(r1, r3, r5)
            if (r1 == 0) goto L6a
            r4 = 16
            long r6 = r12.length
            r2 = 24385264(0x17416f0, double:1.2047921E-316)
            boolean r1 = range(r2, r4, r6)
            if (r1 == 0) goto L6a
            r1 = 24240560(0x171e1b0, double:1.1976428E-316)
            byte[] r1 = r12.read(r1, r14)
            r2 = 24385264(0x17416f0, double:1.2047921E-316)
            byte[] r2 = r12.read(r2, r14)
            byte[] r10 = join(r1, r2)
            java.lang.String r8 = "已认证的兼容区段"
            r9 = -1
            r4 = 24240560(0x171e1b0, double:1.1976428E-316)
            r6 = 24385264(0x17416f0, double:1.2047921E-316)
            r1 = r10
            r2 = r17
            r3 = r18
            r14 = r10
            r10 = r0
            io.github.deltaconfig.core.LibraryMaskResolver$Result r1 = authenticate(r1, r2, r3, r4, r6, r8, r9, r10)     // Catch: java.lang.Throwable -> L62
            if (r1 == 0) goto L5e
            java.util.Arrays.fill(r14, r15)
            return r1
        L5e:
            java.util.Arrays.fill(r14, r15)
            goto L6a
        L62:
            r0 = move-exception
            goto L66
        L64:
            r0 = move-exception
            r14 = r10
        L66:
            java.util.Arrays.fill(r14, r15)
            throw r0
        L6a:
            java.util.List r1 = discover(r12, r13)
            boolean r2 = r1.isEmpty()
            if (r2 != 0) goto L1ad
            java.util.TreeMap r13 = new java.util.TreeMap
            r13.<init>()
            java.util.Iterator r2 = r1.iterator()
        L7d:
            boolean r3 = r2.hasNext()
            r4 = 0
            if (r3 == 0) goto L9d
            java.lang.Object r3 = r2.next()
            io.github.deltaconfig.core.LibraryMaskResolver$Candidate r3 = (io.github.deltaconfig.core.LibraryMaskResolver.Candidate) r3
            long r5 = r3.first
            java.lang.Long r5 = java.lang.Long.valueOf(r5)
            r13.put(r5, r4)
            long r5 = r3.second
            java.lang.Long r3 = java.lang.Long.valueOf(r5)
            r13.put(r3, r4)
            goto L7d
        L9d:
            java.util.Set r2 = r13.entrySet()     // Catch: java.lang.Throwable -> L18a
            java.util.Iterator r2 = r2.iterator()     // Catch: java.lang.Throwable -> L18a
        La7:
            boolean r3 = r2.hasNext()     // Catch: java.lang.Throwable -> L18a
            if (r3 == 0) goto Lc7
            java.lang.Object r3 = r2.next()     // Catch: java.lang.Throwable -> L18a
            java.util.Map$Entry r3 = (java.util.Map.Entry) r3     // Catch: java.lang.Throwable -> L18a
            java.lang.Object r5 = r3.getKey()     // Catch: java.lang.Throwable -> L18a
            java.lang.Long r5 = (java.lang.Long) r5     // Catch: java.lang.Throwable -> L18a
            long r5 = r5.longValue()     // Catch: java.lang.Throwable -> L18a
            r7 = 16
            byte[] r5 = r12.read(r5, r7)     // Catch: java.lang.Throwable -> L18a
            r3.setValue(r5)     // Catch: java.lang.Throwable -> L18a
            goto La7
        Lc7:
            java.util.Iterator r12 = r1.iterator()     // Catch: java.lang.Throwable -> L18a
            r14 = r4
        Lcc:
            boolean r1 = r12.hasNext()     // Catch: java.lang.Throwable -> L187
            if (r1 == 0) goto L15f
            java.lang.Object r1 = r12.next()     // Catch: java.lang.Throwable -> L187
            io.github.deltaconfig.core.LibraryMaskResolver$Candidate r1 = (io.github.deltaconfig.core.LibraryMaskResolver.Candidate) r1     // Catch: java.lang.Throwable -> L187
            long r2 = r1.first     // Catch: java.lang.Throwable -> L187
            java.lang.Long r2 = java.lang.Long.valueOf(r2)     // Catch: java.lang.Throwable -> L187
            java.lang.Object r2 = r13.get(r2)     // Catch: java.lang.Throwable -> L187
            byte[] r2 = (byte[]) r2     // Catch: java.lang.Throwable -> L187
            long r3 = r1.second     // Catch: java.lang.Throwable -> L187
            java.lang.Long r3 = java.lang.Long.valueOf(r3)     // Catch: java.lang.Throwable -> L187
            java.lang.Object r3 = r13.get(r3)     // Catch: java.lang.Throwable -> L187
            byte[] r3 = (byte[]) r3     // Catch: java.lang.Throwable -> L187
            byte[] r10 = join(r2, r3)     // Catch: java.lang.Throwable -> L187
            long r4 = r1.first     // Catch: java.lang.Throwable -> L159
            long r6 = r1.second     // Catch: java.lang.Throwable -> L159
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L159
            r2.<init>()     // Catch: java.lang.Throwable -> L159
            java.lang.String r3 = "ELF ARM64 指令重定位（"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L159
            int r3 = r1.xor     // Catch: java.lang.Throwable -> L159
            r3 = r3 & 255(0xff, float:3.57E-43)
            java.lang.String r3 = encodingDescription(r3)     // Catch: java.lang.Throwable -> L159
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L159
            java.lang.String r3 = "）"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L159
            java.lang.String r8 = r2.toString()     // Catch: java.lang.Throwable -> L159
            int r1 = r1.xor     // Catch: java.lang.Throwable -> L159
            r9 = r1 & 255(0xff, float:3.57E-43)
            r1 = r10
            r2 = r17
            r3 = r18
            r11 = r10
            r10 = r0
            io.github.deltaconfig.core.LibraryMaskResolver$Result r1 = authenticate(r1, r2, r3, r4, r6, r8, r9, r10)     // Catch: java.lang.Throwable -> L157
            if (r1 != 0) goto L130
            java.util.Arrays.fill(r11, r15)     // Catch: java.lang.Throwable -> L187
            r11 = r17
            goto Lcc
        L130:
            if (r14 == 0) goto L14f
            byte[] r2 = r14.mask     // Catch: java.lang.Throwable -> L157
            byte[] r3 = r1.mask     // Catch: java.lang.Throwable -> L157
            boolean r2 = java.security.MessageDigest.isEqual(r2, r3)     // Catch: java.lang.Throwable -> L157
            byte[] r1 = r1.mask     // Catch: java.lang.Throwable -> L157
            java.util.Arrays.fill(r1, r15)     // Catch: java.lang.Throwable -> L157
            if (r2 == 0) goto L142
            goto L150
        L142:
            byte[] r0 = r14.mask     // Catch: java.lang.Throwable -> L157
            java.util.Arrays.fill(r0, r15)     // Catch: java.lang.Throwable -> L157
            java.security.GeneralSecurityException r0 = new java.security.GeneralSecurityException     // Catch: java.lang.Throwable -> L157
            java.lang.String r1 = "认证材料存在歧义：多个不同候选通过认证，已停止操作"
            r0.<init>(r1)     // Catch: java.lang.Throwable -> L157
            throw r0     // Catch: java.lang.Throwable -> L157
        L14f:
            r14 = r1
        L150:
            java.util.Arrays.fill(r11, r15)     // Catch: java.lang.Throwable -> L187
            r11 = r17
            goto Lcc
        L157:
            r0 = move-exception
            goto L15b
        L159:
            r0 = move-exception
            r11 = r10
        L15b:
            java.util.Arrays.fill(r11, r15)     // Catch: java.lang.Throwable -> L187
            throw r0     // Catch: java.lang.Throwable -> L187
        L15f:
            if (r14 == 0) goto L17f
        L163:
            java.util.Collection r0 = r13.values()
            java.util.Iterator r0 = r0.iterator()
        L16b:
            boolean r1 = r0.hasNext()
            if (r1 == 0) goto L17d
            java.lang.Object r1 = r0.next()
            byte[] r1 = (byte[]) r1
            if (r1 == 0) goto L16b
            java.util.Arrays.fill(r1, r15)
            goto L16b
        L17d:
            return r14
        L17f:
            java.security.GeneralSecurityException r0 = new java.security.GeneralSecurityException     // Catch: java.lang.Throwable -> L187
            java.lang.String r1 = "已定位 ARM64 候选，但配置 HMAC 未匹配；请核对同设备配套配置及游戏库"
            r0.<init>(r1)     // Catch: java.lang.Throwable -> L187
            throw r0     // Catch: java.lang.Throwable -> L187
        L187:
            r0 = move-exception
            r4 = r14
            goto L18b
        L18a:
            r0 = move-exception
        L18b:
            java.util.Collection r1 = r13.values()
            java.util.Iterator r1 = r1.iterator()
        L193:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L1a5
            java.lang.Object r2 = r1.next()
            byte[] r2 = (byte[]) r2
            if (r2 == 0) goto L193
            java.util.Arrays.fill(r2, r15)
            goto L193
        L1a5:
            if (r4 == 0) goto L1ac
            byte[] r1 = r4.mask
            java.util.Arrays.fill(r1, r15)
        L1ac:
            throw r0
        L1ad:
            java.security.GeneralSecurityException r0 = new java.security.GeneralSecurityException
            java.lang.String r1 = "认证算法或游戏库布局未识别：未找到受支持的 ARM64 材料装载结构"
            r0.<init>(r1)
            throw r0
    }

    private static long signExtend(long r0, int r2) {
            int r2 = 64 - r2
            long r0 = r0 << r2
            long r0 = r0 >> r2
            return r0
    }

    private static int u16(byte[] r1, int r2) {
            r0 = r1[r2]
            r0 = r0 & 255(0xff, float:3.57E-43)
            int r2 = r2 + 1
            r1 = r1[r2]
            r1 = r1 & 255(0xff, float:3.57E-43)
            int r1 = r1 << 8
            r1 = r1 | r0
            return r1
    }

    private static long u32(byte[] r2, int r3) {
            int r2 = int32(r2, r3)
            long r2 = (long) r2
            r0 = 4294967295(0xffffffff, double:2.1219957905E-314)
            long r2 = r2 & r0
            return r2
    }

    private static long u64(byte[] r3, int r4) throws java.io.IOException {
            long r0 = u32(r3, r4)
            int r4 = r4 + 4
            long r3 = u32(r3, r4)
            r2 = 32
            long r3 = r3 << r2
            long r3 = r3 | r0
            r0 = 0
            int r0 = (r3 > r0 ? 1 : (r3 == r0 ? 0 : -1))
            if (r0 < 0) goto L15
            return r3
        L15:
            java.io.IOException r3 = new java.io.IOException
            java.lang.String r4 = "ELF 使用了超出支持范围的 64 位地址"
            r3.<init>(r4)
            throw r3
    }
}
