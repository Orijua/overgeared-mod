package com.orijua.overgearedmod.notification;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.event.ClassChangedEvent;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

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
    }

    private static void onLevelUp(LevelUpEvent event) {
        int points = (event.newLevel() - event.previousLevel()) * Balance.current().statPointsPerLevel();
        send(event.player(), Kind.LEVEL_UP, List.of(
                Component.translatable("notification.overgearedmod.level_up.body", event.newLevel()),
                Component.translatable("notification.overgearedmod.level_up.points", points)));
    }

    private static void onTitleEarned(TitleEarnedEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.TITLE, event.title()).ifPresent(title ->
                send(event.player(), Kind.TITLE, nameAndDescription(title.name(), title.description())));
    }

    private static void onQuestCompleted(QuestCompletedEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.QUEST, event.quest()).ifPresent(quest ->
                send(event.player(), Kind.QUEST, List.of(quest.name())));
    }

    private static void onClassChanged(ClassChangedEvent event) {
        Definitions.get(event.player().level(), ModDatapackRegistries.PLAYER_CLASS, event.playerClass()).ifPresent(playerClass ->
                send(event.player(), Kind.CLASS, nameAndDescription(playerClass.name(), playerClass.description())));
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
