package com.orijua.overgearedmod.core.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared look of every Satisfy screen and HUD element, modeled on the novel's system windows: a translucent
 * steel-blue panel, light-blue double lines on top and bottom, a crest above and white text with a shadow.
 * Change colors here, not in each screen.
 */
public final class UiStyle {
    public static final int TEXT = 0xFFFFFF;
    public static final int MUTED = 0xC4D3E6;
    public static final int HEADING = 0xD6EEFF;
    public static final int GOLD = 0xFFD700;
    public static final int GOOD = 0x6CFF7A;
    public static final int BAD = 0xFF6B6B;
    public static final int MANA = 0xFF3F8CFF;
    public static final int XP = 0xFF55CC55;
    public static final int COOLDOWN = 0xFFFFC23F;

    /** Window background, lighter at the top like the reference window. */
    public static final int WINDOW_TOP = 0xD27D97B8;
    public static final int WINDOW_BOTTOM = 0xD23C5679;
    /** The bright outer line and the softer inner one of the top and bottom edges. */
    public static final int LINE_BRIGHT = 0xFFE6F4FF;
    public static final int LINE_SOFT = 0xFF9DC4EA;
    private static final int SIDE_LINE = 0x90D6ECFF;
    private static final int CREST_FILL = 0xFFCFE9FF;
    private static final int CREST_OUTLINE = 0xFF4F7CB0;

    private static final int BAR_BORDER = 0xC09DC4EA;
    private static final int BAR_EMPTY = 0xC01B2A40;

    public static final int LINE = 12;

    /**
     * Half of the crest drawn above windows, the last column being the middle one; it is mirrored for the
     * right half. '#' is filled; the outline is added around it. A placeholder until a PNG replaces it.
     */
    private static final String[] CREST_HALF = {
            "................#",
            "...............##",
            "..###.........###",
            ".#...#.......####",
            "#..#..#.....#####",
            "#...#..#...##.###",
            ".#.....##.##..###",
            "..##....###...###",
            "....####..#..####",
            ".........#.######",
            "..........##..###",
            "............####.",
    };
    public static final int CREST_HEIGHT = CREST_HALF.length;

    private UiStyle() {
    }

    /** A Satisfy window with its crest on top. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height, 1.0f);
    }

    /** A Satisfy window faded to {@code alpha} (0-1), for windows that fade in and out. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
        frame(graphics, x, y, width, height, alpha);
        // Sits on the top edge, overlapping it by 2 pixels so it looks attached but clears the content below.
        crest(graphics, x + width / 2, y - CREST_HEIGHT + 2, alpha);
    }

    /** The window without the crest, for small boxes such as buttons. */
    public static void frame(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
        graphics.fillGradient(x, y, x + width, y + height, withAlpha(WINDOW_TOP, alpha), withAlpha(WINDOW_BOTTOM, alpha));
        graphics.fill(x, y, x + 1, y + height, withAlpha(SIDE_LINE, alpha));
        graphics.fill(x + width - 1, y, x + width, y + height, withAlpha(SIDE_LINE, alpha));
        int bright = withAlpha(LINE_BRIGHT, alpha);
        int soft = withAlpha(LINE_SOFT, alpha);
        graphics.fill(x, y, x + width, y + 1, bright);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, soft);
        graphics.fill(x, y + height - 1, x + width, y + height, bright);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, soft);
    }

    /** The ornament centered on {@code centerX}, its top at {@code top}. */
    public static void crest(GuiGraphics graphics, int centerX, int top, float alpha) {
        int half = CREST_HALF[0].length() - 1;
        int fill = withAlpha(CREST_FILL, alpha);
        int outline = withAlpha(CREST_OUTLINE, alpha);
        for (int row = -1; row <= CREST_HEIGHT; row++) {
            for (int column = -half - 1; column <= half + 1; column++) {
                int x = centerX + column;
                int y = top + row;
                if (crestAt(row, column)) {
                    graphics.fill(x, y, x + 1, y + 1, fill);
                } else if (crestAt(row - 1, column) || crestAt(row + 1, column) || crestAt(row, column - 1) || crestAt(row, column + 1)) {
                    graphics.fill(x, y, x + 1, y + 1, outline);
                }
            }
        }
    }

    /** Whether the crest is filled at {@code row} and {@code column} (0 = the middle column). */
    private static boolean crestAt(int row, int column) {
        int half = CREST_HALF[0].length() - 1;
        if (row < 0 || row >= CREST_HEIGHT || Math.abs(column) > half) {
            return false;
        }
        return CREST_HALF[row].charAt(half - Math.abs(column)) == '#';
    }

    /**
     * {@code color} with its alpha scaled by {@code alpha}; RGB colors without alpha count as opaque.
     * Don't draw text below ~0.05: the font treats an alpha under 4 as fully opaque.
     */
    public static int withAlpha(int color, float alpha) {
        int base = (color >>> 24) == 0 ? 0xFF : color >>> 24;
        return (Math.round(base * Math.clamp(alpha, 0, 1)) << 24) | (color & 0xFFFFFF);
    }

    /** A Satisfy bar: light-blue 1px border, dark translucent track, filled left to right with a lit top edge. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        bar(graphics, x, y, width, height, fraction, color, 1);
    }

    /** The same bar faded to {@code alpha} (0 to 1), for HUD parts that appear and disappear. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color, float alpha) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, withAlpha(BAR_BORDER, alpha));
        graphics.fill(x, y, x + width, y + height, withAlpha(BAR_EMPTY, alpha));
        int filled = Math.round(width * Math.clamp(fraction, 0, 1));
        graphics.fillGradient(x, y, x + filled, y + height, withAlpha(lighter(color), alpha), withAlpha(color, alpha));
    }

    /** {@code color} blended a third of the way to white, keeping its alpha. */
    private static int lighter(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (color & 0xFF000000) | ((r + (255 - r) / 3) << 16) | ((g + (255 - g) / 3) << 8) | (b + (255 - b) / 3);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color);
    }

    /** {@code text} cut to {@code maxWidth} with "…" at the end, or whole if it fits (the style is lost when cut). */
    public static Component ellipsize(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return Component.literal(font.substrByWidth(text, maxWidth - font.width("…")).getString() + "…");
    }

    public static void heading(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x, y, HEADING);
    }
}
