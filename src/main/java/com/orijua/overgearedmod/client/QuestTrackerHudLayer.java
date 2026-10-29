package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Top-left list of accepted quests with their progress; finished ones turn green. */
final class QuestTrackerHudLayer implements LayeredDraw.Layer {
    private static final int MAX_SHOWN = 3;
    private static final int X = 4;
    private static final int Y = 4;
    private static final int LINE = 10;
    private static final int BACKGROUND = 0x60000000;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PlayerQuests quests = QuestService.get(player);
        if (quests.active().isEmpty()) {
            return;
        }

        int y = Y;
        int shown = 0;
        for (Map.Entry<ResourceLocation, Integer> active : quests.active().entrySet()) {
            if (shown == MAX_SHOWN) {
                break;
            }
            QuestDefinition quest = Definitions.get(player.level(), ModDatapackRegistries.QUEST, active.getKey()).orElse(null);
            if (quest == null) {
                continue;
            }
            int progress = QuestService.progress(player, quest, active.getValue());
            int target = quest.goal().target();
            Component line = quest.name().copy().append(" " + progress + "/" + target);
            int width = minecraft.font.width(line);
            graphics.fill(X - 2, y - 1, X + width + 2, y + LINE - 1, BACKGROUND);
            graphics.drawString(minecraft.font, line, X, y, progress >= target ? UiStyle.GOOD : UiStyle.TEXT);
            y += LINE;
            shown++;
        }
    }
}
