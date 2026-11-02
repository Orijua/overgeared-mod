package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.feature.smithing.ForgingMinigame.Phase;
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
    void theEdgesOfTheBandCountAsInside() {
        ForgingMinigame low = new ForgingMinigame(ZONE);
        low.startHeating(0);
        low.stopHeating(heatTime(ZONE));
        assertEquals(1.0, low.heatScore(), 1e-9, "bottom edge");

        ForgingMinigame high = new ForgingMinigame(ZONE);
        high.startHeating(0);
        high.stopHeating(heatTime(ZONE + ForgingMinigame.ZONE_WIDTH));
        assertEquals(1.0, high.heatScore(), 1e-9, "top edge");
    }

    @Test
    void strikingBeforeTheHammerPhaseIsRejected() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        assertEquals(-1, game.strike(0), 1e-9, "before heating starts");
        assertEquals(Phase.HEAT, game.phase());
        game.startHeating(0);
        assertEquals(-1, game.strike(500), 1e-9, "while heating");
        assertEquals(Phase.HEAT, game.phase());
        assertEquals(0, game.strikesDone());
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
    void heatWeighsThirtyPercent() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        long now = heatTime(ZONE + 0.01);
        game.stopHeating(now);
        for (int i = 0; i < ForgingMinigame.STRIKES; i++) {
            game.strike(now); // always at the edge: every strike scores 0
        }
        assertEquals(0.3, game.precision(), 1e-9, "perfect heat, worthless strikes");
    }

    @Test
    void strikesWeighSeventyPercent() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.update(ForgingMinigame.HEAT_FULL_MS); // burnt: heat 0
        long now = ForgingMinigame.HEAT_FULL_MS;
        for (int i = 0; i < ForgingMinigame.STRIKES; i++) {
            now += middle(i);
            game.strike(now);
        }
        assertEquals(0.7, game.precision(), 1e-9, "burnt metal, perfect strikes");
    }

    /** Band 0.6 to 0.72; the tolerance outside it is 0.25 (private in the class). */
    @Test
    void heatOutsideTheBandFadesAndThenScoresNothing() {
        ForgingMinigame over = new ForgingMinigame(ZONE);
        over.startHeating(0);
        over.stopHeating(2110); // 0.844: 0.124 over the band
        assertEquals(1 - 0.124 / 0.25, over.heatScore(), 1e-9);

        ForgingMinigame far = new ForgingMinigame(ZONE);
        far.startHeating(0);
        far.stopHeating(2450); // 0.98: 0.26 over, past the tolerance
        assertEquals(0.0, far.heatScore(), 1e-9);

        ForgingMinigame cold = new ForgingMinigame(ZONE);
        cold.startHeating(0);
        cold.stopHeating(750); // 0.3: 0.3 under the band
        assertEquals(0.0, cold.heatScore(), 1e-9);
    }

    /** A game at the hammer phase with its first sweep starting at 1000 ms. */
    private static ForgingMinigame hammerPhase() {
        ForgingMinigame game = new ForgingMinigame(ZONE);
        game.startHeating(0);
        game.stopHeating(1000);
        return game;
    }

    @Test
    void strikesBetweenTheEdgeAndTheMiddleScoreInBetween() {
        // First sweep: 1600 ms across and back, position = 2t/1600 on the way out.
        assertEquals(0.5, hammerPhase().strike(1000 + 180), 1e-9, "position 0.225: distance 0.275");
        assertEquals(1.0, hammerPhase().strike(1000 + 360), 1e-9, "position 0.45: exactly on the perfect margin");
        assertEquals(0.9778, hammerPhase().strike(1000 + 352), 1e-4, "position 0.44: just outside it");
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
