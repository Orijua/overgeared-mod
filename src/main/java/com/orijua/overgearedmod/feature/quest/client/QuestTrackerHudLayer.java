package com.orijua.overgearedmod.feature.quest.client;

import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestDeadlines;
import com.orijua.overgearedmod.feature.quest.QuestDefinition;
import com.orijua.overgearedmod.feature.quest.QuestService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Top-left Satisfy panel with the quests the player follows (Quests tab) and their progress; finished ones turn green. */
public final class QuestTrackerHudLayer implements LayeredDraw.Layer {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int PADDING = 5;
    private static final int LINE = 10;
    /** Fainter than a screen window, so it doesn't hide the world behind it. */
    private static final float ALPHA = 0.8f;

    private record Line(Component text, boolean done) {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PlayerQuests quests = QuestService.get(player);
        List<Line> lines = new ArrayList<>();
        for (ResourceLocation id : quests.tracked()) {
            Integer stored = quests.active().get(id);
            QuestDefinition quest = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).orElse(null);
            if (stored == null || quest == null) {
                continue;
            }
            int progress = QuestService.progress(player, quest, stored);
            int target = quest.goal().target();
            MutableComponent text = quest.name().copy().append(" " + progress + "/" + target);
            quests.deadline(id).ifPresent(deadline -> text.append(" ⌛").append(QuestDeadlines.timeLeft(deadline, QuestDeadlines.now(player))));
            lines.add(new Line(text, progress >= target));
        }
        if (lines.isEmpty()) {
            return;
        }

        Component heading = Component.translatable("tab.overgearedmod.quests");
        int width = minecraft.font.width(heading);
        for (Line line : lines) {
            width = Math.max(width, minecraft.font.width(line.text()));
        }
        width += 2 * PADDING;
        int height = PADDING + LINE + 3 + lines.size() * LINE + PADDING - 2;

        UiStyle.frame(graphics, X, Y, width, height, ALPHA);
        int y = Y + PADDING;
        graphics.drawString(minecraft.font, heading, X + PADDING, y, UiStyle.HEADING);
        y += LINE + 3;
        for (Line line : lines) {
            graphics.drawString(minecraft.font, line.text(), X + PADDING, y, line.done() ? UiStyle.GOOD : UiStyle.TEXT);
            y += LINE;
        }
    }
}
