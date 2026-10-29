package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

    @GameTest(template = TestSupport.EMPTY)
    public static void rawMithrilSmeltsIntoIngot(GameTestHelper helper) {
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(ModItems.RAW_MITHRIL.get())), helper.getLevel())
                .map(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ModItems.MITHRIL_INGOT.get()), "raw mithril should smelt into an ingot, got " + result);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void mithrilOreGenerates(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .containsKey(Overgearedmod.id("ore_mithril")), "the placed feature should load from the datapack");
        helper.succeed();
    }
}
