package com.orijua.overgearedmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Balance knobs, written to config/overgearedmod-common.toml.
 * Read values with {@code Config.X.get()} at use time; they can change on reload.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Satisfy XP gained from kills.")
            .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue STAT_POINTS_PER_LEVEL = BUILDER
            .comment("Free stat points granted on each level up.")
            .defineInRange("statPointsPerLevel", 5, 0, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
