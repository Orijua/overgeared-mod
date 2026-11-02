package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.progression.ProgressionRules;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.smelting.MeltingRecipe;
import com.orijua.overgearedmod.feature.smelting.SmelterBlock;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.smelting.SmelterBlockEntity;
import com.orijua.overgearedmod.feature.smelting.SmelterMenu;
import com.orijua.overgearedmod.feature.smelting.SmelterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

    /**
     * Melt lights the fire. Heating up burns fuel twice as fast; once hot, each piece burns out holding its heat and the
     * next goes in, but only with metal to melt: without metal the rest waits and the heat drops. Put out douses it.
     * Sticks: 100 ticks of fuel, 700° (too cold for iron, so the metal stays).
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void fireBurnsPieceByPieceAndHeatingCostsDouble(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.STICK, 5));
        tick(helper, smelter, 50);
        helper.assertTrue(smelter.temperature() == SmelterRules.AMBIENT && fuel(smelter) == 5, "not lit: nothing burns");

        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) && smelter.fireOn(), "Melt lights the fire");
        tick(helper, smelter, 20);
        helper.assertTrue(helper.getBlockState(SMELTER).getValue(SmelterBlock.LIT) && fuel(smelter) == 4, "the first stick burns");
        tick(helper, smelter, 40);
        helper.assertTrue(fuel(smelter) == 3, "heating up: a 100-tick stick lasted only 50 ticks");
        tick(helper, smelter, 90);
        helper.assertTrue(smelter.temperature() == 700, "hot: the heat holds at the sticks' 700°: " + smelter.temperature());

        smelter.items().setItem(SmelterBlockEntity.INPUT, ItemStack.EMPTY);
        int waiting = fuel(smelter);
        tick(helper, smelter, 150);
        helper.assertTrue(fuel(smelter) == waiting, "no metal: the burning stick finished, the rest waits");
        helper.assertTrue(smelter.temperature() < 700, "and with nothing burning it cools: " + smelter.temperature());

        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON));
        tick(helper, smelter, 1);
        helper.assertTrue(fuel(smelter) == waiting - 1, "metal again, still lit: it takes the next stick");
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.PUT_OUT_BUTTON), "put out");
        tick(helper, smelter, 1);
        helper.assertFalse(smelter.fireOn() || helper.getBlockState(SMELTER).getValue(SmelterBlock.LIT), "at once");
        helper.succeed();
    }

    private static int fuel(SmelterBlockEntity smelter) {
        return smelter.items().getItem(SmelterBlockEntity.FUEL).getCount();
    }

    /** Started by a smith, it waits for the heat, then melts piece after piece. */
    @GameTest(template = TestSupport.EMPTY)
    public static void automaticMeltingWaitsForHeatThenGivesIngots(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 2));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        SmelterMenu menu = menu(helper, smith, smelter);
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON), "a novice can melt iron");
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
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.BLAZE_ROD));
        SmelterMenu menu = menu(helper, smith, smelter);
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) || smelter.melting(), "a novice can't");
        ProgressionService.addSmithingXp(smith, ProgressionRules.smithingXpFor(7));
        helper.assertTrue(ProgressionService.effectiveSmithingLevel(smith) == 7, "level 7: " + ProgressionService.effectiveSmithingLevel(smith));
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) || smelter.melting(), "level 7 is not enough");
        ProgressionService.addSmithingXp(smith, ProgressionRules.smithingXpFor(8) - ProgressionRules.smithingXpFor(7));
        helper.assertTrue(ProgressionService.effectiveSmithingLevel(smith) == 8, "level 8: " + ProgressionService.effectiveSmithingLevel(smith));
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON) && smelter.melting(), "a smith of level 8 can");
        helper.succeed();
    }

    /** The bellows need the melting heat exactly: 899 degrees is not enough for iron, 900 is. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theBellowsNeedTheExactMeltingHeat(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL));
        setTemperature(helper, smelter, 899);
        helper.assertTrue(smelter.temperature() == 899, "set to 899: " + smelter.temperature());
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "899: one degree short");
        setTemperature(helper, smelter, 900);
        helper.assertTrue(smelter.temperature() == 900, "set to 900: " + smelter.temperature());
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "900: the bellows start");
        helper.succeed();
    }

    /** Loads a temperature (burning, so it would not go out) through the block entity's saved data. */
    private static void setTemperature(GameTestHelper helper, SmelterBlockEntity smelter, int temperature) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = smelter.saveWithoutMetadata(registries);
        tag.putInt("temperature", temperature);
        tag.putBoolean("fire_on", true);
        tag.putInt("burn_time", 1000);
        tag.putInt("burn_total", 1000);
        tag.putInt("fuel_heat", 1050);
        smelter.loadWithComponents(tag, registries);
    }

    /** Bad hands lose the metal as slag: that melt gives no ingot and no XP; the rest give their ingot and double XP. */
    @GameTest(template = TestSupport.EMPTY)
    public static void slagGivesNoIngotAndNoXp(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 40));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL));
        setTemperature(helper, smelter, 1000);
        ResourceLocation recipeId = null;
        int expectedXp = melting(helper, Items.RAW_IRON).orElseThrow().smithingXp() * SmelterRules.MANUAL_XP_MULTIPLIER;
        int slag = 0;
        int melted = 0;
        for (int i = 0; i < 40; i++) {
            if (recipeId == null) {
                menu(helper, smith, smelter).clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON);
                recipeId = smelter.currentRecipeId();
                helper.assertTrue(recipeId != null, "the iron has a recipe");
            }
            int outBefore = smelter.items().getItem(SmelterBlockEntity.OUTPUT).getCount();
            int inBefore = smelter.items().getItem(SmelterBlockEntity.INPUT).getCount();
            int xpBefore = ProgressionService.get(smith).smithingXp();
            helper.assertTrue(smelter.finishManual(smith, 0.0, recipeId), "a botched melt still spends the metal");
            int ingots = smelter.items().getItem(SmelterBlockEntity.OUTPUT).getCount() - outBefore;
            int gained = ProgressionService.get(smith).smithingXp() - xpBefore;
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == inBefore - 1, "one piece spent");
            helper.assertTrue(ingots == 0 || ingots == 1, "never an extra ingot with a botched hand: " + ingots);
            if (ingots == 0) {
                slag++;
                helper.assertTrue(gained == 0, "slag gives no XP: " + gained);
            } else {
                melted++;
                helper.assertTrue(gained == expectedXp, "a good melt gives double XP: expected " + expectedXp + ", got " + gained);
            }
        }
        helper.assertTrue(slag > 0 && melted > 0, "with 60 % slag over 40 melts both outcomes show: slag " + slag + ", melted " + melted);
        helper.succeed();
    }

    /** Changing the metal while the automatic melt is under way restarts the progress. */
    @GameTest(template = TestSupport.EMPTY)
    public static void changingTheMetalRestartsTheMelt(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 4));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.AUTO_BUTTON), "melting starts");
        int guard = 0;
        while (menu.meltProgress() <= 0 && guard++ < 2000) {
            tick(helper, smelter, 1);
        }
        helper.assertTrue(menu.meltProgress() > 0, "the melt got going");
        tick(helper, smelter, 20);
        int time = melting(helper, Items.RAW_IRON).orElseThrow().time();
        helper.assertTrue(menu.meltProgress() >= 20f / time, "well under way: " + menu.meltProgress());

        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(ModItems.RAW_JAFFA.get(), 4));
        tick(helper, smelter, 1);
        helper.assertTrue(menu.meltProgress() <= 1f / melting(helper, ModItems.RAW_JAFFA.get()).orElseThrow().time() + 1e-6f,
                "another metal starts from the beginning: " + menu.meltProgress());
        helper.succeed();
    }

    /** The bellows light a cold smelter first and need it hot to start; their result is only believed after a real game. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = SmelterRules.MIN_MANUAL_TICKS + 40)
    public static void manualMeltNeedsHeatAndARealGame(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 3));
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON) || smelter.fireOn(), "cold and no fuel: nothing");

        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        helper.assertFalse(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "cold: the bellows don't start yet");
        helper.assertTrue(smelter.fireOn(), "but they light the fire to heat it up");
        tick(helper, smelter, 200);
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "hot now: the bellows start");
        helper.assertFalse(menu.finishManual(smith, 1.0), "a result in the same tick is too fast to be a real game");
        helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 3, "refused results spend nothing");

        menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON);
        int xpBefore = ProgressionService.get(smith).smithingXp();
        helper.runAtTickTime(SmelterRules.MIN_MANUAL_TICKS + 2, () -> {
            helper.assertTrue(menu.finishManual(smith, 1.0), "a plausible result is accepted");
            ItemStack ingots = smelter.items().getItem(SmelterBlockEntity.OUTPUT);
            helper.assertTrue(ingots.is(Items.IRON_INGOT) && ingots.getCount() >= 1, "a perfect hand never makes slag: " + ingots);
            int extras = GoalTracker.milestones(smith, SmelterBlockEntity.BELLOWS_EXTRA);
            helper.assertTrue(extras == (ingots.getCount() == 2 ? 1 : 0), "an extra ingot is a bellows milestone: " + extras);
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 2, "one piece melted");
            int gained = ProgressionService.get(smith).smithingXp() - xpBefore;
            int expected = melting(helper, Items.RAW_IRON).orElseThrow().smithingXp() * SmelterRules.MANUAL_XP_MULTIPLIER;
            helper.assertTrue(gained == expected, "double smithing XP: expected " + expected + ", got " + gained);
            helper.succeed();
        });
    }

    /** Hot smelter with iron and the bellows started; the caller then changes things before the result arrives. */
    private static SmelterMenu startedManualMelt(GameTestHelper helper, ServerPlayer smith, SmelterBlockEntity smelter) {
        SmelterMenu menu = menu(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON));
        smelter.items().setItem(SmelterBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON); // cold: lights the fire
        tick(helper, smelter, 200);
        helper.assertTrue(menu.clickMenuButton(smith, SmelterMenu.MANUAL_BUTTON), "hot now: the bellows start");
        return menu;
    }

    /** Changing the metal during the minigame loses the melt: the result is for the metal that was there at the start. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = SmelterRules.MIN_MANUAL_TICKS + 40)
    public static void manualMeltRefusedIfTheMetalChanges(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = startedManualMelt(helper, smith, smelter);
        smelter.items().setItem(SmelterBlockEntity.INPUT, new ItemStack(ModItems.RAW_BLUE_ORICHALCUM.get()));
        helper.runAtTickTime(SmelterRules.MIN_MANUAL_TICKS + 2, () -> {
            helper.assertFalse(menu.finishManual(smith, 1.0), "another metal: the result is refused");
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 1, "the orichalcum is still there");
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.OUTPUT).isEmpty(), "nothing came out");
            helper.succeed();
        });
    }

    /** A smelter that cooled below the melting temperature during the minigame gives nothing. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = SmelterRules.MIN_MANUAL_TICKS + 40)
    public static void manualMeltRefusedIfTheSmelterCooled(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = startedManualMelt(helper, smith, smelter);
        menu.clickMenuButton(smith, SmelterMenu.PUT_OUT_BUTTON);
        tick(helper, smelter, 200);
        helper.assertTrue(smelter.temperature() < 900, "cooled below the melting point of iron: " + smelter.temperature());
        helper.runAtTickTime(SmelterRules.MIN_MANUAL_TICKS + 2, () -> {
            helper.assertFalse(menu.finishManual(smith, 1.0), "cold: the result is refused");
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).getCount() == 1, "nothing consumed");
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.OUTPUT).isEmpty(), "nothing came out");
            helper.succeed();
        });
    }

    /** The criterion is the heat, not the fire: a doused smelter that is still hot melts. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = SmelterRules.MIN_MANUAL_TICKS + 40)
    public static void manualMeltSurvivesThePutOutWhileHot(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmelterBlockEntity smelter = placeSmelter(helper);
        SmelterMenu menu = startedManualMelt(helper, smith, smelter);
        menu.clickMenuButton(smith, SmelterMenu.PUT_OUT_BUTTON);
        tick(helper, smelter, 10);
        helper.assertTrue(smelter.temperature() >= 900, "still hot: " + smelter.temperature());
        helper.runAtTickTime(SmelterRules.MIN_MANUAL_TICKS + 2, () -> {
            helper.assertTrue(menu.finishManual(smith, 1.0), "hot enough: accepted");
            helper.assertTrue(smelter.items().getItem(SmelterBlockEntity.INPUT).isEmpty(), "the piece melted");
            helper.succeed();
        });
    }
}
