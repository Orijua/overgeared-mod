package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.RatingRoller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingRulesTest {
    @Test
    void autoForgeTakesOneToFourSeconds() {
        assertEquals(80, ForgingRules.autoForgeTicks(0, 0), "a novice takes 4 s");
        assertEquals(50, ForgingRules.autoForgeTicks(20, 5), "80 - 4*5 smithing - 20/2 dex");
        assertEquals(79, ForgingRules.autoForgeTicks(3, 0), "odd dexterity rounds down: 3/2 = 1");
        assertEquals(20, ForgingRules.autoForgeTicks(500, 50), "never under 1 s");
        assertEquals(80, ForgingRules.autoForgeTicks(-10, -10), "negative stats count as 0, never slower than 4 s");
    }

    @Test
    void manualBonusIsRiskAndReward() {
        assertEquals(-40.0, ForgingRules.manualSkillBonus(0), 1e-9);
        assertEquals(0.0, ForgingRules.manualSkillBonus(0.5), 1e-9, "half precision equals the automatic forge");
        assertEquals(40.0, ForgingRules.manualSkillBonus(1), 1e-9);
        assertEquals(40.0, ForgingRules.manualSkillBonus(7), 1e-9, "a bogus client value is clamped");
        assertEquals(-40.0, ForgingRules.manualSkillBonus(Double.NaN), 1e-9, "and so is NaN");
        assertEquals(-40.0, ForgingRules.manualSkillBonus(-3), 1e-9, "below 0 is clamped too");
        assertEquals(40.0, ForgingRules.manualSkillBonus(Double.POSITIVE_INFINITY), 1e-9);
        assertEquals(-40.0, ForgingRules.manualSkillBonus(Double.NEGATIVE_INFINITY), 1e-9);
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
        assertFalse(ForgingRules.plausibleManualDuration(ForgingRules.MIN_MANUAL_TICKS - 1), "one tick under the minimum");
        assertTrue(ForgingRules.plausibleManualDuration(ForgingRules.MIN_MANUAL_TICKS));
        assertTrue(ForgingRules.plausibleManualDuration(ForgingRules.MAX_MANUAL_TICKS), "the maximum itself is fine");
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
        assertFalse(ForgingRules.batchUnlocked(-1, 100), "no size before the first");
        assertFalse(ForgingRules.batchUnlocked(ForgingRules.BATCH_SIZES.length, 100), "no size past the last");
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
        assertEquals(0, ForgingRules.batchTotal(5, 20, 2, 0), "no handles at all, not 'no handle needed'");
        assertEquals(0, ForgingRules.batchTotal(5, 20, 0, -1), "a recipe of zero materials makes nothing");
        assertEquals(0, ForgingRules.batchTotal(5, 20, -1, -1), "nor a negative one");
        assertEquals(0, ForgingRules.batchTotal(-3, 20, 2, -1), "a negative size makes nothing");
    }

    @Test
    void greatSuccessGrowsWithSkillAndAPreciseHand() {
        assertEquals(0.01, ForgingRules.greatSuccessChance(0, 0, -1), 1e-9, "1 % for a novice");
        assertEquals(0.01 + 50 * 0.0004 + 20 * 0.0001, ForgingRules.greatSuccessChance(20, 50, -1), 1e-9);
        assertEquals(ForgingRules.MAX_GREAT_SUCCESS, ForgingRules.greatSuccessChance(5000, 300, -1), 1e-9, "capped");
        assertEquals(0.02, ForgingRules.greatSuccessChance(0, 0, 0.8), 1e-9, "doubled from 80 % precision");
        assertEquals(0.01, ForgingRules.greatSuccessChance(0, 0, 0.79), 1e-9, "not below");
    }

    /** Intended: the panel uses the same function, so what it shows matches what the forge rolls. */
    @Test
    void aPreciseHandDoublesEvenTheCappedChance() {
        assertEquals(2 * ForgingRules.MAX_GREAT_SUCCESS, ForgingRules.greatSuccessChance(5000, 300, 1.0), 1e-9);
        assertEquals(0.16, ForgingRules.greatSuccessChance(5000, 300, 1.0), 1e-9);
    }

    @Test
    void aGreatSuccessRaisesOneStepWithinTheRange() {
        assertEquals(ItemRating.RARE, ForgingRules.raise(ItemRating.NORMAL, ItemRating.MYTH));
        assertEquals(ItemRating.LEGENDARY, ForgingRules.raise(ItemRating.LEGENDARY, ItemRating.LEGENDARY), "Fracaso stays within its range");
        assertEquals(ItemRating.LEGENDARY, ForgingRules.raise(ItemRating.UNIQUE, ItemRating.LEGENDARY), "never lowers a roll above the maximum");
        assertEquals(ItemRating.LEGENDARY, ForgingRules.raise(ItemRating.LEGENDARY, ItemRating.UNIQUE), "a roll above the maximum stays as it is");
        assertEquals(ItemRating.MYTH, ForgingRules.raise(ItemRating.MYTH, ItemRating.MYTH));
    }
}
