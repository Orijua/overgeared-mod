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

/**
 * The Satisfy system window: top-center panel with its crest, a light-blue heading and white text a bit
 * larger than the rest of the HUD, fading in and out, one at a time.
 */
final class SystemNotificationHudLayer implements LayeredDraw.Layer {
    private static final int TOP = 30;
    private static final int PADDING = 9;
    private static final int MIN_WIDTH = 170;
    /** Text is drawn this much larger than normal; widths and line heights below are already scaled. */
    private static final float TEXT_SCALE = 1.2f;
    private static final int MAX_TEXT_WIDTH = 200;
    private static final int LINE = 12;

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
        int width = Math.max(MIN_WIDTH, Math.round(textWidth * TEXT_SCALE) + 2 * PADDING);
        int height = PADDING + LINE + 5 + body.size() * LINE + PADDING - 2;
        int x = (graphics.guiWidth() - width) / 2;
        int centerX = x + width / 2;
        float alpha = shown.alpha();

        UiStyle.panel(graphics, x, TOP, width, height, alpha);
        int y = TOP + PADDING;
        drawScaled(graphics, font, heading.getVisualOrderText(), centerX, y, UiStyle.withAlpha(UiStyle.HEADING, alpha));
        y += LINE;
        graphics.fill(x + PADDING, y, x + width - PADDING, y + 1, UiStyle.withAlpha(UiStyle.LINE_SOFT, alpha));
        y += 5;
        for (FormattedCharSequence line : body) {
            drawScaled(graphics, font, line, centerX, y, UiStyle.withAlpha(UiStyle.TEXT, alpha));
            y += LINE;
        }
    }

    private static void drawScaled(GuiGraphics graphics, Font font, FormattedCharSequence text, int centerX, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }
}
