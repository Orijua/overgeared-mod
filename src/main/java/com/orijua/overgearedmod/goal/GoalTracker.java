package com.orijua.overgearedmod.goal;

import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ItemForgedEvent;
import com.orijua.overgearedmod.event.LevelUpEvent;
import com.orijua.overgearedmod.event.MobKilledEvent;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

import java.util.Optional;

/** Listens to the domain events of "something countable happened": updates counters, then checks titles and hidden quests. */
public final class GoalTracker {
    private GoalTracker() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(GoalTracker::onKill);
        gameBus.addListener(GoalTracker::onForged);
        gameBus.addListener(GoalTracker::onLevelUp);
    }

    public static PlayerCounters counters(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS);
    }

    private static void onKill(MobKilledEvent event) {
        PlayerCounters counters = counters(event.player())
                .increment(KillGoal.counterKey(Optional.empty()))
                .increment(KillGoal.counterKey(Optional.of(event.victim().getType())));
        update(event.player(), counters);
    }

    /** A forge of rating R also counts for every "at least X" goal with X ≤ R. */
    private static void onForged(ItemForgedEvent event) {
        PlayerCounters counters = counters(event.player()).increment(ForgeGoal.counterKey(Optional.empty()));
        for (ItemRating tier : ItemRating.values()) {
            if (tier.ordinal() <= event.rating().ordinal()) {
                counters = counters.increment(ForgeGoal.counterKey(Optional.of(tier)));
            }
        }
        update(event.player(), counters);
    }

    private static void onLevelUp(LevelUpEvent event) {
        checkGoals(event.player());
    }

    private static void update(ServerPlayer player, PlayerCounters counters) {
        player.setData(ModAttachments.PLAYER_COUNTERS, counters);
        checkGoals(player);
    }

    /** Everything that unlocks on lifetime goals: titles and hidden quests. */
    private static void checkGoals(ServerPlayer player) {
        TitleService.checkUnlocks(player);
        QuestService.checkDiscoveries(player);
    }
}
