package com.orijua.overgearedmod.feature.rumor;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RumorRulesTest {
    private static final UUID PLAYER = UUID.fromString("0f4c2a4e-5b1d-4d55-9c1e-5c2f3a1b7e90");

    @Test
    void noRumorsLeft() {
        assertEquals(-1, RumorRules.pick(0, 3, PLAYER, "overgearedmod:blacksmith"));
    }

    @Test
    void sameRumorAllDay() {
        assertEquals(RumorRules.day(24000L * 5), RumorRules.day(24000L * 5 + 23999));
        assertEquals(RumorRules.pick(4, 5, PLAYER, "overgearedmod:blacksmith"), RumorRules.pick(4, 5, PLAYER, "overgearedmod:blacksmith"));
    }

    @Test
    void anotherOneTomorrowAndAllComeRound() {
        assertNotEquals(RumorRules.pick(4, 5, PLAYER, "overgearedmod:blacksmith"), RumorRules.pick(4, 6, PLAYER, "overgearedmod:blacksmith"));
        Set<Integer> heard = new HashSet<>();
        for (long day = 0; day < 4; day++) {
            heard.add(RumorRules.pick(4, day, PLAYER, "overgearedmod:blacksmith"));
        }
        assertEquals(Set.of(0, 1, 2, 3), heard);
    }
}
