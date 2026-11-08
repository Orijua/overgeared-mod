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

    /** Sets the balance (negative becomes 0); only writes, and so syncs, when the value changes. */
    public static void setWon(ServerPlayer player, long won) {
        Account current = get(player);
        Account next = current.withWon(won);
        if (!next.equals(current)) {
            player.setData(ModAttachments.ACCOUNT.get(), next);
        }
    }

    public static void deposit(ServerPlayer player, long amount) {
        setWon(player, AccountRules.add(won(player), amount));
    }

    public static void withdraw(ServerPlayer player, long amount) {
        setWon(player, AccountRules.subtract(won(player), amount));
    }
}
