package com.orijua.overgearedmod.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApprenticeRulesTest {
    @Test
    void theFirstApprenticeComesAtFiftyAndOneMoreEveryTen() {
        assertEquals(0, ApprenticeRules.allowedApprentices(49));
        assertEquals(1, ApprenticeRules.allowedApprentices(50));
        assertEquals(1, ApprenticeRules.allowedApprentices(59));
        assertEquals(2, ApprenticeRules.allowedApprentices(60));
        assertEquals(5, ApprenticeRules.allowedApprentices(90));
        assertEquals(5, ApprenticeRules.allowedApprentices(400), "five at most for now");
        assertEquals(50, ApprenticeRules.levelForApprentice(0));
        assertEquals(90, ApprenticeRules.levelForApprentice(4));
        assertEquals(-1, ApprenticeRules.levelForApprentice(5), "no sixth");
    }

    @Test
    void anApprenticeNeverReachesHisMaster() {
        assertEquals(19, ApprenticeRules.workingLevel(30, 20));
        assertEquals(12, ApprenticeRules.workingLevel(12, 50));
        assertEquals(0, ApprenticeRules.workingLevel(5, 0));
    }

    @Test
    void practiceMakesFewerBotchesAndFasterPieces() {
        assertEquals(ApprenticeRules.MAX_FAIL_CHANCE, ApprenticeRules.failChance(0), 1e-9);
        assertEquals(0.10, ApprenticeRules.failChance(50), 1e-9);
        assertEquals(ApprenticeRules.MIN_FAIL_CHANCE, ApprenticeRules.failChance(100), 1e-9, "never sure");
        assertEquals(ApprenticeRules.SLOWEST_TICKS, ApprenticeRules.ticksPerPiece(0));
        assertEquals(700, ApprenticeRules.ticksPerPiece(50));
        assertEquals(ApprenticeRules.FASTEST_TICKS, ApprenticeRules.ticksPerPiece(100));
    }

    @Test
    void theWageIsPaidInWholeDaysUpToTwoWeeksAhead() {
        assertEquals(3, ApprenticeRules.daysBought(9, 0));
        assertEquals(3, ApprenticeRules.daysBought(11, 0), "leftover emeralds stay");
        assertEquals(0, ApprenticeRules.daysBought(2, 0), "not even a day");
        assertEquals(4, ApprenticeRules.daysBought(64, 10), "up to fourteen days ahead");
        assertEquals(0, ApprenticeRules.daysBought(64, ApprenticeRules.MAX_PAID_DAYS));
    }
}
