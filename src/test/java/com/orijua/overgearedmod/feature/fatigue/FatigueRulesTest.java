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
    void theLevelFollowsTheWarningThresholds() {
        List<Double> warn = List.of(0.25, 0.10, 0.05);
        assertEquals(FatigueRules.Level.NORMAL, FatigueRules.level(0.5, warn));
        assertEquals(FatigueRules.Level.LOW, FatigueRules.level(0.25, warn), "the border is included");
        assertEquals(FatigueRules.Level.CRITICAL, FatigueRules.level(0.10, warn));
        assertEquals(FatigueRules.Level.NORMAL, FatigueRules.level(0.3, List.of()), "default thresholds");
        assertEquals(FatigueRules.Level.LOW, FatigueRules.level(0.2, List.of()));
        assertEquals(FatigueRules.Level.CRITICAL, FatigueRules.level(0.05, List.of(0.25)), "default critical 0.10 when only one is given");
    }

    @Test
    void aNormalSessionLastsFortyMinutes() {
        assertEquals(40 * 60, secondsToEmpty(Activity.NORMAL), 1.0);
    }

    @Test
    void restingDoublesAndFightingHalvesTheSession() {
        assertEquals(50 * 60, secondsToEmpty(Activity.REST), 1.0, "40 min / 0.8");
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
        assertEquals(base, FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(1, 1, 0)));
        assertEquals(base / 2, FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(2, 1, 0)), 1e-12);
        assertEquals(FatigueRules.recoverPerSecond(B, N) * 2, FatigueRules.recoverPerSecond(B, new SessionModifiers(1, 2, 0)), 1e-12);
    }

    @Test
    void eachWarningFiresOnceWhenCrossedDownwards() {
        List<Double> warn = B.warnAt();
        assertEquals(OptionalDouble.of(0.15), FatigueRules.crossedWarning(0.16, 0.15, warn), "reaching it counts");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.15, 0.14, warn), "already past: not again");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.9, 0.8, warn), "nothing crossed");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.1, 0.5, warn), "going up crosses nothing");
        assertEquals(OptionalDouble.of(0.08), FatigueRules.crossedWarning(0.09, 0.07, warn));
        assertEquals(OptionalDouble.of(0.02), FatigueRules.crossedWarning(0.03, 0.0, warn));
    }

    @Test
    void aBigJumpReportsOnlyTheMostUrgentWarning() {
        assertEquals(OptionalDouble.of(0.08), FatigueRules.crossedWarning(0.30, 0.05, B.warnAt()), "15 % and 8 % crossed: the 8 %");
        assertEquals(OptionalDouble.of(0.02), FatigueRules.crossedWarning(0.30, 0.0, B.warnAt()));
    }

    @Test
    void theCapsuleNeedsTheMinimumEnergy() {
        assertTrue(FatigueRules.canConnect(B, 0.25), "exactly the minimum");
        assertTrue(FatigueRules.canConnect(B, 1.0));
        assertFalse(FatigueRules.canConnect(B, 0.2499));
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
        FatigueBalance off = new FatigueBalance(false, 40, 0.5, 2, 20, 10, 0.1, 15, List.of(0.25), 0.3);
        assertEquals(0.0, FatigueRules.drainPerSecond(off, Activity.COMBAT, N));
        assertEquals(1.0, FatigueRules.afterForge(off, 1.0, N));
        assertTrue(FatigueRules.canConnect(off, 0.0));
        assertEquals(OptionalDouble.empty(), FatigueRules.minutesLeft(off, 0.5, Activity.NORMAL, N));
    }

    @Test
    void extremeConfigurationsDoNotDivideByZero() {
        FatigueBalance silly = new FatigueBalance(true, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, List.of(), 0.3);
        assertTrue(Double.isFinite(FatigueRules.drainPerSecond(silly, Activity.NORMAL, N)));
        assertTrue(Double.isFinite(FatigueRules.recoverPerSecond(silly, N)));
        assertEquals(0.0, FatigueRules.drainPerSecond(silly, Activity.REST, N), "a zero multiplier is a free activity");
        assertEquals(OptionalDouble.empty(), FatigueRules.minutesLeft(silly, 0.5, Activity.REST, N));
        assertTrue(Double.isFinite(FatigueRules.drainPerSecond(B, Activity.NORMAL, new SessionModifiers(0, 1, 0))));
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

    @Test
    void thresholdsThatAreNotExactInAFloatStillCompareEqual() {
        FatigueBalance b = new FatigueBalance(true, 40, 0.5, 2, 20, 10, 0.35, 15, List.of(0.35, 0.1), 0.3);
        assertTrue(FatigueRules.canConnect(b, 0.35f), "35 % saved as a float meets a 35 % minimum");
        assertFalse(FatigueRules.canConnect(b, 0.34f));
        assertEquals(OptionalDouble.of(0.35), FatigueRules.crossedWarning(0.36f, 0.35f, b.warnAt()), "reaching 35 % warns");
        assertEquals(OptionalDouble.empty(), FatigueRules.crossedWarning(0.35f, 0.349f, b.warnAt()), "already there: not again");
    }

    @Test
    void anEnergyThatIsNotANumberCountsAsEmpty() {
        assertEquals(0.0, FatigueRules.clamp(Double.NaN));
    }

    @Test
    void combatLastsTenSecondsAfterTheLastDamage() {
        assertFalse(FatigueRules.inCombat(1000, java.util.OptionalLong.empty()));
        assertTrue(FatigueRules.inCombat(1000, java.util.OptionalLong.of(1000)));
        assertTrue(FatigueRules.inCombat(1000, java.util.OptionalLong.of(801)), "199 ticks ago");
        assertFalse(FatigueRules.inCombat(1000, java.util.OptionalLong.of(800)), "200 ticks ago");
        assertTrue(FatigueRules.inCombat(1000, java.util.OptionalLong.of(5000)), "a tick in the future counts as combat");
    }

    @Test
    void movingLessThanHalfABlockInASecondIsResting() {
        assertTrue(FatigueRules.isResting(0.0));
        assertTrue(FatigueRules.isResting(0.2499));
        assertFalse(FatigueRules.isResting(0.25), "exactly half a block squared is already moving");
        assertFalse(FatigueRules.isResting(9.0));
    }

    @Test
    void standingStillCostsMostOfTheNormalDrain() {
        assertEquals(0.8, FatigueBalance.DEFAULT.restMultiplier(), 1e-9);
    }

    @Test
    void workingAtAnAnvilOrSmelterCostsAsCombatEvenStandingStill() {
        assertEquals(Activity.COMBAT, FatigueRules.activity(false, true, 1000, java.util.OptionalLong.empty(), true, 0.0));
        assertEquals(Activity.COMBAT, FatigueRules.activity(true, false, 1000, java.util.OptionalLong.empty(), true, 0.0));
    }

    @Test
    void miningIsWorkNotRestForTheWorkWindow() {
        java.util.OptionalLong broke = java.util.OptionalLong.of(1000);
        assertEquals(Activity.NORMAL, FatigueRules.activity(false, false, 1000 + FatigueRules.WORK_WINDOW_TICKS - 1, broke, true, 0.0));
        assertEquals(Activity.REST, FatigueRules.activity(false, false, 1000 + FatigueRules.WORK_WINDOW_TICKS, broke, true, 0.0));
    }

    @Test
    void stillIsRestMovingIsNormalAndTheFirstSecondIsNormal() {
        assertEquals(Activity.REST, FatigueRules.activity(false, false, 0, java.util.OptionalLong.empty(), true, 0.1));
        assertEquals(Activity.NORMAL, FatigueRules.activity(false, false, 0, java.util.OptionalLong.empty(), true, 1.0));
        assertEquals(Activity.NORMAL, FatigueRules.activity(false, false, 0, java.util.OptionalLong.empty(), false, 0.0));
    }
}
