package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.equipment.ItemRating;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * An option a recipe always gives, with a set value, instead of a random roll, e.g.
 * {"option": "overgearedmod:fracaso_bisection", "value": 1, "min_rating": "legendary"}.
 * {@code min_rating} keeps it for forges that come out at least that good.
 */
public record FixedOption(ResourceLocation option, float value, ItemRating minRating) {
    public static final Codec<FixedOption> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("option").forGetter(FixedOption::option),
            Codec.FLOAT.fieldOf("value").forGetter(FixedOption::value),
            ItemRating.CODEC.optionalFieldOf("min_rating", ItemRating.NORMAL).forGetter(FixedOption::minRating)
    ).apply(instance, FixedOption::new));

    public static final StreamCodec<ByteBuf, List<FixedOption>> LIST_STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, FixedOption::option,
            ByteBufCodecs.FLOAT, FixedOption::value,
            ItemRating.STREAM_CODEC, FixedOption::minRating,
            FixedOption::new).apply(ByteBufCodecs.list());

    public FixedOption(ResourceLocation option, float value) {
        this(option, value, ItemRating.NORMAL);
    }

    /** The options of {@code fixed} a forge rated {@code rating} gets, ready to stamp on the item. */
    public static List<RolledOption> forRating(List<FixedOption> fixed, ItemRating rating) {
        return fixed.stream()
                .filter(option -> rating.compareTo(option.minRating()) >= 0)
                .map(option -> new RolledOption(option.option(), option.value()))
                .toList();
    }
}
