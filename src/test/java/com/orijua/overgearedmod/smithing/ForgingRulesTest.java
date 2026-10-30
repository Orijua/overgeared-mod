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
}
