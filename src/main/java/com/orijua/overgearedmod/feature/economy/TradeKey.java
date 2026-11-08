package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * What the trade saturation counts: a category and a rating ("sword/epic"). In the future a shared market (MMO) would
 * price by item and rating instead; that is not implemented.
 */
public record TradeKey(TradeCategory category, ItemRating rating) {
    /** Saved as text, "category/rating". An unknown part fails the decoding. */
    public static final Codec<TradeKey> CODEC = Codec.STRING.comapFlatMap(
            text -> parse(text).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown trade key: " + text)),
            TradeKey::asString);

    public static final StreamCodec<ByteBuf, TradeKey> STREAM_CODEC = StreamCodec.composite(
            TradeCategory.STREAM_CODEC, TradeKey::category,
            ItemRating.STREAM_CODEC, TradeKey::rating,
            TradeKey::new);

    public String asString() {
        return category.getSerializedName() + "/" + rating.getSerializedName();
    }

    public static Optional<TradeKey> parse(String text) {
        int slash = text.indexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }
        TradeCategory category = null;
        for (TradeCategory candidate : TradeCategory.values()) {
            if (candidate.getSerializedName().equals(text.substring(0, slash))) {
                category = candidate;
            }
        }
        ItemRating rating = null;
        for (ItemRating candidate : ItemRating.values()) {
            if (candidate.getSerializedName().equals(text.substring(slash + 1))) {
                rating = candidate;
            }
        }
        return category == null || rating == null ? Optional.empty() : Optional.of(new TradeKey(category, rating));
    }
}
