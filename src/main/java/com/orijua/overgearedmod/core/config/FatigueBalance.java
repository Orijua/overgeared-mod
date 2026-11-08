package com.orijua.overgearedmod.core.config;

import java.util.List;

/**
 * The tunable numbers of the fatigue, apart from {@link Balance} (which is already long). Rules take it as a
 * parameter so they stay pure; the game passes {@link #current()} (the server config), tests pass {@link #DEFAULT}.
 * Every number is the mod's own design, not the canon's (the canon only says that a connection time exists, ch. 1488).
 *
 * @param enabled             off: nothing tires, nothing is refused, nothing forces a disconnection
 * @param sessionMinutes      real minutes of normal play from full energy to empty
 * @param restMultiplier      how much of the normal drain a player who stands still pays
 * @param combatMultiplier    the same while fighting (damage given or taken in the last seconds)
 * @param forgeCostSeconds    seconds of normal play that each forged item costs, on top of the drain
 * @param recoveryMinutes     real minutes to go from empty to full while resting in the real world
 * @param minEnergyToConnect  the capsule refuses to connect below this energy (0 to 1)
 * @param graceSeconds        time between running out and the forced disconnection
 * @param warnAt              energies (1 to 0, descending) that each trigger a warning once when crossed downwards
 * @param sleepRecovery       energy (0 to 1) that sleeping through the night in the real world gives back
 */
public record FatigueBalance(
        boolean enabled,
        double sessionMinutes,
        double restMultiplier,
        double combatMultiplier,
        double forgeCostSeconds,
        double recoveryMinutes,
        double minEnergyToConnect,
        int graceSeconds,
        List<Double> warnAt,
        double sleepRecovery) {

    public static final FatigueBalance DEFAULT = new FatigueBalance(
            true, 40.0, 0.5, 2.0, 20.0, 10.0, 0.25, 15, List.of(0.15, 0.08, 0.02), 0.30);

    public FatigueBalance {
        warnAt = List.copyOf(warnAt);
    }

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static FatigueBalance override;

    /** The active server config, or the defaults before a world is loaded (e.g. on the title screen). */
    public static FatigueBalance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.fatigueBalance() : DEFAULT;
    }

    /** For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again. */
    public static void withOverride(FatigueBalance balance, Runnable action) {
        FatigueBalance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }
}
