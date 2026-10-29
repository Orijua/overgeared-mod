package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.client.ui.UiStyle;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;

/**
 * Mana bar above the food bar. Registered right above vanilla's food layer, so it takes the next
 * right-side row ({@link Gui#rightHeight}) and the air bubbles move up above it.
 */
final class ManaHudLayer implements LayeredDraw.Layer {
    private static final int WIDTH = 81;
    private static final int ROW_HEIGHT = 10;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!HudVisibility.showSurvivalBars(minecraft)) {
            return;
        }
        float maxMana = ProgressionRules.maxMana(Balance.current(), ProgressionService.effectiveStats(player).intelligence());
        float mana = player.getData(ModAttachments.MANA);

        Gui gui = minecraft.gui;
        int left = graphics.guiWidth() / 2 + 91 - WIDTH;
        int top = graphics.guiHeight() - gui.rightHeight + 2;
        gui.rightHeight += ROW_HEIGHT;

        UiStyle.bar(graphics, left, top, WIDTH, 5, maxMana <= 0 ? 0 : mana / maxMana, UiStyle.MANA);
    }
}
