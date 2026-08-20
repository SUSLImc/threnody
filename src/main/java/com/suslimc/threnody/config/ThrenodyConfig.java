package com.suslimc.threnody.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ThrenodyConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(builder);
        COMMON_SPEC = builder.build();
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    private ThrenodyConfig() {
    }

    public static final class CommonConfig {
        public final ForgeConfigSpec.BooleanValue spawnEnabled;
        public final ForgeConfigSpec.IntValue spawnLightThreshold;
        public final ForgeConfigSpec.IntValue memoryCapacity;
        public final ForgeConfigSpec.IntValue memoryDecayMinutes;
        public final ForgeConfigSpec.BooleanValue disableBlockBreaking;

        CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("spawn");
            spawnEnabled = builder.comment("Enable Threnody spawning").define("spawnEnabled", true);
            spawnLightThreshold = builder.comment("Maximum light level for spawning (0-15)").defineInRange("spawnLightThreshold", 7, 0, 15);
            builder.pop();

            builder.push("memory");
            memoryCapacity = builder.comment("Per-player memory capacity (number of entries)").defineInRange("memoryCapacity", 128, 1, 1024);
            memoryDecayMinutes = builder.comment("Minutes before an encounter is forgotten").defineInRange("memoryDecayMinutes", 60, 1, 10080);
            builder.pop();

            builder.push("performance");
            disableBlockBreaking = builder.comment("Disable block breaking by Threnody entities").define("disableBlockBreaking", false);
            builder.pop();
        }
    }
}
