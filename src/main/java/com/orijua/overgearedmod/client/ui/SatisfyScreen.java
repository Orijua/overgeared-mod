package com.orijua.overgearedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/** The Satisfy window (key K): a panel with one button per tab on top and the active tab below. */
public class SatisfyScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 210;
    private static final int TAB_BAR_HEIGHT = 26;
    private static int lastTab;

    private final List<SatisfyTab> tabs;
    private int left;
    private int top;

    public SatisfyScreen(List<Supplier<SatisfyTab>> tabFactories) {
        super(Component.translatable("screen.overgearedmod.status"));
        this.tabs = tabFactories.stream().map(Supplier::get).toList();
        lastTab = Math.min(lastTab, tabs.size() - 1);
    }

    private SatisfyTab current() {
        return tabs.get(lastTab);
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;

        int tabWidth = (PANEL_WIDTH - 8) / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            int index = i;
            Button button = Button.builder(tabs.get(i).title(), b -> select(index))
                    .bounds(left + 4 + i * tabWidth, top + 4, tabWidth - 2, 18)
                    .build();
            button.active = i != lastTab;
            addRenderableWidget(button);
        }
        current().init(left + 8, top + TAB_BAR_HEIGHT + 4, PANEL_WIDTH - 16, PANEL_HEIGHT - TAB_BAR_HEIGHT - 12,
                this::addRenderableWidget);
    }

    private void select(int index) {
        lastTab = index;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        current().tick();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiStyle.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        current().render(graphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
