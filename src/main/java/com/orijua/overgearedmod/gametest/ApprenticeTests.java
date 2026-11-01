package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.npc.ApprenticeContractItem;
import com.orijua.overgearedmod.npc.ApprenticeSmith;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        helper.assertTrue(barrel.getItem(1).isEmpty(), "and makes nothing");
        apprentice.discard();
        helper.succeed();
    }
}
