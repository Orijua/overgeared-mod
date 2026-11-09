package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.equipment.EquipRequirements;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.option.FixedOption;
import com.orijua.overgearedmod.feature.progression.StatType;
import com.orijua.overgearedmod.feature.smelting.MeltingRecipe;
import com.orijua.overgearedmod.feature.smelting.SmelterRules;
import com.orijua.overgearedmod.feature.smithing.ForgingExtras;
import com.orijua.overgearedmod.feature.smithing.ForgingRecipe;
import com.orijua.overgearedmod.feature.world.SatisfyShapedRecipe;
import com.orijua.overgearedmod.feature.world.SatisfyShapelessRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Crafting, furnace, smelter (melting) and smithing-anvil (forging) recipes. */
final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        smelting(output);
        melting(output);
        forging(output);
    }

    /**
     * Wraps the output so the crafting recipes saved through it become Satisfy-only: the builders still make the
     * recipe and its unlock advancement, and the recipe is swapped for its Satisfy-only twin on the way out.
     */
    private static RecipeOutput satisfyOnly(RecipeOutput output) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
                Recipe<?> swapped = recipe instanceof ShapedRecipe shaped ? new SatisfyShapedRecipe(shaped)
                        : recipe instanceof ShapelessRecipe shapeless ? new SatisfyShapelessRecipe(shapeless) : recipe;
                output.accept(id, swapped, advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }
        };
    }

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMELTER.get())
                .pattern("BBB").pattern("BFB").pattern("iii")
                .define('B', Items.BRICKS).define('F', Items.BLAST_FURNACE).define('i', Items.IRON_INGOT)
                .unlockedBy("has_blast_furnace", has(Items.BLAST_FURNACE))
                .save(satisfyOnly(output), Overgearedmod.id("smelter"));
        // The basic capsule (own design; tiers come later): glass, a bed to lie on, iron and redstone for the circuits.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPSULE.get())
                .pattern("GGG").pattern("IBI").pattern("IRI")
                .define('G', Items.GLASS).define('B', ItemTags.BEDS).define('I', Items.IRON_INGOT).define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, Overgearedmod.id("capsule"));
        // The account terminal and the phone (own design): glass panes for the screen, iron and redstone for the circuits.
        // Not satisfyOnly: they are crafted in the real world, where they work.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ACCOUNT_TERMINAL.get())
                .pattern("GGG").pattern("IRI").pattern("III")
                .define('G', Items.GLASS_PANE).define('I', Items.IRON_INGOT).define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, Overgearedmod.id("account_terminal"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PHONE.get())
                .pattern("G").pattern("R").pattern("I")
                .define('G', Items.GLASS_PANE).define('I', Items.IRON_INGOT).define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, Overgearedmod.id("phone"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMITHING_ANVIL.get())
                .pattern("III").pattern(" i ").pattern("iii")
                .define('I', Items.IRON_BLOCK).define('i', ModItems.MITHRIL_INGOT.get())
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(satisfyOnly(output), Overgearedmod.id("smithing_anvil"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.WORKSHOP_LEDGER.get())
                .requires(Items.BOOK).requires(Items.IRON_INGOT)
                .unlockedBy("has_apprentice_contract", has(ModItems.APPRENTICE_CONTRACT.get()))
                .save(satisfyOnly(output), Overgearedmod.id("workshop_ledger"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ENHANCEMENT_STONE.get(), 2)
                .requires(ModItems.MITHRIL_INGOT.get()).requires(Items.AMETHYST_SHARD)
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(satisfyOnly(output), Overgearedmod.id("enhancement_stone"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RESPEC_SCROLL.get())
                .requires(Items.PAPER).requires(Items.LAPIS_LAZULI).requires(ModItems.MITHRIL_INGOT.get())
                .unlockedBy("has_mithril_ingot", has(ModItems.MITHRIL_INGOT.get()))
                .save(satisfyOnly(output), Overgearedmod.id("respec_scroll"));
    }

    /**
     * Furnace recipes. Mithril and blue orichalcum have none: they are advanced metals and only melt in the smelter
     * ({@link #melting}).
     */
    private void smelting(RecipeOutput output) {
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
        forge(output, "iron_smithing_hammer", Items.IRON_INGOT, 4, Items.LEATHER, ModItems.IRON_SMITHING_HAMMER.get(), 15, Optional.empty());
        forge(output, "jaffa_smithing_hammer", ModItems.JAFFA_INGOT.get(), 4, Items.LEATHER, ModItems.JAFFA_SMITHING_HAMMER.get(), 25, Optional.empty());
        forge(output, "mithril_smithing_hammer", ModItems.MITHRIL_INGOT.get(), 4, Items.LEATHER, ModItems.MITHRIL_SMITHING_HAMMER.get(), 45,
                Optional.empty());
        forge(output, "jaffa_sword", ModItems.JAFFA_INGOT.get(), 2, Items.STICK, ModItems.JAFFA_SWORD.get(), 15,
                Optional.of(new EquipRequirements(2, Map.of(StatType.STRENGTH, 2))));
        forge(output, "grid_legendary_sword", Items.DIAMOND, 4, Items.BLAZE_ROD, ModItems.GRID_LEGENDARY_SWORD.get(), 60,
                Optional.of(new EquipRequirements(10, Map.of(StatType.STRENGTH, 8))));
        // Fracaso (canon cap. 11): 15 blue orichalcum on its blueprint, only for the Legendary Smith Successor. It comes out
        // Unique or Legendary with the canon's fixed effects, and asks for what nobody has (level 300, 5000 STR).
        forge(output, "fracaso_sword", ModItems.BLUE_ORICHALCUM_INGOT.get(), 15, ModItems.FRACASO_BLUEPRINT.get(),
                ModItems.FRACASO_SWORD.get(), 500, Optional.of(new EquipRequirements(300, Map.of(StatType.STRENGTH, 5000))),
                new ForgingExtras(Optional.of(Overgearedmod.id("legendary_smith_successor")), ItemRating.UNIQUE, ItemRating.LEGENDARY,
                        List.of(new FixedOption(Overgearedmod.id("fracaso_agility"), 30, ItemRating.NORMAL, ItemRating.UNIQUE),
                                new FixedOption(Overgearedmod.id("fracaso_agility"), 50, ItemRating.LEGENDARY),
                                new FixedOption(Overgearedmod.id("fracaso_block"), 0.15f),
                                new FixedOption(Overgearedmod.id("fracaso_darkness"), 0.2f),
                                new FixedOption(Overgearedmod.id("fracaso_fear"), 20),
                                new FixedOption(Overgearedmod.id("fracaso_bisection"), 1, ItemRating.LEGENDARY))));
    }

    /**
     * Smelter recipes. Iron and jaffa also melt in the furnace (the smelter is better: a chance of an extra ingot);
     * mithril and blue orichalcum only here. Coal reaches about 1180 degrees, coal blocks 1300, blaze rods 1500 and lava 1600.
     * Iron (1150-1250) and jaffa (950-1000) follow the canon's temperatures; the rest keep the default zone width.
     */
    private void melting(RecipeOutput output) {
        melt(output, "iron_ingot", Items.IRON_INGOT, 1150, 1250, 0, 160, 3, Items.RAW_IRON, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE);
        melt(output, "jaffa_ingot", ModItems.JAFFA_INGOT.get(), 950, 1000, 0, 160, 4,
                ModItems.RAW_JAFFA.get(), ModItems.JAFFA_ORE.get(), ModItems.DEEPSLATE_JAFFA_ORE.get());
        melt(output, "mithril_ingot", ModItems.MITHRIL_INGOT.get(), 1200, 3, 240, 8,
                ModItems.RAW_MITHRIL.get(), ModItems.MITHRIL_ORE.get(), ModItems.DEEPSLATE_MITHRIL_ORE.get());
        melt(output, "blue_orichalcum_ingot", ModItems.BLUE_ORICHALCUM_INGOT.get(), 1450, 8, 400, 20,
                ModItems.RAW_BLUE_ORICHALCUM.get(), ModItems.BLUE_ORICHALCUM_ORE.get(), ModItems.DEEPSLATE_BLUE_ORICHALCUM_ORE.get());
    }

    /** A smelter recipe per source at data/overgearedmod/recipe/melting/&lt;result&gt;_from_&lt;source&gt;.json. */
    private static void melt(RecipeOutput output, String result, ItemLike ingot, int minTemperature, int smithingLevel, int time,
                             int smithingXp, ItemLike... sources) {
        melt(output, result, ingot, minTemperature, minTemperature + SmelterRules.ZONE_WIDTH, smithingLevel, time, smithingXp, sources);
    }

    private static void melt(RecipeOutput output, String result, ItemLike ingot, int minTemperature, int maxTemperature,
                             int smithingLevel, int time, int smithingXp, ItemLike... sources) {
        for (ItemLike source : sources) {
            MeltingRecipe recipe = new MeltingRecipe(Ingredient.of(source), new ItemStack(ingot), minTemperature, maxTemperature,
                    smithingLevel, time, smithingXp);
            output.accept(Overgearedmod.id("melting/" + result + "_from_" + getItemName(source)), recipe, null);
        }
    }

    /** A smithing anvil recipe at data/overgearedmod/recipe/forging/<name>.json. {@code handle} may be null. */
    private static void forge(RecipeOutput output, String name, ItemLike material, int materialCount, ItemLike handle,
                              ItemLike result, int smithingXp, Optional<EquipRequirements> requirements) {
        forge(output, name, material, materialCount, handle, result, smithingXp, requirements, ForgingExtras.NONE);
    }

    private static void forge(RecipeOutput output, String name, ItemLike material, int materialCount, ItemLike handle,
                              ItemLike result, int smithingXp, Optional<EquipRequirements> requirements, ForgingExtras extras) {
        ForgingRecipe recipe = new ForgingRecipe(Ingredient.of(material), materialCount,
                handle == null ? Ingredient.EMPTY : Ingredient.of(handle), new ItemStack(result), smithingXp, requirements, extras);
        output.accept(Overgearedmod.id("forging/" + name), recipe, null);
    }
}
