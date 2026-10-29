package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Balance;

import java.util.List;
import java.util.Map;

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

    /** XP for killing a mob, from its max health (with the defaults a zombie, 20 HP, gives 30). */
    public static int killXp(Balance balance, float victimMaxHealth) {
        return Math.max(1, (int) Math.round(victimMaxHealth * balance.killXpMultiplier()));
    }

    public static int smithingLevel(int smithingXp) {
        return (int) Math.floor(Math.sqrt(Math.max(0, smithingXp) / 10.0));
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

    /** Adds flat stat bonuses (from class, titles...) on top of the allocated stats. */
    public static PlayerStats withBonuses(PlayerStats stats, List<Map<StatType, Integer>> bonuses) {
        PlayerStats result = stats;
        for (Map<StatType, Integer> bonus : bonuses) {
            for (Map.Entry<StatType, Integer> entry : bonus.entrySet()) {
                result = result.with(entry.getKey(), result.get(entry.getKey()) + entry.getValue());
            }
        }
        return result;
    }

    /** Spends one free point on {@code stat}; returns the same instance if there are none. */
    public static PlayerStats allocate(PlayerStats stats, StatType stat) {
        if (stats.freePoints() <= 0) {
            return stats;
        }
        return stats.with(stat, stats.get(stat) + 1)
                .withProgress(stats.level(), stats.xp(), stats.freePoints() - 1);
    }
}
