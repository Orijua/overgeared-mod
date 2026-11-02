package com.orijua.overgearedmod.feature.smelting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmelterRulesTest {
    @Test
    void fuelHeatsUpToItsHeatAndTheSmelterCoolsWithout() {
        assertEquals(26, SmelterRules.nextTemperature(20, 1000, true), "burning fuel heats");
        assertEquals(1000, SmelterRules.nextTemperature(998, 1000, true), "but never above the fuel's heat");
        assertEquals(1497, SmelterRules.nextTemperature(1500, 1000, true), "hotter than the fuel: it cools towards it");
        assertEquals(999, SmelterRules.nextTemperature(1000, 1000, false), "nothing burns: it cools slowly");
        assertEquals(SmelterRules.AMBIENT, SmelterRules.nextTemperature(21, 1000, false), "never below the ambient");
        assertEquals(1000, SmelterRules.nextTemperature(1000, 1000, true), "burning at its heat, it holds");
        assertEquals(1000, SmelterRules.nextTemperature(1002, 1000, true), "the cooling never goes under the fuel's heat");
    }

    @Test
    void heatingUpBurnsFuelTwiceAsFast() {
        assertEquals(2, SmelterRules.fuelTicksPerTick(500, 1050), "still heating");
        assertEquals(1, SmelterRules.fuelTicksPerTick(1050, 1050), "holding the heat");
        assertEquals(1, SmelterRules.fuelTicksPerTick(1500, 1050), "hotter than this fuel");
    }

    @Test
    void automaticExtraIngotGrowsWithSmithing() {
        assertEquals(0.0, SmelterRules.autoExtraChance(0), 1e-9);
        assertEquals(0.1, SmelterRules.autoExtraChance(10), 1e-9);
        assertEquals(SmelterRules.MAX_AUTO_EXTRA_CHANCE, SmelterRules.autoExtraChance(400), 1e-9, "capped");
        assertEquals(0.0, SmelterRules.autoExtraChance(-5), 1e-9, "a negative level counts as 0");
    }

    @Test
    void manualIsRiskAndReward() {
        assertEquals(SmelterRules.MAX_SLAG_CHANCE, SmelterRules.slagChance(0), 1e-9, "a botched melt often turns to slag");
        assertEquals(0.0, SmelterRules.slagChance(0.4), 1e-9, "from 40 % on, no slag");
        assertEquals(0.0, SmelterRules.manualExtraChance(0.69), 1e-9, "no extra ingot below 70 %");
        assertEquals(SmelterRules.MAX_MANUAL_EXTRA_CHANCE, SmelterRules.manualExtraChance(1), 1e-9, "a perfect melt");
        assertEquals(SmelterRules.MAX_MANUAL_EXTRA_CHANCE, SmelterRules.manualExtraChance(9), 1e-9, "bogus values are clamped");
        assertEquals(SmelterRules.MAX_SLAG_CHANCE, SmelterRules.slagChance(Double.NaN), 1e-9, "and NaN counts as 0");
        assertEquals(0.0, SmelterRules.manualExtraChance(0.7), 1e-9, "exactly 70 %: the extra chance starts from zero");
        assertEquals(0.3, SmelterRules.manualExtraChance(0.85), 1e-9, "halfway up the range");
        assertEquals(0.3, SmelterRules.slagChance(0.2), 1e-9, "halfway down the slag range");
        assertTrue(SmelterRules.slagChance(0.39) > 0, "just under 40 % there is still some");
    }

    @Test
    void manualOutcomeFollowsTheRollAgainstSlagAndExtra() {
        assertEquals(0, SmelterRules.manualIngots(0, 0.0), "botched, lowest roll: slag");
        assertEquals(0, SmelterRules.manualIngots(0, 0.59), "just under the 60 % slag chance");
        assertEquals(1, SmelterRules.manualIngots(0, 0.6), "the slag chance itself is not slag");
        assertEquals(2, SmelterRules.manualIngots(1, 0.59), "perfect: an extra ingot under 60 %");
        assertEquals(1, SmelterRules.manualIngots(1, 0.6));
        assertEquals(1, SmelterRules.manualIngots(0.5, 0.0), "the neutral zone has neither slag nor extra");
    }

    @Test
    void manualResultsMustTakeAPlausibleTime() {
        assertFalse(SmelterRules.plausibleManualDuration(10), "faster than the 6 s game");
        assertFalse(SmelterRules.plausibleManualDuration(SmelterRules.MIN_MANUAL_TICKS - 1));
        assertTrue(SmelterRules.plausibleManualDuration(SmelterRules.MIN_MANUAL_TICKS));
        assertTrue(SmelterRules.plausibleManualDuration(130));
        assertTrue(SmelterRules.plausibleManualDuration(SmelterRules.MAX_MANUAL_TICKS));
        assertFalse(SmelterRules.plausibleManualDuration(SmelterRules.MAX_MANUAL_TICKS + 1));
        assertFalse(SmelterRules.plausibleManualDuration(20 * 600), "far too late");
        assertTrue(SmelterRules.MIN_MANUAL_TICKS <= 60, "room for a server at 10 TPS (the game lasts 120 ticks)");
    }
}
