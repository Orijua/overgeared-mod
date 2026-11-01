package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.smithing.ForgingRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialTests {
    private MaterialTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void mithrilOreDropsRawMithril(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        List<ItemStack> drops = Block.getDrops(ModBlocks.MITHRIL_ORE.get().defaultBlockState(), helper.getLevel(), pos,
                null, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(ModItems.RAW_MITHRIL.get())), "ore should drop raw mithril: " + drops);
        helper.assertTrue(ModBlocks.MITHRIL_ORE.get().defaultBlockState().requiresCorrectToolForDrops(), "needs a pickaxe");
        helper.succeed();
    }

    /** Mithril is an advanced metal: it melts into an ingot in the smelter only (SmelterTests checks the furnace refuses it). */
    @GameTest(template = TestSupport.EMPTY)
    public static void rawMithrilMeltsIntoIngotInTheSmelter(GameTestHelper helper) {
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(ModRecipes.MELTING.get(), new SingleRecipeInput(new ItemStack(ModItems.RAW_MITHRIL.get())), helper.getLevel())
                .map(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ModItems.MITHRIL_INGOT.get()), "raw mithril should melt into an ingot, got " + result);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void mithrilOreGenerates(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .containsKey(Overgearedmod.id("ore_mithril")), "the placed feature should load from the datapack");
        helper.succeed();
    }

    /** Jaffa is iron tier: a stone pickaxe is enough, and both ores drop raw jaffa. */
    @GameTest(template = TestSupport.EMPTY)
    public static void jaffaOreDropsRawJaffaWithAStonePickaxe(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block ore : List.of(ModBlocks.JAFFA_ORE.get(), ModBlocks.DEEPSLATE_JAFFA_ORE.get())) {
            List<ItemStack> drops = Block.getDrops(ore.defaultBlockState(), helper.getLevel(), pos, null, null,
                    new ItemStack(Items.STONE_PICKAXE));
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(ModItems.RAW_JAFFA.get())), ore + " should drop raw jaffa: " + drops);
            helper.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()), "stone pickaxe tier");
            helper.assertFalse(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()), "not wood");
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void jaffaSmeltsIntoIngotAndForgesTheJaffaSword(GameTestHelper helper) {
        for (Item source : List.of(ModItems.RAW_JAFFA.get(), ModItems.JAFFA_ORE.get(), ModItems.DEEPSLATE_JAFFA_ORE.get())) {
            ItemStack result = helper.getLevel().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(source)), helper.getLevel())
                    .map(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()))
                    .orElse(ItemStack.EMPTY);
            helper.assertTrue(result.is(ModItems.JAFFA_INGOT.get()), source + " should smelt into a jaffa ingot, got " + result);
        }
        ItemStack sword = helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(),
                        new ForgingRecipeInput(new ItemStack(ModItems.JAFFA_INGOT.get(), 2), new ItemStack(Items.STICK)), helper.getLevel())
                .map(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(sword.is(ModItems.JAFFA_SWORD.get()), "2 jaffa ingots + a stick forge the Jaffa sword, got " + sword);
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(),
                        new ForgingRecipeInput(new ItemStack(ModItems.MITHRIL_INGOT.get(), 2), new ItemStack(Items.STICK)), helper.getLevel())
                .isEmpty(), "mithril no longer makes it");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void jaffaOreGenerates(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE).containsKey(Overgearedmod.id("ore_jaffa")),
                "the placed feature should load from the datapack");
        helper.assertTrue(registries.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(Overgearedmod.id("add_ore_jaffa")),
                "and be added to the overworld's biomes");
        helper.succeed();
    }

    /** The hardest mineral (canon): only a diamond pickaxe breaks it for its raw orichalcum. */
    @GameTest(template = TestSupport.EMPTY)
    public static void blueOrichalcumNeedsADiamondPickaxe(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block ore : List.of(ModBlocks.BLUE_ORICHALCUM_ORE.get(), ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE.get())) {
            List<ItemStack> drops = Block.getDrops(ore.defaultBlockState(), helper.getLevel(), pos, null, null,
                    new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(ModItems.RAW_BLUE_ORICHALCUM.get())), ore + " drops raw orichalcum: " + drops);
            helper.assertFalse(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()), "iron isn't enough");
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void blueOrichalcumGeneratesUnderForests(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE).containsKey(Overgearedmod.id("ore_blue_orichalcum")),
                "the placed feature should load");
        helper.assertTrue(registries.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                .containsKey(Overgearedmod.id("add_ore_blue_orichalcum")), "and its biome modifier");
        TagKey<Biome> forests = TagKey.create(Registries.BIOME, Overgearedmod.id("has_ore/blue_orichalcum"));
        helper.assertTrue(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DARK_FOREST).is(forests), "under forests");
        helper.assertFalse(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).is(forests), "not under plains");
        helper.succeed();
    }
}
