package com.orijua.overgearedmod.feature.economy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** One piece of the daily offer: its position, the piece (rated, as it would be delivered) and its price in Won. */
public record OfferEntry(int position, ItemStack stack, long price) {
    public static final StreamCodec<RegistryFriendlyByteBuf, OfferEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OfferEntry::position,
            ItemStack.STREAM_CODEC, OfferEntry::stack,
            ByteBufCodecs.VAR_LONG, OfferEntry::price,
            OfferEntry::new);
}
