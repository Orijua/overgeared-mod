package com.orijua.overgearedmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpGainFeedbackTest {
    private static final float EPSILON = 1e-3f;

    /** Every level needs 100 XP, so the bar fraction is xp / 100. */
    private static XpGainFeedback feedback() {
        return new XpGainFeedback(level -> 100);
    }

    /** Feeds frames every 50 ms from {@code from} to {@code to}, like the HUD does. */
    private static void run(XpGainFeedback gain, long from, long to, int level, int xp) {
        for (long now = from; now <= to; now += 50) {
            gain.update(now, level, xp);
        }
    }

    @Test
    void joiningAWorldIsNotAGain() {
        XpGainFeedback gain = feedback();
        run(gain, 0, 1000, 5, 40);
        assertEquals(0, gain.amount());
        assertEquals(0, gain.popupAlpha(1000), EPSILON);
        assertEquals(0.4f, gain.barFill(), EPSILON);
    }

    @Test
    void gainsInARowAddUp() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 0);
        gain.update(50, 1, 25);
        run(gain, 100, 1000, 1, 25);
        gain.update(1050, 1, 40);
        assertEquals(40, gain.amount());
        assertEquals(1, gain.popupAlpha(1050), EPSILON);
    }

    @Test
    void popupStaysAfterTheLastGainThenFadesAndTheNextGainCountsAnew() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 0);
        gain.update(50, 1, 25);
        assertEquals(1, gain.popupAlpha(50 + XpGainFeedback.HOLD_MILLIS), EPSILON);
        float fading = gain.popupAlpha(50 + XpGainFeedback.HOLD_MILLIS + XpGainFeedback.FADE_OUT_MILLIS / 2);
        assertTrue(fading > 0 && fading < 1);
        run(gain, 100, 3000, 1, 25);
        assertEquals(0, gain.amount());
        gain.update(3050, 1, 30);
        assertEquals(5, gain.amount());
    }

    @Test
    void popupRisesOnceFromTheStartOfTheCount() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 0);
        gain.update(50, 1, 10);
        assertEquals(0, gain.rise(50), EPSILON);
        assertEquals(1, gain.rise(50 + XpGainFeedback.RISE_MILLIS), EPSILON);
        run(gain, 100, 1000, 1, 10);
        gain.update(1050, 1, 20);
        assertEquals(1, gain.rise(1050), EPSILON);
    }

    @Test
    void barFillsGraduallyWithTheNewPartGlowing() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 20);
        gain.update(50, 1, 80);
        float first = gain.barFill();
        assertTrue(first > 0.2f && first < 0.8f);
        assertEquals(0.2f, gain.glowStart(), EPSILON);
        assertEquals(1, gain.glow(50), EPSILON);
        run(gain, 100, 2500, 1, 80);
        assertEquals(0.8f, gain.barFill(), EPSILON);
        assertEquals(0.2f, gain.glowStart(), EPSILON);
    }

    @Test
    void levelUpCountsBothLevelsAndWrapsTheBar() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 90);
        gain.update(50, 3, 10);
        // 10 to finish level 1, 100 for level 2, 10 into level 3.
        assertEquals(120, gain.amount());
        run(gain, 100, 3000, 3, 10);
        assertEquals(0.1f, gain.barFill(), EPSILON);
        // The glow started in level 1, so in level 3 it covers the bar from the start.
        assertEquals(0, gain.glowStart(), EPSILON);
    }

    @Test
    void losingXpMovesTheBarAtOnceWithoutAPopup() {
        XpGainFeedback gain = feedback();
        gain.update(0, 4, 60);
        gain.update(50, 4, 30);
        assertEquals(0, gain.amount());
        assertEquals(0.3f, gain.barFill(), EPSILON);
    }

    @Test
    void resetForgetsTheLastValues() {
        XpGainFeedback gain = feedback();
        gain.update(0, 1, 0);
        gain.reset();
        gain.update(50, 9, 50);
        assertEquals(0, gain.amount());
        assertEquals(0.5f, gain.barFill(), EPSILON);
    }
}
