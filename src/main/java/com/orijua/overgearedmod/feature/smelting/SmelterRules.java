package com.orijua.overgearedmod.feature.smelting;

/**
 * Pure rules of the smelter (no Minecraft state). Own design inspired by the canon, where smelting blue orichalcum
 * takes advanced smithing: every metal needs the furnace above its melting temperature, fuel decides how hot it can
 * get, and a smith who works the bellows by hand melts faster and purer, at the risk of losing metal to slag.
 */
public final class SmelterRules {
    /** Temperatures in degrees: a cold smelter, and the hottest it can get. */
    public static final int AMBIENT = 20;
    public static final int MAX_TEMPERATURE = 1800;
    /** Degrees gained per tick while the fuel burns below its heat, and lost per tick while it burns above it. */
    public static final int HEAT_RATE = 6;
    public static final int COOL_RATE = 3;
    /** Degrees lost per tick once nothing burns: slowly, as a big furnace keeps its heat. */
    public static final int IDLE_COOL_RATE = 1;
    /** The bellows' good zone: from the metal's melting temperature to this many degrees above it. */
    public static final int ZONE_WIDTH = 150;

    /** Automatic melting: a chance of a second ingot, 1 % per smithing level, up to this. */
    public static final double MAX_AUTO_EXTRA_CHANCE = 0.25;
    /** Manual melting: from this precision up, a growing chance of a second ingot, up to the maximum. */
    public static final double MANUAL_EXTRA_FROM = 0.7;
    public static final double MAX_MANUAL_EXTRA_CHANCE = 0.6;
    /** Manual melting: below this precision, a growing chance that the metal is lost as slag, up to the maximum. */
    public static final double SLAG_BELOW = 0.4;
    public static final double MAX_SLAG_CHANCE = 0.6;
    public static final int MANUAL_XP_MULTIPLIER = 2;
    /**
     * A manual melt result is only believed between these many ticks after it began. The minigame lasts 6 s (120 ticks
     * at 20 TPS); the minimum is half of that, so a server down to about 10 TPS still accepts a real game.
     */
    public static final int MIN_MANUAL_TICKS = 60;
    public static final int MAX_MANUAL_TICKS = 20 * 120;

    private SmelterRules() {
    }

    /**
     * One tick of heat: burning fuel pulls the temperature towards its {@code fuelHeat}, up quickly or down to it;
     * with the fire out the smelter cools slowly, never below the ambient.
     */
    public static int nextTemperature(int current, int fuelHeat, boolean burning) {
        if (!burning) {
            return Math.max(AMBIENT, current - IDLE_COOL_RATE);
        }
        return current < fuelHeat ? Math.min(fuelHeat, current + HEAT_RATE) : Math.max(fuelHeat, current - COOL_RATE);
    }

    /** Fuel ticks spent per game tick: heating up from cold costs double, holding the heat the normal rate. */
    public static int fuelTicksPerTick(int temperature, int fuelHeat) {
        return temperature < fuelHeat ? 2 : 1;
    }

    public static double autoExtraChance(int smithingLevel) {
        return Math.min(MAX_AUTO_EXTRA_CHANCE, Math.max(0, smithingLevel) * 0.01);
    }

    public static double manualExtraChance(double precision) {
        double p = clampPrecision(precision);
        return p < MANUAL_EXTRA_FROM ? 0 : (p - MANUAL_EXTRA_FROM) / (1 - MANUAL_EXTRA_FROM) * MAX_MANUAL_EXTRA_CHANCE;
    }

    public static double slagChance(double precision) {
        double p = clampPrecision(precision);
        return p >= SLAG_BELOW ? 0 : (SLAG_BELOW - p) / SLAG_BELOW * MAX_SLAG_CHANCE;
    }

    /**
     * Ingots a manual melt gives for a precision and a roll in [0, 1): 0 (slag) under the slag chance, 2 under the
     * extra chance, 1 otherwise. The slag chance is checked first.
     */
    public static int manualIngots(double precision, double roll) {
        if (roll < slagChance(precision)) {
            return 0;
        }
        return roll < manualExtraChance(precision) ? 2 : 1;
    }

    /** Whatever the client sent, as a number from 0 to 1 (NaN counts as 0). */
    public static double clampPrecision(double precision) {
        return Double.isNaN(precision) ? 0 : Math.clamp(precision, 0.0, 1.0);
    }

    public static boolean plausibleManualDuration(long ticks) {
        return ticks >= MIN_MANUAL_TICKS && ticks <= MAX_MANUAL_TICKS;
    }
}
