package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferRulesTest {
    @Test
    void theOfferHasPerRankPiecesForEveryRatingUpToTheMaximum() {
        assertEquals(12, OfferRules.size(6, ItemRating.RARE));
        assertEquals(6, OfferRules.size(6, ItemRating.NORMAL));
        assertEquals(0, OfferRules.size(0, ItemRating.RARE));
    }

    @Test
    void positionsGoFromNormalUp() {
        for (int position = 0; position < 6; position++) {
            assertEquals(ItemRating.NORMAL, OfferRules.ratingAt(position, 6));
        }
        for (int position = 6; position < 12; position++) {
            assertEquals(ItemRating.RARE, OfferRules.ratingAt(position, 6));
        }
    }

    @Test
    void theSeedIsDeterministicAndNeverRepeats() {
        Set<Long> seen = new HashSet<>();
        for (long day = 0; day <= 1000; day++) {
            for (int position = 0; position < 12; position++) {
                ItemRating rating = OfferRules.ratingAt(position, 6);
                assertEquals(OfferRules.seed(day, rating, position), OfferRules.seed(day, rating, position));
                assertTrue(seen.add(OfferRules.seed(day, rating, position)), "repeated at day " + day + " position " + position);
            }
        }
        for (long day : new long[]{-1L, -1000L, Long.MIN_VALUE + 1, Long.MAX_VALUE}) {
            for (int position = 0; position < 12; position++) {
                assertTrue(seen.add(OfferRules.seed(day, OfferRules.ratingAt(position, 6), position)), "repeated at day " + day);
            }
        }
    }

    @Test
    void thePriceIsTheReferenceTimesTheMultiplierTimesTheWonPerEmerald() {
        assertEquals(400L, OfferRules.price(1, 4.0, 100));
        assertEquals(4800L, OfferRules.price(12, 4.0, 100));
        assertEquals(1L, OfferRules.price(0, 4.0, 100), "never below 1 Won");
        assertEquals(Long.MAX_VALUE, OfferRules.price(Long.MAX_VALUE, 1000.0, 1_000_000), "saturates");
    }

    @Test
    void ratingAtOutsideTheDomainThrows() {
        assertThrows(IllegalArgumentException.class, () -> OfferRules.ratingAt(0, 0));
        assertThrows(IllegalArgumentException.class, () -> OfferRules.ratingAt(0, -1));
        assertThrows(IllegalArgumentException.class, () -> OfferRules.ratingAt(-1, 6));
        assertThrows(IllegalArgumentException.class, () -> OfferRules.ratingAt(36, 6));
        assertEquals(ItemRating.MYTH, OfferRules.ratingAt(35, 6));
        assertEquals(ItemRating.NORMAL, OfferRules.ratingAt(0, 1));
        assertEquals(ItemRating.MYTH, OfferRules.ratingAt(5, 1));
    }

    @Test
    void priceBordersAndRounding() {
        assertEquals(4L, OfferRules.price(1, 1.5, 3), "4.5 rounds down");
        assertEquals(1L, OfferRules.price(1, 1.0, 1));
        assertEquals(1L, OfferRules.price(0, 1.0, 100));
        assertEquals(1L, OfferRules.price(-5, 4.0, 100), "a negative reference still costs 1");
        assertEquals(Long.MAX_VALUE, OfferRules.price(Long.MAX_VALUE / 2, 1000.0, 1_000_000));
    }

    @Test
    void theSeedKeepsItsGoldenValueAndDiffersByRating() {
        assertEquals(GOLDEN_SEED, OfferRules.seed(42, ItemRating.RARE, 7), "the offer of every day would change");
        Set<Long> seeds = new HashSet<>();
        for (ItemRating rating : ItemRating.values()) {
            assertTrue(seeds.add(OfferRules.seed(42, rating, 3)), "same seed for rating " + rating);
        }
    }

    private static final long GOLDEN_SEED = -3336707969042032636L;

    @Test
    void buyingIsAlwaysDearerThanSellingAndConvertingBack() {
        TradeBalance trade = TradeBalance.DEFAULT;
        for (long reference = 1; reference <= 5000; reference++) {
            double back = TradeRules.sellPrice(reference, 0, trade) * 100 * 0.95;
            assertTrue(OfferRules.price(reference, 4.0, 100) > back, "reference " + reference);
        }
    }
}
