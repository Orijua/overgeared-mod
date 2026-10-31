package com.orijua.overgearedmod.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * One page of the Satisfy window. A tab adds its widgets in {@link #init} and draws its text in
 * {@link #render}; data is always read fresh from the local player, which the server keeps synced.
 */
public abstract class SatisfyTab {
    private final Component title;
    protected int left;
    protected int top;
    protected int width;
    protected int height;

    protected SatisfyTab(Component title) {
        this.title = title;
    }

    public Component title() {
        return title;
    }

    public final void init(int left, int top, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        addWidgets(addWidget);
    }

    protected void addWidgets(Consumer<AbstractWidget> addWidget) {
    }

    /** Width of the Satisfy window while this tab is open. */
    public int panelWidth() {
        return SatisfyScreen.PANEL_WIDTH;
    }

    public void tick() {
    }

    /** Mouse wheel over the window. Return true if the tab scrolled, so its widgets are rebuilt. */
    public boolean mouseScrolled(double wheel) {
        return false;
    }

    /** Left click on the window that no widget took. Return true if the tab changed, so its widgets are rebuilt. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        return false;
    }

    /** Return true when the data behind the widgets changed (e.g. a class was chosen) so the window rebuilds them. */
    public boolean needsRebuild() {
        return false;
    }

    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY);

    protected static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
