package com.orijua.overgearedmod.notification;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ClassChangedEvent;
import com.orijua.overgearedmod.event.ItemForgedEvent;
import com.orijua.overgearedmod.event.LevelUpEvent;
import com.orijua.overgearedmod.event.QuestCompletedEvent;
import com.orijua.overgearedmod.event.QuestDiscoveredEvent;
import com.orijua.overgearedmod.event.TitleEarnedEvent;
import com.orijua.overgearedmod.network.SystemNotificationPayload;
import com.orijua.overgearedmod.notification.SystemNotification.Kind;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Turns domain events into Satisfy system windows on the player's screen. The chat messages stay as a log. */
public final class NotificationEvents {
    private NotificationEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NotificationEvents::onLevelUp);
        gameBus.addListener(NotificationEvents::onTitleEarned);
        gameBus.addListener(NotificationEvents::onQuestCompleted);
        gameBus.addListener(NotificationEvents::onClassChanged);
        gameBus.addListener(NotificationEvents::onQuestDiscovered);
        gameBus.addListener(NotificationEvents::onForged);
    }

    private static void onLevelUp(LevelUpEvent event) {
        int points = (event.newLevel() - event.previousLevel()) * Balance.current().statPointsPerLevel();
        send(event.player(), Kind.LEVEL_UP, List.of(
                Component.translatable("notification.overgearedmod.level_up.body", event.newLevel()),
                Component.translatable("notification.overgearedmod.level_up.points", points)));
    }

    private static void onTitleEarned(TitleEarnedEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.TITLE, event.title()).ifPresent(title -> {
            if (title.rare()) {
                sendGrand(event.player(), Kind.RARE_TITLE, nameAndDescription(title.name(), title.description()), new ItemStack(Items.NAME_TAG));
            } else {
                send(event.player(), Kind.TITLE, nameAndDescription(title.name(), title.description()));
            }
        });
    }

    /** A Legendary (or Myth) piece out of the anvil is a big moment. */
    private static void onForged(ItemForgedEvent event) {
        if (event.rating().ordinal() >= ItemRating.LEGENDARY.ordinal()) {
            sendGrand(event.player(), Kind.LEGENDARY_ITEM, List.of(
                    Component.translatable("notification.overgearedmod.legendary_item.body", event.forged().getHoverName(), event.rating().displayName())),
                    event.forged().copyWithCount(1));
        }
    }

    /** The grand window, with the icon rising like a totem; nearby players hear it too. */
    public static void sendGrand(ServerPlayer player, Kind kind, List<Component> lines, ItemStack icon) {
        PacketDistributor.sendToPlayer(player, new SystemNotificationPayload(new SystemNotification(kind, lines, Optional.of(icon))));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void onQuestCompleted(QuestCompletedEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.QUEST, event.quest()).ifPresent(quest ->
                send(event.player(), Kind.QUEST, List.of(quest.name())));
    }

    private static void onClassChanged(ClassChangedEvent event) {
        // A class nobody can pick (hidden, earned by a secret quest) is a big moment.
        Definitions.get(event.player().level(), ModDatapackRegistries.PLAYER_CLASS, event.playerClass()).ifPresent(playerClass -> {
            if (playerClass.selectable()) {
                send(event.player(), Kind.CLASS, nameAndDescription(playerClass.name(), playerClass.description()));
            } else {
                sendGrand(event.player(), Kind.HIDDEN_CLASS, nameAndDescription(playerClass.name(), playerClass.description()),
                        new ItemStack(Items.NETHER_STAR));
            }
        });
    }

    private static void onQuestDiscovered(QuestDiscoveredEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.QUEST, event.quest()).ifPresent(quest ->
                send(event.player(), Kind.QUEST_DISCOVERED, nameAndDescription(quest.name(), quest.description())));
    }

    private static List<Component> nameAndDescription(Component name, Component description) {
        List<Component> lines = new ArrayList<>(List.of(name));
        if (!description.getString().isEmpty()) {
            lines.add(description);
        }
        return lines;
    }

    public static void send(ServerPlayer player, Kind kind, List<Component> lines) {
        PacketDistributor.sendToPlayer(player, new SystemNotificationPayload(new SystemNotification(kind, lines)));
    }
}
