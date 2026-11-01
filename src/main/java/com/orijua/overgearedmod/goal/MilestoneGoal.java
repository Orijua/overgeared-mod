package com.orijua.overgearedmod.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Something that happened in the story, reached {@code count} times: {"type": "milestone", "milestone":
 * "overgearedmod:read_pagma_book", "count": 1}. The code that makes it happen calls {@link GoalTracker#milestone}; the
 * text shown for it is "goal.&lt;namespace&gt;.milestone.&lt;path&gt;".
 */
public record MilestoneGoal(ResourceLocation milestone, int count) implements Goal {
    public static final MapCodec<MilestoneGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("milestone").forGetter(MilestoneGoal::milestone),
            Codec.intRange(1, 100_000).optionalFieldOf("count", 1).forGetter(MilestoneGoal::count)
    ).apply(instance, MilestoneGoal::new));

    public static String counterKey(ResourceLocation milestone) {
        return "milestone/" + milestone;
    }

    @Override
    public GoalType type() {
        return GoalType.MILESTONE;
    }

    @Override
    public int target() {
        return count;
    }

    @Override
    public int progress(PlayerCounters counters, PlayerStats stats) {
        return counters.get(counterKey(milestone));
    }

    @Override
    public Component describe() {
        return Component.translatable("goal." + milestone.getNamespace() + ".milestone." + milestone.getPath());
    }
}
