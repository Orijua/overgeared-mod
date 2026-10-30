package com.orijua.overgearedmod.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingRollerTest {
    @Test
    void lowRollsAreNormalAndTheHighestAreMyth() {
        assertEquals(ItemRating.NORMAL, RatingRoller.roll(0, 0, 0.0));
        assertEquals(ItemRating.MYTH, RatingRoller.roll(0, 0, 0.99999));
    }

    @Test
    void novicesFollowTheBaseOdds() {
        // Base weights: 60% Normal, 25% Rare...
        assertEquals(ItemRating.NORMAL, RatingRoller.roll(0, 0, 0.59));
        assertEquals(ItemRating.RARE, RatingRoller.roll(0, 0, 0.61));
    }

    @Test
    void skillShiftsTheOddsUp() {
        double novice = averageTier(0, 0);
        double master = averageTier(50, 10);
        assertTrue(master > novice, "master " + master + " should beat novice " + novice);
    }

    private static double averageTier(int dexterity, int smithingLevel) {
        int samples = 10_000;
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += RatingRoller.roll(dexterity, smithingLevel, (i + 0.5) / samples).ordinal();
        }
        return sum / samples;
    }
}
