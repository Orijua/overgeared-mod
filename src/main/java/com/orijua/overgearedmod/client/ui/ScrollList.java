package com.orijua.overgearedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Which rows of a list a tab is showing. The tab draws (and builds buttons for) rows
 * {@link #first()} to {@link #end}, forwards the mouse wheel to {@link #scroll}, and draws the bar.
 */
public final class ScrollList {
    private static final int BAR_WIDTH = 3;
    private static final int TRACK = 0x60FFFFFF;
    private static final int THUMB = 0xFFA0A0D0;

    private int visibleRows;
    private int offset;

    public ScrollList(int visibleRows) {
        setVisibleRows(visibleRows);
    }

    /** For a list whose height changes with what is above it; call {@link #clamp} after. */
    public void setVisibleRows(int visibleRows) {
        this.visibleRows = Math.max(1, visibleRows);
    }

    public int first() {
        return offset;
    }

    public int end(int total) {
        return Math.min(total, offset + visibleRows);
    }

    /** Keeps the window valid when the list got shorter. */
    public void clamp(int total) {
        offset = Math.clamp(offset, 0, Math.max(0, total - visibleRows));
    }

    /** One row per wheel notch. @return true if the visible rows changed. */
    public boolean scroll(double wheel, int total) {
        int before = offset;
        offset -= (int) Math.signum(wheel);
        clamp(total);
        return offset != before;
    }

    /** A thin bar at {@code x}, only when there is something to scroll. */
    public void renderBar(GuiGraphics graphics, int x, int y, int height, int total) {
        if (total <= visibleRows) {
            return;
        }
        graphics.fill(x, y, x + BAR_WIDTH, y + height, TRACK);
        int thumbHeight = Math.max(8, height * visibleRows / total);
        int thumbY = y + (height - thumbHeight) * offset / (total - visibleRows);
        graphics.fill(x, thumbY, x + BAR_WIDTH, thumbY + thumbHeight, THUMB);
    }
}
