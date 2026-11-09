package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.config.DebtBalance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebtBalanceTest {
    @Test
    void theDefaultIsTheDesignedOne() {
        assertEquals(6400L, DebtBalance.DEFAULT.starterDebt());
        assertEquals(25, DebtBalance.DEFAULT.quotaPercent());
        assertEquals(DebtBalance.DEFAULT, DebtBalance.current());
    }

    @Test
    void theDebtMustBeAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> new DebtBalance(0, 25));
        assertThrows(IllegalArgumentException.class, () -> new DebtBalance(-1, 25));
        assertThrows(IllegalArgumentException.class, () -> new DebtBalance(DebtBalance.MAX_STARTER_DEBT + 1, 25));
        assertEquals(1L, new DebtBalance(1, 0).starterDebt());
    }

    @Test
    void thePercentIsBetweenZeroAndAHundred() {
        assertThrows(IllegalArgumentException.class, () -> new DebtBalance(6400, -1));
        assertThrows(IllegalArgumentException.class, () -> new DebtBalance(6400, 101));
        assertEquals(100, new DebtBalance(6400, 100).quotaPercent());
        assertEquals(0, new DebtBalance(6400, 0).quotaPercent());
    }

    @Test
    void anOverrideLastsOnlyWhileItRuns() {
        DebtBalance custom = new DebtBalance(100, 50);
        DebtBalance.withOverride(custom, () -> assertEquals(custom, DebtBalance.current()));
        assertEquals(DebtBalance.DEFAULT, DebtBalance.current());
    }
}
