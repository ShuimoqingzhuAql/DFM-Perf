package io.github.dfmperf.test;

import io.github.dfmperf.core.ConfigCrypto;
import io.github.dfmperf.core.ConfigPlan;
import io.github.dfmperf.core.Ini;
import io.github.dfmperf.core.Material;
import io.github.dfmperf.core.Repo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** JVM 侧测试：用工具生成的样本验证本 APK 的配置引擎（不依赖 Android）。 */
public final class HostTest {
    public static void main(String[] args) throws Exception {
        Path fix = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        byte[] material = ConfigCrypto.hexToBytes(args[2]);
        Files.createDirectories(out);

        byte[] eng = Files.readAllBytes(fix.resolve("Engine.ini"));
        byte[] gam = Files.readAllBytes(fix.resolve("Game.ini"));
        byte[] gp = Files.readAllBytes(fix.resolve("UserGameplay.ini"));
        Files.write(out.resolve("UserGameplay.ini"), gp);

        byte[] key = ConfigCrypto.deriveKey(material, gp);
        String plain = ConfigCrypto.decodeIni(eng, key);
        System.out.println("BEFORE quality=" + Ini.get(plain, "DFMPerfGearConfig", "DFMQualityLevel")
                + " fps=" + Ini.get(plain, "DFMPerfGearConfig", "DFMFPSLevel"));
        System.out.println("BEFORE pad=" + Ini.get(new String(gam, "UTF-8"),
                ConfigPlan.PAD_SECTION, "SmallPadConfig").substring(0, 60) + "...");

        ConfigPlan plan = ConfigPlan.plan(eng, gam, gp, material, 5, 11, 67, 1080);
        Files.write(out.resolve("Engine.ini"), plan.engine);
        Files.write(out.resolve("Game.ini"), plan.game);
        System.out.println("PLAN " + plan.summary);
        System.out.println("PAD_PIXELS=" + ConfigPlan.padPixels(67, 1080));
        System.out.println("ENGINE_BYTES=" + plan.engine.length + " GAME_BYTES=" + plan.game.length);

        ConfigPlan reset = ConfigPlan.removeOverrides(plan.engine, plan.game, gp, material);
        Files.createDirectories(out.resolve("reset"));
        Files.write(out.resolve("reset/Engine.ini"), reset.engine);
        Files.write(out.resolve("reset/Game.ini"), reset.game);
        System.out.println("RESET_OK=" + (ConfigCrypto.decodeIni(reset.engine, key).length() > 0));

        // Repo 端到端（伪造 dataDir 结构）
        Path data = out.resolve("data");
        Path cfg = data.resolve("files/UE4Game/DeltaForce/DeltaForce/Saved/Config/Android");
        Path ext = out.resolve("ext");
        Files.createDirectories(cfg);
        Files.createDirectories(ext.resolve("UE4Game/DeltaForce/DeltaForce/Saved/Config/Android"));
        Files.write(cfg.resolve("Engine.ini"), eng);
        Files.write(cfg.resolve("Game.ini"), gam);
        Files.write(ext.resolve("UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/UserGameplay.ini"), gp);
        Repo repo = new Repo(
                cfg.resolve("Engine.ini").toFile(),
                cfg.resolve("Game.ini").toFile(),
                ext.resolve("UE4Game/DeltaForce/DeltaForce/Saved/Config/Android/UserGameplay.ini").toFile(),
                out.resolve("libUE4.so").toFile(),
                data.toFile());
        Material m = Material.of(material, key, "host-test");
        System.out.println("REPO1 " + repo.apply(m, 5, 11, 100, 1080));
        System.out.println("REPO2 " + repo.apply(m, 5, 11, 100, 1080));
        System.out.println("REPO3 " + repo.removeOverrides(m));
        System.out.println("REPO4 " + repo.restoreLatest(m));
        byte[] re = Files.readAllBytes(cfg.resolve("Engine.ini"));
        System.out.println("RESTORED_MATCHES_PLAN=" + Arrays.equals(re, plan.engine) + " (应为 false，备份是写入前状态)");
        System.out.println("STATE=" + repo.state());
    }
}
