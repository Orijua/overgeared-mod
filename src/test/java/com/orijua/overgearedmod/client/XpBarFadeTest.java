package com.orijua.overgearedmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpBarFadeTest {
    private static final float EPSILON = 1e-4f;

    /** Feeds frames every 50 ms from {@code from} to {@code to}, like the HUD does, and returns the last opacity. */
    private static float run(XpBarFade fade, long from, long to, int level, int xp) {
        float alpha = 0;
        for (long now = from; now <= to; now += 50) {
            alpha = fade.update(now, level, xp);
        }
        return alpha;
    }

    @Test
    void joiningAWorldIsNotAGain() {
        XpBarFade fade = new XpBarFade();
        assertEquals(0, run(fade, 0, 3000, 5, 40), EPSILON);
    }

    @Test
    void gainFadesInStaysAndFadesOut() {
        XpBarFade fade = new XpBarFade();
        fade.update(0, 1, 0);
        assertEquals(0.25f, fade.update(50, 1, 10), EPSILON);
        assertEquals(1, run(fade, 100, 5000, 1, 10), EPSILON);
        // 5 s after the gain at 50 ms the fade-out starts and takes 600 ms.
        assertTrue(run(fade, 5050, 5350, 1, 10) < 1);
        assertEquals(0, run(fade, 5400, 5700, 1, 10), EPSILON);
    }

    @Test
    void anotherGainRestartsTheWaitWithoutAddingToIt() {
        XpBarFade fade = new XpBarFade();
        fade.update(0, 1, 0);
        run(fade, 50, 4000, 1, 10);
        fade.update(4050, 1, 20);
        // Still fully shown just before 5 s after the second gain, already hidden 5.6 s after it.
        assertEquals(1, run(fade, 4100, 9000, 1, 20), EPSILON);
        assertEquals(0, run(fade, 9050, 9700, 1, 20), EPSILON);
    }

    @Test
    void gainWhileFadingOutTurnsBackFromWhereItIs() {
        XpBarFade fade = new XpBarFade();
        fade.update(0, 1, 0);
        run(fade, 50, 5000, 1, 10);
        float fading = run(fade, 5050, 5350, 1, 10);
        assertTrue(fading > 0 && fading < 1);
        float next = fade.update(5400, 1, 20);
        assertEquals(fading + 0.25f, next, EPSILON);
    }

    @Test
    void levelUpIsAGainEvenThoughXpDrops() {
        XpBarFade fade = new XpBarFade();
        fade.update(0, 1, 70);
        assertTrue(fade.update(50, 2, 5) > 0);
    }

    @Test
    void resetForgetsTheLastValues() {
        XpBarFade fade = new XpBarFade();
        fade.update(0, 1, 0);
        fade.reset();
        assertEquals(0, run(fade, 50, 1000, 9, 99), EPSILON);
    }
}
