package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(19, ApprenticeRules.workingLevel(30, 20, NpcGrade.NAMED));
        assertEquals(12, ApprenticeRules.workingLevel(12, 50, NpcGrade.NAMED));
        assertEquals(0, ApprenticeRules.workingLevel(5, 0, NpcGrade.NAMED));
    }

    @Test
    void hisGradeCapsHisLevelAndScalesWhatHeLearns() {
        assertEquals(40, ApprenticeRules.workingLevel(65, 90, NpcGrade.NORMAL), "a normal NPC stops at 40");
        assertEquals(65, ApprenticeRules.workingLevel(65, 90, NpcGrade.SEMI_NAMED));
        assertEquals(70, ApprenticeRules.workingLevel(85, 90, NpcGrade.SEMI_NAMED), "a semi-named one at 70");
        assertEquals(89, ApprenticeRules.workingLevel(200, 90, NpcGrade.NAMED), "a named one only by his master");
        assertEquals(10, ApprenticeRules.xpGained(10, NpcGrade.NORMAL));
        assertEquals(15, ApprenticeRules.xpGained(10, NpcGrade.SEMI_NAMED));
        assertEquals(20, ApprenticeRules.xpGained(10, NpcGrade.NAMED));
    }

    @Test
    void gradesAreRolledNinetyNineOne() {
        assertEquals(NpcGrade.NAMED, NpcGrade.roll(0.0));
        assertEquals(NpcGrade.NAMED, NpcGrade.roll(0.0099));
        assertEquals(NpcGrade.SEMI_NAMED, NpcGrade.roll(0.01));
        assertEquals(NpcGrade.SEMI_NAMED, NpcGrade.roll(0.0999));
        assertEquals(NpcGrade.NORMAL, NpcGrade.roll(0.10));
        assertEquals(NpcGrade.NORMAL, NpcGrade.roll(0.9999));
    }

    @Test
    void masteryFollowsTheLevel() {
        assertEquals(MasteryStage.BEGINNER, MasteryStage.of(19));
        assertEquals(MasteryStage.INTERMEDIATE, MasteryStage.of(20));
        assertEquals(MasteryStage.ADVANCED, MasteryStage.of(59));
        assertEquals(MasteryStage.MASTER, MasteryStage.of(60));
        assertTrue(MasteryStage.MASTER.atLeast(MasteryStage.ADVANCED));
        assertFalse(MasteryStage.INTERMEDIATE.atLeast(MasteryStage.ADVANCED));
    }

    @Test
    void onlyAMasterBecomesACraftsman() {
        assertFalse(ApprenticeRules.earnsCraftsman(MasteryStage.ADVANCED, ItemRating.MYTH, true), "mastery first");
        assertTrue(ApprenticeRules.earnsCraftsman(MasteryStage.MASTER, ItemRating.UNIQUE, true), "his own masterpiece");
        assertFalse(ApprenticeRules.earnsCraftsman(MasteryStage.MASTER, ItemRating.EPIC, true), "an Epic is not one");
        assertTrue(ApprenticeRules.earnsCraftsman(MasteryStage.MASTER, ItemRating.LEGENDARY, false), "illumination by a Legendary");
        assertFalse(ApprenticeRules.earnsCraftsman(MasteryStage.MASTER, ItemRating.UNIQUE, false), "watching a Unique is not enough");
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
