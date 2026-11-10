package com.orijua.overgearedmod.core.save;

import com.orijua.overgearedmod.core.save.SaveMigrations.Step;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaveMigrationsTest {
    private static final Step TO_2 = new Step(2, "two", player -> { });
    private static final Step TO_3 = new Step(3, "three", player -> { });
    private static final Step TO_4 = new Step(4, "four", player -> { });
    private static final List<Step> STEPS = List.of(TO_2, TO_3, TO_4);

    @Test
    void aBaselineSaveNeedsEveryStep() {
        assertEquals(STEPS, SaveMigrations.pending(SaveMigrations.BASELINE, STEPS));
    }

    @Test
    void aSaveInTheMiddleNeedsOnlyTheLaterSteps() {
        assertEquals(List.of(TO_3, TO_4), SaveMigrations.pending(2, STEPS));
        assertEquals(List.of(TO_4), SaveMigrations.pending(3, STEPS));
    }

    @Test
    void anUpToDateOrNewerSaveNeedsNone() {
        assertEquals(List.of(), SaveMigrations.pending(4, STEPS));
        assertEquals(List.of(), SaveMigrations.pending(5, STEPS));
    }

    @Test
    void theStepsMustBeOnePerVersionInOrder() {
        assertDoesNotThrow(() -> SaveMigrations.validate(List.of(), SaveMigrations.BASELINE));
        assertDoesNotThrow(() -> SaveMigrations.validate(STEPS, 4));

        assertThrows(IllegalStateException.class, () -> SaveMigrations.validate(STEPS, 5), "a version without its step");
        assertThrows(IllegalStateException.class, () -> SaveMigrations.validate(STEPS, 3), "a step beyond the version");
        assertThrows(IllegalStateException.class, () -> SaveMigrations.validate(List.of(TO_3), 2), "a step to the wrong version");
        assertThrows(IllegalStateException.class, () -> SaveMigrations.validate(List.of(TO_3, TO_2), 3), "steps out of order");
        assertThrows(IllegalStateException.class, () -> SaveMigrations.validate(List.of(), 0), "below the baseline");
    }

    @Test
    void theRealStepsMatchTheCurrentVersion() {
        // Loading the class runs its own validation; this fails with its message if they do not match.
        assertDoesNotThrow(() -> SaveMigrations.pending(SaveMigrations.CURRENT, List.of()));
    }
}
