package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.OfferEntry;
import com.orijua.overgearedmod.feature.economy.OfferRules;
import com.orijua.overgearedmod.feature.economy.TerminalShop;
import com.orijua.overgearedmod.feature.economy.TradeGoods;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.feature.economy.TradeService.Result;
import com.orijua.overgearedmod.feature.equipment.EquipmentBonuses;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            // Epic iron sword, 125 of 250 damage: reference 40 x (0,25 + 0,75 x 0,5) = 25, sold at 40 % = 10.
            helper.assertTrue(seen == 10, "a half-worn epic sword pays 10 (25 x 0,4), got " + seen);
            helper.assertTrue(seen < 16, "and less than a new one (16)");
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
            // The worn one (200 of 250 damage): reference 40 x (0,25 + 0,75 x 0,2) = 16, sold at 40 % = 6 (6,4). It leaves pressure 1,
            // so the new one of the same key pays 40 x 0,4 x 0,95 = 15 (15,2).
            helper.assertTrue(TradeGoods.reference(stacks.get(0), B) == 16, "the worn sword is worth 16: " + TradeGoods.reference(stacks.get(0), B));
            helper.assertTrue(prices.length == 2 && prices[0] == 6 && prices[1] == 15, "6 for the worn one, 15 for the new: " + java.util.Arrays.toString(prices));
            List<TradeSellPayload.Entry> entries = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                entries.add(new TradeSellPayload.Entry(i, stacks.get(i), prices[i]));
            }
            long clientTotal = TradeRules.total(prices);
            helper.assertTrue(clientTotal == 21, "the total is 21: " + clientTotal);
            TradeService.Batch batch = TradeService.sellAll(player, entries, 0, TradeValueTests::pay);
            helper.assertTrue(batch.sold() == 2 && batch.paid() == clientTotal, "the client total " + clientTotal + " is what is paid: " + batch);
            helper.assertTrue(emeralds(player) == clientTotal, "emeralds " + emeralds(player));
        });
        helper.succeed();
    }

    // ---- The multiplier of a single object

    @GameTest(template = TestSupport.EMPTY)
    public static void theUniqueObjectsHaveTheirOwnMultiplier(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            long fracaso = TradeGoods.reference(rated(ModItems.FRACASO_SWORD.get(), ItemRating.LEGENDARY), B);
            helper.assertTrue(fracaso == 150, "a legendary Fracaso is worth 150 (x0.25, not netherite), got " + fracaso);
            long jaffa = TradeGoods.reference(rated(ModItems.JAFFA_SWORD.get(), ItemRating.RARE), B);
            helper.assertTrue(jaffa == 24, "a rare Jaffa sword is worth 24 (x2), got " + jaffa);
            long jaffaHammer = TradeGoods.reference(rated(ModItems.JAFFA_SMITHING_HAMMER.get(), ItemRating.RARE), B);
            helper.assertTrue(jaffaHammer == 24, "a rare Jaffa hammer is worth 24 (x2), got " + jaffaHammer);
            long mithrilHammer = TradeGoods.reference(rated(ModItems.MITHRIL_SMITHING_HAMMER.get(), ItemRating.RARE), B);
            helper.assertTrue(mithrilHammer == 36, "a rare mithril hammer is worth 36 (x3), got " + mithrilHammer);
        });
        helper.succeed();
    }

    /** The durability left multiplies on top of the multiplier of a single object, and a piece without exception keeps its material. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theDurabilityMultipliesOnTopOfTheExceptionAndAPieceWithoutOneKeepsItsMaterial(GameTestHelper helper) {
        TradeBalance.withOverride(B, () -> {
            ItemStack brokenFracaso = rated(ModItems.FRACASO_SWORD.get(), ItemRating.LEGENDARY);
            brokenFracaso.setDamageValue(brokenFracaso.getMaxDamage());
            helper.assertTrue(brokenFracaso.getDamageValue() > 0, "the Fracaso has durability to lose");
            long broken = TradeGoods.reference(brokenFracaso, B);
            helper.assertTrue(broken == 37, "a broken legendary Fracaso: 600 x 0,25 x 0,25 = 37, got " + broken);
            ItemStack worn = rated(ModItems.FRACASO_SWORD.get(), ItemRating.LEGENDARY);
            worn.setDamageValue(worn.getMaxDamage() / 2);
            long wornPrice = TradeGoods.reference(worn, B);
            helper.assertTrue(wornPrice > broken && wornPrice < 150, "a half-worn one is between the broken (37) and the new (150): " + wornPrice);

            ItemStack halfJaffa = damaged(ModItems.JAFFA_SWORD.get(), ItemRating.RARE, 125);
            helper.assertTrue(halfJaffa.getMaxDamage() == 250, "the Jaffa sword is iron-sized: " + halfJaffa.getMaxDamage());
            long jaffa = TradeGoods.reference(halfJaffa, B);
            helper.assertTrue(jaffa == 15, "a half-worn rare Jaffa sword: 12 x 2 x 0,625 = 15, got " + jaffa);
            ItemStack brokenJaffa = rated(ModItems.JAFFA_SWORD.get(), ItemRating.RARE);
            brokenJaffa.setDamageValue(brokenJaffa.getMaxDamage());
            helper.assertTrue(TradeGoods.reference(brokenJaffa, B) == 6, "a broken rare Jaffa sword: 12 x 2 x 0,25 = 6, got " + TradeGoods.reference(brokenJaffa, B));

            // Grid's legendary sword has no exception: it is still weighed as diamond (x2) and the damage multiplies on it.
            ItemStack grid = rated(ModItems.GRID_LEGENDARY_SWORD.get(), ItemRating.LEGENDARY);
            helper.assertFalse(B.itemMultiplier().containsKey(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(grid.getItem())),
                    "no exception for it");
            helper.assertTrue(TradeGoods.material(grid, B) == 2.0, "it keeps the diamond multiplier: " + TradeGoods.material(grid, B));
            helper.assertTrue(TradeGoods.reference(grid, B) == 1200, "new: 600 x 2 = 1200, got " + TradeGoods.reference(grid, B));
            grid.setDamageValue(grid.getMaxDamage());
            helper.assertTrue(TradeGoods.reference(grid, B) == 300, "broken: 1200 x 0,25 = 300, got " + TradeGoods.reference(grid, B));
        });
        helper.succeed();
    }

    private static final Map<ResourceLocation, Double> DOUBLE_IRON_SWORD = Map.of(ResourceLocation.withDefaultNamespace("iron_sword"), 2.0);

    /** The real world's player with a stored Satisfy character (one piece of dirt in its inventory) and a lot of Won. */
    private static ServerPlayer playerWithStoredCharacter(GameTestHelper helper, long won) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIRT));
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, CharacterSnapshot.capture(player)));
        player.getInventory().clearContent();
        AccountService.setWon(player, won);
        return player;
    }

    /** The position of the first piece of the offer that is an iron sword (the offer is built of nothing else). */
    private static int ironSwordPosition(GameTestHelper helper, List<OfferEntry> offer) {
        helper.assertFalse(offer.isEmpty(), "the offer is not empty");
        for (int i = 0; i < offer.size(); i++) {
            if (offer.get(i).stack().is(Items.IRON_SWORD)) {
                return i;
            }
        }
        helper.fail("no iron sword in the offer");
        return -1;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anExceptionRegeneratesTheOfferAtTheNewPriceAndTheOldPriceIsStale(GameTestHelper helper) {
        TerminalBalance ironSwords = TerminalBalance.DEFAULT.withOffer(6, ItemRating.RARE, List.of(ResourceLocation.withDefaultNamespace("iron_sword")));
        TradeBalance exception = TradeBalances.withItemMultipliers(DOUBLE_IRON_SWORD);
        long startingWon = 10_000_000L;
        ServerPlayer player = playerWithStoredCharacter(helper, startingWon);
        CharacterVault vaultBefore = player.getData(ModAttachments.CHARACTER_VAULT.get());
        CapsuleTests.outsideSatisfy(helper, () -> {
            List<OfferEntry> offerBefore = TerminalShop.offer(helper.getLevel(), 7L, ironSwords, B);
            List<OfferEntry> offerAfter = TerminalShop.offer(helper.getLevel(), 7L, ironSwords, exception);
            int position = ironSwordPosition(helper, offerBefore);
            OfferEntry before = offerBefore.get(position);
            OfferEntry after = offerAfter.get(position);
            helper.assertTrue(ItemStack.matches(before.stack(), after.stack()), "the same piece");
            helper.assertTrue(after.price() > before.price(), "the exception raises the price: " + before.price() + " -> " + after.price());
            helper.assertTrue(after.price() == OfferRules.price(TradeGoods.reference(after.stack(), exception),
                    ironSwords.buyMultiplier(), ironSwords.wonPerEmerald()), "the price comes from the one reference");

            helper.assertTrue(TerminalShop.buyOffer(player, position, before.stack(), before.price(), 7L, ironSwords, exception).result()
                    == TerminalShop.Result.STALE, "the price seen before the change is stale");
            helper.assertTrue(AccountService.won(player) == startingWon, "the stale attempt charged nothing: " + AccountService.won(player));
            helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vaultBefore), "and the stored character is intact");

            TerminalShop.Outcome bought = TerminalShop.buyOffer(player, position, after.stack(), after.price(), 7L, ironSwords, exception);
            helper.assertTrue(bought.result() == TerminalShop.Result.OK, "the new price buys: " + bought);
            helper.assertTrue(bought.won() == after.price() && AccountService.won(player) == startingWon - after.price(),
                    "exactly the new price is charged: " + AccountService.won(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDefaultOfferDoesNotChangeWithTheExceptions(GameTestHelper helper) {
        TradeBalance plain = TradeBalances.withItemMultipliers(Map.of());
        for (long day : new long[]{0L, 3L, 77L}) {
            for (OfferEntry entry : TerminalShop.offer(helper.getLevel(), day, TerminalBalance.DEFAULT, B)) {
                ItemStack stack = entry.stack();
                helper.assertTrue(stack.getDamageValue() == 0, "an offered piece is new: " + stack);
                ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
                long reference = TradeRules.reference(rating, TradeGoods.material(stack, plain), stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0),
                        EquipmentBonuses.isWeapon(stack), 1.0, plain);
                helper.assertTrue(entry.price() == OfferRules.price(reference, TerminalBalance.DEFAULT.buyMultiplier(),
                        TerminalBalance.DEFAULT.wonPerEmerald()), "price of " + stack + " on day " + day + ": " + entry.price());
            }
        }
        helper.succeed();
    }
}
