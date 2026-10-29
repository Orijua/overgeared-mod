package com.orijua.overgearedmod.quest;

import com.orijua.overgearedmod.goal.Goal;
import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class QuestService {
    public enum Result { SUCCESS, UNKNOWN_QUEST, NOT_AVAILABLE, NOT_ACTIVE, NOT_DONE }

    private QuestService() {
    }

    public static PlayerQuests get(Player player) {
        return player.getData(ModAttachments.PLAYER_QUESTS);
    }

    /** Progress of an accepted quest. Pure: also used by the client to draw the Quests tab. */
    public static int progress(Goal goal, int baseline, PlayerCounters counters, PlayerStats stats) {
        int current = goal.progress(counters, stats);
        return Math.min(goal.target(), goal.cumulative() ? current - baseline : current);
    }

    public static int progress(Player player, QuestDefinition quest, int baseline) {
        return progress(quest.goal(), baseline, player.getData(ModAttachments.PLAYER_COUNTERS), ProgressionService.get(player));
    }

    public static boolean canAccept(PlayerQuests quests, ResourceLocation id, QuestDefinition quest) {
        return !quests.isActive(id) && (quest.repeatable() || !quests.isCompleted(id));
    }

    public static Result accept(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> quest = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id);
        if (quest.isEmpty()) {
            return Result.UNKNOWN_QUEST;
        }
        PlayerQuests quests = get(player);
        if (!canAccept(quests, id, quest.get())) {
            return Result.NOT_AVAILABLE;
        }
        Goal goal = quest.get().goal();
        int baseline = goal.cumulative()
                ? goal.progress(player.getData(ModAttachments.PLAYER_COUNTERS), ProgressionService.get(player))
                : 0;
        player.setData(ModAttachments.PLAYER_QUESTS, quests.accept(id, baseline));
        player.displayClientMessage(Component.translatable("message.overgearedmod.quest_accepted", quest.get().name()), true);
        return Result.SUCCESS;
    }

    public static Result claim(ServerPlayer player, ResourceLocation id) {
        Optional<QuestDefinition> found = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id);
        if (found.isEmpty()) {
            return Result.UNKNOWN_QUEST;
        }
        QuestDefinition quest = found.get();
        PlayerQuests quests = get(player);
        if (!quests.isActive(id)) {
            return Result.NOT_ACTIVE;
        }
        if (progress(player, quest, quests.active().get(id)) < quest.goal().target()) {
            return Result.NOT_DONE;
        }

        player.setData(ModAttachments.PLAYER_QUESTS, quests.complete(id));
        for (ItemStack reward : quest.itemRewards()) {
            ItemStack copy = reward.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
        if (quest.xpReward() > 0) {
            ProgressionService.awardXp(player, quest.xpReward());
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.quest_completed", quest.name()), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        return Result.SUCCESS;
    }
}
