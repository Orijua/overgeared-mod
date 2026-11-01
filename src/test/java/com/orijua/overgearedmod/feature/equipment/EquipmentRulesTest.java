package com.orijua.overgearedmod.feature.equipment;

import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.StatType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentRulesTest {
    private static final Balance B = Balance.DEFAULT;

    @Test
    void betterRatingsRollMoreOptions() {
        assertEquals(0, EquipmentRules.optionCount(ItemRating.NORMAL));
        assertEquals(1, EquipmentRules.optionCount(ItemRating.EPIC));
        assertEquals(2, EquipmentRules.optionCount(ItemRating.UNIQUE));
        assertEquals(3, EquipmentRules.optionCount(ItemRating.MYTH));
    }

    @Test
    void rolledValuesStayInRangeAndBetterRatingsRollHigher() {
        for (ItemRating rating : ItemRating.values()) {
            float low = EquipmentRules.rollValue(1, 5, rating, 0.0);
            float high = EquipmentRules.rollValue(1, 5, rating, 0.999);
            assertTrue(low >= 1 && high <= 5, rating + " out of range: " + low + ".." + high);
        }
        assertEquals(1.0f, EquipmentRules.rollValue(1, 5, ItemRating.NORMAL, 0.0));
        assertTrue(EquipmentRules.rollValue(1, 5, ItemRating.MYTH, 0.0) > EquipmentRules.rollValue(1, 5, ItemRating.RARE, 0.0));
    }

    @Test
    void weightedPicksAreDistinctAndLimitedByPool() {
        List<String> picked = EquipmentRules.pickWeighted(List.of("a", "b", "c"), s -> 10, 3, () -> 0.0);
        assertEquals(3, new HashSet<>(picked).size());
        assertEquals(2, EquipmentRules.pickWeighted(List.of("a", "b"), s -> 10, 5, () -> 0.5).size());
    }

    @Test
    void weightedPickFollowsWeights() {
        // "b" holds 90% of the weight: a roll of 0.5 lands on it.
        assertEquals(List.of("b"), EquipmentRules.pickWeighted(List.of("a", "b"), s -> s.equals("a") ? 10 : 90, 1, () -> 0.5));
    }

    @Test
    void enhancementChanceDropsPerLevelAndIsClamped() {
        assertEquals(0.95, EquipmentRules.enhancementChance(B, 0, 0), 1e-9);
        assertEquals(0.86, EquipmentRules.enhancementChance(B, 1, 0), 1e-9);
        assertTrue(EquipmentRules.enhancementChance(B, 5, 20) > EquipmentRules.enhancementChance(B, 5, 0), "DEX helps");
        assertEquals(0.05, EquipmentRules.enhancementChance(B, 10, 0), 1e-9, "never below the minimum");
        assertEquals(0.95, EquipmentRules.enhancementChance(B, 0, 1000), 1e-9, "never above the base");
    }

    @Test
    void requirementsCheckLevelAndStats() {
        EquipRequirements requirements = new EquipRequirements(2, Map.of(StatType.STRENGTH, 2));
        PlayerStats weak = PlayerStats.DEFAULT.withProgress(2, 0, 0);
        assertFalse(requirements.metBy(weak));
        assertTrue(requirements.metBy(weak.with(StatType.STRENGTH, 2)));
        assertFalse(requirements.metBy(PlayerStats.DEFAULT.with(StatType.STRENGTH, 5)), "level 1 is too low");
    }
}
