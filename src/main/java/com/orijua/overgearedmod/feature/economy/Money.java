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

    /**
     * An amount of Won, emeralds or debt for the player to read: digits in groups of three with a dot ({@code 6.400}),
     * the same in every language and on every JVM (it never reads the default locale). A leading minus is kept.
     */
    public static String format(long amount) {
        String digits = Long.toString(amount);
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3 + 1);
        if (negative) {
            out.append('-');
        }
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append('.');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    /** A price computed with doubles (shares, saturation), rounded down to whole units. */
    public static long floor(double value) {
        return (long) Math.floor(value + EPSILON);
    }
}
