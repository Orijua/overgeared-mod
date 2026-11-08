package com.orijua.overgearedmod.feature.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountRulesTest {
    @Test
    void addsToTheBalance() {
        assertEquals(100, AccountRules.add(0, 100));
    }

    @Test
    void addingSaturatesAtTheMaximum() {
        assertEquals(Long.MAX_VALUE, AccountRules.add(Long.MAX_VALUE - 1, 10));
    }

    @Test
    void subtractingNeverGoesBelowZero() {
        assertEquals(0, AccountRules.subtract(50, 100));
    }

    @Test
    void subtractingNothingChangesNothing() {
        assertEquals(100, AccountRules.subtract(100, 0));
    }

    @Test
    void aNegativeAmountIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AccountRules.add(10, -1));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.subtract(10, -1));
    }

    @Test
    void addingOneToTheMaximumMinusOneReachesTheMaximum() {
        assertEquals(Long.MAX_VALUE, AccountRules.add(Long.MAX_VALUE - 1, 1));
    }

    @Test
    void addingNothingToTheMaximumKeepsIt() {
        assertEquals(Long.MAX_VALUE, AccountRules.add(Long.MAX_VALUE, 0));
    }

    @Test
    void subtractingTheWholeBalanceLeavesZero() {
        assertEquals(0, AccountRules.subtract(37, 37));
    }

    @Test
    void subtractingNothingFromNothingIsZero() {
        assertEquals(0, AccountRules.subtract(0, 0));
    }

    @Test
    void addingTheMinimumLongIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AccountRules.add(10, Long.MIN_VALUE));
    }

    @Test
    void subtractingTheMinimumLongIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AccountRules.subtract(10, Long.MIN_VALUE));
    }
}
