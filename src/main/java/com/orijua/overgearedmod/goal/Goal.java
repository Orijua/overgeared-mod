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

    Component describe();
}
