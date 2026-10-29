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
        double deathXpLoss) {

    public static final Balance DEFAULT = new Balance(
            5, 1.0, 2.0, 1.0, 0.25, 50, 25,
            20.0f, 5.0f, 1.0f, 0.2f,
            0.95, 0.09, 0.005, 0.05,
            0.10);

    /** The active server config, or the defaults before a world is loaded (e.g. on the title screen). */
    public static Balance current() {
        return Config.isLoaded() ? Config.balance() : DEFAULT;
    }
}
