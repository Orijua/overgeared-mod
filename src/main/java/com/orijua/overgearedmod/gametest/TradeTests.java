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
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.feature.economy.TradeService.Result;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import com.orijua.overgearedmod.feature.notification.SystemNotificationPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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

/** The economy, step 2: selling equipment in the Trade tab, and its saturation. */
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
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0);
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == 16, "sold for 16: " + outcome);
            helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 0, "the sword is gone from the inventory");
            helper.assertTrue(emeralds(player) == 16, "16 Satisfy emeralds, got " + emeralds(player));
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1, "sword/epic has pressure 1");
            helper.assertTrue(AccountService.won(player) == 0, "no Won");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void twoSalesOfTheSameKeyLowerThePrice(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(2, rated(Items.IRON_HELMET, ItemRating.EPIC));
            TradeService.Outcome first = TradeService.sell(player, 0, 0);
            helper.assertTrue(first.result() == Result.SOLD && first.paid() == 16, "first sword, expected 16, got " + first);
            TradeService.Outcome second = TradeService.sell(player, 1, 0);
            helper.assertTrue(second.result() == Result.SOLD && second.paid() == 15, "the second sword pays with the pressure of the first, expected 15, got " + second);
            TradeService.Outcome helmet = TradeService.sell(player, 2, 0);
            helper.assertTrue(helmet.result() == Result.SOLD && helmet.paid() == 16, "an epic helmet is another key, expected 16, got " + helmet);
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 2, "the sword key has pressure 2");
        });
        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, ServerPlayer player, int slot, ItemStack stack, Result expected, String what) {
        ItemStack before = stack.copy();
        player.getInventory().setItem(slot, stack);
        TradeService.Outcome outcome = TradeService.sell(player, slot, 0);
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
                TradeService.Outcome outcome = TradeService.sell(player, slot, 0);
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
            TradeService.Outcome first = TradeService.sell(player, 0, 0);
            helper.assertTrue(first.result() == Result.SOLD && first.paid() == 16, "slot 0: expected 16, got " + first);
            TradeService.Outcome last = TradeService.sell(player, 35, 0);
            helper.assertTrue(last.result() == Result.SOLD && last.paid() == 15, "slot 35 with pressure 1: expected 15, got " + last);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void outsideSatisfyChecksBeforeTheSlot(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            CapsuleTests.outsideSatisfy(helper, () -> {
                for (int slot : new int[]{99, 3}) {
                    TradeService.Outcome outcome = TradeService.sell(player, slot, 0);
                    helper.assertTrue(outcome.result() == Result.OUTSIDE_SATISFY, "slot " + slot + " (empty or invalid): expected OUTSIDE_SATISFY, got " + outcome);
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
                TradeService.Outcome outcome = TradeService.sell(player, 3, 0);
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
            helper.assertTrue(TradeService.sell(player, 3, 0).result() == Result.SOLD, "the first goes through");
            helper.assertTrue(TradeService.sell(player, 3, 0).result() == Result.EMPTY, "the second finds the slot empty");
            helper.assertTrue(emeralds(player) == 16, "paid once");
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
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0, (p, amount) -> {
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
            TradeService.Outcome outcome = TradeService.sell(player, 5, 0);
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
                helper.assertTrue(TradeService.sell(player, i, 0).result() == Result.SOLD, "sale " + i);
            }
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 3, "pressure 3 the same day");
            helper.assertTrue(pressure(player, SWORD_EPIC, 1) == 1, "pressure 1 a day later");
            helper.assertTrue(pressure(player, SWORD_EPIC, 5) == 0, "pressure 0 five days later");
            player.getInventory().setItem(4, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome again = TradeService.sell(player, 4, 5);
            helper.assertTrue(again.result() == Result.SOLD && again.paid() == 16, "the full price again, expected 16, got " + again);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSaturationSurvivesDeath(GameTestHelper helper) {
        run(() -> {
            ServerPlayer old = TestSupport.fakePlayer(helper);
            old.getInventory().setItem(3, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.sell(old, 3, 0);
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
            category(helper, ModItems.JAFFA_SWORD.get(), TradeCategory.SWORD, 2.0);
            category(helper, ModItems.FRACASO_SWORD.get(), TradeCategory.SWORD, 0.25);
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

    private static TradeService.Outcome sellExpecting(ServerPlayer player, int slot, ItemStack seen, long price) {
        return TradeService.sell(player, slot, 0, new TradeService.Expected(seen, price));
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDifferentPieceInTheSlotIsStale(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack seen = rated(Items.IRON_SWORD, ItemRating.EPIC);
            ItemStack now = rated(Items.IRON_SWORD, ItemRating.LEGENDARY);
            player.getInventory().setItem(3, now.copy());
            TradeService.Outcome outcome = sellExpecting(player, 3, seen, 16);
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
            TradeService.Outcome outcome = sellExpecting(player, 3, sword, 16);
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
            TradeService.Outcome outcome = sellExpecting(player, 3, sword, 16);
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
            TradeService.Outcome outcome = sellExpecting(player, 3, ItemStack.EMPTY, 16);
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
            TradeSellPayload payload = new TradeSellPayload(List.of(new TradeSellPayload.Entry(3, sword.copy(), 16)));
            TradeService.Batch first = TradeService.sell(player, payload);
            helper.assertTrue(first.sold() == 1 && first.paid() == 16 && first.skipped() == 0 && first.first() == Result.SOLD, "the first sells: " + first);
            helper.assertTrue(player.getInventory().getItem(3).is(ModItems.SATISFY_EMERALD.get()) && player.getInventory().getItem(3).getCount() == 16,
                    "the 16 emeralds land in the freed slot: " + player.getInventory().getItem(3));
            TradeService.Batch second = TradeService.sell(player, payload);
            helper.assertTrue(second.sold() == 0 && second.skipped() == 1 && second.first() == Result.STALE, "the same packet again is STALE, got " + second);
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
            helper.assertTrue(TradeService.sell(player, 3, 0).result() == Result.SOLD, "the first goes through");
            ItemStack inSlot = player.getInventory().getItem(3);
            helper.assertTrue(inSlot.is(ModItems.SATISFY_EMERALD.get()) && inSlot.getCount() == 16, "16 emeralds in slot 3, got " + inSlot);
            TradeService.Outcome second = TradeService.sell(player, 3, 0);
            helper.assertTrue(second.result() == Result.REFUSED && second.refusal().orElseThrow() == TradeRules.Refusal.STACKED, "without the fingerprint: STACKED, got " + second);
            TradeService.Outcome stale = sellExpecting(player, 3, sword, 16);
            helper.assertTrue(stale.result() == Result.STALE, "with the sword fingerprint: STALE, got " + stale);
            helper.assertTrue(emeralds(player) == 16 && pressure(player, SWORD_EPIC, 0) == 1, "16 emeralds and pressure 1 in total");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSellPayloadCodecRoundTrips(GameTestHelper helper) {
        ItemStack rich = rated(Items.IRON_SWORD, ItemRating.LEGENDARY);
        rich.set(ModDataComponents.ENHANCEMENT, 4);
        for (int count : new int[]{0, 1, 36}) {
            List<TradeSellPayload.Entry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                entries.add(new TradeSellPayload.Entry(i == 0 ? Integer.MAX_VALUE : i - 1, i % 2 == 0 ? rich : ItemStack.EMPTY, 171000L + i));
            }
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            TradeSellPayload.STREAM_CODEC.encode(buffer, new TradeSellPayload(entries));
            TradeSellPayload back = TradeSellPayload.STREAM_CODEC.decode(buffer);
            helper.assertTrue(back.entries().size() == count, count + " entries, got " + back.entries().size());
            for (int i = 0; i < count; i++) {
                TradeSellPayload.Entry want = entries.get(i);
                TradeSellPayload.Entry got = back.entries().get(i);
                helper.assertTrue(got.slot() == want.slot() && got.expectedPrice() == want.expectedPrice() && ItemStack.matches(got.expected(), want.expected()),
                        "entry " + i + ": got " + got);
            }
        }
        // 37 entries written by hand (the encoder refuses them): the decoder refuses them too.
        RegistryFriendlyByteBuf tooMany = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        tooMany.writeVarInt(37);
        for (int i = 0; i < 37; i++) {
            tooMany.writeVarInt(0);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(tooMany, ItemStack.EMPTY);
            tooMany.writeVarLong(1L);
        }
        boolean refused = false;
        try {
            TradeSellPayload.STREAM_CODEC.decode(tooMany);
        } catch (RuntimeException e) {
            refused = true;
        }
        helper.assertTrue(refused, "a batch of 37 entries does not decode");
        helper.succeed();
    }

    // ---- BUG-056: cap, parts and minimum price ----

    private static void withBalance(TradeBalance balance, Runnable body) {
        TradeBalance.withOverride(balance, body);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSaleOverTheCapFailsBeforeTakingThePiece(GameTestHelper helper) {
        withBalance(TradeBalances.withMaxSellEmeralds(10), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0);
            helper.assertTrue(outcome.result() == Result.FAILED, "16 over a cap of 10 fails, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(emeralds(player) == 0 && emeraldDrops(helper, player).isEmpty(), "no emeralds anywhere");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
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
            TradeService.Outcome outcome = TradeService.sell(player, 5, 0, (p, amount) -> {
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
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0, (p, amount) -> {
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
            TradeService.Outcome outcome = TradeService.sell(player, 5, 0);
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
            helper.assertTrue(TradeService.sell(player, 3).result() == Result.SOLD, "sold");
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
            TradeService.sell(old, 3, 0);
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
                helper.assertTrue(TradeService.sell(player, i, 0).result() == Result.SOLD, "rare sale " + i);
            }
            player.getInventory().setItem(2, rated(Items.IRON_SWORD, ItemRating.EPIC));
            TradeService.Outcome epic = TradeService.sell(player, 2, 0);
            helper.assertTrue(epic.paid() == 16, "an epic sword has its own pressure 0: expected 16, got " + epic);
            player.getInventory().setItem(3, rated(Items.IRON_AXE, ItemRating.EPIC));
            TradeService.Outcome axe = TradeService.sell(player, 3, 0);
            helper.assertTrue(axe.paid() == 16, "an epic axe is another key than the epic sword: expected 16, got " + axe);
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
            TradeService.Outcome sale = TradeService.sell(player, 0, 0);
            helper.assertTrue(sale.result() == Result.SOLD && sale.paid() == 2, "a sale at the cap: expected 2, got " + sale);
            TradeService.Outcome another = TradeService.sell(player, 1, 0);
            helper.assertTrue(another.result() == Result.SOLD && another.paid() == 2, "the next one at the cap: expected 2, got " + another);
        });
        helper.succeed();
    }

    // ---- Selling several pieces at once ----

    /** The real payment (emeralds into the inventory, the rest on the ground), for tests that call the batch directly. */
    private static void pay(ServerPlayer player, long amount) {
        ItemStack pile = new ItemStack(ModItems.SATISFY_EMERALD.get(), (int) amount);
        player.getInventory().add(pile);
        if (!pile.isEmpty()) {
            player.drop(pile, false);
        }
    }

    /** What the tab would send: the piece in each slot and the price the batch pays for it in this order. */
    private static List<TradeSellPayload.Entry> entriesFor(ServerPlayer player, int... slots) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot : slots) {
            stacks.add(player.getInventory().getItem(slot).copy());
        }
        long[] prices = TradeGoods.salePrices(stacks, TradeService.saturation(player), 0, B);
        List<TradeSellPayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            entries.add(new TradeSellPayload.Entry(slots[i], stacks.get(i), prices[i]));
        }
        return entries;
    }

    private static String keyOf(java.util.Optional<net.minecraft.network.chat.Component> message) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) message.orElseThrow().getContents()).getKey();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellAllSellsEveryPieceAndPaysTheTotal(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < 3; slot++) {
                player.getInventory().setItem(slot, rated(Items.IRON_SWORD, ItemRating.EPIC));
            }
            player.getInventory().setItem(3, rated(Items.IRON_HELMET, ItemRating.EPIC));
            List<TradeSellPayload.Entry> entries = entriesFor(player, 0, 1, 2, 3);
            long[] prices = entries.stream().mapToLong(TradeSellPayload.Entry::expectedPrice).toArray();
            helper.assertTrue(TradeRules.total(prices) == 61, "16 + 15 + 14 + 16 = 61, got " + java.util.Arrays.toString(prices));
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 4 && batch.paid() == 61 && batch.skipped() == 0 && batch.first() == Result.SOLD, "all sold: " + batch);
            helper.assertTrue(emeralds(player) == 61, "61 emeralds, got " + emeralds(player));
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 3, "sword pressure 3");
            helper.assertTrue(pressure(player, new TradeKey(TradeCategory.HELMET, ItemRating.EPIC), 0) == 1, "helmet pressure 1");
            for (int slot = 1; slot < 4; slot++) {
                helper.assertTrue(player.getInventory().getItem(slot).isEmpty(), "slot " + slot + " is empty");
            }
            helper.assertTrue(TradeService.message(batch).isEmpty(), "no message for a full sale");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellAllMatchesSellingOneByOne(GameTestHelper helper) {
        run(() -> {
            ServerPlayer batch = TestSupport.fakePlayer(helper);
            ServerPlayer single = TestSupport.fakePlayer(helper);
            Item[] items = {Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_SWORD, Items.IRON_SWORD, Items.IRON_HELMET};
            for (int slot = 0; slot < items.length; slot++) {
                batch.getInventory().setItem(slot, rated(items[slot], ItemRating.EPIC));
                single.getInventory().setItem(slot, rated(items[slot], ItemRating.EPIC));
            }
            TradeService.Batch result = TradeService.sellAll(batch, entriesFor(batch, 0, 1, 2, 3, 4), 0, TradeTests::pay);
            helper.assertTrue(result.sold() == 5, "five sold: " + result);
            for (int slot = 0; slot < items.length; slot++) {
                helper.assertTrue(TradeService.sell(single, slot, 0, TradeTests::pay).result() == Result.SOLD, "single sale " + slot);
            }
            helper.assertTrue(emeralds(batch) == emeralds(single) && emeralds(batch) == result.paid(),
                    "same emeralds: batch " + emeralds(batch) + ", one by one " + emeralds(single) + ", paid " + result.paid());
            helper.assertTrue(TradeService.saturation(batch).equals(TradeService.saturation(single)), "same saturation");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aStaleEntryInTheMiddleIsSkipped(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(1, rated(Items.IRON_AXE, ItemRating.EPIC));
            player.getInventory().setItem(2, rated(Items.IRON_HELMET, ItemRating.EPIC));
            List<TradeSellPayload.Entry> entries = entriesFor(player, 0, 1, 2);
            ItemStack changed = rated(Items.IRON_AXE, ItemRating.RARE);
            player.getInventory().setItem(1, changed.copy());
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 2 && batch.skipped() == 1 && batch.first() == Result.STALE && batch.paid() == 32, "two sold, one skipped: " + batch);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(1), changed), "the changed axe stays");
            helper.assertTrue(pressure(player, new TradeKey(TradeCategory.AXE, ItemRating.EPIC), 0) == 0
                    && pressure(player, new TradeKey(TradeCategory.AXE, ItemRating.RARE), 0) == 0, "no pressure on the axe");
            helper.assertTrue("message.overgearedmod.trade_skipped".equals(keyOf(TradeService.message(batch))), "the message says some were skipped");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aStaleEntryCascadesOnlyItsKey(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            for (int slot = 0; slot < 3; slot++) {
                player.getInventory().setItem(slot, rated(Items.IRON_SWORD, ItemRating.EPIC));
            }
            List<TradeSellPayload.Entry> entries = entriesFor(player, 0, 1, 2);
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.RARE));
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 1 && batch.skipped() == 2 && batch.paid() == 16,
                    "the middle one changed, so the third saw a price (14) the server no longer has (15): " + batch);
            helper.assertTrue(player.getInventory().getItem(2).is(Items.IRON_SWORD), "the third sword stays");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1, "only the first one counted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aBatchWithRepeatedSlotsTouchesNothing(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            List<TradeSellPayload.Entry> entries = List.of(new TradeSellPayload.Entry(3, sword.copy(), 16), new TradeSellPayload.Entry(3, sword.copy(), 15));
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 0 && batch.paid() == 0 && batch.skipped() == 2 && batch.first() == Result.INVALID_SLOT, "refused whole: " + batch);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY) && emeralds(player) == 0, "nothing counted or paid");
            helper.assertTrue("message.overgearedmod.trade_stale".equals(keyOf(TradeService.message(batch))), "the stale message");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aBatchWithAnOutOfRangeSlotTouchesNothing(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(0, sword.copy());
            List<List<TradeSellPayload.Entry>> batches = new ArrayList<>();
            batches.add(List.of(new TradeSellPayload.Entry(0, sword.copy(), 16), new TradeSellPayload.Entry(36, sword.copy(), 16)));
            batches.add(List.of(new TradeSellPayload.Entry(-1, sword.copy(), 16)));
            List<TradeSellPayload.Entry> tooMany = new ArrayList<>();
            for (int i = 0; i < 37; i++) {
                tooMany.add(new TradeSellPayload.Entry(i % 36, sword.copy(), 16));
            }
            batches.add(tooMany);
            batches.add(List.of());
            for (List<TradeSellPayload.Entry> entries : batches) {
                TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
                helper.assertTrue(batch.sold() == 0 && batch.first() == Result.INVALID_SLOT, entries.size() + " entries: refused, got " + batch);
                helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), sword), "the sword stays");
                helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY) && emeralds(player) == 0, "nothing counted or paid");
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanedPieceInABatchIsNotSold(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack loaned = rated(Items.IRON_SWORD, ItemRating.EPIC);
            loaned.set(ModDataComponents.LOANED_FOR, Overgearedmod.id("some_quest"));
            player.getInventory().setItem(0, loaned.copy());
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.EPIC));
            List<TradeSellPayload.Entry> entries = List.of(
                    new TradeSellPayload.Entry(0, loaned.copy(), 16),
                    new TradeSellPayload.Entry(1, rated(Items.IRON_SWORD, ItemRating.EPIC), 16));
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 1 && batch.skipped() == 1 && batch.first() == Result.REFUSED && batch.paid() == 16, "only the normal one sells: " + batch);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), loaned), "the loaned sword stays");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 1, "only one counted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void sellAllOutsideSatisfyTouchesNothing(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack sword = rated(Items.IRON_SWORD, ItemRating.EPIC);
            player.getInventory().setItem(3, sword.copy());
            List<TradeSellPayload.Entry> entries = entriesFor(player, 3);
            CapsuleTests.outsideSatisfy(helper, () -> {
                TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeTests::pay);
                helper.assertTrue(batch.sold() == 0 && batch.skipped() == 1 && batch.first() == Result.OUTSIDE_SATISFY, "outside Satisfy: " + batch);
                helper.assertTrue("message.overgearedmod.trade_outside".equals(keyOf(TradeService.message(batch))), "the outside message");
            });
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), sword), "the sword stays");
            helper.assertTrue(emeralds(player) == 0 && TradeService.saturation(player).equals(TradeSaturation.EMPTY), "nothing paid or counted");
        });
        helper.succeed();
    }

    /** Swaps the connection of a fake player for one that keeps the system notifications it is sent. */
    private static List<SystemNotification> recordNotifications(ServerPlayer player) {
        List<SystemNotification> sent = new ArrayList<>();
        new ServerGamePacketListenerImpl(player.server, new Connection(PacketFlow.CLIENTBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(Packet<?> packet) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SystemNotificationPayload payload) {
                    sent.add(payload.notification());
                }
            }
        };
        return sent;
    }

    private static String firstLineKey(SystemNotification notification) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) notification.lines().get(0).getContents()).getKey();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void oneNotificationForTheBatch(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            List<SystemNotification> sent = recordNotifications(player);
            for (int slot = 0; slot < 3; slot++) {
                player.getInventory().setItem(slot, rated(Items.IRON_SWORD, ItemRating.EPIC));
            }
            TradeService.sellAll(player, entriesFor(player, 0, 1, 2), 0, TradeTests::pay);
            helper.assertTrue(sent.size() == 1 && sent.get(0).kind() == SystemNotification.Kind.TRADE
                    && firstLineKey(sent.get(0)).equals("notification.overgearedmod.trade.sold_many"), "one sold_many notification, got " + sent);
            sent.clear();
            player.getInventory().setItem(5, rated(Items.IRON_HELMET, ItemRating.EPIC));
            TradeService.sellAll(player, entriesFor(player, 5), 0, TradeTests::pay);
            helper.assertTrue(sent.size() == 1 && firstLineKey(sent.get(0)).equals("notification.overgearedmod.trade.sold"), "one sold notification, got " + sent);
            sent.clear();
            player.getInventory().setItem(6, rated(Items.IRON_BOOTS, ItemRating.EPIC));
            TradeService.sell(player, 6, 0);
            helper.assertTrue(sent.size() == 1 && firstLineKey(sent.get(0)).equals("notification.overgearedmod.trade.sold"), "a single sale still notifies once, got " + sent);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPartialPaymentInABatchCountsWhatWasPaid(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack legendary = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            player.getInventory().setItem(5, legendary);
            player.getInventory().setItem(6, rated(Items.IRON_SWORD, ItemRating.EPIC));
            List<TradeSellPayload.Entry> entries = entriesFor(player, 5, 6);
            int[] calls = {0};
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, (p, amount) -> {
                if (calls[0]++ == 1) {
                    throw new IllegalStateException("the second pile broke");
                }
                pay(p, amount);
            });
            helper.assertTrue(batch.sold() == 2 && batch.failed() == 1 && batch.skipped() == 0 && batch.paid() == 80 && batch.first() == Result.FAILED,
                    "64 of the legendary plus 16 of the sword: " + batch);
            helper.assertTrue(player.getInventory().countItem(ModItems.GRID_LEGENDARY_SWORD.get()) == 0, "the legendary does NOT come back");
            helper.assertTrue(emeralds(player) == 80, "80 emeralds, got " + emeralds(player));
            helper.assertTrue(pressure(player, new TradeKey(TradeCategory.SWORD, ItemRating.LEGENDARY), 0) == 1 && pressure(player, SWORD_EPIC, 0) == 1, "both counted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPartialPaymentAloneStillNotifiesWhatWasPaid(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            List<SystemNotification> sent = recordNotifications(player);
            ItemStack legendary = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            legendary.set(ModDataComponents.ENHANCEMENT, 5);
            player.getInventory().setItem(5, legendary);
            int[] calls = {0};
            TradeService.Batch batch = TradeService.sellAll(player, entriesFor(player, 5), 0, (p, amount) -> {
                if (calls[0]++ == 1) {
                    throw new IllegalStateException("the second pile broke");
                }
                pay(p, amount);
            });
            helper.assertTrue(batch.sold() == 1 && batch.paid() == 64 && batch.failed() == 1 && batch.skipped() == 0, "partial alone: " + batch);
            helper.assertTrue(sent.size() == 1 && firstLineKey(sent.get(0)).equals("notification.overgearedmod.trade.sold"), "one sold notification, got " + sent);
            helper.assertTrue(TradeService.message(batch).map(c -> ((net.minecraft.network.chat.contents.TranslatableContents) c.getContents()).getKey())
                    .orElse("").equals("message.overgearedmod.trade_failed"), "trade_failed message");
            helper.assertTrue(player.getInventory().countItem(ModItems.GRID_LEGENDARY_SWORD.get()) == 0, "the piece does not come back");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aTotalFailureFirstDoesNotStopTheNext(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, rated(Items.IRON_SWORD, ItemRating.EPIC));
            player.getInventory().setItem(1, rated(Items.IRON_HELMET, ItemRating.EPIC));
            List<TradeSellPayload.Entry> entries = entriesFor(player, 0, 1);
            long helmetPrice = entries.get(1).expectedPrice();
            int[] calls = {0};
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, (p, amount) -> {
                if (calls[0]++ == 0) {
                    throw new IllegalStateException("first call broke");
                }
                pay(p, amount);
            });
            helper.assertTrue(batch.sold() == 1 && batch.failed() == 1 && batch.skipped() == 0 && batch.paid() == helmetPrice
                    && batch.first() == Result.FAILED, "total failure then a sale: " + batch);
            helper.assertTrue(player.getInventory().getItem(0).is(Items.IRON_SWORD), "the first piece went back to its slot");
            helper.assertTrue(player.getInventory().countItem(Items.IRON_HELMET) == 0, "the second was sold");
            helper.assertTrue(pressure(player, SWORD_EPIC, 0) == 0, "no pressure for the failed sale");
        });
        helper.succeed();
    }


    @GameTest(template = TestSupport.EMPTY)
    public static void aBatchOfExactly36Sells(GameTestHelper helper) {
        run(() -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            Item[] items = {Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_BOOTS};
            ItemRating[] ratings = {ItemRating.RARE, ItemRating.EPIC, ItemRating.UNIQUE};
            int[] slots = new int[36];
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, rated(items[i % 4], ratings[(i / 4) % 3]));
                slots[i] = i;
            }
            TradeService.Batch batch = TradeService.sellAll(player, entriesFor(player, slots), 0, TradeTests::pay);
            helper.assertTrue(batch.sold() == 36 && batch.skipped() == 0 && batch.failed() == 0 && batch.first() == Result.SOLD, "36 sold: " + batch);
            helper.assertTrue(batch.paid() > 0, "paid something: " + batch);
            for (int i = 0; i < 36; i++) {
                int slot = i;
                helper.assertTrue(!player.getInventory().getItem(slot).is(items[slot % 4]), "no piece left in slot " + slot);
            }
        });
        helper.succeed();
    }
}
