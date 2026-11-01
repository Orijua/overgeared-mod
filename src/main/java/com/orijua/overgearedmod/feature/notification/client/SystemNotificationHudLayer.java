package com.orijua.overgearedmod.feature.notification.client;

import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.feature.notification.ClientNotifications;
import com.orijua.overgearedmod.feature.notification.NotificationQueue;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Satisfy system window: top-center panel with its crest, a light-blue heading and white text a bit
 * larger than the rest of the HUD, fading in and out, one at a time. A grand one (a hidden class, a Legendary piece,
 * a rare title) is larger, gold and pulsing, and when it appears its icon rises on the screen like a totem, with
 * totem particles around the player.
 */
public final class SystemNotificationHudLayer implements LayeredDraw.Layer {
    private static final int TOP = 30;
    private static final int PADDING = 9;
    private static final int MIN_WIDTH = 170;
    /** Text is drawn this much larger than normal; widths and line heights below are already scaled. */
    private static final float TEXT_SCALE = 1.2f;
    private static final float GRAND_SCALE = 1.5f;
    private static final int MAX_TEXT_WIDTH = 200;
    private static final int LINE = 12;
    private static final int GRAND_LINE = 15;

    /** The grand window already celebrated (identity), so its totem plays once. */
    private SystemNotification celebrated;

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
        SystemNotification notification = shown.notification();
        boolean grand = notification.kind().grand();
        if (grand && notification != celebrated) {
            celebrated = notification;
            celebrate(minecraft, notification);
        }
        float scale = grand ? GRAND_SCALE : TEXT_SCALE;
        int line = grand ? GRAND_LINE : LINE;
        Font font = minecraft.font;
        Component heading = notification.kind().heading();
        List<FormattedCharSequence> body = new ArrayList<>();
        for (Component text : notification.lines()) {
            body.addAll(font.split(text, MAX_TEXT_WIDTH));
        }

        int textWidth = font.width(heading);
        for (FormattedCharSequence text : body) {
            textWidth = Math.max(textWidth, font.width(text));
        }
        int width = Math.max(MIN_WIDTH, Math.round(textWidth * scale) + 2 * PADDING);
        int height = PADDING + line + 5 + body.size() * line + PADDING - 2;
        int x = (graphics.guiWidth() - width) / 2;
        int centerX = x + width / 2;
        float alpha = shown.alpha();

        UiStyle.panel(graphics, x, TOP, width, height, alpha);
        if (grand) {
            // A gold glow that breathes around the window.
            float pulse = 0.35f + 0.25f * Mth.sin(Util.getMillis() / 250.0f);
            int glow = UiStyle.withAlpha(0xFFFFD700, alpha * pulse);
            graphics.fill(x - 2, TOP - 2, x + width + 2, TOP, glow);
            graphics.fill(x - 2, TOP + height, x + width + 2, TOP + height + 2, glow);
            graphics.fill(x - 2, TOP, x, TOP + height, glow);
            graphics.fill(x + width, TOP, x + width + 2, TOP + height, glow);
        }
        int y = TOP + PADDING;
        drawScaled(graphics, font, heading.getVisualOrderText(), centerX, y, UiStyle.withAlpha(grand ? UiStyle.GOLD : UiStyle.HEADING, alpha), scale);
        y += line;
        graphics.fill(x + PADDING, y, x + width - PADDING, y + 1, UiStyle.withAlpha(grand ? UiStyle.GOLD : UiStyle.LINE_SOFT, alpha));
        y += 5;
        for (FormattedCharSequence text : body) {
            drawScaled(graphics, font, text, centerX, y, UiStyle.withAlpha(UiStyle.TEXT, alpha), scale);
            y += line;
        }
    }

    /** The totem moment: the icon rises on the screen and particles burst around the player. */
    private static void celebrate(Minecraft minecraft, SystemNotification notification) {
        notification.icon().ifPresent(minecraft.gameRenderer::displayItemActivation);
        minecraft.particleEngine.createTrackingEmitter(minecraft.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
    }

    private static void drawScaled(GuiGraphics graphics, Font font, FormattedCharSequence text, int centerX, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }
}
