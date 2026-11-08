package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.Activity;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.SessionModifiers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FatigueRulesTest {
    private static final FatigueBalance B = FatigueBalance.DEFAULT;
    private static final SessionModifiers N = SessionModifiers.NEUTRAL;

    private static double secondsToEmpty(Activity activity) {
        return 1.0 / FatigueRules.drainPerSecond(B, activity, N);
    }

    @Test
    void aNormalSessionLastsFortyMinutes() {
        assertEquals(40 * 60, secondsToEmpty(Activity.NORMAL), 1.0);
    }

    @Test
    void restingDoublesAndFightingHalvesTheSession() {
        assertEquals(80 * 60, secondsToEmpty(Activity.REST), 1.0);
        assertEquals(20 * 60, secondsToEmpty(Activity.COMBAT), 1.0);
    }

    @Test
    void theRealWorldRefillsInTenMinutes() {
        assertEquals(1.0, FatigueRules.afterRecovery(B, 0.0, N, 10 * 60), 1e-9);
        assertEquals(0.5, FatigueRules.afterRecovery(B, 0.0, N, 5 * 60), 1e-9);
    }

    @Test
    void energyNeverLeavesZeroToOne() {
        assertEquals(0.0, FatigueRules.afterDrain(B, 0.01, Activity.COMBAT, N, 100000));
        assertEquals(1.0, FatigueRules.afterRecovery(B, 0.99, N, 100000));
        assertEquals(0.0, FatigueRules.afterForge(B, 0.001, N));
        assertEquals(1.0, FatigueRules.clamp(7.0));
        assertEquals(0.0, FatigueRules.clamp(-3.0));
    }

    @Test
    void aForgeCostsTwentySecondsOfNormalPlay() {
        double after = FatigueRules.afterForge(B, 1.0, N);
        assertEquals(1.0 - 20.0 / (40 * 60), after, 1e-9);
    }

    @Test
    void neutralModifiersChangeNothingAndOthersScale() {
        double base = FatigueRules.drainPerSecond(B, Activity.NORMAL, N);
        assertEquals(base, FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(1, 1, 1)));
        assertEquals(base / 2, FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(2, 1, 1)), 1e-12);
        assertEquals(base * 1.5, FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(1, 1.5, 1)), 1e-12);
        assertEquals(FatigueRules.recoverPerSecond(B, N) * 2, FatigueRules.recoverPerSecond(B, new SessionModifiers(1, 1, 2)), 1e-12);
    }

    @Test
    void eachWarningFiresOnceWhenCrossedDownwards() {
        List<Double> warn = B.warnAt();
        assertEquals(OptionalDouble.of(0.25), FatigueRules.crossedWarning(0.26, 0.25, warn), "reaching it counts");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.25, 0.24, warn), "already past: not again");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.9, 0.8, warn), "nothing crossed");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.1, 0.5, warn), "going up crosses nothing");
        assertEquals(OptionalDouble.of(0.10), FatigueRules.crossedWarning(0.11, 0.09, warn));
        assertEquals(OptionalDouble.of(0.02), FatigueRules.crossedWarning(0.03, 0.0, warn));
    }

    @Test
    void aBigJumpReportsOnlyTheMostUrgentWarning() {
        assertEquals(OptionalDouble.of(0.10), FatigueRules.crossedWarning(0.30, 0.05, B.warnAt()), "25 % and 10 % crossed: the 10 %");
        assertEquals(OptionalDouble.of(0.02), FatigueRules.crossedWarning(0.30, 0.0, B.warnAt()));
    }

    @Test
    void theCapsuleNeedsTheMinimumEnergy() {
        assertTrue(FatigueRules.canConnect(B, 0.10), "exactly the minimum");
        assertTrue(FatigueRules.canConnect(B, 1.0));
        assertFalse(FatigueRules.canConnect(B, 0.0999));
        assertFalse(FatigueRules.canConnect(B, 0.0));
    }

    @Test
    void minutesLeftAreRoundedUp() {
        assertEquals(OptionalDouble.of(40), FatigueRules.minutesLeft(B, 1.0, Activity.NORMAL, N));
        assertEquals(OptionalDouble.of(20), FatigueRules.minutesLeft(B, 0.5, Activity.NORMAL, N));
        assertEquals(OptionalDouble.of(10), FatigueRules.minutesLeft(B, 0.5, Activity.COMBAT, N));
        assertEquals(OptionalDouble.of(1), FatigueRules.minutesLeft(B, 0.001, Activity.NORMAL, N));
        assertEquals(OptionalDouble.of(0), FatigueRules.minutesLeft(B, 0.0, Activity.NORMAL, N));
    }

    @Test
    void withTheFatigueOffNothingDrainsAndEveryoneMayConnect() {
        FatigueBalance off = new FatigueBalance(false, 40, 0.5, 2, 20, 10, 0.1, 15, List.of(0.25));
        assertEquals(0.0, FatigueRules.drainPerSecond(off, Activity.COMBAT, N));
        assertEquals(1.0, FatigueRules.afterForge(off, 1.0, N));
        assertTrue(FatigueRules.canConnect(off, 0.0));
        assertEquals(OptionalDouble.empty(), FatigueRules.minutesLeft(off, 0.5, Activity.NORMAL, N));
    }

    @Test
    void extremeConfigurationsDoNotDivideByZero() {
        FatigueBalance silly = new FatigueBalance(true, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, List.of());
        assertTrue(Double.isFinite(FatigueRules.drainPerSecond(silly, Activity.NORMAL, N)));
        assertTrue(Double.isFinite(FatigueRules.recoverPerSecond(silly, N)));
        assertEquals(0.0, FatigueRules.drainPerSecond(silly, Activity.REST, N), "a zero multiplier is a free activity");
        assertEquals(OptionalDouble.empty(), FatigueRules.minutesLeft(silly, 0.5, Activity.REST, N));
        assertTrue(Double.isFinite(FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(0, 1, 1))));
    }

    @Test
    void theDefaultsAreSortedAndInRange() {
        List<Double> warn = B.warnAt();
        for (int i = 0; i < warn.size(); i++) {
            assertTrue(warn.get(i) > 0.0 && warn.get(i) < 1.0);
            if (i > 0) {
                assertTrue(warn.get(i) < warn.get(i - 1), "descending");
            }
        }
    }
}
