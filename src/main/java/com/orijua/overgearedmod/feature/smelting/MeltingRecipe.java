package com.orijua.overgearedmod.feature.smelting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.core.registry.ModRecipes;
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

import java.util.Optional;

/**
 * A smelter recipe, from data/&lt;namespace&gt;/recipe/*.json with "type": "overgearedmod:melting": one {@code ingredient}
 * melts into {@code result} once the smelter is at {@code min_temperature} or hotter, taking {@code time} ticks when
 * left alone. The optional {@code max_temperature} (default min + {@link SmelterRules#ZONE_WIDTH}) is the top of the
 * bellows' good zone. Starting it needs {@code smithing_level}; each ingot gives {@code smithing_xp}.
 */
public record MeltingRecipe(Ingredient ingredient, ItemStack result, int minTemperature, int maxTemperature, int smithingLevel,
                            int time, int smithingXp) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MeltingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
            Codec.intRange(SmelterRules.AMBIENT, SmelterRules.MAX_TEMPERATURE - 1)
                    .fieldOf("min_temperature").forGetter(MeltingRecipe::minTemperature),
            // Optional: the top of the bellows zone; by default min_temperature + ZONE_WIDTH (not written when it is the default).
            Codec.intRange(SmelterRules.AMBIENT + 1, SmelterRules.MAX_TEMPERATURE)
                    .optionalFieldOf("max_temperature").forGetter(r -> r.maxTemperature == r.minTemperature + SmelterRules.ZONE_WIDTH
                            ? Optional.empty() : Optional.of(r.maxTemperature)),
            Codec.intRange(0, 1000).optionalFieldOf("smithing_level", 0).forGetter(MeltingRecipe::smithingLevel),
            Codec.intRange(1, 20 * 600).optionalFieldOf("time", 200).forGetter(MeltingRecipe::time),
            Codec.intRange(0, 10_000).optionalFieldOf("smithing_xp", 5).forGetter(MeltingRecipe::smithingXp)
    ).apply(instance, (ingredient, result, min, max, level, time, xp) ->
            new MeltingRecipe(ingredient, result, min, Math.max(min + 1, max.orElse(min + SmelterRules.ZONE_WIDTH)), level, time, xp)));

    // Seven fields: more than StreamCodec.composite takes, so it is written by hand.
    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient);
                ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                buf.writeVarInt(recipe.minTemperature);
                buf.writeVarInt(recipe.maxTemperature);
                buf.writeVarInt(recipe.smithingLevel);
                buf.writeVarInt(recipe.time);
                buf.writeVarInt(recipe.smithingXp);
            },
            buf -> new MeltingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), ItemStack.STREAM_CODEC.decode(buf),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    /** A recipe with the default zone width above its melting temperature. */
    public MeltingRecipe(Ingredient ingredient, ItemStack result, int minTemperature, int smithingLevel, int time, int smithingXp) {
        this(ingredient, result, minTemperature, minTemperature + SmelterRules.ZONE_WIDTH, smithingLevel, time, smithingXp);
    }

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
