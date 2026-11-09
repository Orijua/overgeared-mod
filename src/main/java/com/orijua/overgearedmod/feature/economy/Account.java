package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The player's money in Satisfy: the balance in Won, the debt of the capsule and where that debt is. Never negative.
 * The state keeps itself coherent: a debt above 0 is always ACTIVE, and an ACTIVE one that reaches 0 is SETTLED.
 */
public record Account(long won, long debt, DebtState debtState) {
    public static final Account EMPTY = new Account(0L, 0L, DebtState.UNASSIGNED);

    public static final Codec<Account> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("won", 0L).forGetter(Account::won),
            Codec.LONG.optionalFieldOf("debt", 0L).forGetter(Account::debt),
            DebtState.CODEC.optionalFieldOf("debt_state", DebtState.UNASSIGNED).forGetter(Account::debtState)
    ).apply(instance, Account::new));

    public static final StreamCodec<ByteBuf, Account> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public Account {
        won = Math.max(0L, won);
        debt = Math.max(0L, debt);
        if (debtState == null) {
            debtState = DebtState.UNASSIGNED;
        }
        if (debt > 0L) {
            debtState = DebtState.ACTIVE;
        } else if (debtState == DebtState.ACTIVE) {
            debtState = DebtState.SETTLED;
        }
    }

    /** An account with a balance and a debt but no recorded state: the state follows from the debt. */
    public Account(long won, long debt) {
        this(won, debt, DebtState.UNASSIGNED);
    }

    public Account withWon(long newWon) {
        return new Account(newWon, debt, debtState);
    }

    public Account withDebt(long newDebt) {
        return new Account(won, newDebt, debtState);
    }
}
