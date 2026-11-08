package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.fatigue.ClientFatigue;
import net.neoforged.neoforge.common.ModConfigSpec;

/** What each player decides for their own screen, in the client config (never synced). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<ClientFatigue.Display> FATIGUE_DISPLAY = BUILDER
            .comment("How the energy bar shows: AUTO only while it is not full, ALWAYS, or HIDDEN (the warnings still show).",
                    "The Overgeared fatigue key cycles through them.")
            .defineEnum("fatigueDisplay", ClientFatigue.Display.AUTO);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static ClientFatigue.Display fatigueDisplay() {
        return SPEC.isLoaded() ? FATIGUE_DISPLAY.get() : ClientFatigue.Display.AUTO;
    }

    public static void setFatigueDisplay(ClientFatigue.Display display) {
        if (SPEC.isLoaded()) {
            FATIGUE_DISPLAY.set(display);
            FATIGUE_DISPLAY.save();
        }
    }
}
