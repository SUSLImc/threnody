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

        public final ForgeConfigSpec.BooleanValue freezeWhenWatched;
        public final ForgeConfigSpec.IntValue stalkDistance;
        public final ForgeConfigSpec.IntValue secondsBeforeHunt;
        public final ForgeConfigSpec.IntValue secondsWatchedBeforeVanish;
        public final ForgeConfigSpec.BooleanValue vanishAfterKill;
        public final ForgeConfigSpec.BooleanValue darknessWhenClose;

        public final ForgeConfigSpec.BooleanValue ambientSounds;
        public final ForgeConfigSpec.BooleanValue heartbeat;

        CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("spawn");
            spawnEnabled = builder
                    .comment("Enable Threnody spawning")
                    .define("spawnEnabled", true);
            spawnLightThreshold = builder
                    .comment("Maximum light level for spawning (0-15)")
                    .defineInRange("spawnLightThreshold", 7, 0, 15);
            builder.pop();

            builder.comment(
                    "Threnody hunts by patience. It holds its distance, stops dead while you are looking",
                    "at it, and only closes the gap when you look away."
            ).push("behaviour");
            freezeWhenWatched = builder
                    .comment("Stand completely still while a player is looking at it")
                    .define("freezeWhenWatched", true);
            stalkDistance = builder
                    .comment("Blocks it tries to keep between itself and its prey while stalking")
                    .defineInRange("stalkDistance", 11, 3, 48);
            secondsBeforeHunt = builder
                    .comment("Seconds of stalking before it stops pretending and charges")
                    .defineInRange("secondsBeforeHunt", 45, 5, 3600);
            secondsWatchedBeforeVanish = builder
                    .comment("Seconds of uninterrupted staring before it refuses to be studied and leaves")
                    .defineInRange("secondsWatchedBeforeVanish", 9, 1, 600);
            vanishAfterKill = builder
                    .comment("Vanish immediately after killing a player")
                    .define("vanishAfterKill", true);
            darknessWhenClose = builder
                    .comment("Drain the light around players it is closing in on")
                    .define("darknessWhenClose", true);
            builder.pop();

            builder.push("audio");
            ambientSounds = builder
                    .comment("Play drones, whispers and distant knocks while it is nearby")
                    .define("ambientSounds", true);
            heartbeat = builder
                    .comment("Play a heartbeat that quickens as it closes in")
                    .define("heartbeat", true);
            builder.pop();

            builder.push("memory");
            memoryCapacity = builder
                    .comment("Per-player memory capacity (number of entries)")
                    .defineInRange("memoryCapacity", 128, 1, 1024);
            memoryDecayMinutes = builder
                    .comment("Minutes before an encounter is forgotten")
                    .defineInRange("memoryDecayMinutes", 60, 1, 10080);
            builder.pop();

            builder.push("performance");
            disableBlockBreaking = builder
                    .comment("Disable block breaking by Threnody entities")
                    .define("disableBlockBreaking", false);
            builder.pop();
        }
    }
}
