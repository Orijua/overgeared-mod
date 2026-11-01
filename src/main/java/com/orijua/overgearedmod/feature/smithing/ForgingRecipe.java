package com.orijua.overgearedmod.feature.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.equipment.EquipRequirements;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A smithing anvil recipe, loaded from data/<namespace>/recipe/*.json with "type": "overgearedmod:forging".
 * Example: data/overgearedmod/recipe/forging/jaffa_sword.json. Special recipes add {@link ForgingExtras}
 * (a class, a rating range, fixed options), e.g. forging/fracaso_sword.json.
 */
public record ForgingRecipe(Ingredient material, int materialCount, Ingredient handle, ItemStack result, int smithingXp,
                            Optional<EquipRequirements> requirements, ForgingExtras extras)
        implements Recipe<ForgingRecipeInput> {

    private static final StreamCodec<ByteBuf, Optional<EquipRequirements>> REQUIREMENTS_STREAM_CODEC =
            ByteBufCodecs.optional(EquipRequirements.STREAM_CODEC);

    public static final MapCodec<ForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(ForgingRecipe::material),
            Codec.intRange(1, 64).optionalFieldOf("material_count", 1).forGetter(ForgingRecipe::materialCount),
            Ingredient.CODEC.optionalFieldOf("handle", Ingredient.EMPTY).forGetter(ForgingRecipe::handle),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
            Codec.intRange(0, 10_000).optionalFieldOf("smithing_xp", 10).forGetter(ForgingRecipe::smithingXp),
            EquipRequirements.CODEC.optionalFieldOf("requirements").forGetter(ForgingRecipe::requirements),
            ForgingExtras.MAP_CODEC.forGetter(ForgingRecipe::extras)
    ).apply(instance, ForgingRecipe::new));

    // Written by hand: StreamCodec.composite takes at most six fields.
    public static final StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material());
                ByteBufCodecs.VAR_INT.encode(buf, recipe.materialCount());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.handle());
                ItemStack.STREAM_CODEC.encode(buf, recipe.result());
                ByteBufCodecs.VAR_INT.encode(buf, recipe.smithingXp());
                REQUIREMENTS_STREAM_CODEC.encode(buf, recipe.requirements());
                ForgingExtras.STREAM_CODEC.encode(buf, recipe.extras());
            },
            buf -> new ForgingRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    REQUIREMENTS_STREAM_CODEC.decode(buf),
                    ForgingExtras.STREAM_CODEC.decode(buf)));

    @Override
    public boolean matches(ForgingRecipeInput input, Level level) {
        // Ingredient.EMPTY only matches an empty slot, so a recipe without handle needs that slot empty.
        return material.test(input.material())
                && input.material().getCount() >= materialCount
                && handle.test(input.handle());
    }

    @Override
    public ItemStack assemble(ForgingRecipeInput input, HolderLookup.Provider registries) {
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
        return NonNullList.of(Ingredient.EMPTY, material, handle);
    }

    /** Keeps it out of the vanilla recipe book, which has no category for it (avoids log spam). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FORGING.get();
    }

    public static final class Serializer implements RecipeSerializer<ForgingRecipe> {
        @Override
        public MapCodec<ForgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
