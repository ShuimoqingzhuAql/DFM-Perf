package io.github.dfmperf.test;

import io.github.dfmperf.core.Material;

import java.io.File;
import java.util.List;

/** 扫描器与认证门禁的独立测试（用真实 libUE4.so）。 */
public final class MaterialTest {
    public static void main(String[] args) throws Exception {
        File lib = new File(args[0]);
        long t0 = System.currentTimeMillis();
        List<long[]> cands = Material.scan(lib);
        long ms = System.currentTimeMillis() - t0;
        System.out.println("candidates=" + cands.size() + " in " + ms + "ms");
        for (long[] c : cands) {
            System.out.println("  first=0x" + Long.toHexString(c[0]) + " second=0x" + Long.toHexString(c[1]));
        }
        // 用合成样本（材料与真实库不同）验证 HMAC 门禁会拒绝
        byte[] gameplay = java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(args[1]));
        try {
            Material m = Material.resolve(lib, gameplay);
            System.out.println("UNEXPECTED resolve ok: " + m.source);
        } catch (Exception e) {
            System.out.println("expected reject: " + e.getMessage());
        }
    }
}
