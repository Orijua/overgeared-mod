package com.orijua.overgearedmod.feature.smithing;

/**
 * The manual forging minigame, as pure state driven by timestamps (ms), so the screen and the unit tests
 * play it the same way.
 * <ol>
 *   <li><b>Heat:</b> hold to heat the metal and release inside the good temperature band. Holding to the top
 *   burns it (heat score 0).</li>
 *   <li><b>Hammer:</b> {@link #STRIKES} strikes. A marker sweeps a bar back and forth, a bit faster each strike;
 *   strike when it crosses the middle. Each strike restarts the sweep from the edge, so mashing scores 0.</li>
 * </ol>
 * {@link #precision()} mixes both (heat 30 %, strikes 70 %) into 0-1.
 */
public final class ForgingMinigame {
    public enum Phase { HEAT, HAMMER, DONE }

    public static final int STRIKES = 5;
    /** Holding from cold to burnt takes this long. */
    public static final long HEAT_FULL_MS = 2500;
    /** Width of the good temperature band, as a fraction of the bar. */
    public static final double ZONE_WIDTH = 0.12;
    /** Time for the marker to go across and back on the first strike; every strike after is quicker. */
    public static final long FIRST_SWEEP_MS = 1600;
    private static final long SWEEP_SPEEDUP_MS = 220;
    private static final long FASTEST_SWEEP_MS = 600;
    /** Strikes this close to the middle (either side) count as perfect. */
    public static final double PERFECT_MARGIN = 0.05;
    /** Heating outside the band still scores something within this distance of it. */
    private static final double HEAT_TOLERANCE = 0.25;
    private static final double HEAT_WEIGHT = 0.3;

    private final double zoneStart;
    private Phase phase = Phase.HEAT;
    private long heatStart = -1;
    private double heatScore;
    private long sweepStart;
    private final double[] strikes = new double[STRIKES];
    private int strikeCount;

    /** @param zoneStart where the good temperature band begins (0-1); the screen picks it at random. */
    public ForgingMinigame(double zoneStart) {
        this.zoneStart = Math.clamp(zoneStart, 0, 1 - ZONE_WIDTH);
    }

    public Phase phase() {
        return phase;
    }

    public double zoneStart() {
        return zoneStart;
    }

    public boolean heating() {
        return phase == Phase.HEAT && heatStart >= 0;
    }

    public double heatScore() {
        return heatScore;
    }

    public int strikesDone() {
        return strikeCount;
    }

    /** Score of the last strike (0-1), or -1 before the first one. */
    public double lastStrike() {
        return strikeCount == 0 ? -1 : strikes[strikeCount - 1];
    }

    // ---- Heat ----

    public void startHeating(long now) {
        if (phase == Phase.HEAT && heatStart < 0) {
            heatStart = now;
        }
    }

    /** 0 = cold, 1 = burnt. Stays at 0 until the player starts holding. */
    public double temperature(long now) {
        if (heatStart < 0) {
            return 0;
        }
        return Math.min(1, (now - heatStart) / (double) HEAT_FULL_MS);
    }

    public void stopHeating(long now) {
        if (heating()) {
            finishHeat(temperature(now), now);
        }
    }

    /** Call every frame: holding to the top burns the metal and moves on to the strikes. */
    public void update(long now) {
        if (heating() && temperature(now) >= 1) {
            finishHeat(1, now);
        }
    }

    private void finishHeat(double temperature, long now) {
        heatScore = scoreHeat(temperature);
        phase = Phase.HAMMER;
        sweepStart = now;
    }

    private double scoreHeat(double temperature) {
        if (temperature >= 1) {
            return 0;
        }
        double distance = temperature < zoneStart ? zoneStart - temperature : Math.max(0, temperature - (zoneStart + ZONE_WIDTH));
        return Math.max(0, 1 - distance / HEAT_TOLERANCE);
    }

    // ---- Hammer ----

    /** How long the marker takes to go across and back for strike {@code index} (0-based). */
    public static long sweepMillis(int index) {
        return Math.max(FASTEST_SWEEP_MS, FIRST_SWEEP_MS - SWEEP_SPEEDUP_MS * index);
    }

    /** Marker position on the bar, 0-1: from the left edge to the right one and back. */
    public double markerPosition(long now) {
        if (phase != Phase.HAMMER) {
            return 0;
        }
        long period = sweepMillis(strikeCount);
        double t = Math.floorMod(now - sweepStart, period) / (double) period;
        return t < 0.5 ? t * 2 : 2 - t * 2;
    }

    /** Strikes now; returns its score (0-1), or -1 outside the hammer phase. */
    public double strike(long now) {
        if (phase != Phase.HAMMER) {
            return -1;
        }
        double distance = Math.abs(markerPosition(now) - 0.5);
        double score = Math.clamp(1 - (distance - PERFECT_MARGIN) / (0.5 - PERFECT_MARGIN), 0, 1);
        strikes[strikeCount++] = score;
        sweepStart = now;
        if (strikeCount == STRIKES) {
            phase = Phase.DONE;
        }
        return score;
    }

    /** The overall result, 0-1: what the server turns into a better or worse rating. */
    public double precision() {
        double total = 0;
        for (int i = 0; i < strikeCount; i++) {
            total += strikes[i];
        }
        return HEAT_WEIGHT * heatScore + (1 - HEAT_WEIGHT) * (total / STRIKES);
    }
}
