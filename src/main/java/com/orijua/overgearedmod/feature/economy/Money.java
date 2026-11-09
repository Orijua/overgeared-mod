package com.orijua.overgearedmod.feature.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How the economy rounds money and shows the commission: the one place for it, so the Trade tab and the terminal never
 * disagree by a Won. Money always rounds down (the player never gets a Won the site did not pay). Pure, no Minecraft.
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

    /**
     * What is left of {@code gross} after the commission, rounded down. Exact: BigDecimal.valueOf(0.05) is 0.05, so 94.05
     * never becomes 94.04999 or 94.0500001, however large the gross is.
     */
    public static long afterCommission(long gross, double commission) {
        return BigDecimal.valueOf(gross)
                .multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(commission)))
                .setScale(0, RoundingMode.FLOOR).longValueExact();
    }

    /** A share as a percent for the player: "5" for 0.05, "2.5" for 0.025. */
    public static String percent(double share) {
        return BigDecimal.valueOf(share * 100).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
