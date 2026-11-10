package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

/** The only place that writes the player's account. */
public final class AccountService {
    private AccountService() {
    }

    public static Account get(ServerPlayer player) {
        return player.getData(ModAttachments.ACCOUNT.get());
    }

    public static long won(ServerPlayer player) {
        return get(player).won();
    }

    /** Writes the account only when it changes, so it only syncs then. */
    private static void write(ServerPlayer player, Account next) {
        if (!next.equals(get(player))) {
            player.setData(ModAttachments.ACCOUNT.get(), next);
        }
    }

    /** Sets the balance (negative becomes 0); only writes, and so syncs, when the value changes. */
    public static void setWon(ServerPlayer player, long won) {
        write(player, get(player).withWon(won));
    }

    /** Takes the starter debt, only if it was never assigned (any other state stays); one write. Returns whether it was taken now. */
    public static boolean assignStarterDebt(ServerPlayer player, long amount) {
        Account before = get(player);
        write(player, AccountRules.withStarterDebt(before, amount));
        return before.debtState() == DebtState.UNASSIGNED;
    }

    /**
     * What a shift did.
     *
     * @param paid    the Won the shift pays in all
     * @param toDebt  the part that went to the debt
     * @param settled whether that part paid the debt off
     */
    public record WorkPay(long paid, long toDebt, boolean settled) {
    }

    /** Pays a shift: the quota to the debt, the rest to the balance, in a single write. */
    public static WorkPay payWork(ServerPlayer player, long workPay, int quotaPercent) {
        Account before = get(player);
        Account after = AccountRules.afterWork(before, workPay, quotaPercent);
        write(player, after);
        return new WorkPay(workPay, AccountRules.quota(before, workPay, quotaPercent), AccountRules.justSettled(before, after));
    }

    /**
     * Pays {@code amount} of the debt from the balance, in a single write; returns whether it was paid off. The caller
     * ({@link DebtService}) checks the amount first: here a bad one throws.
     */
    public static boolean payDebt(ServerPlayer player, long amount) {
        Account before = get(player);
        Account after = AccountRules.afterDebtPayment(before, amount);
        write(player, after);
        return AccountRules.justSettled(before, after);
    }

    /** For debugging: sets the debt (the state follows from it: 0 over an ACTIVE debt settles it). */
    public static void setDebt(ServerPlayer player, long debt) {
        write(player, get(player).withDebt(debt));
    }

    /** For debugging: no debt and never assigned, keeping the balance. */
    public static void clearDebt(ServerPlayer player) {
        write(player, new Account(won(player), 0L, DebtState.UNASSIGNED));
    }

    public static void deposit(ServerPlayer player, long amount) {
        setWon(player, AccountRules.add(won(player), amount));
    }

    public static void withdraw(ServerPlayer player, long amount) {
        setWon(player, AccountRules.subtract(won(player), amount));
    }
}
