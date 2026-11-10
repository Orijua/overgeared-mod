package com.orijua.overgearedmod.feature.economy;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtRulesTest {
    private static final Account ACTIVE = new Account(0, 6400, DebtState.ACTIVE);

    @Test
    void percentRoundsDown() {
        assertEquals(250, AccountRules.percentOf(1000, 25));
        assertEquals(249, AccountRules.percentOf(999, 25));
        assertEquals(0, AccountRules.percentOf(12345, 0));
        assertEquals(12345, AccountRules.percentOf(12345, 100));
        assertEquals(0, AccountRules.percentOf(0, 25));
        assertEquals(0, AccountRules.percentOf(99, 1));
        assertEquals(1, AccountRules.percentOf(100, 1));
    }

    @Test
    void percentNeverOverflows() {
        assertEquals(Long.MAX_VALUE, AccountRules.percentOf(Long.MAX_VALUE, 100));
        assertEquals(Long.MAX_VALUE / 100, AccountRules.percentOf(Long.MAX_VALUE / 100, 100));
        for (int percent : new int[]{1, 25, 33, 99}) {
            BigInteger exact = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100));
            assertEquals(exact.longValueExact(), AccountRules.percentOf(Long.MAX_VALUE, percent), percent + "%");
        }
    }

    @Test
    void percentRefusesBadInput() {
        assertThrows(IllegalArgumentException.class, () -> AccountRules.percentOf(-1, 25));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.percentOf(100, -1));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.percentOf(100, 101));
    }

    @Test
    void quotaIsOnlyForAnActiveDebt() {
        assertEquals(250, AccountRules.quota(ACTIVE, 1000, 25));
        assertEquals(0, AccountRules.quota(Account.EMPTY, 1000, 25));
        assertEquals(0, AccountRules.quota(new Account(0, 0, DebtState.SETTLED), 1000, 25));
    }

    @Test
    void quotaNeverExceedsTheDebt() {
        assertEquals(100, AccountRules.quota(new Account(0, 100, DebtState.ACTIVE), 1000, 100));
        assertEquals(100, AccountRules.quota(new Account(0, 100), 1000, 50));
        assertEquals(0, AccountRules.quota(ACTIVE, 0, 25));
    }

    @Test
    void quotaAtTheLimitsOfThePercent() {
        assertEquals(0, AccountRules.quota(ACTIVE, 1000, 0));
        assertEquals(1000, AccountRules.quota(ACTIVE, 1000, 100));
        assertEquals(6400, AccountRules.quota(ACTIVE, 10_000, 100));
    }

    @Test
    void aShiftSplitsThePay() {
        Account after = AccountRules.afterWork(ACTIVE, 1000, 25);
        assertEquals(750, after.won());
        assertEquals(6150, after.debt());
        assertEquals(DebtState.ACTIVE, after.debtState());
    }

    @Test
    void twentySixShiftsPayOffTheStarterDebt() {
        Account account = ACTIVE;
        for (int shift = 1; shift <= 26; shift++) {
            Account before = account;
            account = AccountRules.afterWork(account, 1000, 25);
            if (shift == 25) {
                assertEquals(DebtState.ACTIVE, account.debtState());
                assertEquals(150, account.debt());
            }
            if (shift == 26) {
                assertEquals(850, account.won() - before.won());
                assertEquals(DebtState.SETTLED, account.debtState());
                assertTrue(AccountRules.justSettled(before, account));
            }
        }
        assertEquals(0, account.debt());
    }

    @Test
    void aShiftPayingNothingChangesNothingAndANegativeOneIsRefused() {
        assertEquals(ACTIVE, AccountRules.afterWork(ACTIVE, 0, 25));
        assertEquals(Account.EMPTY, AccountRules.afterWork(Account.EMPTY, 0, 25));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterWork(ACTIVE, -1, 25));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterWork(Account.EMPTY, -1, 25));
    }

    @Test
    void theLastQuotaPaysOnlyWhatIsLeft() {
        Account almost = new Account(0, 100, DebtState.ACTIVE);
        Account after = AccountRules.afterWork(almost, 1000, 25);
        assertEquals(900, after.won());
        assertEquals(0, after.debt());
        assertEquals(DebtState.SETTLED, after.debtState());
        assertTrue(AccountRules.justSettled(almost, after));
    }

    @Test
    void withoutDebtTheWholePayGoesToTheBalance() {
        assertEquals(1000, AccountRules.afterWork(Account.EMPTY, 1000, 25).won());
        Account settled = new Account(5, 0, DebtState.SETTLED);
        assertEquals(new Account(1005, 0, DebtState.SETTLED), AccountRules.afterWork(settled, 1000, 25));
    }

    @Test
    void theBalanceSaturates() {
        Account rich = new Account(Long.MAX_VALUE - 10, 6400, DebtState.ACTIVE);
        assertEquals(Long.MAX_VALUE, AccountRules.afterWork(rich, 1000, 25).won());
    }

    @Test
    void theBalanceSaturatesAlsoWhenTheShiftSettlesTheDebt() {
        Account rich = new Account(Long.MAX_VALUE - 10, 100, DebtState.ACTIVE);
        Account after = AccountRules.afterWork(rich, 1000, 25);
        assertEquals(new Account(Long.MAX_VALUE, 0, DebtState.SETTLED), after);
        assertTrue(AccountRules.justSettled(rich, after));
    }

    @Test
    void maxDebtPaymentIsTheLeastOfBalanceAndDebt() {
        assertEquals(300, AccountRules.maxDebtPayment(new Account(300, 6400, DebtState.ACTIVE)));
        assertEquals(50, AccountRules.maxDebtPayment(new Account(300, 50, DebtState.ACTIVE)));
        assertEquals(0, AccountRules.maxDebtPayment(new Account(0, 50, DebtState.ACTIVE)));
        assertEquals(0, AccountRules.maxDebtPayment(new Account(300, 0, DebtState.SETTLED)));
        assertEquals(0, AccountRules.maxDebtPayment(new Account(300, 0, DebtState.UNASSIGNED)));
        assertEquals(50, AccountRules.maxDebtPayment(new Account(Long.MAX_VALUE, 50, DebtState.ACTIVE)));
        assertEquals(Long.MAX_VALUE, AccountRules.maxDebtPayment(new Account(Long.MAX_VALUE, Long.MAX_VALUE, DebtState.ACTIVE)));
    }

    @Test
    void aPartialPaymentKeepsTheDebtActive() {
        Account after = AccountRules.afterDebtPayment(new Account(500, 6400, DebtState.ACTIVE), 200);
        assertEquals(new Account(300, 6200, DebtState.ACTIVE), after);
        assertFalse(AccountRules.justSettled(new Account(500, 6400, DebtState.ACTIVE), after));
    }

    @Test
    void anExactPaymentSettlesTheDebt() {
        Account before = new Account(500, 400, DebtState.ACTIVE);
        Account after = AccountRules.afterDebtPayment(before, 400);
        assertEquals(new Account(100, 0, DebtState.SETTLED), after);
        assertTrue(AccountRules.justSettled(before, after));
        assertTrue(AccountRules.isDebtSettled(after));
        assertFalse(AccountRules.isDebtSettled(before));
    }

    @Test
    void payingAllTheBalanceBelowTheDebtKeepsTheDebtActive() {
        Account before = new Account(300, 400, DebtState.ACTIVE);
        Account after = AccountRules.afterDebtPayment(before, 300);
        assertEquals(new Account(0, 100, DebtState.ACTIVE), after);
        assertFalse(AccountRules.justSettled(before, after));
    }

    @Test
    void payingExactlyTheDebtWithExactlyTheBalanceSettlesIt() {
        Account before = new Account(400, 400, DebtState.ACTIVE);
        Account after = AccountRules.afterDebtPayment(before, 400);
        assertEquals(new Account(0, 0, DebtState.SETTLED), after);
        assertTrue(AccountRules.justSettled(before, after));
    }

    @Test
    void badPaymentsAreRefused() {
        Account account = new Account(300, 400, DebtState.ACTIVE);
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterDebtPayment(account, 0));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterDebtPayment(account, -5));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterDebtPayment(account, 401));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterDebtPayment(account, 301));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.afterDebtPayment(Account.EMPTY, 1));
    }

    @Test
    void theStarterDebtIsOnlyTakenOnce() {
        Account taken = AccountRules.withStarterDebt(new Account(10, 0), 6400);
        assertEquals(new Account(10, 6400, DebtState.ACTIVE), taken);
        assertSame(taken, AccountRules.withStarterDebt(taken, 1));
        Account settled = new Account(10, 0, DebtState.SETTLED);
        assertSame(settled, AccountRules.withStarterDebt(settled, 6400));
        assertThrows(IllegalArgumentException.class, () -> AccountRules.withStarterDebt(Account.EMPTY, 0));
    }

    @Test
    void theSmallestStarterDebtIsOne() {
        assertEquals(new Account(0, 1, DebtState.ACTIVE), AccountRules.withStarterDebt(Account.EMPTY, 1));
    }

    @Test
    void anActiveDebtIsNotReplacedByTheStarterDebt() {
        assertSame(ACTIVE, AccountRules.withStarterDebt(ACTIVE, 1));
        assertSame(ACTIVE, AccountRules.withStarterDebt(ACTIVE, 0));
    }

    @Test
    void justSettledNeedsTheTransition() {
        Account settled = new Account(0, 0, DebtState.SETTLED);
        assertFalse(AccountRules.justSettled(settled, settled));
        assertFalse(AccountRules.justSettled(Account.EMPTY, settled));
        assertFalse(AccountRules.justSettled(ACTIVE, ACTIVE));
    }
}
