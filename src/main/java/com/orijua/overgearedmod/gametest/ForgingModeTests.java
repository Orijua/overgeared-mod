package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatType;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.smithing.ForgingRecipeInput;
import com.orijua.overgearedmod.feature.smithing.ForgingRules;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The two ways to forge, through the real anvil menu. A fake player's menu is not ticked by the server,
 * so the tests call {@code broadcastChanges()} themselves, as ServerPlayer.tick does every tick.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ForgingModeTests {
    /** A novice forges automatically in the longest time (ForgingRulesTest checks it); a constant, for timeoutTicks. */
    private static final int NOVICE_TICKS = ForgingRules.MAX_AUTO_TICKS;

    private ForgingModeTests() {
    }

    /** An anvil menu loaded with 2 jaffa ingots + a stick (the Jaffa sword recipe). */
    private static SmithingAnvilMenu anvilWithJaffaInputs(GameTestHelper helper, ServerPlayer smith) {
        SmithingAnvilMenu menu = new SmithingAnvilMenu(1, smith.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
        menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.STICK));
        helper.assertTrue(menu.canForge(), "2 jaffa + stick should preview the Jaffa sword");
        return menu;
    }

    private static boolean hasForgedJaffaSword(ServerPlayer smith) {
        return smith.getInventory().items.stream()
                .anyMatch(stack -> stack.is(ModItems.JAFFA_SWORD.get()) && stack.has(ModDataComponents.ITEM_RATING));
    }

    private static boolean inputsUntouched(SmithingAnvilMenu menu) {
        return menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().getCount() == 2
                && menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).getItem().is(Items.STICK);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resultCanOnlyBeForgedWithTheButtons(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        helper.assertFalse(menu.getSlot(SmithingAnvilMenu.RESULT_SLOT).mayPickup(smith), "the result slot is only a preview");
        helper.assertTrue(menu.quickMoveStack(smith, SmithingAnvilMenu.RESULT_SLOT).isEmpty(), "and can't be shift-clicked out");
        helper.assertTrue(inputsUntouched(menu) && !hasForgedJaffaSword(smith), "nothing was forged");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = NOVICE_TICKS + 40)
    public static void autoForgeGivesTheItemOnceItsTimeIsUp(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        helper.assertTrue(menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON), "auto forging starts");
        helper.runAtTickTime(NOVICE_TICKS / 2, () -> {
            menu.broadcastChanges();
            helper.assertTrue(menu.autoForging() && menu.autoProgress() > 0 && menu.autoProgress() < 1, "half way: " + menu.autoProgress());
            helper.assertTrue(inputsUntouched(menu) && !hasForgedJaffaSword(smith), "nothing is spent before it finishes");
        });
        helper.runAtTickTime(NOVICE_TICKS - 2, () -> {
            menu.broadcastChanges();
            helper.assertTrue(menu.autoForging(), "just before its time it is still forging");
            helper.assertTrue(!hasForgedJaffaSword(smith) && inputsUntouched(menu), "and has given nothing yet");
        });
        helper.runAtTickTime(NOVICE_TICKS + 5, () -> {
            menu.broadcastChanges();
            helper.assertFalse(menu.autoForging(), "finished");
            helper.assertTrue(hasForgedJaffaSword(smith), "the rated sword goes straight to the inventory");
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().isEmpty(), "both ingots were spent");
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = NOVICE_TICKS + 40)
    public static void autoForgeStopsWhenTheInputsChange(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON);
        helper.runAtTickTime(10, () -> {
            menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(ItemStack.EMPTY); // the player takes the stick out
            menu.broadcastChanges();
            helper.assertFalse(menu.autoForging(), "changing the inputs cancels it");
            menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.STICK));
        });
        helper.runAtTickTime(NOVICE_TICKS + 5, () -> {
            menu.broadcastChanges();
            helper.assertTrue(inputsUntouched(menu) && !hasForgedJaffaSword(smith), "a cancelled forge spends and gives nothing");
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void manualForgeRefusesResultsWithoutARealGame(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        helper.assertFalse(menu.finishManual(smith, 1.0), "no minigame was started");
        menu.clickMenuButton(smith, SmithingAnvilMenu.MANUAL_FORGE_BUTTON);
        helper.assertFalse(menu.finishManual(smith, 1.0), "a result in the same tick is too fast to be a real game");
        helper.assertTrue(inputsUntouched(menu) && !hasForgedJaffaSword(smith), "refused results spend and give nothing");
        helper.succeed();
    }

    /** Changing the inputs to another recipe during the minigame loses its result: it was played for other materials. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = ForgingRules.MIN_MANUAL_TICKS + 40)
    public static void manualForgeRefusedIfTheRecipeChanged(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        helper.assertTrue(menu.clickMenuButton(smith, SmithingAnvilMenu.MANUAL_FORGE_BUTTON), "the minigame starts");
        menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(Items.DIAMOND, 4));
        menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.BLAZE_ROD));
        menu.broadcastChanges();
        helper.assertTrue(menu.canForge(), "diamonds + blaze rod make Grid's sword, another recipe");
        helper.runAtTickTime(ForgingRules.MIN_MANUAL_TICKS + 10, () -> {
            helper.assertFalse(menu.finishManual(smith, 1.0), "the result is for the Jaffa sword, not for this");
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().getCount() == 4
                    && menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).getItem().is(Items.BLAZE_ROD), "nothing was spent");
            helper.assertTrue(smith.getInventory().items.stream().noneMatch(stack -> stack.has(ModDataComponents.ITEM_RATING)),
                    "and nothing was given");
            helper.succeed();
        });
    }

    /**
     * Smith's quality test (2 forges of Rare or better) counts automatic and manual forges alike: after one of each, its
     * progress is 2. The smith has so much dexterity that both swords are Rare or better, so the count can't be 0 = 0.
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = NOVICE_TICKS + ForgingRules.MIN_MANUAL_TICKS + 60)
    public static void bothForgingModesCountForTheQualityTest(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        BlacksmithNpc npc = TestSupport.smith(helper);
        ResourceLocation qualityTest = Overgearedmod.id("quality_test");
        TestSupport.completed(smith, "smith_jaffa_sword", "first_commission");
        helper.assertTrue(QuestService.accept(smith, qualityTest, npc) == QuestService.Result.SUCCESS, "quality test accepted at Smith");
        int forgesBefore = GoalTracker.counters(smith).get("forge");
        // So much dexterity that Normal is negligible: both swords are Rare or better, and the check can't pass vacuously.
        ProgressionService.set(smith, ProgressionService.get(smith).with(StatType.DEXTERITY, 2000));

        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON);
        helper.runAtTickTime(NOVICE_TICKS + 5, () -> {
            menu.broadcastChanges();
            helper.assertTrue(hasForgedJaffaSword(smith), "the automatic forge finished");
            menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
            menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.STICK));
            menu.broadcastChanges();
            menu.clickMenuButton(smith, SmithingAnvilMenu.MANUAL_FORGE_BUTTON);
        });
        helper.runAtTickTime(NOVICE_TICKS + ForgingRules.MIN_MANUAL_TICKS + 10, () -> {
            helper.assertTrue(menu.finishManual(smith, 1.0), "the manual forge is accepted");
            helper.assertTrue(GoalTracker.counters(smith).get("forge") == forgesBefore + 2, "both forges were counted: " + forgesBefore + " -> " + GoalTracker.counters(smith).get("forge"));
            long rareOrBetter = smith.getInventory().items.stream()
                    .filter(stack -> stack.is(ModItems.JAFFA_SWORD.get()))
                    .map(stack -> stack.get(ModDataComponents.ITEM_RATING))
                    .filter(rating -> rating != null && rating.ordinal() >= ItemRating.RARE.ordinal())
                    .count();
            int progress = QuestService.progress(smith, Definitions.get(helper.getLevel(), ModDatapackRegistries.QUEST, qualityTest)
                    .orElseThrow(), QuestService.get(smith).active().get(qualityTest));
            helper.assertTrue(rareOrBetter == 2, "with that much dexterity both swords are Rare or better: " + rareOrBetter);
            helper.assertTrue(progress == 2, "quality test progress " + progress + " counts both forges");
            helper.succeed();
        });
    }

    private static long forgedJaffaSwords(ServerPlayer smith) {
        return smith.getInventory().items.stream()
                .filter(stack -> stack.is(ModItems.JAFFA_SWORD.get()) && stack.has(ModDataComponents.ITEM_RATING))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    /** A master smith (level 30: every batch size) with ingots and sticks for {@code swords} Jaffa swords. */
    private static SmithingAnvilMenu masterAnvil(GameTestHelper helper, ServerPlayer smith, int swords) {
        ProgressionService.addSmithingXp(smith, 10 * 30 * 30);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(ModItems.JAFFA_INGOT.get(), 2 * swords));
        menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.STICK, swords));
        return menu;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void batchSizesStayLockedForANovice(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        menu.clickMenuButton(smith, SmithingAnvilMenu.BATCH_BUTTON);
        helper.assertTrue(menu.batchChoice() == 0, "below smithing 10 the anvil only forges ×1");
        helper.succeed();
    }

    /** ×5 asked, materials for 3: three rated swords, one after another, then it stops by itself. */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 5 * ForgingRules.MIN_AUTO_TICKS + 40)
    public static void batchForgesUntilTheMaterialsRunOut(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = masterAnvil(helper, smith, 3);
        menu.clickMenuButton(smith, SmithingAnvilMenu.BATCH_BUTTON);
        helper.assertTrue(menu.batchChoice() == 1, "a master smith can pick ×5");
        helper.assertTrue(menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON), "the batch starts");
        helper.assertTrue(menu.batchCount() == 3, "capped at what the materials make: " + menu.batchCount());
        helper.onEachTick(menu::broadcastChanges);
        helper.runAtTickTime(ForgingRules.MIN_AUTO_TICKS + 5, () ->
                helper.assertTrue(forgedJaffaSwords(smith) == 1 && menu.autoForging(), "one forged, the next one under way: forged " + forgedJaffaSwords(smith) + ", forging " + menu.autoForging() + ", done " + menu.batchDone() + "/" + menu.batchCount()));
        helper.runAtTickTime(3 * ForgingRules.MIN_AUTO_TICKS + 10, () -> {
            helper.assertTrue(forgedJaffaSwords(smith) == 3, "three swords: " + forgedJaffaSwords(smith));
            helper.assertFalse(menu.autoForging(), "out of materials, the batch ends");
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().isEmpty(), "every ingot was spent");
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 5 * ForgingRules.MIN_AUTO_TICKS + 40)
    public static void stopEndsTheBatchKeepingWhatWasForged(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = masterAnvil(helper, smith, 5);
        menu.clickMenuButton(smith, SmithingAnvilMenu.BATCH_BUTTON);
        menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON);
        helper.onEachTick(menu::broadcastChanges);
        helper.runAtTickTime(ForgingRules.MIN_AUTO_TICKS + 5, () -> {
            helper.assertTrue(menu.clickMenuButton(smith, SmithingAnvilMenu.STOP_BUTTON), "Stop works mid-batch: forged " + forgedJaffaSwords(smith) + ", done " + menu.batchDone() + "/" + menu.batchCount());
            helper.assertFalse(menu.autoForging(), "stopped");
        });
        helper.runAtTickTime(3 * ForgingRules.MIN_AUTO_TICKS, () -> {
            helper.assertTrue(forgedJaffaSwords(smith) == 1, "the forged sword stays, no more come: " + forgedJaffaSwords(smith));
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().getCount() == 8, "the rest of the ingots are untouched");
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = ForgingRules.MIN_MANUAL_TICKS + 40)
    public static void manualForgeGivesTheItemAndDoubleSmithingXp(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        SmithingAnvilMenu menu = anvilWithJaffaInputs(helper, smith);
        int recipeXp = helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(),
                new ForgingRecipeInput(new ItemStack(ModItems.JAFFA_INGOT.get(), 2), new ItemStack(Items.STICK)), helper.getLevel())
                .orElseThrow().value().smithingXp();
        menu.clickMenuButton(smith, SmithingAnvilMenu.MANUAL_FORGE_BUTTON);
        helper.runAtTickTime(ForgingRules.MIN_MANUAL_TICKS + 2, () -> {
            int xpBefore = ProgressionService.get(smith).smithingXp();
            helper.assertTrue(menu.finishManual(smith, 1.0), "a plausible minigame result is accepted");
            helper.assertTrue(hasForgedJaffaSword(smith), "the rated sword goes straight to the inventory");
            int gained = ProgressionService.get(smith).smithingXp() - xpBefore;
            helper.assertTrue(gained == recipeXp * ForgingRules.MANUAL_XP_MULTIPLIER, "double smithing XP: expected "
                    + recipeXp * ForgingRules.MANUAL_XP_MULTIPLIER + ", got " + gained);
            helper.assertFalse(menu.finishManual(smith, 1.0), "one minigame forges once");
            helper.succeed();
        });
    }
}
