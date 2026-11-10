package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.capsule.CapsuleTier;

/**
 * The tunable numbers of the capsule tiers, apart from {@link Balance}. The mod's own design, not the canon's. The basic
 * capsule has no numbers here: it uses the {@link FatigueBalance} as is.
 *
 * @param advancedSessionMinutes  real minutes of normal play from full energy to empty with the advanced capsule
 * @param advancedRecoveryMinutes real minutes to refill from empty in the real world after an advanced session
 * @param diamondSessionMinutes   the same for the diamond capsule
 * @param diamondRecoveryMinutes  the same for the diamond capsule
 * @param advancedLicenseWon      Won the advanced license costs at the terminal
 * @param diamondLicenseWon       Won the diamond license costs at the terminal
 * @param restedMinutes           minutes of "rested" bonus a diamond connection grants (0 turns it off)
 * @param restedXpPercent         extra level XP (percent) while rested
 * @param restedMinEnergy         the energy (0 to 1) needed when connecting to get the rested bonus
 */
public record CapsuleBalance(
        double advancedSessionMinutes,
        double advancedRecoveryMinutes,
        double diamondSessionMinutes,
        double diamondRecoveryMinutes,
        long advancedLicenseWon,
        long diamondLicenseWon,
        int restedMinutes,
        int restedXpPercent,
        double restedMinEnergy) {

    public static final double MAX_SESSION_MINUTES = 1440.0;
    public static final double MIN_RECOVERY_MINUTES = 0.1;
    public static final long MAX_LICENSE_WON = 1_000_000_000L;
    public static final int MAX_RESTED_MINUTES = 1440;
    public static final int MAX_RESTED_XP_PERCENT = 1000;

    public static final CapsuleBalance DEFAULT = new CapsuleBalance(55.0, 8.0, 75.0, 6.0, 3000L, 15000L, 10, 10, 0.90);

    public CapsuleBalance {
        checkRange("advancedSessionMinutes", advancedSessionMinutes, 1.0, MAX_SESSION_MINUTES);
        checkRange("diamondSessionMinutes", diamondSessionMinutes, 1.0, MAX_SESSION_MINUTES);
        checkRange("advancedRecoveryMinutes", advancedRecoveryMinutes, MIN_RECOVERY_MINUTES, MAX_SESSION_MINUTES);
        checkRange("diamondRecoveryMinutes", diamondRecoveryMinutes, MIN_RECOVERY_MINUTES, MAX_SESSION_MINUTES);
        checkRange("advancedLicenseWon", advancedLicenseWon, 1.0, MAX_LICENSE_WON);
        checkRange("diamondLicenseWon", diamondLicenseWon, 1.0, MAX_LICENSE_WON);
        checkRange("restedMinutes", restedMinutes, 0.0, MAX_RESTED_MINUTES);
        checkRange("restedXpPercent", restedXpPercent, 0.0, MAX_RESTED_XP_PERCENT);
        checkRange("restedMinEnergy", restedMinEnergy, 0.0, 1.0);
    }

    private static void checkRange(String name, double value, double min, double max) {
        // NaN fails both comparisons, so it is rejected too.
        if (!(value >= min && value <= max)) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ": " + value);
        }
    }

    /** Real minutes of normal play from full to empty with this tier; the basic one is the fatigue's. */
    public double sessionMinutes(CapsuleTier tier, FatigueBalance fatigue) {
        return switch (tier) {
            case BASIC -> fatigue.sessionMinutes();
            case ADVANCED -> advancedSessionMinutes;
            case DIAMOND -> diamondSessionMinutes;
        };
    }

    /** Real minutes to refill from empty after a session with this tier; the basic one is the fatigue's. */
    public double recoveryMinutes(CapsuleTier tier, FatigueBalance fatigue) {
        return switch (tier) {
            case BASIC -> fatigue.recoveryMinutes();
            case ADVANCED -> advancedRecoveryMinutes;
            case DIAMOND -> diamondRecoveryMinutes;
        };
    }

    /** Won the license of this tier costs. The basic capsule has no license. */
    public long licenseWon(CapsuleTier tier) {
        return switch (tier) {
            case BASIC -> throw new IllegalArgumentException("The basic capsule has no license");
            case ADVANCED -> advancedLicenseWon;
            case DIAMOND -> diamondLicenseWon;
        };
    }

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static CapsuleBalance override;

    /** The active server config, or the defaults before a world is loaded. */
    public static CapsuleBalance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.capsuleBalance() : DEFAULT;
    }

    /** For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again. */
    public static void withOverride(CapsuleBalance balance, Runnable action) {
        CapsuleBalance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }
}
