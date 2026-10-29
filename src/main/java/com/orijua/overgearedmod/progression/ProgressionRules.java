package com.orijua.overgearedmod.progression;

/**
 * Pure progression formulas (project design, not canon numbers).
 * No Minecraft state here, so the rules are easy to read, tweak and unit-test.
 */
public final class ProgressionRules {
    private ProgressionRules() {
    }

    public static int xpToNextLevel(int level) {
        return 50 + 25 * level;
    }

    /** XP for killing a mob, from its max health (a zombie with 20 HP gives 30). */
    public static int killXp(float victimMaxHealth, double multiplier) {
        return Math.max(1, (int) Math.round(victimMaxHealth * 1.5 * multiplier));
    }

    public static int smithingLevel(int smithingXp) {
        return (int) Math.floor(Math.sqrt(Math.max(0, smithingXp) / 10.0));
    }

    public static float maxMana(int intelligence) {
        return 20.0f + 5.0f * intelligence;
    }

    /** Mana regenerated per second. */
    public static float manaRegenPerSecond(int intelligence) {
        return 1.0f + 0.2f * intelligence;
    }

    public record XpResult(PlayerStats stats, int levelsGained) {
    }

    /** Adds XP and resolves any number of level ups, each granting {@code pointsPerLevel}. */
    public static XpResult addXp(PlayerStats stats, int amount, int pointsPerLevel) {
        int level = stats.level();
        int xp = stats.xp() + Math.max(0, amount);
        int points = stats.freePoints();
        int gained = 0;
        while (xp >= xpToNextLevel(level)) {
            xp -= xpToNextLevel(level);
            level++;
            points += pointsPerLevel;
            gained++;
        }
        return new XpResult(stats.withProgress(level, xp, points), gained);
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
