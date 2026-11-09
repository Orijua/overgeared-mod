package com.orijua.overgearedmod.feature.economy;

/**
 * How the economy rounds money: the one place for it, so the Trade tab and the terminal never disagree by a Won.
 * Money always rounds down (the player never gets a unit the site did not pay). Pure, no Minecraft.
 */
public final class Money {
    /** Absorbs floating point noise before rounding down, so 94.99999999999999 counts as 95. */
    private static final double EPSILON = 1e-9;

    private Money() {
    }

    /** A price computed with doubles (shares, saturation), rounded down to whole units. */
    public static long floor(double value) {
        return (long) Math.floor(value + EPSILON);
    }
}
