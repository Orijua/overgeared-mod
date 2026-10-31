package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Satisfy level and XP above the mana bar: the numbers ("Lv 3 · 45/75 XP") right-aligned, with a thin
 * Satisfy bar under them. It is the mod's own XP, not vanilla's experience bar, which stays for enchanting.
 * The level is always shown; the numbers after it and the bar only for a while after gaining XP ({@link XpBarFade}).
 */
final class XpHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int BAR_HEIGHT = 2;
    private static final int ROW_HEIGHT = 14;
    /** Below this the faded text is skipped, since the font draws an almost transparent color as opaque. */
    private static final float MIN_TEXT_ALPHA = 0.05f;

    private final XpBarFade fade = new XpBarFade();
    private LocalPlayer seenPlayer;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!HudVisibility.showSurvivalBars(minecraft)) {
            return;
        }
        if (minecraft.player != seenPlayer) {
            // Joining a world or respawning: what the new player already has is not a gain.
            seenPlayer = minecraft.player;
            fade.reset();
        }
        PlayerStats stats = ProgressionService.get(minecraft.player);
        float alpha = fade.update(Util.getMillis(), stats.level(), stats.xp());
        int needed = ProgressionRules.xpToNextLevel(Balance.current(), stats.level());
        Component level = Component.translatable("hud.overgearedmod.level", stats.level());
        Component xp = Component.translatable("hud.overgearedmod.xp", stats.xp(), needed);

        Gui gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        // A right-side row spans [guiHeight - rightHeight, +10); end the bar near its bottom, just above the mana bar.
        int bottom = graphics.guiHeight() - gui.rightHeight + 8;
        gui.rightHeight += ROW_HEIGHT;

        int textY = bottom - BAR_HEIGHT - 11;
        int xpX = right - minecraft.font.width(xp);
        graphics.drawString(minecraft.font, level, xpX - minecraft.font.width(level), textY, UiStyle.HEADING);
        if (alpha > 0) {
            UiStyle.bar(graphics, right - WIDTH, bottom - BAR_HEIGHT, WIDTH, BAR_HEIGHT, stats.xp() / (float) needed, UiStyle.XP, alpha);
        }
        if (alpha >= MIN_TEXT_ALPHA) {
            graphics.drawString(minecraft.font, xp, xpX, textY, UiStyle.withAlpha(UiStyle.HEADING, alpha));
        }
    }
}
