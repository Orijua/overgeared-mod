package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.core.config.Balance;

import java.util.List;

/**
 * Pure progression formulas (project design, not canon numbers).
 * No Minecraft state here; every tunable number comes from {@link Balance}.
 */
public final class ProgressionRules {
    private ProgressionRules() {
    }

    public static int xpToNextLevel(Balance balance, int level) {
        return balance.xpBase() + balance.xpPerLevel() * level;
    }

    /** What a mob was like when it died, as far as XP is concerned. */
    public record Victim(float maxHealth, double attackDamage, int armor, boolean fromSpawner, boolean elite) {
    }

    /** XP for a kill: tougher and harder-hitting mobs (so higher levels) give more, elites even more; spawner mobs give less (anti-farming). */
    public static int killXp(Balance balance, Victim victim) {
        double strength = victim.maxHealth()
                + victim.attackDamage() * balance.killXpPerAttackDamage()
                + victim.armor() * balance.killXpPerArmor();
        double xp = strength * balance.killXpMultiplier() * (victim.fromSpawner() ? balance.spawnerXpMultiplier() : 1.0)
                * (victim.elite() ? balance.eliteXpMultiplier() : 1.0);
        return Math.max(1, (int) Math.round(xp));
    }

    public static int smithingLevel(int smithingXp) {
        return (int) Math.floor(Math.sqrt(Math.max(0, smithingXp) / 10.0));
    }

    /** The smithing experience at which {@code level} starts (the inverse of {@link #smithingLevel}). */
    public static int smithingXpFor(int level) {
        return Math.max(0, level) * Math.max(0, level) * 10;
    }

    public static float maxMana(Balance balance, int intelligence) {
        return balance.baseMana() + balance.manaPerIntelligence() * intelligence;
    }

    /** Mana regenerated per second. */
    public static float manaRegenPerSecond(Balance balance, int intelligence) {
        return balance.baseManaRegen() + balance.manaRegenPerIntelligence() * intelligence;
    }

    public record XpResult(PlayerStats stats, int levelsGained) {
    }

    /** Adds XP and resolves any number of level ups, each granting the configured stat points. */
    public static XpResult addXp(Balance balance, PlayerStats stats, int amount) {
        int level = stats.level();
        int xp = stats.xp() + Math.max(0, amount);
        int points = stats.freePoints();
        int gained = 0;
        while (xp >= xpToNextLevel(balance, level)) {
            xp -= xpToNextLevel(balance, level);
            level++;
            points += balance.statPointsPerLevel();
            gained++;
        }
        return new XpResult(stats.withProgress(level, xp, points), gained);
    }

    /** Final value of one stat: every ADD summed, then raised by the sum of every MULTIPLY, rounded down. */
    public static int total(StatType stat, List<StatModifier> modifiers) {
        double flat = 0;
        double percent = 0;
        for (StatModifier modifier : modifiers) {
            if (modifier.stat() != stat) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD -> flat += modifier.value();
                case MULTIPLY -> percent += modifier.value();
            }
        }
        // The epsilon keeps 10 * 1.1 = 11.000000000000002 or 10.999999999999998 from flooring to 10.
        return (int) Math.floor(flat * (1 + percent) + 1e-9);
    }

    /**
     * {@code stats} with each base stat replaced by its {@link #total}. The modifiers must include the
     * allocated points ({@link StatModifier#allocated}); level, XP and the rest are kept from {@code stats}.
     */
    public static PlayerStats effective(PlayerStats stats, List<StatModifier> modifiers) {
        PlayerStats result = stats;
        for (StatType stat : StatType.values()) {
            result = result.with(stat, total(stat, modifiers));
        }
        return result;
    }

    /** Death penalty: loses part of the XP gathered towards the next level, never a whole level. */
    public static PlayerStats applyDeathPenalty(Balance balance, PlayerStats stats) {
        int lost = (int) Math.floor(stats.xp() * balance.deathXpLoss());
        return stats.withProgress(stats.level(), stats.xp() - lost, stats.freePoints());
    }

    /** Takes every allocated point back into free points (bonuses from class and titles are untouched). */
    public static PlayerStats refundAll(PlayerStats stats) {
        int allocated = 0;
        PlayerStats result = stats;
        for (StatType stat : StatType.values()) {
            allocated += stats.get(stat);
            result = result.with(stat, 0);
        }
        return result.withProgress(stats.level(), stats.xp(), stats.freePoints() + allocated);
    }

    /** Spends one free point on {@code stat}; returns the same instance if there are none. */
    public static PlayerStats allocate(PlayerStats stats, StatType stat) {
        return allocate(stats, stat, 1);
    }

    /**
     * Spends up to {@code amount} free points on {@code stat} (all that are left if fewer);
     * returns the same instance if there are none or {@code amount} isn't positive.
     */
    public static PlayerStats allocate(PlayerStats stats, StatType stat, int amount) {
        int spent = Math.min(amount, stats.freePoints());
        if (spent <= 0) {
            return stats;
        }
        return stats.with(stat, stats.get(stat) + spent)
                .withProgress(stats.level(), stats.xp(), stats.freePoints() - spent);
    }
}
