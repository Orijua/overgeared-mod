package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Client → server, from the Trade tab: sell or export the piece in an inventory slot. The server checks everything, including that
 * the piece and price are the ones the player saw ({@code expected}, {@code expectedPrice}); an empty piece is just stale. */
public record TradeSellPayload(int slot, TradeMode mode, ItemStack expected, long expectedPrice) implements CustomPacketPayload {
    public static final Type<TradeSellPayload> TYPE = new Type<>(Overgearedmod.id("trade_sell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradeSellPayload::slot,
            TradeMode.STREAM_CODEC, TradeSellPayload::mode,
            ItemStack.OPTIONAL_STREAM_CODEC, TradeSellPayload::expected,
            ByteBufCodecs.VAR_LONG, TradeSellPayload::expectedPrice,
            TradeSellPayload::new);

    @Override
    public Type<TradeSellPayload> type() {
        return TYPE;
    }
}
