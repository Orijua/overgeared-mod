package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Crafting, smelting and smithing-anvil (forging) recipes. */
final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        smelting(output);
        forging(output);
    }

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMITHING_ANVIL.get())
                .pattern("III").pattern(" i ").pattern("iii")
                .define('I', Items.IRON_BLOCK).define('i', ModItems.MITHRIL_INGOT.get())
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(output, Overgearedmod.id("smithing_anvil"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ENHANCEMENT_STONE.get(), 2)
                .requires(ModItems.MITHRIL_INGOT.get()).requires(Items.AMETHYST_SHARD)
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(output, Overgearedmod.id("enhancement_stone"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RESPEC_SCROLL.get())
                .requires(Items.PAPER).requires(Items.LAPIS_LAZULI).requires(ModItems.MITHRIL_INGOT.get())
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(output, Overgearedmod.id("respec_scroll"));
    }

    private void smelting(RecipeOutput output) {
        ingot(output, ModItems.MITHRIL_INGOT.get(), 0.8f, ModItems.RAW_MITHRIL.get(), ModItems.MITHRIL_ORE.get(), ModItems.DEEPSLATE_MITHRIL_ORE.get());
        ingot(output, ModItems.JAFFA_INGOT.get(), 0.7f, ModItems.RAW_JAFFA.get(), ModItems.JAFFA_ORE.get(), ModItems.DEEPSLATE_JAFFA_ORE.get());
    }

    /** Smelting (10 s) and blasting (5 s) recipes from each source into the ingot, like vanilla's iron. */
    private static void ingot(RecipeOutput output, ItemLike ingot, float xp, ItemLike... sources) {
        String ingotName = getItemName(ingot);
        for (ItemLike source : sources) {
            String name = ingotName + "_from_%s_" + getItemName(source);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(source), RecipeCategory.MISC, ingot, xp, 200)
                    .group(ingotName).unlockedBy(getHasName(source), has(source))
                    .save(output, Overgearedmod.id(name.formatted("smelting")));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(source), RecipeCategory.MISC, ingot, xp, 100)
                    .group(ingotName).unlockedBy(getHasName(source), has(source))
                    .save(output, Overgearedmod.id(name.formatted("blasting")));
        }
    }

    private void forging(RecipeOutput output) {
        forge(output, "iron_sword", Items.IRON_INGOT, 2, Items.STICK, Items.IRON_SWORD, 10, Optional.empty());
        forge(output, "iron_chestplate", Items.IRON_INGOT, 8, null, Items.IRON_CHESTPLATE, 20, Optional.empty());
        forge(output, "jaffa_sword", ModItems.JAFFA_INGOT.get(), 2, Items.STICK, ModItems.JAFFA_SWORD.get(), 15,
                Optional.of(new EquipRequirements(2, Map.of(StatType.STRENGTH, 2))));
        forge(output, "grid_legendary_sword", Items.DIAMOND, 4, Items.BLAZE_ROD, ModItems.GRID_LEGENDARY_SWORD.get(), 60,
                Optional.of(new EquipRequirements(10, Map.of(StatType.STRENGTH, 8))));
    }

    /** A smithing anvil recipe at data/overgearedmod/recipe/forging/<name>.json. {@code handle} may be null. */
    private static void forge(RecipeOutput output, String name, ItemLike material, int materialCount, ItemLike handle,
                              ItemLike result, int smithingXp, Optional<EquipRequirements> requirements) {
        ForgingRecipe recipe = new ForgingRecipe(Ingredient.of(material), materialCount,
                handle == null ? Ingredient.EMPTY : Ingredient.of(handle), new ItemStack(result), smithingXp, requirements);
        output.accept(Overgearedmod.id("forging/" + name), recipe, null);
    }
}
