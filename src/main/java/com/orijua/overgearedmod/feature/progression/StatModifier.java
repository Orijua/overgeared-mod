package com.orijua.overgearedmod.feature.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One contribution to a stat: allocated points, a class, a title and, later, equipment, talents, buffs...
 * {@link ProgressionRules#total} combines them: first every ADD, then the sum of every MULTIPLY on top.
 */
public record StatModifier(StatSource source, StatType stat, double value, Operation operation) {
    public enum Operation {
        /** Flat points: {@code 3} is +3. */
        ADD,
        /** Percentage of the summed points: {@code 0.1} is +10%. Several percentages add up before applying. */
        MULTIPLY
    }

    public static StatModifier add(StatSource source, StatType stat, double value) {
        return new StatModifier(source, stat, value, Operation.ADD);
    }

    public static StatModifier multiply(StatSource source, StatType stat, double value) {
        return new StatModifier(source, stat, value, Operation.MULTIPLY);
    }

    /** The flat bonuses of a class or title, as written in its JSON ({@code "bonus_stats": {"strength": 3}}). */
    public static List<StatModifier> fromBonuses(StatSource source, Map<StatType, Integer> bonuses) {
        List<StatModifier> modifiers = new ArrayList<>();
        bonuses.forEach((stat, value) -> modifiers.add(add(source, stat, value)));
        return modifiers;
    }

    /** The points the player put in each stat, so the allocated value is one more source like the rest. */
    public static List<StatModifier> allocated(PlayerStats stats) {
        List<StatModifier> modifiers = new ArrayList<>();
        for (StatType stat : StatType.values()) {
            modifiers.add(add(StatSource.ALLOCATED, stat, stats.get(stat)));
        }
        return modifiers;
    }
}
