package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.smelting.MeltingRecipe;
import com.orijua.overgearedmod.feature.smithing.ForgingRecipe;
import com.orijua.overgearedmod.feature.world.SatisfyShapedRecipe;
import com.orijua.overgearedmod.feature.world.SatisfyShapelessRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A recipe needs two registrations: its type (to look recipes up) and its serializer (to read the JSON). */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Overgearedmod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Overgearedmod.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ForgingRecipe>> FORGING =
            RECIPE_TYPES.register("forging", () -> RecipeType.simple(Overgearedmod.id("forging")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MeltingRecipe>> MELTING =
            RECIPE_TYPES.register("melting", () -> RecipeType.simple(Overgearedmod.id("melting")));

    public static final DeferredHolder<RecipeSerializer<?>, ForgingRecipe.Serializer> FORGING_SERIALIZER =
            RECIPE_SERIALIZERS.register("forging", ForgingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, MeltingRecipe.Serializer> MELTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("melting", MeltingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, SatisfyShapedRecipe.Serializer> SATISFY_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("satisfy_shaped", SatisfyShapedRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, SatisfyShapelessRecipe.Serializer> SATISFY_SHAPELESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("satisfy_shapeless", SatisfyShapelessRecipe.Serializer::new);

    private ModRecipes() {
    }
}
