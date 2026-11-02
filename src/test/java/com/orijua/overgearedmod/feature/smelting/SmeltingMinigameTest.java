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
        assertEquals(975.0, game.temperature(), 1e-9, "900 plus half of the 150-degree zone");
        assertEquals(0.0, game.progress(), 1e-9);
    }

    @Test
    void leavingTheBellowsAloneLetsItCoolOutOfTheZone() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        game.update(SmeltingMinigame.DURATION_MS);
        assertTrue(game.done());
        // 975 - 90 degrees per second * 6 s = 435, well above the ambient floor of 20.
        assertTrue(435 > SmelterRules.AMBIENT, "the ambient floor doesn't come into play");
        assertEquals(435.0, game.temperature(), 1e-9);
        // Only the 75 degrees between the start and the bottom of the zone count: 75 / 90 s = 833 ms.
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
        assertFalse(game.inZone(), "525 degrees over the zone (1575 against 1050)");
        game.update(1000);
        assertEquals(0.0, game.precision(), 1e-9, "too hot doesn't count");
    }

    @Test
    void aLongFrameCountsTheSameAsManyShortOnes() {
        SmeltingMinigame longFrame = new SmeltingMinigame(900);
        longFrame.update(SmeltingMinigame.DURATION_MS);
        SmeltingMinigame shortFrames = new SmeltingMinigame(900);
        for (int i = 0; i < SmeltingMinigame.DURATION_MS; i++) {
            shortFrames.update(1);
        }
        assertEquals(longFrame.precision(), shortFrames.precision(), 1e-3);
        assertEquals(longFrame.temperature(), shortFrames.temperature(), 1e-3);

        // With a pump in the middle, so the crossing of the zone falls inside a long frame.
        SmeltingMinigame longPumped = new SmeltingMinigame(900);
        longPumped.update(1000);
        longPumped.pump();
        longPumped.update(SmeltingMinigame.DURATION_MS - 1000);
        SmeltingMinigame shortPumped = new SmeltingMinigame(900);
        for (int i = 0; i < 1000; i++) {
            shortPumped.update(1);
        }
        shortPumped.pump();
        for (int i = 1000; i < SmeltingMinigame.DURATION_MS; i++) {
            shortPumped.update(1);
        }
        assertEquals(longPumped.precision(), shortPumped.precision(), 1e-3);
        assertEquals(longPumped.temperature(), shortPumped.temperature(), 1e-3);
    }

    @Test
    void pumpingNeverGoesPastTheMaximum() {
        SmeltingMinigame game = new SmeltingMinigame(1700); // starts at 1775
        game.pump();
        assertEquals(SmelterRules.MAX_TEMPERATURE, game.temperature(), 1e-9, "not 1835");
    }

    @Test
    void zeroOrNegativeTimeChangesNothing() {
        SmeltingMinigame game = new SmeltingMinigame(900);
        game.update(1000);
        double temperature = game.temperature();
        double progress = game.progress();
        double precision = game.precision();
        game.update(0);
        game.update(-500);
        assertEquals(temperature, game.temperature(), 1e-9);
        assertEquals(progress, game.progress(), 1e-9);
        assertEquals(precision, game.precision(), 1e-9);
    }

    @Test
    void theZoneEdgesCountAsInside() {
        SmeltingMinigame top = new SmeltingMinigame(900);
        top.pump();
        top.pump(); // 975 -> 1095
        top.update(500); // -45
        assertEquals(1050.0, top.temperature(), 1e-9);
        assertTrue(top.inZone(), "the top edge is inside");

        SmeltingMinigame bottom = new SmeltingMinigame(900);
        bottom.pump(); // 1035
        bottom.update(1500); // -135
        assertEquals(900.0, bottom.temperature(), 1e-9);
        assertTrue(bottom.inZone(), "the bottom edge is inside");
        bottom.update(1);
        assertEquals(899.91, bottom.temperature(), 1e-9);
        assertFalse(bottom.inZone(), "a hair below is out");
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
