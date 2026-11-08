package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server, from the Trade tab: sell or export the piece in an inventory slot. The server checks everything. */
public record TradeSellPayload(int slot, TradeMode mode) implements CustomPacketPayload {
    public static final Type<TradeSellPayload> TYPE = new Type<>(Overgearedmod.id("trade_sell"));
    public static final StreamCodec<ByteBuf, TradeSellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradeSellPayload::slot,
            TradeMode.STREAM_CODEC, TradeSellPayload::mode,
            TradeSellPayload::new);

    @Override
    public Type<TradeSellPayload> type() {
        return TYPE;
    }
}
