package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.client.ui.SatisfyScreen;
import com.orijua.overgearedmod.client.ui.UiStyle;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Tooltips inside the mod's screens use the Satisfy window colors instead of vanilla's purple frame. */
final class SatisfyTooltips {
    private SatisfyTooltips() {
    }

    static void onTooltipColor(RenderTooltipEvent.Color event) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof SatisfyScreen || screen instanceof SmithingAnvilScreen) {
            event.setBackgroundStart(UiStyle.WINDOW_TOP | 0xF0000000);
            event.setBackgroundEnd(UiStyle.WINDOW_BOTTOM | 0xF0000000);
            event.setBorderStart(UiStyle.LINE_BRIGHT);
            event.setBorderEnd(UiStyle.LINE_SOFT);
        }
    }
}
