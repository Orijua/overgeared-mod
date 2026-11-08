package com.orijua.overgearedmod.feature.fatigue.client;

import com.orijua.overgearedmod.core.client.UiStyle;
import com.orijua.overgearedmod.feature.fatigue.ClientFatigue;
import com.orijua.overgearedmod.feature.fatigue.FatigueDebugPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** The operator's debug panel (`/satisfy debug fatigue overlay`): the fatigue's numbers, to check how it works. */
public final class FatigueDebugLayer implements LayeredDraw.Layer {
    private static final String[] ACTIVITIES = {"reposo (0,5x)", "normal (1x)", "combate (2x)"};

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        FatigueDebugPayload p = ClientFatigue.debug();
        Minecraft minecraft = Minecraft.getInstance();
        if (!p.visible() || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        List<String> lines = List.of(
                String.format(Locale.ROOT, "Energía: %.2f %%", p.energy() * 100),
                "Actividad: " + ACTIVITIES[Math.clamp(p.activity(), 0, 2)],
                String.format(Locale.ROOT, "Gasto: %.4f %%/s", p.drainPerSecond() * 100),
                "Hasta vaciarse: " + (p.secondsToEmpty() < 0 ? "-" : p.secondsToEmpty() + " s"),
                "Margen: " + (p.graceLeft() < 0 ? "no" : p.graceLeft() + " s"),
                "Último combate: " + (p.secondsSinceCombat() < 0 ? "-" : "hace " + p.secondsSinceCombat() + " s"),
                "Inmortal: " + (p.protectedNow() ? "sí" : "no"));
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        UiStyle.panel(graphics, 2, 2, width + 8, lines.size() * 10 + 6, 0.85f);
        for (int i = 0; i < lines.size(); i++) {
            UiStyle.text(graphics, minecraft.font, Component.literal(lines.get(i)), 6, 6 + i * 10, i == 0 ? UiStyle.GOLD : UiStyle.TEXT);
        }
    }
}
