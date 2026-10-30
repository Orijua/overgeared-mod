package com.orijua.overgearedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A yes/no Satisfy window over the screen that opened it, for actions that can't be undone
 * (e.g. claiming a quest that replaces the player's class). Either answer goes back to {@code parent}.
 */
public class SatisfyConfirmScreen extends Screen {
    private static final int WIDTH = 230;
    private static final int PADDING = 10;
    private static final int BUTTON_WIDTH = 80;

    private final Screen parent;
    private final Component message;
    private final Runnable onConfirm;
    private List<FormattedCharSequence> lines = List.of();
    private int left;
    private int top;
    private int height;

    public SatisfyConfirmScreen(Screen parent, Component title, Component message, Runnable onConfirm) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        lines = font.split(message, WIDTH - 2 * PADDING);
        height = PADDING + UiStyle.LINE + 4 + lines.size() * UiStyle.LINE + 8 + 18 + PADDING;
        left = (width - WIDTH) / 2;
        top = (super.height - height) / 2;
        int buttonY = top + height - PADDING - 18;
        addRenderableWidget(Button.builder(CommonComponents.GUI_YES, b -> {
            onConfirm.run();
            onClose();
        }).bounds(left + WIDTH / 2 - BUTTON_WIDTH - 4, buttonY, BUTTON_WIDTH, 18).build(SatisfyButton::new));
        addRenderableWidget(Button.builder(CommonComponents.GUI_NO, b -> onClose())
                .bounds(left + WIDTH / 2 + 4, buttonY, BUTTON_WIDTH, 18).build(SatisfyButton::new));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiStyle.panel(graphics, left, top, WIDTH, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = left + WIDTH / 2;
        int y = top + PADDING;
        graphics.drawCenteredString(font, title, centerX, y, UiStyle.HEADING);
        y += UiStyle.LINE;
        graphics.fill(left + PADDING, y, left + WIDTH - PADDING, y + 1, UiStyle.LINE_SOFT);
        y += 4;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, centerX, y, UiStyle.TEXT);
            y += UiStyle.LINE;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
