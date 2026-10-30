package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * Satisfy level and XP above the mana bar: the numbers ("Lv 3 · 45/75 XP") right-aligned, with a thin
 * Satisfy bar under them. It is the mod's own XP, not vanilla's experience bar, which stays for enchanting.
 */
final class XpHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int BAR_HEIGHT = 2;
    private static final int ROW_HEIGHT = 14;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!HudVisibility.showSurvivalBars(minecraft)) {
            return;
        }
        PlayerStats stats = ProgressionService.get(minecraft.player);
        int needed = ProgressionRules.xpToNextLevel(Balance.current(), stats.level());
        Component text = Component.translatable("hud.overgearedmod.xp", stats.level(), stats.xp(), needed);

        Gui gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        // A right-side row spans [guiHeight - rightHeight, +10); end the bar near its bottom, just above the mana bar.
        int bottom = graphics.guiHeight() - gui.rightHeight + 8;
        gui.rightHeight += ROW_HEIGHT;

        UiStyle.bar(graphics, right - WIDTH, bottom - BAR_HEIGHT, WIDTH, BAR_HEIGHT, stats.xp() / (float) needed, UiStyle.XP);
        graphics.drawString(minecraft.font, text, right - minecraft.font.width(text), bottom - BAR_HEIGHT - 11, UiStyle.HEADING);
    }
}
