package com.orijua.overgearedmod.goal;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.network.chat.Component;

/**
 * A measurable objective, shared by titles and quests: {"type": "kill", "entity": "minecraft:zombie", "count": 10}.
 * Quests measure cumulative goals from the moment they are accepted; titles measure lifetime totals.
 */
public interface Goal {
    Codec<Goal> CODEC = GoalType.CODEC.dispatch("type", Goal::type, GoalType::codec);

    GoalType type();

    int target();

    int progress(PlayerCounters counters, PlayerStats stats);

    /** True when progress only grows by counting events (kills, forges); false for states like "reach level N". */
    default boolean cumulative() {
        return true;
    }

    /** What a quest stores when accepted: the count so far for counted goals (so only later events count), else 0. */
    default int startingValue(PlayerCounters counters, PlayerStats stats) {
        return cumulative() ? progress(counters, stats) : 0;
    }

    /** A quest's progress, from the value it stored when accepted (see {@link #startingValue}). */
    default int questProgress(int stored, PlayerCounters counters, PlayerStats stats) {
        int current = progress(counters, stats);
        return cumulative() ? current - stored : current;
    }

    Component describe();
}
