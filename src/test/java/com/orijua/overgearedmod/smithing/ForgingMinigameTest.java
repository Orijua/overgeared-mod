package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.smithing.ForgingMinigame.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingMinigameTest {
    private static final double ZONE = 0.6;

    /** Time after starting to hold at which the temperature reaches {@code temperature}. */
    private static long heatTime(double temperature) {
        return Math.round(temperature * ForgingMinigame.HEAT_FULL_MS);
    }

    /** Time into a sweep at which the marker is in the middle of the bar (a quarter of the period). */
    private static long middle(int strike) {
        return ForgingMinigame.sweepMillis(strike) / 4;
    }

    @Test
    void releasingInTheBandScoresFullHeat() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.stopHeating(heatTime(ZONE + ForgingMinigame.ZONE_WIDTH / 2));
        assertEquals(1.0, game.heatScore(), 1e-9);
        assertEquals(Phase.HAMMER, game.phase());
    }

    @Test
    void releasingTooEarlyScoresLess() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.stopHeating(heatTime(ZONE - 0.1));
        assertEquals(0.6, game.heatScore(), 1e-6, "0.1 short of the band, out of 0.25 tolerance");
    }

    @Test
    void holdingToTheTopBurnsTheMetal() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.update(ForgingMinigame.HEAT_FULL_MS);
        assertEquals(0.0, game.heatScore(), 1e-9);
        assertEquals(Phase.HAMMER, game.phase(), "burning moves on to the strikes by itself");
    }

    @Test
    void strikingInTheMiddleIsPerfectAndAtTheEdgeIsZero() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.stopHeating(1000);
        assertEquals(1.0, game.strike(1000 + middle(0)), 1e-9, "marker in the middle");
        assertEquals(0.0, game.strike(1000 + middle(0)), 1e-9, "right after a strike the marker restarts at the edge");
    }

    @Test
    void sweepsGetFasterEveryStrike() {
        for (int i = 1; i < ForgingMinigame.STRIKES; i++) {
            assertTrue(ForgingMinigame.sweepMillis(i) < ForgingMinigame.sweepMillis(i - 1), "strike " + i + " should be quicker");
        }
    }

    @Test
    void perfectGameScoresFullPrecisionAndEnds() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        long now = heatTime(ZONE + 0.01);
        game.stopHeating(now);
        for (int i = 0; i < ForgingMinigame.STRIKES; i++) {
            now += middle(i);
            game.strike(now);
        }
        assertEquals(Phase.DONE, game.phase());
        assertEquals(1.0, game.precision(), 1e-9);
        assertEquals(-1, game.strike(now + 100), 1e-9, "no strikes after the end");
    }

    @Test
    void burntAndMashedGameScoresZero() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.update(ForgingMinigame.HEAT_FULL_MS);
        for (int i = 0; i < ForgingMinigame.STRIKES; i++) {
            game.strike(ForgingMinigame.HEAT_FULL_MS); // mashing: every strike lands at the edge
        }
        assertEquals(0.0, game.precision(), 1e-9);
    }
}
