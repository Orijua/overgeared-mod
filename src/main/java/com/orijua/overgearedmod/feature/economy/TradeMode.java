package com.orijua.overgearedmod.feature.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/** How a piece of equipment leaves the player's inventory: sold for Satisfy emeralds or exported for Won. */
public enum TradeMode {
    SELL,
    EXPORT;

    // CLAMP: a bogus id can never index outside the enum.
    public static final StreamCodec<ByteBuf, TradeMode> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(TradeMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP), TradeMode::ordinal);
}
