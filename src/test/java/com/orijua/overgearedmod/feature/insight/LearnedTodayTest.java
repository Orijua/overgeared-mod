package com.orijua.overgearedmod.feature.insight;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnedTodayTest {
    private static final UUID A = new UUID(1, 1);
    private static final UUID B = new UUID(2, 2);

    @Test
    void theSameDayBlocksAndTheNextOneUnblocks() {
        LearnedToday learned = LearnedToday.EMPTY.with(A, 5);
        assertTrue(learned.has(A, 5));
        assertFalse(learned.has(B, 5), "another target is free");
        assertFalse(learned.has(A, 6), "the next day unblocks");
    }

    @Test
    void withOnAnotherDayDropsYesterdaysTargets() {
        LearnedToday learned = LearnedToday.EMPTY.with(A, 5).with(B, 5).with(B, 6);
        assertEquals(6, learned.day());
        assertEquals(1, learned.targets().size());
        assertFalse(learned.has(A, 6));
        assertTrue(learned.has(B, 6));
    }

    @Test
    void itNeverGrowsPastTheCap() {
        LearnedToday learned = LearnedToday.EMPTY;
        for (int i = 0; i < LearnedToday.MAX_TARGETS + 50; i++) {
            learned = learned.with(new UUID(i, i), 1);
        }
        assertEquals(LearnedToday.MAX_TARGETS, learned.targets().size());
    }

    @Test
    void itSurvivesTheCodec() {
        LearnedToday learned = LearnedToday.EMPTY.with(A, 7).with(B, 7);
        var json = LearnedToday.CODEC.encodeStart(JsonOps.INSTANCE, learned).getOrThrow();
        assertEquals(learned, LearnedToday.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
