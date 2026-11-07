package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped crafting recipe that only works in Satisfy: in the real world the grid simply does not match, so crafting
 * fails in silence. Same JSON as {@code minecraft:crafting_shaped}, with {@code "type": "overgearedmod:satisfy_shaped"}.
 */
public final class SatisfyShapedRecipe extends ShapedRecipe {
    public SatisfyShapedRecipe(ShapedRecipe base) {
        // The result does not depend on the registries (ShapedRecipe ignores the argument), hence the null.
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(null), base.showNotification());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return Satisfy.is(level) && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SATISFY_SHAPED_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<SatisfyShapedRecipe> {
        // Reuses vanilla's codecs, so the JSON and the packet are exactly the shaped recipe's.
        private static final MapCodec<SatisfyShapedRecipe> CODEC = RecipeSerializer.SHAPED_RECIPE.codec()
                .xmap(SatisfyShapedRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, SatisfyShapedRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPED_RECIPE.streamCodec().map(SatisfyShapedRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<SatisfyShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SatisfyShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
