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
        assertTrue(game.temperature() < 900, "cooled below the zone");
        assertTrue(game.precision() < 0.5, "only the first part counted: " + game.precision());
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
