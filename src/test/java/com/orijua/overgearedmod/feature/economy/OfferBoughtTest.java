package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferBoughtTest {
    @Test
    void emptyHasNothingOnAnyDay() {
        assertFalse(OfferBought.EMPTY.has(0, 0));
        assertFalse(OfferBought.EMPTY.has(0, Long.MIN_VALUE + 1));
        assertTrue(OfferBought.EMPTY.positionsOn(5).isEmpty());
    }

    @Test
    void aBoughtPositionBlocksOnlyThatDay() {
        OfferBought bought = OfferBought.EMPTY.with(3, 5);
        assertTrue(bought.has(3, 5));
        assertFalse(bought.has(4, 5));
        assertFalse(bought.has(3, 6));
        assertEquals(Set.of(3), bought.positionsOn(5));
        assertTrue(bought.positionsOn(6).isEmpty());
    }

    @Test
    void anotherDayDropsTheOldPositions() {
        OfferBought bought = OfferBought.EMPTY.with(3, 5).with(4, 5).with(1, 6);
        assertEquals(6, bought.day());
        assertEquals(Set.of(1), bought.positions());
    }

    @Test
    void neverGrowsBeyondTheLimit() {
        OfferBought bought = OfferBought.EMPTY;
        for (int position = 0; position < 100; position++) {
            bought = bought.with(position, 1);
        }
        assertEquals(OfferBought.MAX_POSITIONS, bought.positions().size());
    }

    @Test
    void atTheLimitAPresentPositionChangesNothingAndANewOneIsDropped() {
        OfferBought full = OfferBought.EMPTY;
        for (int position = 0; position < OfferBought.MAX_POSITIONS; position++) {
            full = full.with(position, 1);
        }
        assertEquals(full, full.with(5, 1));
        assertEquals(full, full.with(OfferBought.MAX_POSITIONS, 1), "a new position beyond the limit is not stored");
    }

    @Test
    void withIsIdempotent() {
        assertEquals(OfferBought.EMPTY.with(3, 1), OfferBought.EMPTY.with(3, 1).with(3, 1));
    }

    @Test
    void aNegativePositionIsStoredAsIs() {
        assertTrue(OfferBought.EMPTY.with(-1, 4).has(-1, 4));
    }

    @Test
    void theNeverDayCanBeMarkedWhenAskedExplicitly() {
        // Observed: Long.MIN_VALUE is "never" only for EMPTY; no real day has that value, so it does not matter.
        assertTrue(OfferBought.EMPTY.with(0, Long.MIN_VALUE).has(0, Long.MIN_VALUE));
    }

    @Test
    void theCodecRoundTrips() {
        OfferBought bought = OfferBought.EMPTY.with(2, 9).with(7, 9);
        var json = OfferBought.CODEC.encodeStart(JsonOps.INSTANCE, bought).getOrThrow();
        assertEquals(bought, OfferBought.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        var emptyJson = OfferBought.CODEC.encodeStart(JsonOps.INSTANCE, OfferBought.EMPTY).getOrThrow();
        assertEquals(OfferBought.EMPTY, OfferBought.CODEC.parse(JsonOps.INSTANCE, emptyJson).getOrThrow());
    }
}
