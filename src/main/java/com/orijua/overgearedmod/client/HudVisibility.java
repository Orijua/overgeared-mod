package com.orijua.overgearedmod.client;

import net.minecraft.client.Minecraft;

final class HudVisibility {
    private HudVisibility() {
    }

    /** Same rule as vanilla's health and food bars: hidden in creative, spectator and with F1. */
    static boolean showSurvivalBars(Minecraft minecraft) {
        return minecraft.player != null && !minecraft.options.hideGui
                && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer();
    }
}
