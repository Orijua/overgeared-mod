package com.orijua.overgearedmod.feature.fatigue.client;

import com.orijua.overgearedmod.core.client.HudVisibility;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.ClientConfig;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.fatigue.ClientFatigue;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The energy bar with its percentage, on the right-hand column above the other bars. Unlike the rest of the mod's
 * interface it also shows in the real world, where the energy refills. By default it only shows while the energy is
 * not full; the fatigue key cycles AUTO, ALWAYS and HIDDEN.
 */
public final class FatigueHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int ROW_HEIGHT = 10;
    /** A teal that tells it apart from mana (blue) and XP (green). */
    public static final int COLOR = 0xFF3FD0C9;
    private static final int LOW = UiStyle.COOLDOWN;
    private static final int CRITICAL = 0xFFFF6B6B;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || !HudVisibility.showSurvivalBars(minecraft)) {
            return;
        }
        ClientFatigue.Display mode = ClientConfig.fatigueDisplay();
        float energy = player.getData(ModAttachments.FATIGUE);
        int percent = Math.round(energy * 100);
        if (mode == ClientFatigue.Display.HIDDEN || (mode == ClientFatigue.Display.AUTO && percent >= 100)) {
            return;
        }
        double low = balance.warnAt().isEmpty() ? 0.25 : balance.warnAt().get(0);
        double critical = balance.warnAt().size() < 2 ? 0.10 : balance.warnAt().get(1);
        int color = energy <= critical ? CRITICAL : energy <= low ? LOW : COLOR;
        // Flashes when about to run out.
        float alpha = energy <= critical && energy > 0 && (Util.getMillis() / 300) % 2 == 0 ? 0.45f : 1.0f;

        Gui gui = minecraft.gui;
        int left = graphics.guiWidth() / 2 + 91 - WIDTH;
        int top = graphics.guiHeight() - gui.rightHeight + 2;
        gui.rightHeight += ROW_HEIGHT;

        UiStyle.bar(graphics, left, top, WIDTH, 5, energy, color, alpha);
        Component text = Component.translatable("hud.overgearedmod.fatigue", percent);
        UiStyle.text(graphics, minecraft.font, text, left - 4 - minecraft.font.width(text), top - 2, UiStyle.withAlpha(color, alpha));
    }
}
