package com.orijua.overgearedmod.feature.capsule;

import java.util.Locale;

/** How the capsule screens and debug commands write numbers, so the same amount reads the same everywhere. */
public final class CapsuleText {
    private CapsuleText() {
    }

    /** A time as "mm:ss" ("30:00", "1440:00" for a day), the same in every locale; a negative time is "00:00". */
    public static String clock(int seconds) {
        long total = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60);
    }

    /**
     * A number of minutes: whole numbers without decimals ("55"), anything else with one ("7.5"), always with a dot
     * whatever the player's locale. A value that rounds to a whole number ("7.96") is written as such ("8").
     */
    public static String minutes(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "-";
        }
        long tenths = Math.round(value * 10.0);
        if (tenths % 10 == 0) {
            return Long.toString(tenths / 10);
        }
        return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }
}
