package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.economy.TradeGoods;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.feature.economy.TradeService.Result;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** What a piece is worth beyond its rating: the durability left and the multiplier of a single object. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class TradeValueTests {
    private static final TradeBalance B = TradeBalance.DEFAULT;

    private TradeValueTests() {
    }

    private static ItemStack rated(Item item, ItemRating rating) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ITEM_RATING, rating);
        return stack;
    }

    private static ItemStack damaged(Item item, ItemRating rating, int damage) {
        ItemStack stack = rated(item, rating);
        stack.setDamageValue(damage);
        return stack;
    }

    private static int emeralds(ServerPlayer player) {
        return player.getInventory().countItem(ModItems.SATISFY_EMERALD.get());
    }

    private static void pay(ServerPlayer player, long amount) {
        ItemStack pile = new ItemStack(ModItems.SATISFY_EMERALD.get(), (int) amount);
        player.getInventory().add(pile);
        if (!pile.isEmpty()) {
            player.drop(pile, false);
        }
    }

    // ---- The durability left

    @GameTest(template = TestSupport.EMPTY)
    public static void theDurabilityLeftLowersTheReference(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            long fresh = TradeGoods.reference(damaged(Items.IRON_SWORD, ItemRating.RARE, 0), B);
            long half = TradeGoods.reference(damaged(Items.IRON_SWORD, ItemRating.RARE, 125), B);
            long nearlyBroken = TradeGoods.reference(damaged(Items.IRON_SWORD, ItemRating.RARE, 249), B);
            helper.assertTrue(fresh == 12, "a new rare iron sword is worth 12, got " + fresh);
            helper.assertTrue(half == 7, "at half durability it is worth 7, got " + half);
            helper.assertTrue(nearlyBroken == 3, "nearly broken it is worth 3, got " + nearlyBroken);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSaleWithTheSeenPricePaysWhatTheBatchRuleSays(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack worn = damaged(Items.IRON_SWORD, ItemRating.EPIC, 125);
            player.getInventory().setItem(3, worn.copy());
            long seen = TradeGoods.salePrices(List.of(worn), TradeService.saturation(player), 0, B)[0];
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0, new TradeService.Expected(worn, seen));
            helper.assertTrue(outcome.result() == Result.SOLD && outcome.paid() == seen, "paid the seen price " + seen + ": " + outcome);
            helper.assertTrue(emeralds(player) == seen, "the emeralds are the seen price, got " + emeralds(player));
            helper.assertTrue(seen < 16, "a worn epic sword pays less than a new one (16), got " + seen);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPieceSeenNewButWornOnTheServerIsStale(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            ItemStack seen = rated(Items.IRON_SWORD, ItemRating.EPIC);
            ItemStack worn = damaged(Items.IRON_SWORD, ItemRating.EPIC, 125);
            player.getInventory().setItem(3, worn.copy());
            TradeService.Outcome outcome = TradeService.sell(player, 3, 0, new TradeService.Expected(seen, 16));
            helper.assertTrue(outcome.result() == Result.STALE, "expected STALE, got " + outcome);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(3), worn), "the worn sword stays");
            helper.assertTrue(emeralds(player) == 0, "nothing paid");
            helper.assertTrue(TradeService.saturation(player).equals(TradeSaturation.EMPTY), "the saturation is untouched");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aBatchWithAWornPieceTotalsWhatSellAllPays(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            player.getInventory().setItem(0, damaged(Items.IRON_SWORD, ItemRating.EPIC, 200));
            player.getInventory().setItem(1, rated(Items.IRON_SWORD, ItemRating.EPIC));
            List<ItemStack> stacks = List.of(player.getInventory().getItem(0).copy(), player.getInventory().getItem(1).copy());
            long[] prices = TradeGoods.salePrices(stacks, TradeService.saturation(player), 0, B);
            List<TradeSellPayload.Entry> entries = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                entries.add(new TradeSellPayload.Entry(i, stacks.get(i), prices[i]));
            }
            long clientTotal = TradeRules.total(prices);
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeValueTests::pay);
            helper.assertTrue(batch.sold() == 2 && batch.paid() == clientTotal, "the client total " + clientTotal + " is what is paid: " + batch);
            helper.assertTrue(emeralds(player) == clientTotal, "emeralds " + emeralds(player));
        });
        helper.succeed();
    }
}
