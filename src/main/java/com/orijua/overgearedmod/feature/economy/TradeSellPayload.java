package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client → server, from the Trade tab: sell the pieces of several inventory slots. The server checks everything, including that
 * each piece and price are the ones the player saw ({@code expected}, {@code expectedPrice}); an empty piece is just stale. */
public record TradeSellPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<TradeSellPayload> TYPE = new Type<>(Overgearedmod.id("trade_sell"));

    /** One piece of the order: its slot, the piece the player saw and the price he saw for it (in the order of the batch). */
    public record Entry(int slot, ItemStack expected, long expectedPrice) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::expected,
                ByteBufCodecs.VAR_LONG, Entry::expectedPrice,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSellPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(TradeRules.MAX_BATCH)).map(TradeSellPayload::new, TradeSellPayload::entries);

    @Override
    public Type<TradeSellPayload> type() {
        return TYPE;
    }
}
