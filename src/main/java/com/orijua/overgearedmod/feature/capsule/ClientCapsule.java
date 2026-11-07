package com.orijua.overgearedmod.feature.capsule;

/**
 * The client's side of connecting: when the "Connecting…" screen started and how long it lasts. No client classes, so
 * the network handler can use it on a dedicated server; the screen that draws it is in {@code capsule.client}.
 */
public final class ClientCapsule {
    private static final long MILLIS_PER_TICK = 50;

    private static volatile long startMillis;
    private static volatile long durationMillis;

    private ClientCapsule() {
    }

    /** {@code ticks} = 0 cancels. */
    public static void connecting(int ticks, long nowMillis) {
        startMillis = nowMillis;
        durationMillis = Math.max(0, ticks) * MILLIS_PER_TICK;
    }

    /** How far the connection is, 0 to 1, or -1 if nothing is connecting. */
    public static float fraction(long nowMillis) {
        long duration = durationMillis;
        if (duration <= 0) {
            return -1.0f;
        }
        float fraction = (nowMillis - startMillis) / (float) duration;
        if (fraction >= 1.0f) {
            // The server moves the player at 1; a late packet must not leave the screen up.
            return nowMillis - startMillis > duration + 1_000 ? -1.0f : 1.0f;
        }
        return Math.max(0.0f, fraction);
    }
}
