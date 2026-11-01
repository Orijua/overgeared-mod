package com.orijua.overgearedmod.feature.smelting;

/**
 * The bellows minigame of a manual melt, played on the client (pure, so it is unit tested). For 6 seconds the metal
 * cools by itself and every pump of the bellows heats it; the precision is the share of that time spent inside the
 * metal's zone (its melting temperature to {@link SmelterRules#ZONE_WIDTH} above). It starts in the middle of the zone.
 */
public final class SmeltingMinigame {
    public static final long DURATION_MS = 6000;
    /** Degrees one pump of the bellows adds, and degrees lost per second. */
    public static final double PUMP = 60;
    public static final double COOL_PER_SECOND = 90;

    private final int zoneMin;
    private final int zoneMax;
    private double temperature;
    private long elapsed;
    private long inZone;

    public SmeltingMinigame(int meltingTemperature) {
        this.zoneMin = meltingTemperature;
        this.zoneMax = meltingTemperature + SmelterRules.ZONE_WIDTH;
        this.temperature = meltingTemperature + SmelterRules.ZONE_WIDTH / 2.0;
    }

    public void pump() {
        if (!done()) {
            temperature = Math.min(SmelterRules.MAX_TEMPERATURE, temperature + PUMP);
        }
    }

    /**
     * Advances the game by {@code deltaMs}: the metal cools steadily, and the part of that time it spends inside the
     * zone adds up (exactly, so a long frame counts the same as many short ones).
     */
    public void update(long deltaMs) {
        if (done() || deltaMs <= 0) {
            return;
        }
        long step = Math.min(deltaMs, DURATION_MS - elapsed);
        double start = temperature;
        double end = Math.max(SmelterRules.AMBIENT, start - COOL_PER_SECOND * step / 1000.0);
        double degreesInZone = Math.max(0, Math.min(start, zoneMax) - Math.max(end, zoneMin));
        inZone += Math.min(step, Math.round(degreesInZone * 1000 / COOL_PER_SECOND));
        temperature = end;
        elapsed += step;
    }

    public boolean inZone() {
        return temperature >= zoneMin && temperature <= zoneMax;
    }

    public boolean done() {
        return elapsed >= DURATION_MS;
    }

    /** Share of the game spent in the zone, 0-1. */
    public double precision() {
        return inZone / (double) DURATION_MS;
    }

    public double temperature() {
        return temperature;
    }

    public int zoneMin() {
        return zoneMin;
    }

    public int zoneMax() {
        return zoneMax;
    }

    /** How far the game is, 0-1. */
    public double progress() {
        return elapsed / (double) DURATION_MS;
    }
}
