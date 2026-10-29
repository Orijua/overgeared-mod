package com.orijua.overgearedmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared look of every Satisfy screen and HUD element. Change colors here, not in each screen. */
public final class UiStyle {
    public static final int TEXT = 0xFFFFFF;
    public static final int MUTED = 0xA0A0A0;
    public static final int GOLD = 0xFFD700;
    public static final int GOOD = 0x55FF55;
    public static final int BAD = 0xFF5555;
    public static final int MANA = 0xFF3F8CFF;
    public static final int COOLDOWN = 0xFFFFC23F;

    private static final int PANEL = 0xD0101018;
    private static final int PANEL_BORDER = 0xFF5A5AA0;
    private static final int BAR_BORDER = 0xFF000000;
    private static final int BAR_EMPTY = 0xFF1A2440;

    public static final int LINE = 12;

    private UiStyle() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.renderOutline(x, y, width, height, PANEL_BORDER);
    }

    /** A bar with a 1px border, filled left to right. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BAR_BORDER);
        graphics.fill(x, y, x + width, y + height, BAR_EMPTY);
        graphics.fill(x, y, x + Math.round(width * Math.clamp(fraction, 0, 1)), y + height, color);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color);
    }

    public static void heading(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x, y, GOLD);
    }
}
