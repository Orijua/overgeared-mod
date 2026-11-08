package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.TradeCategory;
import com.orijua.overgearedmod.feature.economy.TradeGoods;
import com.orijua.overgearedmod.feature.economy.TradeKey;
import com.orijua.overgearedmod.feature.economy.TradeMode;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.feature.economy.TradeService.Result;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The economy, step 2: selling and exporting equipment in the Trade tab, and its saturation. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class TradeTests {
    private static final TradeBalance B = TradeBalance.DEFAULT;
    private static final TradeKey SWORD_EPIC = new TradeKey(TradeCategory.SWORD, ItemRating.EPIC);

    private TradeTests() {
    }

    private static ItemStack rated(Item item, ItemRating rating) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ITEM_RATING, rating);
        return stack;
    }

    private static int emeralds(ServerPlayer player) {
        return player.getInventory().countItem(ModItems.SATISFY_EMERALD.get());
    }

    private static int pressure(ServerPlayer player, TradeKey key, long day) {
        return TradeService.saturation(player).pressure(key, day, B);
    }

    private static void run(Runnable body) {
        TradeBalance.withOverride(B, body);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellingAnEpicIronSwordPaysSixteenEmeralds(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.SELL, 0);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 16, "sold for 16: " + outcome);
            helper.assertTrue(player.getInventory().getItem(3).isEmpty() || player.getInventory().getItem(3).is(ModItems.SATISFY_EMERALD.get()),
                    "the sword is gone from its slot");
            helper.assertTrue(emeralds(player) == 16, "16 Satisfy emeralds, got " + emeralds(player));
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1, "sword/epic has pressure 1");
            helper.assertTrue(AccountService.won(player) == 0, "no Won");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void exportingAnEpicIronSwordPaysWon(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.EXPORT, 0);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 3800, "exported for 3800: " + outcome);
            helper.assertTrue(player.getInventory().getItem(3).isEmpty(), "the sword is gone");
            helper.assertTrue(AccountService.won(player) == 3800, "3800 Won");
            helper.assertTrue(emeralds(player) == 0, "no emeralds");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellingAndExportingShareTheCounter(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(2, rated(Items.IRON_HELMET, ItemRating.EPIC));
            helper.assertTrue(TradeService.sell(player, 0, TradeMode.SELL, 0).result() == Result.SOLD, "first sale");
            TradeService.Outcome second = TradeService.sell(player, 1, TradeMode.EXPORT, 0);
            helper.assertTrue(second.paid() == 3610, "the export pays with the pressure of the sale: " + second);
            TradeService.Outcome helmet = TradeService.sell(player, 2, TradeMode.EXPORT, 0);
            helper.assertTrue(helmet.paid() == 3800, "an epic helmet is another key: " + helmet);
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 2, "the sword key has pressure 2");
        });
        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, ServerPlayer player, int slot, ItemStack stack, Result expected, String what) {
        ItemStack before = stack.copy();
        if (slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
            player.getInventory().setItem(slot, stack);
        }
        TradeService.Outcome outcome = TradeService.sell(player, slot, TradeMode.SELL, 0);
        helper.assertTrue(outcome.result() == expected, what + ": expected " + expected + ", got " + outcome);
        helper.assertTrue(outcome.paid() == 0 && emeralds(player) == 0 && AccountService.won(player) == 0, what + ": nothing is paid");
        helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), what + ": the saturation is untouched");
        if (slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), before), what + ": the piece stays");
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void whatCannotBeSoldStaysIntact(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack loaned = rated(Items.IRON_SWORD, ItemRating.EPIC);
            loaned.set(ModDataComponents.LOANED_FOR, Overgearedmod.id("some_quest"));
            assertRefused(helper, player, 2, loaned, Result.REFUSED, "a quest loan");
            assertRefused(helper, player, 2, new ItemStack(Items.IRON_SWORD), Result.REFUSED, "no rating");
            assertRefused(helper, player, 2, rated(Items.STICK, ItemRating.EPIC), Result.REFUSED, "not damageable");
            ItemStack two = rated(Items.IRON_SWORD, ItemRating.EPIC);
            two.setCount(2);
            assertRefused(helper, player, 2, two, Result.REFUSED, "stacked");
            assertRefused(helper, player, 2, ItemStack.EMPTY, Result.EMPTY, "an empty slot");
            for (int slot : new int[]{-1, 36, 39, 40, 99}) {
                player.getInventory().setItem(Math.max(0, Math.min(slot, 35)), ItemStack.EMPTY);
                assertRefused(helper, player, slot, rated(Items.IRON_SWORD, ItemRating.EPIC), Result.INVALID_SLOT, "slot " + slot);
            }
            // The armor and the off hand really hold an epic sword and it is not sold.
            player.getInventory().armor.set(0, rated(Items.IRON_BOOTS, ItemRating.EPIC));
            player.getInventory().offhand.set(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            helper.assertTrue(TradeService.sell(player, 36, TradeMode.SELL, 0).result() == Result.INVALID_SLOT, "armor slot refused");
            helper.assertTrue(TradeService.sell(player, 40, TradeMode.SELL, 0).result() == Result.INVALID_SLOT, "off hand refused");
            helper.assertTrue(!player.getInventory().armor.get(0).isEmpty() && !player.getInventory().offhand.get(0).isEmpty(), "both stay");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void nothingSellsOutsideSatisfy(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            CapsuleTests.outsideSatisfy(helper, () -> {
                TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.SELL, 0);
                helper.assertTrue(outcome.result() == Result.OUTSIDE_SATISFY, "outside Satisfy: " + outcome);
            });
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(emeralds(player) == 0 && TradeService.saturation(player).equals(TradeSaturation.EMPTY), "nothing paid or counted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sendingTheSameSlotTwiceSellsOnce(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            helper.assertTrue(TradeService.sell(player, 3, TradeMode.EXPORT, 0).result() == Result.SOLD, "the first goes through");
            helper.assertTrue(TradeService.sell(player, 3, TradeMode.EXPORT, 0).result() == Result.EMPTY, "the second finds the slot empty");
            helper.assertTrue(AccountService.won(player) == 3800, "paid once");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1, "counted once");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aFailedPaymentGivesTheSwordBack(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.SELL, 0, (p, mode, amount) -> {
                throw new IllegalStateException("the payment broke");
            });
            helper.assertTrue(outcome.result() == Result.FAILED, "failed: " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword is back in its slot");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
            helper.assertTrue(AccountService.won(player) == 0 && emeralds(player) == 0, "nothing was paid");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aFullInventoryDropsTheRestOfTheEmeralds(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            ItemStack legendary = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            player.getInventory().setItem(5, legendary);
            TradeService.Outcome outcome = TradeService.sell(player, 5, TradeMode.SELL, 0);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 720, "720 for a legendary diamond piece: " + outcome);
            int carried = emeralds(player);
            int onTheGround = 0;
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6),
                    entity -> entity.getItem().is(ModItems.SATISFY_EMERALD.get()));
            for (ItemEntity drop : drops) {
                onTheGround += drop.getItem().getCount();
            }
            helper.assertTrue(carried + onTheGround == 720, "720 emeralds between the inventory (" + carried + ") and the ground (" + onTheGround + ")");
            helper.assertTrue(carried <= 64, "only the freed slot held emeralds: " + carried);
            drops.forEach(ItemEntity::discard);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void thePressureRecoversWithTheDays(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int i = 0; i < 3; i++) {
                player.getInventory().setItem(i, rated(Items.IRON_SWORD, ItemRating.EPIC));
                helper.assertTrue(TradeService.sell(player, i, TradeMode.EXPORT, 0).result() == Result.SOLD, "sale " + i);
            }
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 3, "pressure 3 the same day");
            helper.assertTrue(pressure(player, SWORD_EPIC, 1) == 1, "pressure 1 a day later");
            helper.assertTrue(pressure(player, SWORD_EPIC, 5) == 0, "pressure 0 five days later");
            player.getInventory().setItem(4, rated(Items.IRON_SWORD, ItemRating.EPIC));
            helper.assertTrue(TradeService.sell(player, 4, TradeMode.EXPORT, 5).paid() == 3800, "the full price again");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSaturationSurvivesDeath(GameTestHelper helper) {
        run(() -> {
            ServerPlayer old = TestSupport.fakePlayer(helper);
            old.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.sell(old, 3, TradeMode.EXPORT, 0);
            ServerPlayer fresh = TestSupport.fakePlayer(helper);
            helper.assertTrue(TradeService.saturation(fresh).equals(TradeSaturation.EMPTY), "a new player has none");
            fresh.restoreFrom(old, false);
            helper.assertTrue(TradeService.saturation(fresh).equals(TradeService.saturation(old)) && pressure(fresh, SWORD_EPIC, 0) == 1,
                    "dying does not touch the saturation");
            ModAttachments.resetAll(fresh);
            helper.assertTrue(TradeService.saturation(fresh).equals(TradeSaturation.EMPTY), "resetAll clears it");
        });
        helper.succeed();
    }

    private static void category(GameTestHelper helper, Item item, TradeCategory expected, double material) {
        ItemStack stack = rated(item, ItemRating.NORMAL);
        TradeCategory actual = TradeGoods.key(stack).category();
        helper.assertTrue(actual == expected, item + ": category " + expected + ", got " + actual);
        double multiplier = TradeGoods.material(stack, B);
        helper.assertTrue(multiplier == material, item + ": material x" + material + ", got " + multiplier);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void realItemsGetTheirCategoryAndMaterial(GameTestHelper helper) {
        run(() -> {
            category(helper, Items.IRON_SWORD, TradeCategory.SWORD, 1.0);
            category(helper, Items.WOODEN_SWORD, TradeCategory.SWORD, 0.5);
            category(helper, Items.GOLDEN_AXE, TradeCategory.AXE, 0.5);
            category(helper, Items.DIAMOND_PICKAXE, TradeCategory.PICKAXE, 2.0);
            category(helper, Items.STONE_SHOVEL, TradeCategory.SHOVEL, 1.0);
            category(helper, Items.BOW, TradeCategory.BOW, 1.0);
            category(helper, Items.SHIELD, TradeCategory.SHIELD, 1.0);
            category(helper, Items.LEATHER_BOOTS, TradeCategory.BOOTS, 0.5);
            category(helper, Items.CHAINMAIL_CHESTPLATE, TradeCategory.CHESTPLATE, 1.0);
            category(helper, Items.NETHERITE_LEGGINGS, TradeCategory.LEGGINGS, 3.0);
            category(helper, Items.IRON_HELMET, TradeCategory.HELMET, 1.0);
            category(helper, ModItems.JAFFA_SWORD.get(), TradeCategory.SWORD, 1.0);
            category(helper, ModItems.FRACASO_SWORD.get(), TradeCategory.SWORD, 3.0);
            category(helper, Items.IRON_HOE, TradeCategory.OTHER, 1.0);
            category(helper, Items.TRIDENT, TradeCategory.OTHER, 1.0);
            category(helper, ModItems.IRON_SMITHING_HAMMER.get(), TradeCategory.OTHER, 1.0);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void enhancementOnlyRaisesTheValueOfWeapons(GameTestHelper helper) {
        run(() -> {
            ItemStack armor = rated(Items.IRON_CHESTPLATE, ItemRating.EPIC);
            armor.set(ModDataComponents.ENHANCEMENT, 5);
            helper.assertTrue(TradeGoods.reference(armor, B) == 40, "armor: the enhancement adds nothing, got " + TradeGoods.reference(armor, B));
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            sword.set(ModDataComponents.ENHANCEMENT, 5);
            helper.assertTrue(TradeGoods.reference(sword, B) == 60, "sword +5 is worth 60, got " + TradeGoods.reference(sword, B));
            ItemStack legendary = rated(Items.DIAMOND_SWORD, ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            helper.assertTrue(TradeGoods.reference(legendary, B) == 1800, "diamond legendary +5 is worth 1800");
            helper.assertTrue(TradeRules.day(48000) == 2, "a game day is 24000 ticks");
        });
        helper.succeed();
    }
}
