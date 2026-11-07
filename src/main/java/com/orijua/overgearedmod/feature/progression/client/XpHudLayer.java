package com.orijua.overgearedmod.feature.progression.client;

import com.orijua.overgearedmod.core.client.HudVisibility;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionRules;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
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
 * Each gain also shows a gold "+N XP" rising above the numbers, and the bar fills gradually with the new
 * part glowing ({@link XpGainFeedback}).
 */
public final class XpHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int BAR_HEIGHT = 2;
    private static final int ROW_HEIGHT = 14;
    /** How many pixels the "+N XP" rises above the numbers. */
    private static final int RISE = 6;
    /** Below this the faded text is skipped, since the font draws an almost transparent color as opaque. */
    private static final float MIN_TEXT_ALPHA = 0.05f;
    private static final int GLOW = 0xFFFFFF;
    private static final long PULSE_MILLIS = 600;

    private final XpBarFade fade = new XpBarFade();
    private final XpGainFeedback gain = new XpGainFeedback(level -> ProgressionRules.xpToNextLevel(Balance.current(), level));
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
            long arrived = Util.getMillis();
            fade.settle(arrived);
            gain.settle(arrived);
        }
        PlayerStats stats = ProgressionService.get(minecraft.player);
        long now = Util.getMillis();
        float alpha = fade.update(now, stats.level(), stats.xp());
        gain.update(now, stats.level(), stats.xp());
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
            int barX = right - WIDTH;
            int barY = bottom - BAR_HEIGHT;
            UiStyle.bar(graphics, barX, barY, WIDTH, BAR_HEIGHT, gain.barFill(), UiStyle.XP, alpha);
            renderGlow(graphics, barX, barY, now, alpha);
            renderLoss(graphics, barX, barY, now, alpha);
        }
        if (alpha >= MIN_TEXT_ALPHA) {
            graphics.drawString(minecraft.font, xp, xpX, textY, UiStyle.withAlpha(UiStyle.HEADING, alpha));
        }
        float popupAlpha = gain.popupAlpha(now);
        if (popupAlpha >= MIN_TEXT_ALPHA) {
            Component popup = Component.translatable("hud.overgearedmod.xp_gain", gain.amount());
            int popupY = textY - 10 - Math.round(RISE * gain.rise(now));
            graphics.drawString(minecraft.font, popup, right - minecraft.font.width(popup), popupY, UiStyle.withAlpha(UiStyle.GOLD, popupAlpha));
        }
        float lossAlpha = gain.lossAlpha(now);
        if (lossAlpha >= MIN_TEXT_ALPHA) {
            Component popup = Component.translatable("hud.overgearedmod.xp_loss", gain.lostAmount());
            // Mirror of the gain: starts at the top and sinks instead of rising.
            int popupY = textY - 10 - RISE + Math.round(RISE * gain.sink(now));
            graphics.drawString(minecraft.font, popup, right - minecraft.font.width(popup), popupY, UiStyle.withAlpha(UiStyle.BAD, lossAlpha));
        }
    }

    /** The part of the bar just lost, in red between the bar's new end and where it was, fading with the "-N XP". */
    private void renderLoss(GuiGraphics graphics, int barX, int barY, long now, float alpha) {
        float red = gain.lossAlpha(now) * alpha;
        int from = barX + Math.round(WIDTH * gain.barFill());
        int to = barX + Math.round(WIDTH * gain.lossEnd());
        if (red <= 0 || to <= from) {
            return;
        }
        graphics.fill(from, barY - 1, to, barY + BAR_HEIGHT + 1, UiStyle.withAlpha(UiStyle.BAD, red * 0.8f));
    }

    /** The part of the bar gained in this count, lit up and pulsing, spilling over the border so it reads at 2 px. */
    private void renderGlow(GuiGraphics graphics, int barX, int barY, long now, float alpha) {
        float glow = gain.glow(now) * alpha;
        int from = barX + Math.round(WIDTH * gain.glowStart());
        int to = barX + Math.round(WIDTH * gain.barFill());
        if (glow <= 0 || to <= from) {
            return;
        }
        float pulse = 0.5f + 0.25f * (float) Math.sin(now * 2 * Math.PI / PULSE_MILLIS);
        graphics.fill(from, barY - 1, to, barY + BAR_HEIGHT + 1, UiStyle.withAlpha(GLOW, glow * pulse));
    }
}
