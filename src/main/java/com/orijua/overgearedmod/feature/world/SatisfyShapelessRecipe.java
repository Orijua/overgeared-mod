package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * A shapeless crafting recipe that only works in Satisfy: in the real world the grid simply does not match. Same JSON as
 * {@code minecraft:crafting_shapeless}, with {@code "type": "overgearedmod:satisfy_shapeless"}.
 */
public final class SatisfyShapelessRecipe extends ShapelessRecipe {
    public SatisfyShapelessRecipe(ShapelessRecipe base) {
        // The result does not depend on the registries (ShapelessRecipe ignores the argument), hence the null.
        super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return Satisfy.is(level) && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SATISFY_SHAPELESS_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<SatisfyShapelessRecipe> {
        private static final MapCodec<SatisfyShapelessRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
                .xmap(SatisfyShapelessRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, SatisfyShapelessRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(SatisfyShapelessRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<SatisfyShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SatisfyShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
