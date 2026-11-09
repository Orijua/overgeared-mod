package com.orijua.overgearedmod.core.config;

/**
 * The tunable numbers of the starter debt, apart from {@link Balance}. The mod's own design, not the canon's.
 *
 * @param starterDebt  Won owed for the starter capsule
 * @param quotaPercent percent of every work shift that goes to the debt while it is active
 */
public record DebtBalance(long starterDebt, int quotaPercent) {
    /** The most the starter debt can be. */
    public static final long MAX_STARTER_DEBT = 1_000_000_000L;

    public static final DebtBalance DEFAULT = new DebtBalance(6400L, 25);

    public DebtBalance {
        if (starterDebt < 1L || starterDebt > MAX_STARTER_DEBT) {
            throw new IllegalArgumentException("starterDebt must be between 1 and " + MAX_STARTER_DEBT + ": " + starterDebt);
        }
        if (quotaPercent < 0 || quotaPercent > 100) {
            throw new IllegalArgumentException("quotaPercent must be between 0 and 100: " + quotaPercent);
        }
    }

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static DebtBalance override;

    /** The active server config, or the defaults before a world is loaded. */
    public static DebtBalance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.debtBalance() : DEFAULT;
    }

    /** For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again. */
    public static void withOverride(DebtBalance balance, Runnable action) {
        DebtBalance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }
}
