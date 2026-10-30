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

    @Test
    void lootWithoutLuckOrMinimumFollowsTheBaseOdds() {
        for (int i = 0; i < 1000; i++) {
            double random01 = (i + 0.5) / 1000;
            assertEquals(RatingRoller.roll(0, 0, random01), RatingRoller.rollLoot(0, ItemRating.NORMAL, random01));
        }
    }

    @Test
    void lootNeverRollsBelowTheMinimum() {
        for (int i = 0; i < 1000; i++) {
            ItemRating rating = RatingRoller.rollLoot(0, ItemRating.RARE, (i + 0.5) / 1000);
            assertTrue(rating.ordinal() >= ItemRating.RARE.ordinal(), "got " + rating);
        }
        assertEquals(ItemRating.RARE, RatingRoller.rollLoot(0, ItemRating.RARE, 0.0), "the lowest roll lands on the minimum");
    }

    @Test
    void lootLuckShiftsTheOddsUp() {
        double plain = 0;
        double lucky = 0;
        for (int i = 0; i < 10_000; i++) {
            double random01 = (i + 0.5) / 10_000;
            plain += RatingRoller.rollLoot(0, ItemRating.NORMAL, random01).ordinal();
            lucky += RatingRoller.rollLoot(40, ItemRating.NORMAL, random01).ordinal();
        }
        assertTrue(lucky > plain, "luck 40 (" + lucky + ") should beat luck 0 (" + plain + ")");
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
