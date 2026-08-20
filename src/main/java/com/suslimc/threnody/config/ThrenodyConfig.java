package com.suslimc.threnody.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

public final class ThrenodyConfig {
    private static final List<String> DEFAULT_WATCHED_COMMANDS = List.of(
            "give",
            "gamemode",
            "effect",
            "enchant",
            "experience",
            "xp",
            "item",
            "tp",
            "teleport",
            "kill",
            "setblock",
            "fill",
            "clone",
            "attribute",
            "data",
            "loot",
            "gamerule",
            "difficulty",
            "time",
            "weather"
    );

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
        public final ForgeConfigSpec.BooleanValue judgmentEnabled;
        public final ForgeConfigSpec.BooleanValue judgmentRevertGameMode;
        public final ForgeConfigSpec.BooleanValue judgmentWatchCommands;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> judgmentWatchedCommands;
        public final ForgeConfigSpec.BooleanValue judgmentExemptOperators;
        public final ForgeConfigSpec.BooleanValue judgmentSummonOnRepeatOffense;
        public final ForgeConfigSpec.IntValue judgmentForgivenessMinutes;

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

            builder.comment(
                    "Threnody treats survival as a covenant.",
                    "Players who escape it through creative mode or cheat commands are judged:",
                    "they are pulled back into survival and the entity begins hunting them directly."
            ).push("judgment");
            judgmentEnabled = builder
                    .comment("Enable the judgment system that punishes cheating players")
                    .define("judgmentEnabled", true);
            judgmentRevertGameMode = builder
                    .comment("Force judged players back into survival mode")
                    .define("judgmentRevertGameMode", true);
            judgmentWatchCommands = builder
                    .comment("Also treat the watched cheat commands as transgressions")
                    .define("judgmentWatchCommands", true);
            judgmentWatchedCommands = builder
                    .comment(
                            "Command names that count as cheating.",
                            "The mod's own test command 'summon' is intentionally excluded by default."
                    )
                    .defineList("judgmentWatchedCommands", DEFAULT_WATCHED_COMMANDS, entry -> entry instanceof String);
            judgmentExemptOperators = builder
                    .comment("Exempt server operators so admins can build and moderate freely")
                    .define("judgmentExemptOperators", false);
            judgmentSummonOnRepeatOffense = builder
                    .comment("Let Threnody appear next to repeat offenders, even during the day")
                    .define("judgmentSummonOnRepeatOffense", true);
            judgmentForgivenessMinutes = builder
                    .comment("Minutes before a recorded transgression is forgiven")
                    .defineInRange("judgmentForgivenessMinutes", 30, 1, 10080);
            builder.pop();
        }
    }
}
