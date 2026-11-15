package io.github.dfmperf.core;

/** 当前配置关键字段摘要，写进日志用于判断配置是否被游戏改写。 */
public final class Diagnostics {
    private Diagnostics() {}

    public static String summary(String engineText, String gameText) {
        return "画质=" + safe(Ini.get(engineText, "DFMPerfGearConfig", "DFMQualityLevel"))
                + " 帧率枚举=" + safe(Ini.get(engineText, "DFMPerfGearConfig", "DFMFPSLevel"))
                + " Unlimit=" + safe(Ini.get(engineText, "QualityFPSSetting", "Unlimit"))
                + " SmallPadSize=" + safe(Ini.get(engineText, "SystemSettings", "PerfGear.SmallPadSize"))
                + " White90=" + safe(Ini.get(engineText, "SystemSettings", "DFMDeviceFrame.White90fps"))
                + " White120=" + safe(Ini.get(engineText, "SystemSettings", "DFMDeviceFrame.White120fps"))
                + " White144=" + safe(Ini.get(engineText, "SystemSettings", "DFMDeviceFrame.White144fps"))
                + " White165=" + safe(Ini.get(engineText, "SystemSettings", "DFMDeviceFrame.White165fps"))
                + " White185=" + safe(Ini.get(engineText, "SystemSettings", "DFMDeviceFrame.White185fps"))
                + " | Pad=" + safe(Ini.get(gameText, ConfigPlan.PAD_SECTION, "SmallPadConfig"));
    }

    private static String safe(String s) {
        if (s == null) return "-";
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
