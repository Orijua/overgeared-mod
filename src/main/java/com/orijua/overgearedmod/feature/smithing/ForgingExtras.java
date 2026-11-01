package com.orijua.overgearedmod.feature.smithing;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.option.FixedOption;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The optional fields of a special forging recipe, such as a blueprint's (Fracaso): who may forge it
 * ({@code required_class}), the ratings it can come out with ({@code min_rating}/{@code max_rating}) and
 * {@code fixed_options}, given instead of random ones. They sit flat in the recipe JSON next to the others.
 */
public record ForgingExtras(Optional<ResourceLocation> requiredClass, ItemRating minRating, ItemRating maxRating,
                            List<FixedOption> fixedOptions) {
    public static final ForgingExtras NONE = new ForgingExtras(Optional.empty(), ItemRating.NORMAL, ItemRating.MYTH, List.of());

    public static final MapCodec<ForgingExtras> MAP_CODEC = RecordCodecBuilder.<ForgingExtras>mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("required_class").forGetter(ForgingExtras::requiredClass),
            ItemRating.CODEC.optionalFieldOf("min_rating", ItemRating.NORMAL).forGetter(ForgingExtras::minRating),
            ItemRating.CODEC.optionalFieldOf("max_rating", ItemRating.MYTH).forGetter(ForgingExtras::maxRating),
            FixedOption.CODEC.listOf().optionalFieldOf("fixed_options", List.of()).forGetter(ForgingExtras::fixedOptions)
    ).apply(instance, ForgingExtras::new)).validate(extras -> extras.minRating().compareTo(extras.maxRating()) <= 0
            ? DataResult.success(extras)
            : DataResult.error(() -> "min_rating is above max_rating"));

    public static final StreamCodec<ByteBuf, ForgingExtras> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ForgingExtras::requiredClass,
            ItemRating.STREAM_CODEC, ForgingExtras::minRating,
            ItemRating.STREAM_CODEC, ForgingExtras::maxRating,
            FixedOption.LIST_STREAM_CODEC, ForgingExtras::fixedOptions,
            ForgingExtras::new);

    /** Whether a smith of class {@code playerClass} (empty: no class) may forge this. */
    public boolean allows(Optional<ResourceLocation> playerClass) {
        return requiredClass.isEmpty() || requiredClass.equals(playerClass);
    }
}
