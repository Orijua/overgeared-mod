package com.orijua.overgearedmod.feature.fatigue.client;

import com.orijua.overgearedmod.core.client.HudVisibility;
import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.core.config.ClientConfig;
import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.feature.capsule.CapsuleText;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.fatigue.ClientFatigue;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules;
import com.orijua.overgearedmod.feature.world.Satisfy;
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

    /** The color of the bars for a level; the terminal uses it too, so both warn the same. */
    public static int colorFor(FatigueRules.Level level) {
        return switch (level) {
            case CRITICAL -> CRITICAL;
            case LOW -> LOW;
            case NORMAL -> COLOR;
        };
    }

    /** The color of the bars for an energy against the configured thresholds. */
    public static int colorFor(float energy, FatigueBalance balance) {
        return colorFor(FatigueRules.level(energy, balance.warnAt()));
    }

    /** The energy as the player reads it ("80 %"), shared by the HUD and the terminal. */
    public static Component energyText(float energy) {
        return Component.translatable("hud.overgearedmod.fatigue", Math.round(energy * 100));
    }

    /** The rested bonus as the player reads it ("Rested 09:59 (+10 % XP)"). */
    public static Component restedText(int seconds) {
        return Component.translatable("hud.overgearedmod.rested",
                CapsuleText.clock(seconds), CapsuleBalance.current().restedXpPercent());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        FatigueBalance balance = FatigueBalance.current();
        if (!HudVisibility.showSurvivalBars(minecraft)) {
            return;
        }
        renderRested(graphics, minecraft, player);
        if (!balance.enabled()) {
            return;
        }
        ClientFatigue.Display mode = ClientConfig.fatigueDisplay();
        float energy = player.getData(ModAttachments.FATIGUE);
        int percent = Math.round(energy * 100);
        if (mode == ClientFatigue.Display.HIDDEN || (mode == ClientFatigue.Display.AUTO && percent >= 100)) {
            return;
        }
        FatigueRules.Level level = FatigueRules.level(energy, balance.warnAt());
        int color = colorFor(level);
        // Flashes when about to run out.
        float alpha = level == FatigueRules.Level.CRITICAL && energy > 0 && (Util.getMillis() / 300) % 2 == 0 ? 0.45f : 1.0f;

        Gui gui = minecraft.gui;
        int left = graphics.guiWidth() / 2 + 91 - WIDTH;
        int top = graphics.guiHeight() - gui.rightHeight + 2;
        gui.rightHeight += ROW_HEIGHT;

        UiStyle.bar(graphics, left, top, WIDTH, 5, energy, color, alpha);
        Component text = energyText(energy);
        UiStyle.text(graphics, minecraft.font, text, left - 4 - minecraft.font.width(text), top - 2, UiStyle.withAlpha(color, alpha));
    }

    /** In Satisfy, while the diamond capsule's rested bonus lasts: one line on the right-hand column. */
    private static void renderRested(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player) {
        int seconds = player.getData(ModAttachments.RESTED_SECONDS);
        if (seconds <= 0 || !Satisfy.is(player.level())) {
            return;
        }
        Gui gui = minecraft.gui;
        Component text = restedText(seconds);
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - gui.rightHeight + 2;
        gui.rightHeight += ROW_HEIGHT;
        UiStyle.text(graphics, minecraft.font, text, right - minecraft.font.width(text), top - 2, UiStyle.GOOD);
    }
}
