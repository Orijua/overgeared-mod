package com.orijua.overgearedmod.goal;

import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/** The single entry point for "something countable happened": updates counters, then checks titles. */
public final class GoalTracker {
    private GoalTracker() {
    }

    public static PlayerCounters counters(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS);
    }

    public static void onKill(ServerPlayer player, EntityType<?> victim) {
        PlayerCounters counters = counters(player)
                .increment(KillGoal.counterKey(Optional.empty()))
                .increment(KillGoal.counterKey(Optional.of(victim)));
        update(player, counters);
    }

    /** A forge of rating R also counts for every "at least X" goal with X ≤ R. */
    public static void onForged(ServerPlayer player, ItemRating rating) {
        PlayerCounters counters = counters(player).increment(ForgeGoal.counterKey(Optional.empty()));
        for (ItemRating tier : ItemRating.values()) {
            if (tier.ordinal() <= rating.ordinal()) {
                counters = counters.increment(ForgeGoal.counterKey(Optional.of(tier)));
            }
        }
        update(player, counters);
    }

    public static void onLevelUp(ServerPlayer player) {
        TitleService.checkUnlocks(player);
    }

    private static void update(ServerPlayer player, PlayerCounters counters) {
        player.setData(ModAttachments.PLAYER_COUNTERS, counters);
        TitleService.checkUnlocks(player);
    }
}
