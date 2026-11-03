package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.npc.ApprenticeAssignments;
import com.orijua.overgearedmod.feature.npc.ApprenticeContractItem;
import com.orijua.overgearedmod.feature.npc.ApprenticeMaterials;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.Illumination;
import com.orijua.overgearedmod.feature.npc.MasteryStage;
import com.orijua.overgearedmod.feature.npc.NpcGrade;
import com.orijua.overgearedmod.feature.npc.Workshop;
import com.orijua.overgearedmod.feature.npc.WorkshopEntry;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/** Apprentice blacksmiths (mass production, phase 2): hiring by smithing level, the wage, and forging from chest to barrel. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeTests {
    private static final BlockPos ANVIL = new BlockPos(1, 1, 1);
    private static final BlockPos CHEST = new BlockPos(0, 1, 1);
    private static final BlockPos BARREL = new BlockPos(2, 1, 1);
    private static final BlockPos STAND = new BlockPos(1, 1, 0);

    private ApprenticeTests() {
    }

    /** A player whose smithing level (no class, so effective = own) is exactly {@code level}. */
    private static ServerPlayer smithOfLevel(GameTestHelper helper, int level) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        setSmithing(player, level);
        return player;
    }

    private static void setSmithing(ServerPlayer player, int level) {
        ProgressionService.set(player, PlayerStats.DEFAULT.withSmithingXp(level * level * 10));
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void apprenticesComeAtFiftyAndOneMoreEveryTenLevels(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 49);
        helper.assertTrue(ProgressionService.effectiveSmithingLevel(player) == 49, "level 49");
        BlockPos at = helper.absolutePos(STAND);
        helper.assertTrue(ApprenticeContractItem.hire(player, at).isEmpty(), "not at 49");
        setSmithing(player, 50);
        Optional<ApprenticeSmith> first = ApprenticeContractItem.hire(player, at);
        helper.assertTrue(first.isPresent(), "the first at 50");
        helper.assertTrue(ApprenticeContractItem.hire(player, at).isEmpty(), "only one at 50");
        setSmithing(player, 60);
        Optional<ApprenticeSmith> second = ApprenticeContractItem.hire(player, at);
        helper.assertTrue(second.isPresent(), "a second at 60");
        helper.assertTrue(player.getData(ModAttachments.APPRENTICES).size() == 2, "two hired");
        helper.assertTrue(first.get().isOwnedBy(player) && first.get().smithingLevel() == 5, "a beginner who works for the player");
        first.get().discard();
        second.get().discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void smithGivesTheFirstContractOnce(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 40);
        ApprenticeContractItem.introduce(player);
        helper.assertTrue(player.getInventory().countItem(ModItems.APPRENTICE_CONTRACT.get()) == 0, "not before 50");
        setSmithing(player, 50);
        ApprenticeContractItem.introduce(player);
        ApprenticeContractItem.introduce(player);
        helper.assertTrue(player.getInventory().countItem(ModItems.APPRENTICE_CONTRACT.get()) == 1, "one contract, once");
        helper.assertTrue(player.getInventory().countItem(ModItems.WORKSHOP_LEDGER.get()) == 1, "and the workshop ledger");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theWageBuysWholeDays(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        helper.assertTrue(apprentice.daysPaid() == 1, "the first day is on Smith");
        ItemStack emeralds = new ItemStack(Items.EMERALD, 10);
        apprentice.pay(player, emeralds);
        helper.assertTrue(apprentice.daysPaid() == 4 && emeralds.getCount() == 1, "three more days for nine emeralds: "
                + apprentice.daysPaid() + ", " + emeralds.getCount() + " left");
        apprentice.discard();
        helper.succeed();
    }

    /** The order's materials leave the chest and the piece lands rated in the barrel; a botch loses the materials. */
    @GameTest(template = TestSupport.EMPTY)
    public static void heForgesFromTheChestIntoTheBarrel(GameTestHelper helper) {
        helper.setBlock(ANVIL, ModBlocks.SMITHING_ANVIL.get());
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(BARREL, Blocks.BARREL);
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        helper.assertTrue(apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get()), "he can forge the Jaffa sword");
        helper.assertFalse(apprentice.takeOrder(player, ModItems.FRACASO_SWORD.get()), "but not a blueprint's");
        Container chest = helper.getBlockEntity(CHEST);
        Container barrel = helper.getBlockEntity(BARREL);

        helper.assertTrue(apprentice.forgeOne(0.99) == ApprenticeSmith.Status.NO_MATERIALS, "nothing in the chest yet");
        chest.setItem(0, new ItemStack(ModItems.JAFFA_INGOT.get(), 4));
        chest.setItem(1, new ItemStack(Items.STICK, 2));
        int before = apprentice.smithingXp();
        helper.assertTrue(apprentice.forgeOne(0.99) == ApprenticeSmith.Status.WORKING, "a piece");
        ItemStack sword = barrel.getItem(0);
        helper.assertTrue(sword.is(ModItems.JAFFA_SWORD.get()) && sword.has(ModDataComponents.ITEM_RATING), "a rated sword in the barrel");
        helper.assertTrue(chest.getItem(0).getCount() == 2 && chest.getItem(1).getCount() == 1, "its materials left the chest");
        helper.assertTrue(apprentice.smithingXp() > before, "he learned");

        apprentice.forgeOne(0.0);
        helper.assertTrue(chest.getItem(0).isEmpty() && chest.getItem(1).isEmpty(), "a botch spends the materials");
        int pieces = 0;
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            pieces += barrel.getItem(i).isEmpty() ? 0 : 1;
        }
        helper.assertTrue(pieces == 1, "and makes nothing: only the first sword is in the barrel, found " + pieces + " pieces");
        apprentice.discard();
        helper.succeed();
    }

    /**
     * He says what he lacks (the Jaffa sword's stick, found in the playtest), takes the materials when he starts a piece,
     * and gives an unfinished piece's materials back when the order is cancelled.
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void heSaysWhatHeLacksAndReturnsAnUnfinishedPiece(GameTestHelper helper) {
        helper.setBlock(ANVIL, ModBlocks.SMITHING_ANVIL.get());
        helper.setBlock(CHEST, Blocks.CHEST);
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get());
        Container chest = helper.getBlockEntity(CHEST);
        chest.setItem(0, new ItemStack(ModItems.JAFFA_INGOT.get(), 2));

        helper.assertTrue(apprentice.startPiece() == ApprenticeSmith.Status.NO_OUTPUT, "no barrel: nowhere for the piece");
        helper.setBlock(BARREL, Blocks.BARREL);
        helper.assertTrue(apprentice.startPiece() == ApprenticeSmith.Status.NO_MATERIALS, "the ingots alone are not enough");
        ItemStack missing = apprentice.shownMissing();
        helper.assertTrue(missing.is(Items.STICK) && missing.getCount() == 1, "he lacks one stick: " + missing);
        helper.assertTrue(chest.getItem(0).getCount() == 2, "nothing taken while something is missing");

        chest.setItem(1, new ItemStack(Items.STICK));
        helper.assertTrue(apprentice.startPiece() == ApprenticeSmith.Status.WORKING, "now he starts");
        helper.assertTrue(apprentice.shownMissing().isEmpty(), "and lacks nothing");
        helper.assertTrue(chest.getItem(0).isEmpty() && chest.getItem(1).isEmpty(), "the materials leave the chest at the start");

        apprentice.cancelOrder(player);
        helper.assertTrue(player.getInventory().countItem(ModItems.JAFFA_INGOT.get()) == 2
                && player.getInventory().countItem(Items.STICK) == 1, "cancelling gives the unfinished piece's materials back");
        apprentice.discard();
        helper.succeed();
    }

    /** Assigned containers (not touching the anvil) replace the anvil's; once one is broken he goes back to the anvil's. */
    @GameTest(template = TestSupport.EMPTY)
    public static void heUsesTheContainersHisMasterAssigns(GameTestHelper helper) {
        BlockPos store = new BlockPos(0, 1, 2);
        BlockPos crate = new BlockPos(2, 1, 2);
        helper.setBlock(ANVIL, ModBlocks.SMITHING_ANVIL.get());
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(BARREL, Blocks.BARREL);
        helper.setBlock(store, Blocks.CHEST);
        helper.setBlock(crate, Blocks.BARREL);
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get());
        Container chest = helper.getBlockEntity(CHEST);
        Container barrel = helper.getBlockEntity(BARREL);
        Container storeChest = helper.getBlockEntity(store);
        Container crateBarrel = helper.getBlockEntity(crate);
        chest.setItem(0, new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
        chest.setItem(1, new ItemStack(Items.STICK, 1));
        storeChest.setItem(0, new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
        storeChest.setItem(1, new ItemStack(Items.STICK, 1));

        helper.assertTrue(apprentice.assign(player, ApprenticeSmith.Slot.MATERIALS, helper.absolutePos(store)), "a chest near the anvil");
        ApprenticeAssignments.start(player, apprentice, ApprenticeSmith.Slot.OUTPUT);
        helper.assertTrue(ApprenticeAssignments.click(player, helper.absolutePos(crate)), "the armed click picks the barrel");
        helper.assertTrue(apprentice.assigned(ApprenticeSmith.Slot.OUTPUT).equals(Optional.of(helper.absolutePos(crate))), "assigned");
        helper.assertFalse(ApprenticeAssignments.click(player, helper.absolutePos(crate)), "one click per Assign");

        helper.assertTrue(apprentice.forgeOne(0.99) == ApprenticeSmith.Status.WORKING, "a piece");
        helper.assertTrue(storeChest.getItem(0).isEmpty() && chest.getItem(0).getCount() == 2, "from the assigned chest only");
        helper.assertTrue(crateBarrel.getItem(0).is(ModItems.JAFFA_SWORD.get()) && barrel.getItem(0).isEmpty(), "into the assigned barrel only");

        helper.setBlock(store, Blocks.AIR);
        helper.assertTrue(apprentice.forgeOne(0.99) == ApprenticeSmith.Status.WORKING, "the broken chest is forgotten");
        helper.assertTrue(apprentice.assigned(ApprenticeSmith.Slot.MATERIALS).isEmpty() && chest.getItem(0).isEmpty(), "and the anvil's is used");

        apprentice.clearAssignments(player);
        helper.assertTrue(apprentice.assigned(ApprenticeSmith.Slot.OUTPUT).isEmpty(), "cleared");
        apprentice.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyNearbyContainersCanBeAssigned(GameTestHelper helper) {
        helper.setBlock(CHEST, Blocks.CHEST);
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        helper.assertFalse(apprentice.assign(player, ApprenticeSmith.Slot.MATERIALS, helper.absolutePos(ANVIL)), "air holds nothing");
        BlockPos far = helper.absolutePos(STAND).south(ApprenticeSmith.ASSIGN_REACH + 4);
        apprentice.setPos(far.getX() + 0.5, far.getY(), far.getZ() + 0.5); // no anvil: measured from himself
        helper.assertFalse(apprentice.assign(player, ApprenticeSmith.Slot.MATERIALS, helper.absolutePos(CHEST)), "too far");
        helper.assertTrue(apprentice.assigned(ApprenticeSmith.Slot.MATERIALS).isEmpty(), "nothing assigned");
        apprentice.discard();
        helper.succeed();
    }

    /** Gold, diamond and the master metals wait for the right stage; iron and jaffa are a beginner's. */
    @GameTest(template = TestSupport.EMPTY)
    public static void materialsNeedTheirMastery(GameTestHelper helper) {
        helper.assertTrue(ApprenticeMaterials.requiredStage(Ingredient.of(Items.IRON_INGOT)) == MasteryStage.BEGINNER, "iron");
        helper.assertTrue(ApprenticeMaterials.requiredStage(Ingredient.of(ModItems.JAFFA_INGOT.get())) == MasteryStage.BEGINNER, "jaffa");
        helper.assertTrue(ApprenticeMaterials.requiredStage(Ingredient.of(Items.GOLD_INGOT)) == MasteryStage.INTERMEDIATE, "gold");
        helper.assertTrue(ApprenticeMaterials.requiredStage(Ingredient.of(Items.DIAMOND)) == MasteryStage.ADVANCED, "diamond");
        helper.assertTrue(ApprenticeMaterials.requiredStage(Ingredient.of(ModItems.MITHRIL_INGOT.get(), Items.IRON_INGOT)) == MasteryStage.MASTER,
                "the hardest item in the ingredient decides");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void hisGradeDecidesHowFarHeGrows(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 80);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        apprentice.setGrade(NpcGrade.NORMAL);
        apprentice.setSmithingXp(65 * 65 * 10);
        helper.assertTrue(apprentice.smithingLevel() == 40 && apprentice.stage() == MasteryStage.ADVANCED, "a normal NPC stops at 40");
        apprentice.setGrade(NpcGrade.NAMED);
        helper.assertTrue(apprentice.smithingLevel() == 65 && apprentice.stage() == MasteryStage.MASTER, "a named one goes on");
        helper.assertTrue(apprentice.shownLevel() == 65 && apprentice.shownStage() == MasteryStage.MASTER, "synced to the window");
        helper.assertTrue(apprentice.getCustomName().getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.GOLD), "with a gold name");
        apprentice.discard();
        helper.succeed();
    }

    /** Only a Master: by his own Unique piece, or by watching his master forge a Legendary nearby (illumination). */
    @GameTest(template = TestSupport.EMPTY)
    public static void aMasterBecomesACraftsman(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 80);
        player.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(STAND)));
        ApprenticeSmith master = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        ApprenticeSmith advanced = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        ApprenticeSmith proud = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        for (ApprenticeSmith apprentice : new ApprenticeSmith[]{master, advanced, proud}) {
            apprentice.setGrade(NpcGrade.SEMI_NAMED);
            apprentice.setSmithingXp(65 * 65 * 10);
        }
        advanced.setSmithingXp(50 * 50 * 10);

        helper.assertTrue(Illumination.enlighten(player, ItemRating.EPIC) == 0, "an Epic enlightens nobody");
        helper.assertTrue(Illumination.enlighten(player, ItemRating.LEGENDARY) == 2, "a Legendary enlightens both Masters");
        helper.assertTrue(master.isCraftsman() && !advanced.isCraftsman(), "not the Advanced one");
        helper.assertTrue(master.getCustomName().getString().endsWith(Component.translatable("entity.overgearedmod.apprentice_smith.craftsman_suffix").getString()),
                "his name says it");

        advanced.setSmithingXp(65 * 65 * 10);
        helper.assertFalse(advanced.judgePiece(ItemRating.EPIC, true), "his own Epic is not a masterpiece");
        helper.assertTrue(advanced.judgePiece(ItemRating.UNIQUE, true), "his own Unique is");
        master.discard();
        advanced.discard();
        proud.discard();
        helper.succeed();
    }

    /** The Workshop tab's entry appears at hiring, follows his order and goes away when he is dismissed. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theWorkshopFollowsHisApprentices(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        List<WorkshopEntry> entries = Workshop.entries(player);
        helper.assertTrue(entries.size() == 1 && entries.get(0).name().equals(apprentice.baseName())
                && entries.get(0).statusValue() == ApprenticeSmith.Status.NO_ORDER && entries.get(0).daysPaid() == 1, "listed at hiring");
        apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get());
        Workshop.report(player, apprentice);
        helper.assertTrue(Workshop.entries(player).get(0).order().equals(Optional.of(BuiltInRegistries.ITEM.getKey(ModItems.JAFFA_SWORD.get()))),
                "with his order");
        apprentice.dismiss(player);
        helper.assertTrue(Workshop.entries(player).isEmpty(), "gone when dismissed");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void hisWindowPaysADayFromTheInventory(GameTestHelper helper) {
        ServerPlayer player = smithOfLevel(helper, 50);
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(STAND)).orElseThrow();
        helper.assertFalse(apprentice.payDay(player), "no emeralds");
        player.getInventory().add(new ItemStack(Items.EMERALD, 5));
        helper.assertTrue(apprentice.payDay(player), "three emeralds from the inventory");
        helper.assertTrue(apprentice.daysPaid() == 2 && apprentice.shownDaysPaid() == 2, "two days, synced to the window");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 2, "two emeralds left");
        apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get());
        helper.assertTrue(apprentice.shownOrder().is(ModItems.JAFFA_SWORD.get()), "the order is synced");
        apprentice.cancelOrder(player);
        helper.assertTrue(apprentice.order().isEmpty() && apprentice.shownOrder().isEmpty(), "cancelled");
        apprentice.discard();
        helper.succeed();
    }
}
