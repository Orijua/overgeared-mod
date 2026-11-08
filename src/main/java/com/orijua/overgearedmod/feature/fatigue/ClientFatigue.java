package com.orijua.overgearedmod.feature.fatigue;

/**
 * The client's side of the fatigue: the last debug snapshot the server sent and how the bar is shown. No client
 * classes, so the network handler can use it on a dedicated server; the layers that draw it are in {@code fatigue.client}.
 */
public final class ClientFatigue {
    /** How the energy bar shows: only while not full (the default), always, or not at all (the warnings still show). */
    public enum Display {
        AUTO, ALWAYS, HIDDEN;

        public Display next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static volatile FatigueDebugPayload debug = FatigueDebugPayload.HIDDEN;

    private ClientFatigue() {
    }

    public static void debug(FatigueDebugPayload payload) {
        debug = payload;
    }

    public static FatigueDebugPayload debug() {
        return debug;
    }
}
