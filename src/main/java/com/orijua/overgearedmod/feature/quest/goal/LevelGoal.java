package com.orijua.overgearedmod.feature.quest.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.network.chat.Component;

/** Reach Satisfy level {@code level}. */
public record LevelGoal(int level) implements Goal {
    public static final MapCodec<LevelGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 1000).fieldOf("level").forGetter(LevelGoal::level)
    ).apply(instance, LevelGoal::new));

    @Override
    public GoalType type() {
        return GoalType.LEVEL;
    }

    @Override
    public int target() {
        return level;
    }

    @Override
    public int progress(PlayerCounters counters, PlayerStats stats) {
        return stats.level();
    }

    @Override
    public boolean cumulative() {
        return false;
    }

    @Override
    public Component describe() {
        return Component.translatable("goal.overgearedmod.level", level);
    }
}
