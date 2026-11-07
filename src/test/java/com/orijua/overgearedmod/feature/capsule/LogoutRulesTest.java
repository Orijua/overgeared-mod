package com.orijua.overgearedmod.feature.capsule;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoutRulesTest {
    @Test
    void noCombatIsNeverDanger() {
        assertFalse(LogoutRules.inDanger(1000, OptionalLong.empty()));
    }

    @Test
    void combatInTheLastThreeSecondsIsDanger() {
        assertTrue(LogoutRules.inDanger(1000, OptionalLong.of(1000)));
        assertTrue(LogoutRules.inDanger(1000, OptionalLong.of(941)), "59 ticks ago");
        assertFalse(LogoutRules.inDanger(1000, OptionalLong.of(940)), "60 ticks ago");
        assertFalse(LogoutRules.inDanger(1000, OptionalLong.of(0)));
    }

    @Test
    void aCombatTickInTheFutureCountsAsDanger() {
        assertTrue(LogoutRules.inDanger(1000, OptionalLong.of(5000)));
    }

    @Test
    void theCountdownAnnouncesWholeSeconds() {
        assertEquals(OptionalInt.of(3), LogoutRules.announcement(60));
        assertEquals(OptionalInt.of(2), LogoutRules.announcement(40));
        assertEquals(OptionalInt.of(1), LogoutRules.announcement(20));
        for (int ticks : new int[]{59, 41, 1, 0, -20}) {
            assertEquals(OptionalInt.empty(), LogoutRules.announcement(ticks), "ticks " + ticks);
        }
    }

    @Test
    void theConstantsAreThreeSeconds() {
        assertEquals(60, LogoutRules.COUNTDOWN_TICKS);
        assertEquals(60, LogoutRules.DANGER_TICKS);
        assertEquals(20, LogoutRules.TICKS_PER_SECOND);
    }
}
