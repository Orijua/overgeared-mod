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
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
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
            helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 0, "the sword is gone from the inventory");
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
            helper.assertTrue(second.result() == Result.SOLD && second.paid() == 3610, "the export pays with the pressure of the sale, expected 3610, got " + second);
            TradeService.Outcome helmet = TradeService.sell(player, 2, TradeMode.EXPORT, 0);
            helper.assertTrue(helmet.result() == Result.SOLD && helmet.paid() == 3800, "an epic helmet is another key, expected 3800, got " + helmet);
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 2, "the sword key has pressure 2");
        });
        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, ServerPlayer player, int slot, ItemStack stack, Result expected, String what) {
        ItemStack before = stack.copy();
        player.getInventory().setItem(slot, stack);
        TradeService.Outcome outcome = TradeService.sell(player, slot, TradeMode.SELL, 0);
        helper.assertTrue(outcome.result() == expected, what + ": expected " + expected + ", got " + outcome);
        helper.assertTrue(outcome.paid() == 0 && emeralds(player) == 0 && AccountService.won(player) == 0, what + ": nothing is paid");
        helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), what + ": the saturation is untouched");
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), before), what + ": the piece stays");
        player.getInventory().setItem(slot, ItemStack.EMPTY);
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
        });
        helper.succeed();
    }

    private static List<ItemStack> snapshot(Inventory inventory) {
        List<ItemStack> copy = new ArrayList<>();
        for (List<ItemStack> list : List.of(inventory.items, inventory.armor, inventory.offhand)) {
            for (ItemStack stack : list) {
                copy.add(stack.copy());
            }
        }
        return copy;
    }

    private static void assertUnchanged(GameTestHelper helper, List<ItemStack> before, List<ItemStack> after, String what) {
        helper.assertTrue(before.size() == after.size(), what + ": same number of slots");
        for (int i = 0; i < before.size(); i++) {
            helper.assertTrue(ItemStack.matches(before.get(i), after.get(i)), what + ": snapshot slot " + i + " changed from " + before.get(i) + " to " + after.get(i));
        }
    }

    private static List<ItemEntity> emeraldDrops(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6),
                entity -> entity.getItem().is(ModItems.SATISFY_EMERALD.get()));
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void invalidSlotsKeepEverythingIntact(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            Inventory inventory = player.getInventory();
            inventory.setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            inventory.armor.set(0, rated(Items.IRON_BOOTS, ItemRating.EPIC));
            inventory.armor.set(1, rated(Items.IRON_LEGGINGS, ItemRating.EPIC));
            inventory.armor.set(2, rated(Items.IRON_CHESTPLATE, ItemRating.EPIC));
            inventory.armor.set(3, rated(Items.IRON_HELMET, ItemRating.EPIC));
            inventory.offhand.set(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            List<ItemStack> before = snapshot(inventory);
            for (int slot : new int[]{-1, 36, 37, 38, 39, 40, 41, 99, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                TradeService.Outcome outcome = TradeService.sell(player, slot, TradeMode.SELL, 0);
                helper.assertTrue(outcome.result() == Result.INVALID_SLOT, "slot " + slot + ": expected INVALID_SLOT, got " + outcome);
            }
            assertUnchanged(helper, before, snapshot(inventory), "invalid slots");
            helper.assertTrue(emeralds(player) == 0 && AccountService.won(player) == 0, "nothing paid");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellingFromBoundarySlotsWorks(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(35, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome first = TradeService.sell(player, 0, TradeMode.SELL, 0);
            helper.assertTrue(first.result() == Result.SOLD && first.paid() == 16, "slot 0: expected 16, got " + first);
            TradeService.Outcome last = TradeService.sell(player, 35, TradeMode.SELL, 0);
            helper.assertTrue(last.result() == Result.SOLD && last.paid() == 15, "slot 35 with pressure 1: expected 15, got " + last);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void outsideSatisfyChecksBeforeTheSlot(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            CapsuleTests.outsideSatisfy(helper, () -> {
                for (TradeMode mode : TradeMode.values()) {
                    for (int slot : new int[]{99, 3}) {
                        TradeService.Outcome outcome = TradeService.sell(player, slot, mode, 0);
                        helper.assertTrue(outcome.result() == Result.OUTSIDE_SATISFY, mode + " slot " + slot + " (empty or invalid): expected OUTSIDE_SATISFY, got " + outcome);
                    }
                }
            });
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
            List<ItemEntity> drops = emeraldDrops(helper, player);
            for (ItemEntity drop : drops) {
                onTheGround += drop.getItem().getCount();
                helper.assertTrue(drop.getItem().getCount() <= 64, "each pile is at most 64: " + drop.getItem().getCount());
            }
            helper.assertTrue(carried == 64, "only the freed slot held emeralds, expected 64, got " + carried);
            helper.assertTrue(onTheGround == 656, "656 on the ground, got " + onTheGround);
            helper.assertTrue(drops.size() == 11, "11 piles on the ground, got " + drops.size());
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
            TradeService.Outcome again = TradeService.sell(player, 4, TradeMode.EXPORT, 5);
            helper.assertTrue(again.result() == Result.SOLD && again.paid() == 3800, "the full price again, expected 3800, got " + again);
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
        });
        helper.succeed();
    }

    // ---- BUG-055: the piece and the price the player saw ----

    private static TradeService.Outcome sellExpecting(ServerPlayer player, int slot, TradeMode mode, ItemStack seen, long price) {
        return TradeService.sell(player, slot, mode, 0, new TradeService.Expected(seen, price));
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDifferentPieceInTheSlotIsStale(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack seen = rated(Items.IRON_SWORD, ItemRating.EPIC);
            ItemStack now = rated(Items.IRON_SWORD, ItemRating.LEGENDARY);
            player.getInventory().setItem(3, now.copy());
            TradeService.Outcome outcome = sellExpecting(player, 3, TradeMode.SELL, seen, 16);
            helper.assertTrue(outcome.result() == Result.STALE, "expected STALE, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), now), "the legendary stays");
            helper.assertTrue(emeralds(player) == 0 && AccountService.won(player) == 0, "nothing paid");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aChangedPriceIsStale(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.setData(ModAttachments.TRADE_SATURATION.get(), TradeSaturation.EMPTY.recordSale(SWORD_EPIC, 0, B));
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = sellExpecting(player, 3, TradeMode.SELL, sword, 16);
            helper.assertTrue(outcome.result() == Result.STALE, "the real price is 15, not the 16 seen: " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1 && emeralds(player) == 0, "nothing moved");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSeenPieceAtTheSeenPriceSells(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = sellExpecting(player, 3, TradeMode.SELL, sword, 16);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 16, "expected SOLD 16, got " + outcome);
            helper.assertTrue(emeralds(player) == 16, "16 emeralds, got " + emeralds(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anEmptyExpectedStackIsStale(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = sellExpecting(player, 3, TradeMode.SELL, ItemStack.EMPTY, 16);
            helper.assertTrue(outcome.result() == Result.STALE, "expected STALE, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void thePacketPathSellsOnceAndTheRepeatIsStale(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < 3; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeSellPayload payload = new TradeSellPayload(3, TradeMode.SELL, sword.copy(), 16);
            TradeService.Outcome first = TradeService.sell(player, payload);
            helper.assertTrue(first.result() == Result.SOLD && first.paid() == 16, "the first sells: " + first);
            helper.assertTrue(player.getInventory().getItem(3).is(ModItems.SATISFY_EMERALD.get()) && player.getInventory().getItem(3).getCount() == 16,
                    "the 16 emeralds land in the freed slot: " + player.getInventory().getItem(3));
            TradeService.Outcome second = TradeService.sell(player, payload);
            helper.assertTrue(second.result() == Result.STALE, "the same packet again is STALE, got " + second);
            helper.assertTrue(emeralds(player) == 16 && pressure(player, SWORD_EPIC, TradeRules.day(player.serverLevel().getGameTime())) == 1, "paid and counted once");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sendingTheSameSlotTwiceInSellMode(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < 3; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            helper.assertTrue(TradeService.sell(player, 3, TradeMode.SELL, 0).result() == Result.SOLD, "the first goes through");
            ItemStack inSlot = player.getInventory().getItem(3);
            helper.assertTrue(inSlot.is(ModItems.SATISFY_EMERALD.get()) && inSlot.getCount() == 16, "16 emeralds in slot 3, got " + inSlot);
            TradeService.Outcome second = TradeService.sell(player, 3, TradeMode.SELL, 0);
            helper.assertTrue(second.result() == Result.REFUSED && second.refusal().orElseThrow() == TradeRules.Refusal.STACKED, "without the fingerprint: STACKED, got " + second);
            TradeService.Outcome stale = sellExpecting(player, 3, TradeMode.SELL, sword, 16);
            helper.assertTrue(stale.result() == Result.STALE, "with the sword fingerprint: STALE, got " + stale);
            helper.assertTrue(emeralds(player) == 16 && pressure(player, SWORD_EPIC, 0) == 1, "16 emeralds and pressure 1 in total");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSellPayloadCodecRoundTrips(GameTestHelper helper) {
        ItemStack rich = rated(Items.IRON_SWORD, ItemRating.LEGENDARY);
        rich.set(ModDataComponents.ENHANCEMENT, 4);
        for (int slot : new int[]{-1, 0, 35, Integer.MAX_VALUE}) {
            for (ItemStack stack : new ItemStack[]{rich, ItemStack.EMPTY}) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
                TradeSellPayload.STREAM_CODEC.encode(buffer, new TradeSellPayload(slot, TradeMode.EXPORT, stack, 171000L));
                TradeSellPayload back = TradeSellPayload.STREAM_CODEC.decode(buffer);
                helper.assertTrue(back.slot() == slot && back.mode() == TradeMode.EXPORT && back.expectedPrice() == 171000L
                        && ItemStack.matches(back.expected(), stack), "slot " + slot + " with " + stack + ": got " + back);
            }
        }
        // A bogus mode id clamps to a real mode (the price fingerprint then catches it).
        RegistryFriendlyByteBuf bogus = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        bogus.writeVarInt(3);
        bogus.writeVarInt(99);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(bogus, rich);
        bogus.writeVarLong(5L);
        TradeSellPayload clamped = TradeSellPayload.STREAM_CODEC.decode(bogus);
        helper.assertTrue(clamped.mode() == TradeMode.EXPORT && clamped.slot() == 3, "a bogus mode id clamps to EXPORT, got " + clamped);
        helper.succeed();
    }

    // ---- BUG-056: cap, parts and minimum price ----

    private static void withBalance(TradeBalance balance, Runnable body) {
        TradeBalance.withOverride(balance, body);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void exportAtZeroFailsAndKeepsThePiece(GameTestHelper helper) {
        withBalance(TradeBalances.withWonPerReference(0), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.EXPORT, 0);
            helper.assertTrue(outcome.result() == Result.FAILED, "an export for 0 fails, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 0 && AccountService.won(player) == 0, "no pressure, no Won");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSaleOverTheCapFailsBeforeTakingThePiece(GameTestHelper helper) {
        withBalance(TradeBalances.withMaxSellEmeralds(10), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.SELL, 0);
            helper.assertTrue(outcome.result() == Result.FAILED, "16 over a cap of 10 fails, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(emeralds(player) == 0 && emeraldDrops(helper, player).isEmpty(), "no emeralds anywhere");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aFailedExportPaymentGivesTheSwordBack(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.EXPORT, 0, (p, mode, amount) -> {
                throw new IllegalStateException("the deposit broke");
            });
            helper.assertTrue(outcome.result() == Result.FAILED, "failed: " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword is back");
            helper.assertTrue(AccountService.won(player) == 0 && TradeService.saturation(player).equals(TradeSaturation.EMPTY), "no Won, no saturation");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPartialPaymentDoesNotDuplicate(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack legendary = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            player.getInventory().setItem(5, legendary);
            int[] calls = {0};
            TradeService.Outcome outcome = TradeService.sell(player, 5, TradeMode.SELL, 0, (p, mode, amount) -> {
                if (calls[0]++ == 1) {
                    throw new IllegalStateException("the second pile broke");
                }
                ItemStack pile = new ItemStack(ModItems.SATISFY_EMERALD.get(), (int) amount);
                p.getInventory().add(pile);
                if (!pile.isEmpty()) {
                    p.drop(pile, false);
                }
            });
            helper.assertTrue(outcome.result() == Result.FAILED && outcome.paid() == 64, "FAILED having paid 64, got " + outcome);
            helper.assertTrue(player.getInventory().countItem(ModItems.GRID_LEGENDARY_SWORD.get()) == 0, "the piece does NOT come back");
            int total = emeralds(player);
            List<ItemEntity> drops = emeraldDrops(helper, player);
            for (ItemEntity drop : drops) {
                total += drop.getItem().getCount();
                drop.discard();
            }
            helper.assertTrue(total == 64, "64 emeralds in total, got " + total);
            helper.assertTrue(pressure(player, new TradeKey(TradeCategory.SWORD, ItemRating.LEGENDARY), 0) == 1, "the sale is counted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theFirstPartFailingRestoresWithoutOverwriting(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, TradeMode.SELL, 0, (p, mode, amount) -> {
                p.getInventory().setItem(3, new ItemStack(Items.DIRT, 5));
                throw new IllegalStateException("broke after something took the slot");
            });
            helper.assertTrue(outcome.result() == Result.FAILED && outcome.paid() == 0, "failed with nothing paid: " + outcome);
            ItemStack inSlot = player.getInventory().getItem(3);
            helper.assertTrue(inSlot.is(Items.DIRT) && inSlot.getCount() == 5, "what took the slot stays: " + inSlot);
            helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 1, "the sword is somewhere else in the inventory");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
        });
        helper.succeed();
    }

    // ---- BUG-060: other cases ----

    @GameTest(template = TestSupport.EMPTY)
    public static void fullInventoryWithPartialEmeraldStack(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            player.getInventory().setItem(0, new ItemStack(ModItems.SATISFY_EMERALD.get(), 30));
            ItemStack legendary = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            player.getInventory().setItem(5, legendary);
            TradeService.Outcome outcome = TradeService.sell(player, 5, TradeMode.SELL, 0);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 720, "sold for 720: " + outcome);
            int carried = emeralds(player);
            helper.assertTrue(carried == 128, "128 in the inventory (slot 0 completed and the freed slot), got " + carried);
            int onTheGround = 0;
            List<ItemEntity> drops = emeraldDrops(helper, player);
            for (ItemEntity drop : drops) {
                onTheGround += drop.getItem().getCount();
                helper.assertTrue(drop.getItem().getCount() <= 64, "each pile is at most 64");
            }
            helper.assertTrue(onTheGround == 622, "622 on the ground, got " + onTheGround);
            drops.forEach(ItemEntity::discard);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellUsesTheServerClock(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            helper.assertTrue(TradeService.sell(player, 3, TradeMode.EXPORT).result() == Result.SOLD, "sold");
            long expected = TradeRules.day(player.serverLevel().getGameTime());
            TradeSaturation.Entry entry = TradeService.saturation(player).entries().get(SWORD_EPIC);
            helper.assertTrue(entry != null && entry.lastDay() == expected, "the stamp is the server day " + expected + ", got " + entry);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSaturationSurvivesSaveAndLoad(GameTestHelper helper) {
        run(() -> {
            ServerPlayer old = TestSupport.fakePlayer(helper);
            old.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.sell(old, 3, TradeMode.EXPORT, 0);
            CompoundTag tag = old.saveWithoutId(new CompoundTag());
            ServerPlayer fresh = TestSupport.fakePlayer(helper);
            helper.assertTrue(TradeService.saturation(fresh).equals(TradeSaturation.EMPTY), "a new player has none");
            fresh.load(tag);
            helper.assertTrue(TradeService.saturation(fresh).equals(TradeService.saturation(old)) && pressure(fresh, SWORD_EPIC, 0) == 1,
                    "after load: " + TradeService.saturation(fresh));
            ServerPlayer moved = TestSupport.fakePlayer(helper);
            moved.restoreFrom(old, true);
            helper.assertTrue(TradeService.saturation(moved).equals(TradeService.saturation(old)), "a dimension change keeps it");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void ratingAndCategoryChangeTheKey(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int i = 0; i < 2; i++) {
                player.getInventory().setItem(i, rated(Items.IRON_SWORD, ItemRating.RARE));
                helper.assertTrue(TradeService.sell(player, i, TradeMode.EXPORT, 0).result() == Result.SOLD, "rare sale " + i);
            }
            player.getInventory().setItem(2, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome epic = TradeService.sell(player, 2, TradeMode.EXPORT, 0);
            helper.assertTrue(epic.paid() == 3800, "an epic sword has its own pressure 0: expected 3800, got " + epic);
            player.getInventory().setItem(3, rated(Items.IRON_AXE, ItemRating.EPIC));
            TradeService.Outcome axe = TradeService.sell(player, 3, TradeMode.EXPORT, 0);
            helper.assertTrue(axe.paid() == 3800, "an epic axe is another key than the epic sword: expected 3800, got " + axe);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void modItemsUseOtherMaterial(GameTestHelper helper) {
        // Limit: a tier of another mod or foreign armor cannot be tested without registering an item after the registry froze.
        TradeBalance other = TradeBalances.withOtherMaterial(7.0);
        for (Item item : new Item[]{Items.BOW, Items.SHIELD, Items.TRIDENT, ModItems.IRON_SMITHING_HAMMER.get()}) {
            double material = TradeGoods.material(rated(item, ItemRating.NORMAL), other);
            helper.assertTrue(material == 7.0, item + ": expected 7.0, got " + material);
        }
        double hoe = TradeGoods.material(rated(Items.IRON_HOE, ItemRating.NORMAL), other);
        helper.assertTrue(hoe == 1.0, "an iron hoe is still iron: expected 1.0, got " + hoe);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSaleAtTheCapPaysTheFloor(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            TradeSaturation saturated = TradeSaturation.EMPTY;
            for (int i = 0; i < 40; i++) {
                saturated = saturated.recordSale(SWORD_EPIC, 0, B);
            }
            player.setData(ModAttachments.TRADE_SATURATION.get(), saturated);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome sale = TradeService.sell(player, 0, TradeMode.SELL, 0);
            helper.assertTrue(sale.result() == Result.SOLD && sale.paid() == 2, "SELL at the cap: expected 2, got " + sale);
            TradeService.Outcome export = TradeService.sell(player, 1, TradeMode.EXPORT, 0);
            helper.assertTrue(export.result() == Result.SOLD && export.paid() == 570, "EXPORT at the cap: expected 570, got " + export);
        });
        helper.succeed();
    }
}
