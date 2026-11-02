package com.orijua.overgearedmod.feature.smelting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmeltingMinigameTest {
    @Test
    void startsInTheMiddleOfTheMetalsZone() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        assertEquals(900, game.zoneMin());
        assertEquals(900 + SmelterRules.ZONE_WIDTH, game.zoneMax());
        assertTrue(game.inZone());
        assertEquals(0.0, game.progress(), 1e-9);
    }

    @Test
    void leavingTheBellowsAloneLetsItCoolOutOfTheZone() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        game.update(SmeltingMinigame.DURATION_MS);
        assertTrue(game.done());
        double start = 900 + SmelterRules.ZONE_WIDTH / 2.0;
        double expectedTemperature = start - SmeltingMinigame.COOL_PER_SECOND * SmeltingMinigame.DURATION_MS / 1000.0;
        assertTrue(expectedTemperature > SmelterRules.AMBIENT, "the ambient floor doesn't come into play");
        assertEquals(435.0, game.temperature(), 1e-9);
        assertEquals(expectedTemperature, game.temperature(), 1e-9);
        // Only the degrees between the start and the bottom of the zone (75) count, at 90 degrees per second.
        long msInZone = Math.round((start - 900) * 1000 / SmeltingMinigame.COOL_PER_SECOND);
        assertEquals(833, msInZone);
        assertEquals(833 / 6000.0, game.precision(), 1e-9);
    }

    @Test
    void pumpingInRhythmKeepsItInTheZone() {
        SmeltingMinigame game = new SmeltingMinigame(1450);
        for (long t = 0; t < SmeltingMinigame.DURATION_MS; t += 100) {
            if (game.temperature() < game.zoneMin() + 50) {
                game.pump();
            }
            game.update(100);
        }
        assertTrue(game.precision() > 0.95, "a steady hand stays in the zone: " + game.precision());
    }

    @Test
    void overheatingCountsAsOutOfTheZone() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        for (int i = 0; i < 10; i++) {
            game.pump();
        }
        assertFalse(game.inZone(), "600 degrees over");
        game.update(1000);
        assertEquals(0.0, game.precision(), 1e-9, "too hot doesn't count");
    }

    @Test
    void theGameEndsAndStopsCounting() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        game.update(SmeltingMinigame.DURATION_MS + 5000);
        double precision = game.precision();
        game.pump();
        game.update(1000);
        assertEquals(precision, game.precision(), 1e-9);
        assertEquals(1.0, game.progress(), 1e-9);
    }
}
