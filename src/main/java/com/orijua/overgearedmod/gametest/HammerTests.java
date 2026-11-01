package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.npc.ApprenticeContractItem;
import com.orijua.overgearedmod.npc.ApprenticeSmith;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.smithing.SmithingHammers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/** Smithing hammers: what they add to a forge, their own options, and apprentices carrying them. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class HammerTests {
    private HammerTests() {
    }

    private static ItemOptionDefinition option(GameTestHelper helper, String id) {
        return Definitions.get(helper.getLevel(), ModDatapackRegistries.ITEM_OPTION, Overgearedmod.id(id)).orElseThrow();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aHammerAddsItsMaterialRatingEnhancementAndOptions(GameTestHelper helper) {
        ItemStack hammer = new ItemStack(ModItems.JAFFA_SMITHING_HAMMER.get());
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(SmithingHammers.skillBonus(registries, hammer) == 10, "a plain jaffa hammer: its material");
        hammer.set(ModDataComponents.ITEM_RATING, ItemRating.EPIC);
        hammer.set(ModDataComponents.ENHANCEMENT, 2);
        hammer.set(ModDataComponents.ITEM_OPTIONS, List.of(new RolledOption(Overgearedmod.id("hammer_precision"), 3.0f),
                new RolledOption(Overgearedmod.id("hammer_learning"), 0.1f)));
        helper.assertTrue(SmithingHammers.skillBonus(registries, hammer) == 10 + 4 + 2 + 3, "+ Epic, +2 and precision 3");
        helper.assertTrue(Math.abs(SmithingHammers.xpBonus(registries, hammer) - 0.1) < 1e-6, "learning: +10 % experience");
        helper.assertTrue(SmithingHammers.skillBonus(registries, new ItemStack(Items.IRON_SWORD)) == 0, "a sword is no hammer");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void hammersOnlyRollTheirOwnOptions(GameTestHelper helper) {
        ItemStack hammer = new ItemStack(ModItems.IRON_SMITHING_HAMMER.get());
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(option(helper, "hammer_precision").canRollOn(hammer) && !option(helper, "hammer_precision").canRollOn(sword),
                "precision is a hammer's");
        helper.assertTrue(!option(helper, "vitality").canRollOn(hammer) && option(helper, "vitality").canRollOn(sword),
                "a generic option never lands on a hammer");
        helper.succeed();
    }

    /** A hammer counts in either hand; with two, only the better one (they never add up). */
    @GameTest(template = TestSupport.EMPTY)
    public static void aHammerCountsInEitherHandButTwoDoNotAddUp(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(SmithingHammers.heldHammer(registries, player).isEmpty(), "no hammer");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SMITHING_HAMMER.get()));
        helper.assertTrue(SmithingHammers.heldHammer(registries, player).equals(Optional.of(EquipmentSlot.MAINHAND)), "in the main hand");
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.MITHRIL_SMITHING_HAMMER.get()));
        helper.assertTrue(SmithingHammers.heldHammer(registries, player).equals(Optional.of(EquipmentSlot.OFFHAND)), "the better of two");
        helper.assertTrue(SmithingHammers.heldHammerStack(registries, player).is(ModItems.MITHRIL_SMITHING_HAMMER.get()), "only that one counts");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        helper.assertTrue(SmithingHammers.heldHammer(registries, player).equals(Optional.of(EquipmentSlot.OFFHAND)), "in the off hand alone");
        helper.succeed();
    }

    /** He takes the best hammer, forges with it (wearing it) and hands it back. */
    @GameTest(template = TestSupport.EMPTY)
    public static void anApprenticeCarriesHisMastersBestHammer(GameTestHelper helper) {
        BlockPos anvil = new BlockPos(1, 1, 1);
        helper.setBlock(anvil, ModBlocks.SMITHING_ANVIL.get());
        helper.setBlock(anvil.west(), Blocks.CHEST);
        helper.setBlock(anvil.east(), Blocks.BARREL);
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ProgressionService.set(player, PlayerStats.DEFAULT.withSmithingXp(50 * 50 * 10));
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, helper.absolutePos(new BlockPos(1, 1, 0))).orElseThrow();

        helper.assertFalse(apprentice.giveHammer(player), "no hammer to give");
        player.getInventory().add(new ItemStack(ModItems.IRON_SMITHING_HAMMER.get()));
        player.getInventory().add(new ItemStack(ModItems.MITHRIL_SMITHING_HAMMER.get()));
        helper.assertTrue(apprentice.giveHammer(player) && apprentice.getMainHandItem().is(ModItems.MITHRIL_SMITHING_HAMMER.get()), "the best one");
        helper.assertTrue(player.getInventory().countItem(ModItems.MITHRIL_SMITHING_HAMMER.get()) == 0, "it left the inventory");

        apprentice.takeOrder(player, ModItems.JAFFA_SWORD.get());
        Container chest = helper.getBlockEntity(anvil.west());
        chest.setItem(0, new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
        chest.setItem(1, new ItemStack(Items.STICK));
        apprentice.forgeOne(0.99);
        helper.assertTrue(apprentice.getMainHandItem().getDamageValue() == 1, "a piece wears it");

        apprentice.takeHammer(player);
        helper.assertTrue(apprentice.getMainHandItem().isEmpty() && player.getInventory().countItem(ModItems.MITHRIL_SMITHING_HAMMER.get()) == 1,
                "back to his master");
        apprentice.discard();
        helper.succeed();
    }
}
