package com.orijua.overgearedmod.feature.equipment;

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

    @Test
    void oddsAddUpToOneAndMatchTheRoll() {
        double[] odds = RatingRoller.odds(20, 3);
        double sum = 0;
        for (double odd : odds) {
            sum += odd;
        }
        assertEquals(1.0, sum, 1e-9);
        // Rolling just below the Normal share still gives Normal; just above, Rare.
        assertEquals(ItemRating.NORMAL, RatingRoller.roll(20, 3, odds[0] - 1e-6));
        assertEquals(ItemRating.RARE, RatingRoller.roll(20, 3, odds[0] + 1e-6));
    }

    @Test
    void novicesSeeTheBaseOdds() {
        double[] odds = RatingRoller.odds(0, 0);
        assertEquals(0.60, odds[ItemRating.NORMAL.ordinal()], 1e-9);
        assertEquals(0.25, odds[ItemRating.RARE.ordinal()], 1e-9);
        assertEquals(0.001, odds[ItemRating.MYTH.ordinal()], 1e-9);
    }

    @Test
    void aRatingRangeOnlyGivesTiersInsideIt() {
        // Fracaso: always Unique or Legendary, never Normal-Epic nor Myth, however the roll goes.
        for (int i = 0; i < 1000; i++) {
            ItemRating rating = RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, (i + 0.5) / 1000);
            assertTrue(rating == ItemRating.UNIQUE || rating == ItemRating.LEGENDARY, "got " + rating);
        }
        assertEquals(ItemRating.UNIQUE, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, 0.0));
        assertEquals(ItemRating.LEGENDARY, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, 0.99999));
    }

    @Test
    void aRatingRangeNeverFallsBackToMythAtTheEdgeOfTheRoll() {
        assertEquals(ItemRating.LEGENDARY, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, 1.0));
        assertEquals(ItemRating.LEGENDARY,
                RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, Math.nextDown(1.0)));
        assertEquals(ItemRating.LEGENDARY, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, 5.0));
        assertEquals(ItemRating.UNIQUE, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, Double.NaN));
        assertEquals(ItemRating.UNIQUE, RatingRoller.roll(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY, 0.0));
        assertEquals(ItemRating.MYTH, RatingRoller.roll(0, 0, 1.0), "without a limit the top roll is still Myth");
    }

    @Test
    void aRatingRangeKeepsTheRelativeOdds() {
        // Novice base weights: Unique 4, Legendary 0.9, so Legendary is 0.9 / 4.9 of the range.
        double[] odds = RatingRoller.odds(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY);
        assertEquals(0, odds[ItemRating.EPIC.ordinal()], 1e-9);
        assertEquals(0, odds[ItemRating.MYTH.ordinal()], 1e-9);
        assertEquals(0.9 / 4.9, odds[ItemRating.LEGENDARY.ordinal()], 1e-9);
        assertEquals(1.0, odds[ItemRating.UNIQUE.ordinal()] + odds[ItemRating.LEGENDARY.ordinal()], 1e-9);
    }

    @Test
    void skillRaisesTheChanceOfTheTopOfARange() {
        double[] novice = RatingRoller.odds(0, 0, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY);
        double[] master = RatingRoller.odds(50, 10, 0, ItemRating.UNIQUE, ItemRating.LEGENDARY);
        assertTrue(master[ItemRating.LEGENDARY.ordinal()] > novice[ItemRating.LEGENDARY.ordinal()]);
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
