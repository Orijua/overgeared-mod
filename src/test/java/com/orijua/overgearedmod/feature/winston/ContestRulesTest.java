package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.core.config.Balance.ContestLoss;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestRulesTest {
    @Test
    void aPieceScoresItsRatingAndItsPrecision() {
        assertEquals(287, ContestRules.score(ItemRating.EPIC, 0.87));
        assertEquals(0, ContestRules.score(ItemRating.NORMAL, 0));
        assertEquals(100, ContestRules.score(ItemRating.NORMAL, 1));
        assertEquals(350, ContestRules.score(ItemRating.UNIQUE, 0.5));
    }

    @Test
    void aBadPrecisionIsClamped() {
        assertEquals(200, ContestRules.score(ItemRating.EPIC, -3));
        assertEquals(300, ContestRules.score(ItemRating.EPIC, 7));
        assertEquals(200, ContestRules.score(ItemRating.EPIC, Double.NaN));
    }

    @Test
    void eupheminaRollsBetweenAFairRareAndAPerfectEpic() {
        assertEquals(ContestRules.RIVAL_MIN, ContestRules.rivalScore(0));
        assertEquals(ContestRules.RIVAL_MAX, ContestRules.rivalScore(1));
        assertEquals(225, ContestRules.rivalScore(0.5));
        assertEquals(ContestRules.RIVAL_MAX, ContestRules.rivalScore(2));
    }

    @Test
    void aPreciseEpicAlwaysWinsAndARareRarelyDoes() {
        assertTrue(ContestRules.wins(ContestRules.score(ItemRating.EPIC, 1), ContestRules.RIVAL_MAX - 1));
        assertFalse(ContestRules.wins(ContestRules.score(ItemRating.RARE, 0.4), ContestRules.RIVAL_MIN));
    }

    @Test
    void aTieGoesToEuphemina() {
        assertFalse(ContestRules.wins(250, 250));
        assertTrue(ContestRules.wins(251, 250));
    }

    @Test
    void autoFailsInASinglePlayerWorldAndGivesARematchInASharedOne() {
        assertTrue(ContestRules.failsForGood(ContestLoss.AUTO, true));
        assertFalse(ContestRules.failsForGood(ContestLoss.AUTO, false));
        assertTrue(ContestRules.failsForGood(ContestLoss.FAIL, false));
        assertFalse(ContestRules.failsForGood(ContestLoss.REMATCH, true));
    }

    @Test
    void theClockRunsDownToZero() {
        assertEquals(ContestRules.TIME_LIMIT_TICKS, ContestRules.ticksLeft(1000, 1000));
        assertEquals(ContestRules.TIME_LIMIT_TICKS - 20, ContestRules.ticksLeft(1000, 1020));
        assertEquals(0, ContestRules.ticksLeft(0, ContestRules.TIME_LIMIT_TICKS + 50));
    }

    @Test
    void aRematchComesTheNextDay() {
        assertTrue(ContestRules.canRematch(-1, 0));
        assertFalse(ContestRules.canRematch(4, 4));
        assertTrue(ContestRules.canRematch(4, 5));
    }

    @Test
    void theStateRemembersTheLossAfterANewStart() {
        ContestState lost = ContestState.NONE.lost(3);
        assertFalse(lost.running());
        ContestState again = lost.start(500, 222);
        assertTrue(again.running());
        assertEquals(222, again.rivalScore());
        assertEquals(3, again.lostDay());
    }
}
