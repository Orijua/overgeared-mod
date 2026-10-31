package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.RatingRoller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingRulesTest {
    @Test
    void autoForgeTakesOneToFourSeconds() {
        assertEquals(80, ForgingRules.autoForgeTicks(0, 0), "a novice takes 4 s");
        assertTrue(ForgingRules.autoForgeTicks(20, 5) < 80, "skill makes it quicker");
        assertEquals(20, ForgingRules.autoForgeTicks(500, 50), "never under 1 s");
    }

    @Test
    void manualBonusIsRiskAndReward() {
        assertEquals(-40.0, ForgingRules.manualSkillBonus(0), 1e-9);
        assertEquals(0.0, ForgingRules.manualSkillBonus(0.5), 1e-9, "half precision equals the automatic forge");
        assertEquals(40.0, ForgingRules.manualSkillBonus(1), 1e-9);
        assertEquals(40.0, ForgingRules.manualSkillBonus(7), 1e-9, "a bogus client value is clamped");
        assertEquals(-40.0, ForgingRules.manualSkillBonus(Double.NaN), 1e-9, "and so is NaN");
    }

    @Test
    void extraOptionOnlyForGoodPrecision() {
        assertEquals(0.0, ForgingRules.extraOptionChance(0.79), 1e-9);
        assertEquals(0.25, ForgingRules.extraOptionChance(0.9), 1e-9);
        assertEquals(0.5, ForgingRules.extraOptionChance(1), 1e-9);
    }

    @Test
    void manualDurationMustBePlausible() {
        assertFalse(ForgingRules.plausibleManualDuration(5), "too fast to be a real minigame");
        assertTrue(ForgingRules.plausibleManualDuration(ForgingRules.MIN_MANUAL_TICKS));
        assertFalse(ForgingRules.plausibleManualDuration(ForgingRules.MAX_MANUAL_TICKS + 1), "left open too long");
    }

    @Test
    void manualPrecisionMovesTheOddsBothWays() {
        double[] auto = RatingRoller.odds(10, 2);
        double[] botched = RatingRoller.odds(10, 2, ForgingRules.manualSkillBonus(0));
        double[] perfect = RatingRoller.odds(10, 2, ForgingRules.manualSkillBonus(1));
        int normal = ItemRating.NORMAL.ordinal();
        assertTrue(botched[normal] > auto[normal], "a botched manual forge is more likely Normal than the automatic one");
        assertTrue(perfect[normal] < auto[normal], "a perfect one is less likely Normal");
        assertTrue(perfect[ItemRating.EPIC.ordinal()] > auto[ItemRating.EPIC.ordinal()], "and more likely Epic");
    }

    @Test
    void batchSizesUnlockWithSmithing() {
        assertFalse(ForgingRules.batchUnlocked(1, 9), "×5 needs smithing 10");
        assertTrue(ForgingRules.batchUnlocked(1, 10));
        assertTrue(ForgingRules.batchUnlocked(0, 0), "×1 is always there");
        assertEquals(0, ForgingRules.nextBatchIndex(0, 0), "a novice stays at ×1");
        assertEquals(2, ForgingRules.nextBatchIndex(1, 25), "×5 → ×10 at smithing 25");
        assertEquals(0, ForgingRules.nextBatchIndex(2, 25), "All is locked until 30, so it wraps to ×1");
        assertEquals(3, ForgingRules.nextBatchIndex(2, 30));
        assertEquals(0, ForgingRules.nextBatchIndex(3, 30), "after All comes ×1 again");
    }

    @Test
    void batchIsCappedByTheMaterials() {
        assertEquals(5, ForgingRules.batchTotal(5, 20, 2, 64), "enough for 10, five asked");
        assertEquals(3, ForgingRules.batchTotal(10, 7, 2, 64), "7 ingots at 2 each make 3");
        assertEquals(2, ForgingRules.batchTotal(10, 20, 2, 2), "only 2 handles");
        assertEquals(32, ForgingRules.batchTotal(ForgingRules.BATCH_ALL, 64, 2, -1), "All, with no handle needed");
        assertEquals(0, ForgingRules.batchTotal(5, 1, 2, -1), "not even one");
    }
}
