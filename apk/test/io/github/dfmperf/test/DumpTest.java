package io.github.dfmperf.test;

import io.github.dfmperf.core.ConfigCrypto;
import io.github.dfmperf.core.Diagnostics;
import io.github.dfmperf.core.Material;
import io.github.dfmperf.core.Repo;

import java.io.File;
import java.nio.file.Paths;

/** 诊断导出在 JVM 上的冒烟测试。 */
public final class DumpTest {
    public static void main(String[] args) throws Exception {
        byte[] material = ConfigCrypto.hexToBytes(args[1]);
        java.nio.file.Path fix = Paths.get(args[0]);
        byte[] gp = java.nio.file.Files.readAllBytes(fix.resolve("UserGameplay.ini"));
        byte[] key = ConfigCrypto.deriveKey(material, gp);
        Material m = Material.of(material, key, "test");
        Repo repo = new Repo(
                fix.resolve("Engine.ini").toFile(),
                fix.resolve("Game.ini").toFile(),
                fix.resolve("UserGameplay.ini").toFile(),
                new File("libUE4.so"),
                fix.toFile());
        for (String line : Diagnostics.dump(repo, m)) System.out.println(line);
        byte[] eng = java.nio.file.Files.readAllBytes(fix.resolve("Engine.ini"));
        byte[] gam = java.nio.file.Files.readAllBytes(fix.resolve("Game.ini"));
        System.out.println("SUMMARY " + Diagnostics.summary(ConfigCrypto.decodeIni(eng, key), new String(gam, "UTF-8")));
    }
}
