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
 * Satisfy level and XP ("Lv 3 · 45/75 XP") right-aligned above the mana bar. It is the mod's own XP,
 * not vanilla's experience bar, which stays for enchanting.
 */
final class XpHudLayer implements LayeredDraw.Layer {
    private static final int ROW_HEIGHT = 10;

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
        int top = graphics.guiHeight() - gui.rightHeight;
        gui.rightHeight += ROW_HEIGHT;

        graphics.drawString(minecraft.font, text, right - minecraft.font.width(text), top, UiStyle.XP);
    }
}
