package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.DebtState;
import com.orijua.overgearedmod.feature.economy.LicenseLock;
import com.orijua.overgearedmod.feature.economy.LicenseOffer;
import com.orijua.overgearedmod.feature.economy.OfferBought;
import com.orijua.overgearedmod.feature.economy.OfferEntry;
import com.orijua.overgearedmod.feature.economy.OfferRules;
import com.orijua.overgearedmod.feature.economy.TerminalBuyEmeraldsPayload;
import com.orijua.overgearedmod.feature.economy.TerminalBuyOfferPayload;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
import com.orijua.overgearedmod.feature.economy.TerminalRules;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalShop;
import com.orijua.overgearedmod.feature.economy.TerminalState;
import com.orijua.overgearedmod.feature.economy.TradeGoods;
import com.orijua.overgearedmod.feature.equipment.EquipmentRules;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.option.RolledOption;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Buying at the terminal and the phone (economy, step 4): Satisfy emeralds for Won and the daily offer of rated equipment,
 * both written into the stored Satisfy character. The services are called directly with the balance of the test.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class TerminalShopTests {
    private static final BlockPos LANDING = new BlockPos(1, 1, 1);
    private static final TerminalBalance D = TerminalBalance.DEFAULT;
    private static final TradeBalance TRADE = TradeBalance.DEFAULT;

    private TerminalShopTests() {
    }

    private static Item emerald() {
        return ModItems.SATISFY_EMERALD.get();
    }

    private static void real(GameTestHelper helper, Runnable body) {
        CapsuleTests.outsideSatisfy(helper, body);
    }

    private static void satisfy(GameTestHelper helper, Runnable body) {
        CapsuleTests.inSatisfy(helper, helper.absolutePos(LANDING), body);
    }

    /** The real world's player with a stored Satisfy character holding what {@code fill} puts in the inventory. */
    private static void withStored(ServerPlayer player, Consumer<Inventory> fill) {
        player.getInventory().clearContent();
        fill.accept(player.getInventory());
        CompoundTag snapshot = CharacterSnapshot.capture(player);
        player.getInventory().clearContent();
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, snapshot));
    }

    private static void fillStone(Inventory inventory, int from, int to) {
        for (int slot = from; slot < to; slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
    }

    private static CharacterVault vault(ServerPlayer player) {
        return player.getData(ModAttachments.CHARACTER_VAULT.get());
    }

    private static ListTag stored(ServerPlayer player) {
        return CharacterSnapshot.inventory(vault(player).stored().orElseThrow());
    }

    /** The entry of a stored slot, or null. */
    private static CompoundTag entryAt(ServerPlayer player, int slot) {
        ListTag list = stored(player);
        for (int i = 0; i < list.size(); i++) {
            if ((list.getCompound(i).getByte("Slot") & 255) == slot) {
                return list.getCompound(i);
            }
        }
        return null;
    }

    private static int countAt(ServerPlayer player, int slot) {
        CompoundTag entry = entryAt(player, slot);
        return entry == null ? 0 : entry.getInt("count");
    }

    private static List<OfferEntry> offer(GameTestHelper helper, long day) {
        return TerminalShop.offer(helper.getLevel(), day, D, TRADE);
    }

    private static OfferBought bought(ServerPlayer player) {
        return player.getData(ModAttachments.OFFER_BOUGHT.get());
    }

    /** A rejected attempt must leave the Won, the whole vault and the bought-today marks exactly as they were. */
    private static void assertUnchanged(GameTestHelper helper, ServerPlayer player, long won, CharacterVault vault, OfferBought bought,
                                        String what) {
        helper.assertTrue(AccountService.won(player) == won, what + ": the Won changed to " + AccountService.won(player));
        helper.assertTrue(vault(player).equals(vault), what + ": the vault changed");
        helper.assertTrue(bought(player).equals(bought), what + ": the bought marks changed");
    }

    // ---- Emeralds ----

    @GameTest(template = TestSupport.EMPTY)
    public static void buyingEmeraldsCompletesTheStackChargesAndKeepsTheRest(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> {
            inv.setItem(0, new ItemStack(emerald(), 60));
            inv.setItem(2, new ItemStack(Items.DIAMOND_SWORD));
        });
        player.getInventory().setItem(5, new ItemStack(Items.COAL, 3));
        AccountService.setWon(player, 4000L);
        CompoundTag sword = entryAt(player, 2).copy();
        real(helper, () -> {
            TerminalShop.Outcome outcome = TerminalShop.buyEmeralds(player, 10, 400, D);
            helper.assertTrue(outcome.result() == TerminalShop.Result.OK, "bought: " + outcome.result());
            helper.assertTrue(outcome.won() == 4000L && outcome.amount() == 10, "outcome: " + outcome);
            helper.assertTrue(AccountService.won(player) == 0L, "charged: " + AccountService.won(player));
            helper.assertTrue(countAt(player, 0) == 64, "slot 0 completed: " + countAt(player, 0));
            helper.assertTrue(countAt(player, 1) == 6 && entryAt(player, 1).getString("id").equals(TerminalRules.EMERALD_ID), "slot 1 got the other 6");
            helper.assertTrue(entryAt(player, 2).equals(sword), "the sword is intact");
            helper.assertTrue(vault(player).live().equals(Optional.of(WorldSide.REAL)), "the live world did not change");
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 3, "the live inventory was not touched");
            helper.assertTrue(TerminalShop.message(outcome).isPresent(), "and says so");

            player.getInventory().clearContent();
            CharacterSnapshot.apply(player, vault(player).stored().orElseThrow());
            helper.assertTrue(player.getInventory().getItem(0).is(emerald()) && player.getInventory().getItem(0).getCount() == 64, "loaded: slot 0");
            helper.assertTrue(player.getInventory().getItem(1).is(emerald()) && player.getInventory().getItem(1).getCount() == 6, "loaded: slot 1");
            helper.assertTrue(player.getInventory().getItem(2).is(Items.DIAMOND_SWORD), "loaded: the sword");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void buyingEmeraldsWithoutEnoughWonOrRoomChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        AccountService.setWon(player, 3999L);
        CharacterVault before = vault(player);
        real(helper, () -> {
            helper.assertTrue(TerminalShop.buyEmeralds(player, 10, 400, D).result() == TerminalShop.Result.NOT_ENOUGH_WON, "3999 Won for 10");
            helper.assertTrue(AccountService.won(player) == 3999L && vault(player).equals(before), "nothing changed");

            withStored(player, inv -> {
                inv.setItem(0, new ItemStack(emerald(), 63));
                fillStone(inv, 1, 36);
            });
            AccountService.setWon(player, 100_000L);
            CharacterVault full = vault(player);
            helper.assertTrue(TerminalShop.buyEmeralds(player, 2, 400, D).result() == TerminalShop.Result.NO_ROOM, "room for 1 only");
            helper.assertTrue(AccountService.won(player) == 100_000L && vault(player).equals(full), "nothing changed");
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.OK, "1 fits");
            helper.assertTrue(countAt(player, 0) == 64 && AccountService.won(player) == 99_600L, "completed and charged");

            withStored(player, inv -> fillStone(inv, 0, 36));
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.NO_ROOM, "36 slots of stone");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anInvalidOrStaleEmeraldPurchaseChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        AccountService.setWon(player, 5000L);
        CharacterVault before = vault(player);
        OfferBought marks = bought(player);
        real(helper, () -> {
            helper.assertTrue(TerminalShop.buyEmeralds(player, 0, 400, D).result() == TerminalShop.Result.INVALID_AMOUNT, "0");
            assertUnchanged(helper, player, 5000L, before, marks, "0");
            helper.assertTrue(TerminalShop.buyEmeralds(player, -3, 400, D).result() == TerminalShop.Result.INVALID_AMOUNT, "negative");
            assertUnchanged(helper, player, 5000L, before, marks, "negative");
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 300, D).result() == TerminalShop.Result.STALE, "the player saw 300 Won each");
            assertUnchanged(helper, player, 5000L, before, marks, "300");
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D.withBuy(500)).result() == TerminalShop.Result.STALE, "the price changed");
            assertUnchanged(helper, player, 5000L, before, marks, "500");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aConfiguredEmeraldPriceIsUsedAsIs(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        AccountService.setWon(player, 5000L);
        CharacterVault before = vault(player);
        OfferBought marks = bought(player);
        TerminalBalance cheap = D.withBuy(50);
        real(helper, () -> {
            helper.assertTrue(cheap.buyWonPerEmerald() == 50, "the configured 50 is the price, there is no floor to raise it");
            helper.assertTrue(TerminalService.state(player, Optional.empty()).buyWonPerEmerald() == 400, "the default state keeps 400");
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 95, cheap).result() == TerminalShop.Result.STALE, "another price is stale");
            assertUnchanged(helper, player, 5000L, before, marks, "seen 95");
            TerminalShop.Outcome ok = TerminalShop.buyEmeralds(player, 10, 50, cheap);
            helper.assertTrue(ok.result() == TerminalShop.Result.OK && ok.won() == 500L, "bought at 50 each: " + ok);
            helper.assertTrue(AccountService.won(player) == 4500L, "charged 500: " + AccountService.won(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void shoppingNeedsTheRealWorldAndAReadableCharacter(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        AccountService.setWon(player, 100_000L);
        CharacterVault before = vault(player);
        OfferBought marks = bought(player);
        satisfy(helper, () -> {
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.IN_SATISFY, "emeralds in Satisfy");
            OfferEntry entry = offer(helper, 3L).get(0);
            helper.assertTrue(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price(), 3L, D, TRADE).result() == TerminalShop.Result.IN_SATISFY,
                    "a piece in Satisfy");
            helper.assertTrue(TerminalShop.message(TerminalShop.buyEmeralds(player, 1, 400, D)).isPresent(), "No signal");
            assertUnchanged(helper, player, 100_000L, before, marks, "in Satisfy");
        });
        real(helper, () -> {
            CharacterVault carryingSatisfy = CharacterVault.of(WorldSide.SATISFY, before.stored().orElseThrow());
            player.setData(ModAttachments.CHARACTER_VAULT.get(), carryingSatisfy);
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.IN_SATISFY,
                    "carrying Satisfy's things in the real world: no signal");

            CharacterVault nothing = new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty());
            player.setData(ModAttachments.CHARACTER_VAULT.get(), nothing);
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.NO_CHARACTER, "nothing stored");
            OfferEntry entry = offer(helper, 3L).get(0);
            helper.assertTrue(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price(), 3L, D, TRADE).result() == TerminalShop.Result.NO_CHARACTER,
                    "nothing stored, a piece");
            assertUnchanged(helper, player, 100_000L, nothing, marks, "nothing stored");
            helper.assertTrue(TerminalShop.message(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price(), 3L, D, TRADE)).isPresent(),
                    "and the message of the purchase");

            CharacterVault unreadable = CharacterVault.of(WorldSide.REAL, new CompoundTag());
            player.setData(ModAttachments.CHARACTER_VAULT.get(), unreadable);
            helper.assertTrue(TerminalShop.buyEmeralds(player, 1, 400, D).result() == TerminalShop.Result.NO_CHARACTER, "unreadable");
            helper.assertTrue(vault(player).equals(unreadable), "and left as it was");
            helper.assertTrue(AccountService.won(player) == 100_000L, "nothing was charged");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPurchasePacketOnlyActsOnTheOpenWindowOfTheSender(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
        AccountService.setWon(player, 100_000L);
        CharacterVault before = vault(player);
        OfferBought marks = bought(player);
        satisfy(helper, () -> {
            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                OfferEntry entry = offer(helper, TerminalService.today(player)).get(0);
                helper.assertTrue(TerminalShop.buyEmeralds(player, new TerminalBuyEmeraldsPayload(7, 1, 400)).result() == TerminalShop.Result.IN_SATISFY,
                        "emeralds with the window open in Satisfy");
                helper.assertTrue(TerminalShop.buyOffer(player, new TerminalBuyOfferPayload(7, 0, entry.stack(), entry.price())).result()
                        == TerminalShop.Result.IN_SATISFY, "a piece with the window open in Satisfy");
                assertUnchanged(helper, player, 100_000L, before, marks, "in Satisfy with a window");
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
        });
        real(helper, () -> {
            helper.assertTrue(TerminalShop.buyEmeralds(player, new TerminalBuyEmeraldsPayload(7, 1, 400)).result() == TerminalShop.Result.NO_TERMINAL,
                    "no window open");
            assertUnchanged(helper, player, 100_000L, before, marks, "no window, emeralds");
            OfferEntry entry = offer(helper, TerminalService.today(player)).get(0);
            helper.assertTrue(TerminalShop.buyOffer(player, new TerminalBuyOfferPayload(7, 0, entry.stack(), entry.price())).result()
                    == TerminalShop.Result.NO_TERMINAL, "nor for a piece");
            assertUnchanged(helper, player, 100_000L, before, marks, "no window, piece");

            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                helper.assertTrue(TerminalShop.buyEmeralds(player, new TerminalBuyEmeraldsPayload(8, 1, 400)).result() == TerminalShop.Result.NO_TERMINAL,
                        "another container id");
                assertUnchanged(helper, player, 100_000L, before, marks, "another id");
                helper.assertTrue(TerminalShop.buyEmeralds(player, new TerminalBuyEmeraldsPayload(7, 1, 400)).result() == TerminalShop.Result.OK,
                        "the right id works");
                TerminalShop.Outcome piece = TerminalShop.buyOffer(player, new TerminalBuyOfferPayload(7, 0, entry.stack(), entry.price()));
                helper.assertTrue(piece.result() == TerminalShop.Result.OK, "a piece by payload: " + piece.result());
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
        });
        helper.succeed();
    }

    // ---- The offer ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theOfferIsTheSameForEveryoneAndChangesWithTheDay(GameTestHelper helper) {
        List<OfferEntry> a = offer(helper, 42L);
        List<OfferEntry> b = offer(helper, 42L);
        helper.assertTrue(a.size() == 12 && b.size() == 12, "12 pieces: " + a.size());
        int differing = 0;
        List<OfferEntry> other = offer(helper, 43L);
        for (int i = 0; i < 12; i++) {
            helper.assertTrue(ItemStack.matches(a.get(i).stack(), b.get(i).stack()) && a.get(i).price() == b.get(i).price(), "same day, same piece " + i);
            if (!ItemStack.matches(a.get(i).stack(), other.get(i).stack())) {
                differing++;
            }
        }
        helper.assertTrue(differing > 6, "another day changes most of the offer: " + differing + " of 12");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theOfferHasSixNormalAndSixRarePiecesOfTheConfiguredTypes(GameTestHelper helper) {
        for (long day : new long[]{0L, 1L, 77L, 1234L}) {
            List<OfferEntry> offer = offer(helper, day);
            for (OfferEntry entry : offer) {
                ItemStack stack = entry.stack();
                ItemRating expected = entry.position() < 6 ? ItemRating.NORMAL : ItemRating.RARE;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                helper.assertTrue(stack.get(ModDataComponents.ITEM_RATING) == expected, "rating of " + entry.position() + " on day " + day);
                helper.assertTrue(D.offerItems().contains(id), "an offered type: " + id);
                helper.assertFalse(id.getPath().contains("diamond") || id.getPath().contains("netherite"), "nothing OP: " + id);
                List<RolledOption> options = stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.of());
                helper.assertTrue(options.size() <= EquipmentRules.optionCount(expected), "options within the rating: " + options.size());
                helper.assertTrue(stack.getCount() == 1, "one unit");
                helper.assertTrue(entry.price() == OfferRules.price(TradeGoods.reference(stack, TRADE), D.buyMultiplier(), D.wonPerEmerald())
                        && entry.price() >= 1L, "price: " + entry.price());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void buyingAPieceChargesPutsItInTheFirstFreeSlotAndMarksIt(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> {
            fillStone(inv, 0, 2);
            inv.setItem(3, new ItemStack(Items.DIRT, 5));
        });
        AccountService.setWon(player, 1_000_000L);
        real(helper, () -> {
            OfferEntry entry = offer(helper, 7L).get(3);
            TerminalShop.Outcome outcome = TerminalShop.buyOffer(player, 3, entry.stack(), entry.price(), 7L, D, TRADE);
            helper.assertTrue(outcome.result() == TerminalShop.Result.OK, "bought: " + outcome.result());
            helper.assertTrue(outcome.won() == entry.price() && AccountService.won(player) == 1_000_000L - entry.price(), "charged the price");
            CompoundTag saved = entryAt(player, 2);
            helper.assertTrue(saved != null, "in the first free slot (2)");
            ItemStack back = ItemStack.parse(player.registryAccess(), saved).orElseThrow();
            helper.assertTrue(ItemStack.matches(back, entry.stack()), "the piece is the one offered");
            helper.assertTrue(player.getData(ModAttachments.OFFER_BOUGHT.get()).has(3, 7L), "marked as bought");
            helper.assertTrue(TerminalShop.message(outcome).isPresent(), "and says so");

            CharacterVault afterFirst = vault(player);
            long wonAfter = AccountService.won(player);
            OfferBought marksAfter = bought(player);
            TerminalShop.Outcome again = TerminalShop.buyOffer(player, 3, entry.stack(), entry.price(), 7L, D, TRADE);
            helper.assertTrue(again.result() == TerminalShop.Result.SOLD_OUT, "one unit per day: " + again.result());
            assertUnchanged(helper, player, wonAfter, afterFirst, marksAfter, "the second one");

            OfferEntry other = offer(helper, 7L).get(4);
            helper.assertTrue(TerminalShop.buyOffer(player, 4, other.stack(), other.price(), 7L, D, TRADE).result() == TerminalShop.Result.OK,
                    "another piece is fine");
            helper.assertTrue(entryAt(player, 4) != null && entryAt(player, 3).getString("id").equals("minecraft:dirt"), "in the next free slot");

            OfferEntry tomorrow = offer(helper, 8L).get(3);
            helper.assertTrue(TerminalShop.buyOffer(player, 3, tomorrow.stack(), tomorrow.price(), 8L, D, TRADE).result() == TerminalShop.Result.OK,
                    "the next day it can be bought again");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPieceOrPriceThatChangedIsStaleAndChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(Items.DIRT)));
        AccountService.setWon(player, 1_000_000L);
        CharacterVault before = vault(player);
        OfferBought marks = bought(player);
        real(helper, () -> {
            OfferEntry entry = offer(helper, 7L).get(0);
            ItemStack two = entry.stack().copyWithCount(2);
            ItemStack reRated = entry.stack().copy();
            reRated.set(ModDataComponents.ITEM_RATING, ItemRating.EPIC);
            record Attempt(String what, int position, ItemStack seen, long price) {
            }
            for (Attempt a : List.of(
                    new Attempt("another piece", 0, new ItemStack(Items.STONE), entry.price()),
                    new Attempt("an empty piece", 0, ItemStack.EMPTY, entry.price()),
                    new Attempt("another price", 0, entry.stack(), entry.price() + 1),
                    new Attempt("two units", 0, two, entry.price()),
                    new Attempt("another rating", 0, reRated, entry.price()),
                    new Attempt("position 12", 12, entry.stack(), entry.price()),
                    new Attempt("position -1", -1, entry.stack(), entry.price()))) {
                helper.assertTrue(TerminalShop.buyOffer(player, a.position(), a.seen(), a.price(), 7L, D, TRADE).result() == TerminalShop.Result.STALE,
                        a.what());
                assertUnchanged(helper, player, 1_000_000L, before, marks, a.what());
            }
            helper.assertFalse(bought(player).has(0, 7L), "nor marked anything");

            // The order of the checks: a stale view comes before a sold-out piece, and no character before everything.
            TerminalShop.buyOffer(player, 0, entry.stack(), entry.price(), 7L, D, TRADE);
            CharacterVault afterBuying = vault(player);
            OfferBought marksAfter = bought(player);
            long wonAfter = AccountService.won(player);
            helper.assertTrue(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price() + 1, 7L, D, TRADE).result() == TerminalShop.Result.STALE,
                    "stale and sold out: stale comes first");
            helper.assertTrue(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price(), 7L, D, TRADE).result() == TerminalShop.Result.SOLD_OUT,
                    "sold out");
            assertUnchanged(helper, player, wonAfter, afterBuying, marksAfter, "after the purchase");
            player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
            helper.assertTrue(TerminalShop.buyOffer(player, 0, entry.stack(), entry.price() + 1, 7L, D, TRADE).result() == TerminalShop.Result.NO_CHARACTER,
                    "no character and stale: no character comes first");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSinglePieceIsTheSameAsInTheWholeOffer(GameTestHelper helper) {
        for (long day : new long[]{0L, 1L, 42L, 1234L, -5L}) {
            List<OfferEntry> whole = offer(helper, day);
            for (int position = 0; position < whole.size(); position++) {
                Optional<OfferEntry> one = TerminalShop.offerEntry(helper.getLevel(), day, position, D, TRADE);
                helper.assertTrue(one.isPresent() && ItemStack.matches(one.get().stack(), whole.get(position).stack())
                        && one.get().price() == whole.get(position).price() && one.get().position() == position,
                        "day " + day + " position " + position);
            }
            helper.assertTrue(TerminalShop.offerEntry(helper.getLevel(), day, -1, D, TRADE).isEmpty(), "position -1");
            helper.assertTrue(TerminalShop.offerEntry(helper.getLevel(), day, whole.size(), D, TRADE).isEmpty(), "position size");
        }
        helper.assertTrue(TerminalShop.offerEntry(helper.getLevel(), 5L, 0, D.withOffer(0, ItemRating.RARE, D.offerItems()), TRADE).isEmpty(), "offer off");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPieceWithoutRoomOrWonChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> fillStone(inv, 0, 36));
        AccountService.setWon(player, 1_000_000L);
        CharacterVault full = vault(player);
        real(helper, () -> {
            OfferEntry entry = offer(helper, 7L).get(5);
            helper.assertTrue(TerminalShop.buyOffer(player, 5, entry.stack(), entry.price(), 7L, D, TRADE).result() == TerminalShop.Result.NO_ROOM, "36 slots of stone");
            assertUnchanged(helper, player, 1_000_000L, full, OfferBought.EMPTY, "no room");
            helper.assertFalse(player.getData(ModAttachments.OFFER_BOUGHT.get()).has(5, 7L), "and it can still be bought later");

            withStored(player, inv -> inv.setItem(0, new ItemStack(Items.DIRT)));
            CharacterVault roomy = vault(player);
            AccountService.setWon(player, entry.price() - 1);
            helper.assertTrue(TerminalShop.buyOffer(player, 5, entry.stack(), entry.price(), 7L, D, TRADE).result() == TerminalShop.Result.NOT_ENOUGH_WON,
                    "one Won short");
            assertUnchanged(helper, player, entry.price() - 1, roomy, OfferBought.EMPTY, "one Won short");
            helper.assertFalse(player.getData(ModAttachments.OFFER_BOUGHT.get()).has(5, 7L), "not marked");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theBuyPayloadsRoundTrip(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        OfferEntry entry = offer(helper, 5L).get(7);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        TerminalBuyOfferPayload.STREAM_CODEC.encode(buf, new TerminalBuyOfferPayload(9, 7, entry.stack(), entry.price()));
        TerminalBuyOfferPayload decoded = TerminalBuyOfferPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(decoded.containerId() == 9 && decoded.position() == 7 && decoded.seenPrice() == entry.price()
                && ItemStack.matches(decoded.seen(), entry.stack()), "the purchase payload");

        RegistryFriendlyByteBuf buf2 = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        TerminalBuyEmeraldsPayload.STREAM_CODEC.encode(buf2, new TerminalBuyEmeraldsPayload(3, 12, 400));
        TerminalBuyEmeraldsPayload emeralds = TerminalBuyEmeraldsPayload.STREAM_CODEC.decode(buf2);
        helper.assertTrue(emeralds.containerId() == 3 && emeralds.amount() == 12 && emeralds.seenUnitPrice() == 400, "the emerald payload");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDecodedOfferPayloadBuysThePiece(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
        AccountService.setWon(player, 1_000_000L);
        real(helper, () -> {
            long today = TerminalService.today(player);
            OfferEntry entry = offer(helper, today).get(7);
            RegistryFriendlyByteBuf buf =new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            TerminalBuyOfferPayload.STREAM_CODEC.encode(buf, new TerminalBuyOfferPayload(7, 7, entry.stack(), entry.price()));
            TerminalBuyOfferPayload decoded = TerminalBuyOfferPayload.STREAM_CODEC.decode(buf);
            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                TerminalShop.Outcome outcome = TerminalShop.buyOffer(player, decoded);
                helper.assertTrue(outcome.result() == TerminalShop.Result.OK, "bought by the decoded payload: " + outcome.result());
                helper.assertTrue(outcome.won() == entry.price() && AccountService.won(player) == 1_000_000L - entry.price(), "charged the price");
                CompoundTag saved = entryAt(player, 1);
                helper.assertTrue(saved != null, "in the first free slot (1)");
                helper.assertTrue(ItemStack.matches(ItemStack.parse(player.registryAccess(), saved).orElseThrow(), entry.stack()), "the piece is the offered one");
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theStateCarriesTheOfferAndWhatWasBought(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        AccountService.setWon(player, 1_000_000L);
        real(helper, () -> {
            long today = TerminalService.today(player);
            OfferEntry entry = offer(helper, today).get(7);
            TerminalShop.Outcome bought = TerminalShop.buyOffer(player, 7, entry.stack(), entry.price(), today, D, TRADE);
            helper.assertTrue(bought.result() == TerminalShop.Result.OK, "bought: " + bought.result());
            helper.assertTrue(OfferBought.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, bought(player)).result().isPresent(), "the mark persists");

            player.setData(ModAttachments.CAPSULE_TIER.get(), com.orijua.overgearedmod.feature.capsule.CapsuleTier.ADVANCED);
            TerminalState state = TerminalService.state(player, Optional.empty());
            RegistryFriendlyByteBuf buf3 = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            TerminalState.STREAM_CODEC.encode(buf3, state);
            TerminalState back = TerminalState.STREAM_CODEC.decode(buf3);
            helper.assertTrue(back.offer().size() == 12 && back.buyWonPerEmerald() == 400, "the state: offer and price");
            for (int i = 0; i < 12; i++) {
                helper.assertTrue(ItemStack.matches(back.offer().get(i).stack(), state.offer().get(i).stack())
                        && back.offer().get(i).price() == state.offer().get(i).price() && back.offer().get(i).position() == i, "piece " + i);
            }
            helper.assertTrue(back.boughtToday().equals(java.util.Set.of(7)), "what was bought today: " + back.boughtToday());
            helper.assertTrue(back.emeraldRoom() == state.emeraldRoom() && back.freeSlots() == state.freeSlots() && back.freeSlots() > 0, "room and slots");
            helper.assertTrue(back.sessionTier() == com.orijua.overgearedmod.feature.capsule.CapsuleTier.ADVANCED, "the tier of the session");
            helper.assertTrue(back.licenses().size() == 2 && back.licenses().equals(state.licenses()), "the licenses on sale: " + back.licenses());

            RegistryFriendlyByteBuf buf4 = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            com.orijua.overgearedmod.feature.economy.TerminalBuyLicensePayload.STREAM_CODEC.encode(buf4,
                    new com.orijua.overgearedmod.feature.economy.TerminalBuyLicensePayload(5, com.orijua.overgearedmod.feature.capsule.CapsuleTier.DIAMOND, 15000L));
            var license = com.orijua.overgearedmod.feature.economy.TerminalBuyLicensePayload.STREAM_CODEC.decode(buf4);
            helper.assertTrue(license.containerId() == 5 && license.tier() == com.orijua.overgearedmod.feature.capsule.CapsuleTier.DIAMOND
                    && license.seenPrice() == 15000L, "the license payload");
        });
        helper.succeed();
    }

    private static RegistryFriendlyByteBuf buffer(ServerPlayer player) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theStateWithAnActiveDebtAndTheDiamondTierRoundTripsAndKeepsItsLicensesWithAMessage(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        player.setData(ModAttachments.ACCOUNT.get(), new Account(50_000L, 500L, DebtState.ACTIVE));
        real(helper, () -> {
            player.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.DIAMOND);
            TerminalState state = TerminalService.state(player, Optional.empty());
            helper.assertTrue(state.sessionTier() == CapsuleTier.DIAMOND, "the tier of the session: " + state.sessionTier());
            helper.assertTrue(state.licenses().size() == 2 && state.licenses().get(0).lock() == LicenseLock.DEBT_ACTIVE
                    && state.licenses().get(1).lock() == LicenseLock.OPEN, "an active debt closes only the advanced license: " + state.licenses());

            RegistryFriendlyByteBuf buf = buffer(player);
            TerminalState.STREAM_CODEC.encode(buf, state);
            TerminalState back = TerminalState.STREAM_CODEC.decode(buf);
            helper.assertTrue(back.sessionTier() == CapsuleTier.DIAMOND, "the tier survives the codec");
            helper.assertTrue(back.licenses().equals(state.licenses()), "the licenses with their locks survive: " + back.licenses());

            TerminalState told = state.withMessage(Optional.of(Component.literal("hello")));
            helper.assertTrue(told.licenses().equals(state.licenses()) && told.sessionTier() == CapsuleTier.DIAMOND
                    && told.message().isPresent(), "withMessage keeps the licenses and the tier");
            RegistryFriendlyByteBuf buf2 = buffer(player);
            TerminalState.STREAM_CODEC.encode(buf2, told);
            TerminalState backTold = TerminalState.STREAM_CODEC.decode(buf2);
            helper.assertTrue(backTold.licenses().equals(state.licenses()) && backTold.message().map(Component::getString).orElse("").equals("hello"),
                    "and so does the codec with a message");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aStateWithAnImpossibleNumberOfLicensesIsRejected(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(emerald(), 5)));
        real(helper, () -> {
            TerminalState plain = TerminalService.state(player, Optional.empty()).withMessage(Optional.empty());
            TerminalState empty = new TerminalState(plain.characterAvailable(), plain.storedEmeralds(), plain.workPay(), plain.today(), plain.order(),
                    plain.icon(), plain.workDay(), plain.buyWonPerEmerald(), plain.emeraldRoom(), plain.freeSlots(), plain.offer(), plain.boughtToday(),
                    plain.debtQuotaPercent(), plain.sessionTier(), List.of(), Optional.empty());
            // The tail of the encoding is [license count][no message]: the count is rewritten by hand.
            for (int count : new int[]{-1, CapsuleTier.values().length + 1, 1000}) {
                RegistryFriendlyByteBuf buf = buffer(player);
                TerminalState.STREAM_CODEC.encode(buf, empty);
                buf.writerIndex(buf.writerIndex() - 2);
                buf.writeVarInt(count);
                buf.writeBoolean(false);
                boolean rejected = false;
                try {
                    TerminalState.STREAM_CODEC.decode(buf);
                } catch (IllegalArgumentException e) {
                    rejected = true;
                }
                helper.assertTrue(rejected, count + " licenses must be rejected");
            }
            // The largest legal number (every tier) still goes through.
            List<LicenseOffer> all = List.of(new LicenseOffer(CapsuleTier.BASIC, 1L, LicenseLock.OPEN),
                    new LicenseOffer(CapsuleTier.ADVANCED, 3000L, LicenseLock.DEBT_ACTIVE), new LicenseOffer(CapsuleTier.DIAMOND, 15000L, LicenseLock.ALREADY_HAVE));
            TerminalState full = new TerminalState(empty.characterAvailable(), empty.storedEmeralds(), empty.workPay(), empty.today(), empty.order(),
                    empty.icon(), empty.workDay(), empty.buyWonPerEmerald(), empty.emeraldRoom(), empty.freeSlots(), empty.offer(), empty.boughtToday(),
                    empty.debtQuotaPercent(), empty.sessionTier(), all, Optional.empty());
            RegistryFriendlyByteBuf buf = buffer(player);
            TerminalState.STREAM_CODEC.encode(buf, full);
            helper.assertTrue(TerminalState.STREAM_CODEC.decode(buf).licenses().equals(all), "three licenses are accepted");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutACharacterTheStateStillCarriesTheOffer(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
        real(helper, () -> {
            TerminalState state = TerminalService.state(player, Optional.empty());
            helper.assertFalse(state.characterAvailable(), "no character");
            helper.assertTrue(state.emeraldRoom() == 0 && state.freeSlots() == 0, "no room");
            helper.assertTrue(state.offer().size() == 12, "the offer is still shown");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anOfferTurnedOffOrWithoutTypesIsEmptyAndNothingIsBought(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStored(player, inv -> inv.setItem(0, new ItemStack(Items.DIRT)));
        AccountService.setWon(player, 1_000_000L);
        CharacterVault before = vault(player);
        TerminalBalance off = D.withOffer(0, ItemRating.RARE, D.offerItems());
        TerminalBalance noTypes = D.withOffer(6, ItemRating.RARE, List.of());
        TerminalBalance junk = D.withOffer(6, ItemRating.RARE, List.of(ResourceLocation.withDefaultNamespace("stone"),
                ResourceLocation.withDefaultNamespace("does_not_exist"), ResourceLocation.withDefaultNamespace("iron_sword")));
        real(helper, () -> {
            helper.assertTrue(TerminalShop.offer(helper.getLevel(), 5L, off, TRADE).isEmpty(), "perRank 0");
            helper.assertTrue(TerminalShop.offer(helper.getLevel(), 5L, noTypes, TRADE).isEmpty(), "no types");
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            helper.assertTrue(TerminalShop.buyOffer(player, 0, sword, 400L, 5L, off, TRADE).result() == TerminalShop.Result.STALE, "stale with the offer off");
            helper.assertTrue(TerminalShop.buyOffer(player, 0, sword, 400L, 5L, noTypes, TRADE).result() == TerminalShop.Result.STALE, "stale without types");
            helper.assertTrue(AccountService.won(player) == 1_000_000L && vault(player).equals(before), "nothing changed");

            List<OfferEntry> offer = TerminalShop.offer(helper.getLevel(), 5L, junk, TRADE);
            helper.assertTrue(offer.size() == 12, "unknown ids and non-equipment are left out, the rest remains");
            for (OfferEntry entry : offer) {
                helper.assertTrue(entry.stack().is(Items.IRON_SWORD), "only the iron sword is left: " + entry.stack());
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theOfferPricesFollowTheBalance(GameTestHelper helper) {
        TerminalBalance dear = new TerminalBalance(100, 1000L, D.workOrders(), 400, 2.0, 6, ItemRating.RARE, D.offerItems());
        List<OfferEntry> base = TerminalShop.offer(helper.getLevel(), 9L, D, TRADE);
        List<OfferEntry> cheap = TerminalShop.offer(helper.getLevel(), 9L, dear, TRADE);
        for (int i = 0; i < 12; i++) {
            helper.assertTrue(cheap.get(i).price() == OfferRules.price(TradeGoods.reference(base.get(i).stack(), TRADE), 2.0, 100),
                    "x2 instead of x4 at " + i);
        }
        helper.succeed();
    }
}
