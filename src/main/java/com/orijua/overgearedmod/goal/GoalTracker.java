package com.orijua.overgearedmod.goal;

import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ItemForgedEvent;
import com.orijua.overgearedmod.event.LevelUpEvent;
import com.orijua.overgearedmod.event.MobKilledEvent;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.title.TitleService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;

import java.util.List;
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

    /** A forge of rating R also counts for every "at least X" goal with X ≤ R, for any item and for this item. */
    private static void onForged(ItemForgedEvent event) {
        PlayerCounters counters = counters(event.player());
        for (Optional<Item> item : List.of(Optional.<Item>empty(), Optional.of(event.forged().getItem()))) {
            counters = counters.increment(ForgeGoal.counterKey(item, Optional.empty()));
            for (ItemRating tier : ItemRating.values()) {
                if (tier.ordinal() <= event.rating().ordinal()) {
                    counters = counters.increment(ForgeGoal.counterKey(item, Optional.of(tier)));
                }
            }
        }
        update(event.player(), counters);
    }

    private static void onLevelUp(LevelUpEvent event) {
        checkGoals(event.player());
    }

    /** A story milestone happened to {@code player} (see {@link MilestoneGoal}): counted, then titles and hidden quests checked. */
    public static void milestone(ServerPlayer player, ResourceLocation milestone) {
        update(player, counters(player).increment(MilestoneGoal.counterKey(milestone)));
    }

    /** How many times {@code player} has reached a milestone. */
    public static int milestones(ServerPlayer player, ResourceLocation milestone) {
        return counters(player).get(MilestoneGoal.counterKey(milestone));
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
