package com.orijua.overgearedmod.smelting;

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
        assertEquals(997, SmelterRules.nextTemperature(1000, 1000, false), "no fuel: it cools");
        assertEquals(SmelterRules.AMBIENT, SmelterRules.nextTemperature(21, 1000, false), "never below the ambient");
    }

    @Test
    void automaticExtraIngotGrowsWithSmithing() {
        assertEquals(0.0, SmelterRules.autoExtraChance(0), 1e-9);
        assertEquals(0.1, SmelterRules.autoExtraChance(10), 1e-9);
        assertEquals(SmelterRules.MAX_AUTO_EXTRA_CHANCE, SmelterRules.autoExtraChance(400), 1e-9, "capped");
    }

    @Test
    void manualIsRiskAndReward() {
        assertEquals(SmelterRules.MAX_SLAG_CHANCE, SmelterRules.slagChance(0), 1e-9, "a botched melt often turns to slag");
        assertEquals(0.0, SmelterRules.slagChance(0.4), 1e-9, "from 40 % on, no slag");
        assertEquals(0.0, SmelterRules.manualExtraChance(0.69), 1e-9, "no extra ingot below 70 %");
        assertEquals(SmelterRules.MAX_MANUAL_EXTRA_CHANCE, SmelterRules.manualExtraChance(1), 1e-9, "a perfect melt");
        assertEquals(SmelterRules.MAX_MANUAL_EXTRA_CHANCE, SmelterRules.manualExtraChance(9), 1e-9, "bogus values are clamped");
        assertEquals(SmelterRules.MAX_SLAG_CHANCE, SmelterRules.slagChance(Double.NaN), 1e-9, "and NaN counts as 0");
    }

    @Test
    void manualResultsMustTakeAPlausibleTime() {
        assertFalse(SmelterRules.plausibleManualDuration(10), "faster than the 6 s game");
        assertTrue(SmelterRules.plausibleManualDuration(130));
        assertFalse(SmelterRules.plausibleManualDuration(20 * 600), "far too late");
    }
}
