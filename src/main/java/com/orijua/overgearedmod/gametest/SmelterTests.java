package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smelting.MeltingRecipe;
import com.orijua.overgearedmod.smelting.SmelterBlock;
import com.orijua.overgearedmod.smelting.SmelterBlockEntity;
import com.orijua.overgearedmod.smelting.SmelterMenu;
import com.orijua.overgearedmod.smelting.SmelterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/** The smelter, through its real block entity and menu. Its ticks are run by hand so heating takes no real time. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SmelterTests {
    private static final BlockPos SMELTER = new BlockPos(1, 1, 1);

    private SmelterTests() {
    }

    private static SmelterBlockEntity placeSmelter(GameTestHelper helper) {
        helper.setBlock(SMELTER, ModBlocks.SMELTER.get());
        return helper.getBlockEntity(SMELTER);
    }

    private static SmelterMenu menu(GameTestHelper helper, ServerPlayer smith, SmelterBlockEntity smelter) {
        return new SmelterMenu(1, smith.getInventory(), smelter, ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(SMELTER)));
    }

    /** Runs the smelter's server tick {@code ticks} times right away, as the level would over that many ticks. */
    private static void tick(GameTestHelper helper, SmelterBlockEntity smelter, int ticks) {
        BlockPos pos = helper.absolutePos(SMELTER);
        for (int i = 0; i < ticks; i++) {
            SmelterBlockEntity.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), smelter);
        }
    }

    private static Optional<MeltingRecipe> melting(GameTestHelper helper, Item item) {
        return helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.MELTING.get(), new SingleRecipeInput(new ItemStack(item)),
                helper.getLevel()).map(holder -> holder.value());
    }

    private static boolean furnaceTakes(GameTestHelper helper, Item item) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(item)),
                helper.getLevel()).isPresent();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void advancedMetalsOnlyMeltInTheSmelter(GameTestHelper helper) {
        helper.assertTrue(furnaceTakes(helper, Items.RAW_IRON) && furnaceTakes(helper, ModItems.RAW_JAFFA.get()), "iron and jaffa: furnace too");
        helper.assertFalse(furnaceTakes(helper, ModItems.RAW_MITHRIL.get()), "mithril: not in the furnace");
        helper.assertFalse(furnaceTakes(helper, ModItems.RAW_BLUE_ORICHALCUM.get()), "blue orichalcum: not in the furnace");
        int[] heats = {900, 950, 1200, 1450};
        int[] levels = {0, 0, 3, 8};
        Item[] metals = {Items.RAW_IRON, ModItems.RAW_JAFFA.get(), ModItems.RAW_MITHRIL.get(), ModItems.RAW_BLUE_ORICHALCUM.get()};
        for (int i = 0; i < metals.length; i++) {
            MeltingRecipe recipe = melting(helper, metals[i]).orElse(null);
            helper.assertTrue(recipe != null, metals[i] + " melts in the smelter");
            helper.assertTrue(recipe.minTemperature() == heats[i] && recipe.smithingLevel() == levels[i],
                    metals[i] + " at " + recipe.minTemperature() + "° and smithing " + recipe.smithingLevel());
        }
        helper.assertTrue(melting(helper, ModItems.BLUE_ORICHALCUM_ORE.get()).isPresent(), "and the ore block itself");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fuelOnlyBurnsWithMetalToMelt(GameTestHelper helper) {
        SmelterBlockEntity smelter = placeSmelter(helper);
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 4));
        tick(helper, smelter, 50);
        helper.assertTrue(smelter.temperature() == SmelterRules.AMBIENT, "no metal: the coal waits");
        helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.FUEL).getCount() == 4, "and isn't spent");

        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON));
        tick(helper, smelter, 300);
        helper.assertTrue(smelter.temperature() == 1050, "coal heats it up to 1050°, got " + smelter.temperature());
        helper.assertTrue(helper.getBlockState(SMELTER).getValue(SmelterBlock.LIT), "it glows while burning");
        helper.succeed();
    }

    /** Started by a smith, it waits for the heat, then melts piece after piece. */
    @GameTest(template = TestSupport.EMPTY)
    public static void automaticMeltingWaitsForHeatThenGivesIngots(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 2));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        helper.assertTrue(menu(helper, smith, smelter).clickMenuButton(smith, SmelterMenu.AUTO_BUTTON), "a novice can melt iron");
        tick(helper, smelter, 100);
        helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.OUTPUT).isEmpty(), "still heating up: " + smelter.temperature() + "°");
        tick(helper, smelter, 600);
        ItemStack ingots = smelter.items().getItem(SmelterBlockEntity.OUTPUT);
        helper.assertTrue(ingots.is(Items.IRON_INGOT) && ingots.getCount() >= 2, "both pieces melted: " + ingots);
        helper.assertFalse(smelter.melting(), "it stops when the metal runs out");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void blueOrichalcumNeedsAdvancedSmithing(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(ModItems.RAW_BLUE_ORICHALCUM.get()));
        SmelterMenu menu = menu(helper, smith, smelter);
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) || smelter.melting(), "a novice can't");
        ProgressionService.addSmithingXp(smith, 640); // smithing level 8
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) && smelter.melting(), "a smith of level 8 can");
        helper.succeed();
    }

    /** The bellows need a hot smelter, and their result is only believed after a real game. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = SmelterRules.MIN_MANUAL_TICKS + 40)
    public static void manualMeltNeedsHeatAndARealGame(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 3));
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "a cold smelter can't be worked");

        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        tick(helper, smelter, 200);
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "hot enough: the bellows start");
        helper.assertFalse(menu.finishManual(smith, 1.0), "a result in the same tick is too fast to be a real game");
        helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 3, "refused results spend nothing");

        menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON);
        int xpBefore = ProgressionService.get(smith).smithingXp();
        helper.runAtTickTime(SmelterRules.MIN_MANUAL_TICKS + 2, () -> {
            helper.assertTrue(menu.finishManual(smith, 1.0), "a plausible result is accepted");
            ItemStack ingots = smelter.items().getItem(SmelterBlockEntity.OUTPUT);
            helper.assertTrue(ingots.is(Items.IRON_INGOT) && ingots.getCount() >= 1, "a perfect hand never makes slag: " + ingots);
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 2, "one piece melted");
            int gained = ProgressionService.get(smith).smithingXp() - xpBefore;
            int expected = melting(helper, Items.RAW_IRON).orElseThrow().smithingXp() * SmelterRules.MANUAL_XP_MULTIPLIER;
            helper.assertTrue(gained == expected, "double smithing XP: expected " + expected + ", got " + gained);
            helper.succeed();
        });
    }
}
