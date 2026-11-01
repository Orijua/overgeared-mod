package com.orijua.overgearedmod.core.client;

import net.minecraft.client.Minecraft;

public final class HudVisibility {
    private HudVisibility() {
    }

    /** Same rule as vanilla's health and food bars: hidden in creative, spectator and with F1. */
    public static boolean showSurvivalBars(Minecraft minecraft) {
        return minecraft.player != null && !minecraft.options.hideGui
                && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer();
    }
}
