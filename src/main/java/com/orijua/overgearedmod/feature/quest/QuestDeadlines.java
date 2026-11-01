package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification.Kind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Quests with a time limit ({@code time_limit_days}): the clock is the overworld's day time, so sleeping through the
 * night counts as time passing. Once a second each player's deadlines are checked: with one day left a system window
 * warns them, and when the time runs out the quest fails for good.
 */
public final class QuestDeadlines {
    /** How often deadlines are checked. */
    static final int CHECK_TICKS = 20;
    /** The warning comes with this much time left: one day. */
    public static final long WARNING_TICKS = QuestDefinition.DAY_TICKS;

    private QuestDeadlines() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(QuestDeadlines::onTick);
    }

    /** The quest clock: the overworld's day time (also on the client, where only the current level is known). */
    public static long now(Player player) {
        Level level = player.level();
        if (level.getServer() != null) {
            Level overworld = level.getServer().overworld();
            return overworld.getDayTime();
        }
        return level.getDayTime();
    }

    /** Whole days and hours left until {@code deadline}, for the "N d M h left" text; never negative. */
    public static Component timeLeft(long deadline, long now) {
        long left = Math.max(0, deadline - now);
        long days = left / QuestDefinition.DAY_TICKS;
        long hours = left % QuestDefinition.DAY_TICKS / (QuestDefinition.DAY_TICKS / 24);
        return Component.translatable("quest.overgearedmod.time_left", days, hours);
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % CHECK_TICKS == 0) {
            check(player);
        }
    }

    /** Warns about the quests on their last day and fails those whose time ran out. */
    public static void check(ServerPlayer player) {
        PlayerQuests quests = QuestService.get(player);
        if (quests.deadlines().isEmpty()) {
            return;
        }
        long now = now(player);
        for (ResourceLocation id : quests.overdue(now)) {
            fail(player, id);
        }
        quests = QuestService.get(player);
        List<ResourceLocation> soon = quests.dueSoon(now, WARNING_TICKS);
        for (ResourceLocation id : soon) {
            quests = quests.warn(id);
            Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).ifPresent(quest -> NotificationEvents.send(player,
                    Kind.QUEST_WARNING, List.of(quest.name(), Component.translatable("notification.overgearedmod.quest_warning.body"))));
        }
        if (!soon.isEmpty()) {
            player.setData(ModAttachments.PLAYER_QUESTS, quests);
        }
    }

    /** The time ran out: the quest closes for good and what was lent for it goes back. */
    static void fail(ServerPlayer player, ResourceLocation id) {
        player.setData(ModAttachments.PLAYER_QUESTS, QuestService.get(player).fail(id));
        QuestService.reclaimLoans(player, id);
        Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).ifPresent(quest -> {
            NotificationEvents.send(player, Kind.QUEST_FAILED, List.of(quest.name()));
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_failed", quest.name()), false);
        });
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
        NeoForge.EVENT_BUS.post(new QuestFailedEvent(player, id));
    }
}
