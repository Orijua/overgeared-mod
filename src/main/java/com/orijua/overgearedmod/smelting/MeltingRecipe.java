package com.orijua.overgearedmod.smelting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A smelter recipe, from data/&lt;namespace&gt;/recipe/*.json with "type": "overgearedmod:melting": one {@code ingredient}
 * melts into {@code result} once the smelter is at {@code min_temperature} or hotter, taking {@code time} ticks when
 * left alone. Starting it needs {@code smithing_level}; each ingot gives {@code smithing_xp}.
 */
public record MeltingRecipe(Ingredient ingredient, ItemStack result, int minTemperature, int smithingLevel, int time, int smithingXp)
        implements Recipe<SingleRecipeInput> {

    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MeltingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
            Codec.intRange(SmelterRules.AMBIENT, SmelterRules.MAX_TEMPERATURE - SmelterRules.ZONE_WIDTH)
                    .fieldOf("min_temperature").forGetter(MeltingRecipe::minTemperature),
            Codec.intRange(0, 1000).optionalFieldOf("smithing_level", 0).forGetter(MeltingRecipe::smithingLevel),
            Codec.intRange(1, 20 * 600).optionalFieldOf("time", 200).forGetter(MeltingRecipe::time),
            Codec.intRange(0, 10_000).optionalFieldOf("smithing_xp", 5).forGetter(MeltingRecipe::smithingXp)
    ).apply(instance, MeltingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MeltingRecipe::ingredient,
            ItemStack.STREAM_CODEC, MeltingRecipe::result,
            ByteBufCodecs.VAR_INT, MeltingRecipe::minTemperature,
            ByteBufCodecs.VAR_INT, MeltingRecipe::smithingLevel,
            ByteBufCodecs.VAR_INT, MeltingRecipe::time,
            ByteBufCodecs.VAR_INT, MeltingRecipe::smithingXp,
            MeltingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** Keeps it out of the vanilla recipe book, which has no category for it. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MELTING.get();
    }

    public static final class Serializer implements RecipeSerializer<MeltingRecipe> {
        @Override
        public MapCodec<MeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
