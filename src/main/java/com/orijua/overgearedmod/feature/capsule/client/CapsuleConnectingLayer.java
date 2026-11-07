package com.orijua.overgearedmod.feature.capsule.client;

import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.feature.capsule.ClientCapsule;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The "Connecting…" screen while the player lies in the capsule: the view fades to dark and a Satisfy panel shows a
 * progress bar. It covers the whole screen, above every other layer, until the server moves the player.
 */
public final class CapsuleConnectingLayer implements LayeredDraw.Layer {
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 46;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        float fraction = ClientCapsule.fraction(Util.getMillis());
        if (fraction < 0.0f || minecraft.player == null) {
            return;
        }
        // The dark comes in over the first half and stays.
        float dark = Mth.clamp(fraction * 2.0f, 0.0f, 1.0f) * 0.92f;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (Math.round(dark * 255) << 24));

        Font font = minecraft.font;
        int x = (graphics.guiWidth() - PANEL_WIDTH) / 2;
        int y = (graphics.guiHeight() - PANEL_HEIGHT) / 2;
        UiStyle.panel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        int dots = (int) (Util.getMillis() / 400 % 4);
        Component title = Component.translatable("screen.overgearedmod.capsule.connecting", ".".repeat(dots));
        graphics.drawCenteredString(font, title, x + PANEL_WIDTH / 2, y + 11, UiStyle.HEADING);
        UiStyle.bar(graphics, x + 14, y + 29, PANEL_WIDTH - 28, 6, fraction, UiStyle.MANA);
    }
}
