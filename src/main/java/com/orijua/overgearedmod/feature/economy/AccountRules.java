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

    private static void requireNotNegative(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
}
