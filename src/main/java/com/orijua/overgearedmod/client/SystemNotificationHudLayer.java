package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.notification.ClientNotifications;
import com.orijua.overgearedmod.notification.NotificationQueue;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The Satisfy system window: top-center panel with a gold heading, fading in and out, one at a time. */
final class SystemNotificationHudLayer implements LayeredDraw.Layer {
    private static final int TOP = 30;
    private static final int PADDING = 8;
    private static final int MIN_WIDTH = 160;
    private static final int MAX_TEXT_WIDTH = 220;
    private static final int LINE = 10;
    private static final int SEPARATOR = 0xFF5A5AA0;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        NotificationQueue.Shown shown = ClientNotifications.QUEUE.current(Util.getMillis()).orElse(null);
        if (shown == null || shown.alpha() < 0.05f) {
            return;
        }
        Font font = minecraft.font;
        Component heading = shown.notification().kind().heading();
        List<FormattedCharSequence> body = new ArrayList<>();
        for (Component line : shown.notification().lines()) {
            body.addAll(font.split(line, MAX_TEXT_WIDTH));
        }

        int textWidth = font.width(heading);
        for (FormattedCharSequence line : body) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int width = Math.max(MIN_WIDTH, textWidth + 2 * PADDING);
        int height = PADDING + LINE + 4 + body.size() * LINE + PADDING - 2;
        int x = (graphics.guiWidth() - width) / 2;
        int centerX = x + width / 2;
        float alpha = shown.alpha();

        UiStyle.panel(graphics, x, TOP, width, height, alpha);
        int y = TOP + PADDING;
        graphics.drawCenteredString(font, heading, centerX, y, UiStyle.withAlpha(UiStyle.GOLD, alpha));
        y += LINE;
        graphics.fill(x + PADDING, y, x + width - PADDING, y + 1, UiStyle.withAlpha(SEPARATOR, alpha));
        y += 4;
        for (FormattedCharSequence line : body) {
            graphics.drawCenteredString(font, line, centerX, y, UiStyle.withAlpha(UiStyle.TEXT, alpha));
            y += LINE;
        }
    }
}
