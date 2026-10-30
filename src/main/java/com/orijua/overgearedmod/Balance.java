package com.orijua.overgearedmod;

/**
 * Every tunable number of the game rules in one value. Rules take it as a parameter so they stay
 * pure and testable; the game passes {@link #current()} (the server config), tests pass {@link #DEFAULT}.
 */
public record Balance(
        int statPointsPerLevel,
        double killXpMultiplier,
        double killXpPerAttackDamage,
        double killXpPerArmor,
        double spawnerXpMultiplier,
        int xpBase,
        int xpPerLevel,
        float baseMana,
        float manaPerIntelligence,
        float baseManaRegen,
        float manaRegenPerIntelligence,
        double enhanceBaseChance,
        double enhanceChanceLossPerLevel,
        double enhanceChancePerDexterity,
        double enhanceMinChance,
        double deathXpLoss,
        int mobLevelSpread,
        double mobHealthPerLevel,
        double mobDamagePerLevel,
        double eliteChance,
        double eliteStatBonus,
        double eliteXpMultiplier,
        double eliteEquipmentChance) {

    public static final Balance DEFAULT = new Balance(
            5, 1.0, 2.0, 1.0, 0.25, 50, 25,
            20.0f, 5.0f, 1.0f, 0.2f,
            0.95, 0.09, 0.005, 0.05,
            0.10,
            2, 0.08, 0.05, 0.05, 0.5, 3.0, 0.25);

    /** Set only inside {@link #withOverride}; read by {@link #current()} before the config. */
    private static Balance override;

    /** The active server config, or the defaults before a world is loaded (e.g. on the title screen). */
    public static Balance current() {
        if (override != null) {
            return override;
        }
        return Config.isLoaded() ? Config.balance() : DEFAULT;
    }

    /**
     * For game tests: {@link #current()} returns {@code balance} while {@code action} runs, then the config again.
     * Safe because a game test runs from start to end on the server thread, with no other test in between.
     */
    public static void withOverride(Balance balance, Runnable action) {
        Balance previous = override;
        override = balance;
        try {
            action.run();
        } finally {
            override = previous;
        }
    }

    /** A copy with a different {@link #eliteEquipmentChance}, e.g. 1.0 so a test always gets the drop. */
    public Balance withEliteEquipmentChance(double chance) {
        return new Balance(statPointsPerLevel, killXpMultiplier, killXpPerAttackDamage, killXpPerArmor, spawnerXpMultiplier,
                xpBase, xpPerLevel, baseMana, manaPerIntelligence, baseManaRegen, manaRegenPerIntelligence,
                enhanceBaseChance, enhanceChanceLossPerLevel, enhanceChancePerDexterity, enhanceMinChance, deathXpLoss,
                mobLevelSpread, mobHealthPerLevel, mobDamagePerLevel, eliteChance, eliteStatBonus, eliteXpMultiplier, chance);
    }
}
