package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.goal.DeliverGoal;
import com.orijua.overgearedmod.feature.quest.goal.Goal;
import com.orijua.overgearedmod.feature.quest.goal.PlayerCounters;
import com.orijua.overgearedmod.feature.skill.SkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public final class QuestService {
    public enum Result { SUCCESS, UNKNOWN_QUEST, NOT_AVAILABLE, NOT_ACTIVE, NOT_DONE, TALK_TO_GIVER, NOTHING_TO_DELIVER, TRACKING_FULL, CANNOT_ABANDON }

    /** How close (in blocks) the player must stand to an NPC to deal with its quests. */
    public static final double GIVER_REACH = 8.0;

    private QuestService() {
    }

    public static PlayerQuests get(Player player) {
        return player.getData(ModAttachments.PLAYER_QUESTS);
    }

    /** Progress of an accepted quest, from the value it stored. Pure: also used by the client to draw the quests. */
    public static int progress(Goal goal, int stored, PlayerCounters counters, PlayerStats stats) {
        return Math.max(0, Math.min(goal.target(), goal.questProgress(stored, counters, stats)));
    }

    /**
     * A quest with a giver is only dealt with at a living NPC of that type, within reach. {@code at} is the NPC whose
     * window the player used, or null from the Quests tab. Quests without a giver are fine anywhere.
     */
    public static boolean atGiver(QuestDefinition quest, Player player, @Nullable Entity at) {
        return quest.giver().isEmpty() || at != null && at.isAlive() && quest.isGivenBy(at.getType())
                && at.distanceToSqr(player) <= GIVER_REACH * GIVER_REACH;
    }

    public static int progress(Player player, QuestDefinition quest, int baseline) {
        return progress(quest.goal(), baseline, player.getData(ModAttachments.PLAYER_COUNTERS), ProgressionService.get(player));
    }

    /**
     * Visible (a hidden quest must be discovered), not already active, not done (unless repeatable), not failed,
     * the player has the required class if there is one, and the previous quest of its chain is completed.
     */
    public static boolean canAccept(Player player, ResourceLocation id, QuestDefinition quest) {
        PlayerQuests quests = get(player);
        return quest.isVisibleTo(id, quests)
                && !quests.isActive(id)
                && !quests.isFailed(id)
                && (quest.repeatable() || !quests.isCompleted(id))
                && quest.allowsClass(player.getData(ModAttachments.PLAYER_IDENTITY).playerClass())
                && quest.isUnlockedBy(quests);
    }

    /** The player opened the Quests tab: their discoveries stop being marked "New!". */
    public static void markDiscoveriesSeen(ServerPlayer player) {
        PlayerQuests quests = get(player);
        PlayerQuests seen = quests.seeAllDiscovered();
        if (seen != quests) {
            player.setData(ModAttachments.PLAYER_QUESTS, seen);
        }
    }

    /** Reveals every hidden quest whose secret goal is now met. Called by GoalTracker after any countable event. */
    public static void checkDiscoveries(ServerPlayer player) {
        PlayerQuests quests = get(player);
        PlayerCounters counters = player.getData(ModAttachments.PLAYER_COUNTERS);
        PlayerStats stats = ProgressionService.get(player);
        List<ResourceLocation> found = new ArrayList<>();
        for (Definitions.Entry<QuestDefinition> entry : Definitions.all(player.level(), ModDatapackRegistries.QUEST)) {
            Optional<Goal> secret = entry.value().discoverWhen();
            if (secret.isPresent() && !quests.isDiscovered(entry.id()) && secret.get().progress(counters, stats) >= secret.get().target()) {
                quests = quests.discover(entry.id());
                found.add(entry.id());
            }
        }
        if (!found.isEmpty()) {
            player.setData(ModAttachments.PLAYER_QUESTS, quests);
            found.forEach(id -> NeoForge.EVENT_BUS.post(new QuestDiscoveredEvent(player, id)));
        }
    }

    /** From the Quests tab: only quests without a giver. */
    public static Result accept(ServerPlayer player, ResourceLocation id) {
        return accept(player, id, null);
    }

    /** {@code at}: the NPC whose quest window the player used, or null (see {@link #atGiver}). */
    public static Result accept(ServerPlayer player, ResourceLocation id, @Nullable Entity at) {
        Optional<QuestDefinition> quest = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id);
        if (quest.isEmpty()) {
            return Result.UNKNOWN_QUEST;
        }
        if (!canAccept(player, id, quest.get())) {
            return Result.NOT_AVAILABLE;
        }
        if (!atGiver(quest.get(), player, at)) {
            return Result.TALK_TO_GIVER;
        }
        PlayerQuests quests = get(player);
        int stored = quest.get().goal().startingValue(player.getData(ModAttachments.PLAYER_COUNTERS), ProgressionService.get(player));
        PlayerQuests accepted = quests.accept(id, stored);
        if (quest.get().timeLimitDays().isPresent()) {
            accepted = accepted.withDeadline(id, QuestDeadlines.now(player) + quest.get().timeLimitDays().get() * QuestDefinition.DAY_TICKS);
        }
        player.setData(ModAttachments.PLAYER_QUESTS, accepted);
        player.displayClientMessage(Component.translatable("message.overgearedmod.quest_accepted", quest.get().name()), true);
        lend(player, id, quest.get());
        quest.get().acceptSkill().ifPresent(skill -> learnOnAccept(player, skill));
        NeoForge.EVENT_BUS.post(new QuestAcceptedEvent(player, id, at));
        return Result.SUCCESS;
    }

    /** The quest's acceptance reward: a skill is created (a system window, as in the canon). */
    private static void learnOnAccept(ServerPlayer player, ResourceLocation skill) {
        if (SkillService.learn(player, skill)) {
            SkillService.definition(player, skill).ifPresent(definition -> NotificationEvents.send(player, SystemNotification.Kind.SKILL,
                    List.of(definition.name(), definition.description())));
        }
    }

    /** Hands over the quest's loaned items, marked as lent for it. */
    private static void lend(ServerPlayer player, ResourceLocation id, QuestDefinition quest) {
        for (ItemStack loan : quest.loanedItems()) {
            ItemStack copy = loan.copy();
            copy.set(ModDataComponents.LOANED_FOR, id);
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
        if (!quest.loanedItems().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_loaned", quest.name()), false);
        }
    }

    /** Takes back, from the inventory, everything lent for quest {@code id}. Returns how many stacks. */
    public static int reclaimLoans(ServerPlayer player, ResourceLocation id) {
        Inventory inventory = player.getInventory();
        int taken = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (id.equals(inventory.getItem(slot).get(ModDataComponents.LOANED_FOR))) {
                inventory.setItem(slot, ItemStack.EMPTY);
                taken++;
            }
        }
        return taken;
    }

    /**
     * Hands the giver what the player carries towards a deliver quest, up to what is still missing; the items are
     * taken from the inventory. Partial deliveries add up.
     */
    public static Result deliver(ServerPlayer player, ResourceLocation id, @Nullable Entity at) {
        Optional<QuestDefinition> found = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id);
        if (found.isEmpty()) {
            return Result.UNKNOWN_QUEST;
        }
        QuestDefinition quest = found.get();
        PlayerQuests quests = get(player);
        if (!quests.isActive(id) || !(quest.goal() instanceof DeliverGoal goal)) {
            return Result.NOT_ACTIVE;
        }
        if (!atGiver(quest, player, at)) {
            return Result.TALK_TO_GIVER;
        }
        int missing = goal.target() - quests.active().get(id);
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && taken < missing; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (goal.accepts(stack)) {
                int amount = Math.min(stack.getCount(), missing - taken);
                stack.shrink(amount);
                taken += amount;
            }
        }
        if (taken == 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_nothing_to_deliver", goal.itemsName()), true);
            return Result.NOTHING_TO_DELIVER;
        }
        PlayerQuests delivered = quests.deliver(id, taken);
        player.setData(ModAttachments.PLAYER_QUESTS, delivered);
        player.displayClientMessage(Component.translatable("message.overgearedmod.quest_delivered", taken, goal.itemsName(),
                delivered.active().get(id), goal.target()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.0f);
        return Result.SUCCESS;
    }

    /** Drops an accepted quest without rewards; it can be accepted again later. Not a quest with a time limit. */
    public static Result abandon(ServerPlayer player, ResourceLocation id) {
        PlayerQuests quests = get(player);
        if (!quests.isActive(id)) {
            return Result.NOT_ACTIVE;
        }
        if (quests.deadline(id).isPresent()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_cannot_abandon"), true);
            return Result.CANNOT_ABANDON;
        }
        player.setData(ModAttachments.PLAYER_QUESTS, quests.abandon(id));
        reclaimLoans(player, id);
        Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).ifPresent(quest ->
                player.displayClientMessage(Component.translatable("message.overgearedmod.quest_abandoned", quest.name()), true));
        return Result.SUCCESS;
    }

    /** Follows an accepted quest on the HUD panel, or stops following it; at most {@link PlayerQuests#MAX_TRACKED}. */
    public static Result toggleTracked(ServerPlayer player, ResourceLocation id) {
        PlayerQuests quests = get(player);
        if (!quests.isActive(id)) {
            return Result.NOT_ACTIVE;
        }
        PlayerQuests toggled = quests.toggleTracked(id);
        if (toggled == quests) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_tracking_full", PlayerQuests.MAX_TRACKED), true);
            return Result.TRACKING_FULL;
        }
        player.setData(ModAttachments.PLAYER_QUESTS, toggled);
        return Result.SUCCESS;
    }

    /** From the Quests tab: only quests without a giver. */
    public static Result claim(ServerPlayer player, ResourceLocation id) {
        return claim(player, id, null);
    }

    /** {@code at}: the NPC whose quest window the player used, or null (see {@link #atGiver}). */
    public static Result claim(ServerPlayer player, ResourceLocation id, @Nullable Entity at) {
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
        if (!atGiver(quest, player, at)) {
            return Result.TALK_TO_GIVER;
        }

        player.setData(ModAttachments.PLAYER_QUESTS, quests.complete(id));
        if (reclaimLoans(player, id) > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_loan_returned", quest.name()), false);
        }
        for (ItemStack reward : quest.itemRewards()) {
            ItemStack copy = reward.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
        if (quest.xpReward() > 0) {
            ProgressionService.awardXp(player, quest.xpReward());
        }
        quest.classReward().ifPresent(classId -> ClassService.grant(player, classId));
        player.displayClientMessage(Component.translatable("message.overgearedmod.quest_completed", quest.name()), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        NeoForge.EVENT_BUS.post(new QuestCompletedEvent(player, id));
        return Result.SUCCESS;
    }
}
