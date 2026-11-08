package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The player's money in Satisfy: the balance in Won and the debt (the debt has no logic yet). Never negative. */
public record Account(long won, long debt) {
    public static final Account EMPTY = new Account(0L, 0L);

    public static final Codec<Account> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("won", 0L).forGetter(Account::won),
            Codec.LONG.optionalFieldOf("debt", 0L).forGetter(Account::debt)
    ).apply(instance, Account::new));

    public static final StreamCodec<ByteBuf, Account> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public Account {
        won = Math.max(0L, won);
        debt = Math.max(0L, debt);
    }

    public Account withWon(long newWon) {
        return new Account(newWon, debt);
    }
}
