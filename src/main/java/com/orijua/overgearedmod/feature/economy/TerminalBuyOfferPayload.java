package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Client → server, from the terminal: buy the piece at {@code position} of today's offer, having seen {@code seen} for
 * {@code seenPrice} Won. The server regenerates the offer and checks all three; an empty piece is just stale.
 */
public record TerminalBuyOfferPayload(int containerId, int position, ItemStack seen, long seenPrice) implements CustomPacketPayload {
    public static final Type<TerminalBuyOfferPayload> TYPE = new Type<>(Overgearedmod.id("terminal_buy_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalBuyOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalBuyOfferPayload::containerId,
            ByteBufCodecs.VAR_INT, TerminalBuyOfferPayload::position,
            ItemStack.OPTIONAL_STREAM_CODEC, TerminalBuyOfferPayload::seen,
            ByteBufCodecs.VAR_LONG, TerminalBuyOfferPayload::seenPrice,
            TerminalBuyOfferPayload::new);

    @Override
    public Type<TerminalBuyOfferPayload> type() {
        return TYPE;
    }
}
