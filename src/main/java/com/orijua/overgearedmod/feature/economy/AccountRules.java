package com.orijua.overgearedmod.feature.economy;

/** Pure arithmetic of the account balance (no Minecraft). */
public final class AccountRules {
    private AccountRules() {
    }

    /** The balance plus the amount, saturating at {@link Long#MAX_VALUE}. */
    public static long add(long balance, long amount) {
        requireNotNegative(amount);
        try {
            return Math.addExact(balance, amount);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    /** The balance minus the amount, never below 0. */
    public static long subtract(long balance, long amount) {
        requireNotNegative(amount);
        return Math.max(0L, balance - amount);
    }

    /** {@code percent} (0 to 100) of the amount, rounded down, without overflowing for any amount. */
    public static long percentOf(long amount, int percent) {
        requireNotNegative(amount);
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100: " + percent);
        }
        return amount / 100 * percent + amount % 100 * percent / 100;
    }

    /** What a payment of {@code workPay} sends to the debt: nothing unless the debt is ACTIVE, never more than what is owed. */
    public static long quota(Account account, long workPay, int percent) {
        if (account.debtState() != DebtState.ACTIVE) {
            return 0L;
        }
        return Math.min(account.debt(), percentOf(workPay, percent));
    }

    /** The account after a shift paying {@code workPay}: the quota goes to the debt and the rest to the balance (saturating). */
    public static Account afterWork(Account account, long workPay, int percent) {
        long quota = quota(account, workPay, percent);
        return new Account(add(account.won(), workPay - quota), account.debt() - quota, account.debtState());
    }

    /** The most that can be paid to the debt now: the least of the balance and the debt; 0 unless ACTIVE. */
    public static long maxDebtPayment(Account account) {
        return account.debtState() == DebtState.ACTIVE ? Math.min(account.won(), account.debt()) : 0L;
    }

    /**
     * The account after paying {@code amount} of the debt from the balance.
     *
     * @throws IllegalArgumentException if the amount is below 1 or above {@link #maxDebtPayment}
     */
    public static Account afterDebtPayment(Account account, long amount) {
        long max = maxDebtPayment(account);
        if (amount < 1L || amount > max) {
            throw new IllegalArgumentException("cannot pay " + amount + " of the debt, the most is " + max);
        }
        return new Account(account.won() - amount, account.debt() - amount, account.debtState());
    }

    /** The account with the starter debt, only when it was not assigned yet; any other state is returned as it is. */
    public static Account withStarterDebt(Account account, long amount) {
        if (account.debtState() != DebtState.UNASSIGNED) {
            return account;
        }
        if (amount < 1L) {
            throw new IllegalArgumentException("the starter debt must be at least 1: " + amount);
        }
        return new Account(account.won(), amount, DebtState.ACTIVE);
    }

    /** Whether a change paid off the debt: it was ACTIVE and now is SETTLED. */
    public static boolean justSettled(Account before, Account after) {
        return before.debtState() == DebtState.ACTIVE && after.debtState() == DebtState.SETTLED;
    }

    public static boolean isDebtSettled(Account account) {
        return account.debtState() == DebtState.SETTLED;
    }

    private static void requireNotNegative(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
}
