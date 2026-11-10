package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.DebtState;
import com.orijua.overgearedmod.feature.economy.LicenseOffer;
import com.orijua.overgearedmod.feature.economy.LicenseLock;
import com.orijua.overgearedmod.feature.economy.LicenseShop;
import com.orijua.overgearedmod.feature.economy.LicenseShop.Outcome;
import com.orijua.overgearedmod.feature.economy.LicenseShop.Result;
import com.orijua.overgearedmod.feature.economy.TerminalBuyLicensePayload;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
import com.orijua.overgearedmod.feature.world.SatisfyShapedRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The capsule licenses sold at the terminal and the crafting recipes that use them up. A rejected order must leave the
 * Won and the inventory exactly as they were.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class LicenseTests {
    private static final CapsuleBalance C = CapsuleBalance.DEFAULT;
    private static final Account SETTLED = new Account(100_000L, 0L, DebtState.SETTLED);
    private static final Account RICH = SETTLED;

    private LicenseTests() {
    }

    private static void real(GameTestHelper helper, Runnable body) {
        CapsuleTests.outsideSatisfy(helper, () -> CapsuleBalance.withOverride(C, body));
    }

    /** The player carries the real world's character, with a phone (so a hand-built window stays valid) in slot 35. */
    private static ServerPlayer buyer(GameTestHelper helper, Account account) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
        player.getInventory().clearContent();
        player.getInventory().setItem(35, new ItemStack(ModItems.PHONE.get()));
        player.setData(ModAttachments.ACCOUNT.get(), account);
        return player;
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static void assertResult(GameTestHelper helper, Result expected, Outcome actual, String what) {
        helper.assertTrue(actual.result() == expected, what + ": expected " + expected + " but got " + actual.result());
    }

    /** A refused order changes neither the account nor the inventory. */
    private static void assertRefused(GameTestHelper helper, ServerPlayer player, Outcome outcome, Result expected, Account before, int advanced, int diamond, String what) {
        assertResult(helper, expected, outcome, what);
        helper.assertTrue(outcome.won() == 0L, what + ": nothing charged");
        helper.assertTrue(AccountService.get(player).equals(before), what + ": the account changed to " + AccountService.get(player));
        helper.assertTrue(count(player, ModItems.ADVANCED_CAPSULE_LICENSE.get()) == advanced && count(player, ModItems.DIAMOND_CAPSULE_LICENSE.get()) == diamond,
                what + ": the inventory changed");
    }

    // ---- The advanced license ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theAdvancedLicenseIsBoughtWithASettledOrNeverTakenDebt(GameTestHelper helper) {
        real(helper, () -> {
            for (DebtState state : List.of(DebtState.SETTLED, DebtState.UNASSIGNED)) {
                ServerPlayer player = buyer(helper, new Account(10_000L, 0L, state));
                Outcome outcome = LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C);
                assertResult(helper, Result.OK, outcome, "debt " + state);
                helper.assertTrue(outcome.tier() == CapsuleTier.ADVANCED, "the outcome names the tier (" + state + ")");
                helper.assertTrue(outcome.won() == 3000L && AccountService.won(player) == 7000L, "3.000 Won charged (" + state + ")");
                helper.assertTrue(count(player, ModItems.ADVANCED_CAPSULE_LICENSE.get()) == 1, "and the license delivered (" + state + ")");
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anActiveDebtClosesTheAdvancedLicense(GameTestHelper helper) {
        real(helper, () -> {
            Account active = new Account(10_000L, 500L, DebtState.ACTIVE);
            ServerPlayer player = buyer(helper, active);
            assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.DEBT_ACTIVE, active, 0, 0, "active debt");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutWonNothingIsBought(GameTestHelper helper) {
        real(helper, () -> {
            Account poor = new Account(2999L, 0L, DebtState.SETTLED);
            ServerPlayer player = buyer(helper, poor);
            assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.NOT_ENOUGH_WON, poor, 0, 0, "2.999 Won");
        });
        helper.succeed();
    }

    // ---- The diamond license ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsRefusedWithoutAnAdvancedCapsule(GameTestHelper helper) {
        real(helper, () -> {
            Account rich = new Account(100_000L, 0L, DebtState.SETTLED);
            ServerPlayer none = buyer(helper, rich);
            assertRefused(helper, none, LicenseShop.buy(none, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, rich, 0, 0, "no advanced capsule, debt settled");

            ServerPlayer basic = buyer(helper, rich);
            basic.getInventory().setItem(0, new ItemStack(ModItems.CAPSULE.get()));
            assertRefused(helper, basic, LicenseShop.buy(basic, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, rich, 0, 0, "a basic capsule is not enough");

            ServerPlayer lastBasic = buyer(helper, rich);
            lastBasic.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.BASIC);
            assertRefused(helper, lastBasic, LicenseShop.buy(lastBasic, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, rich, 0, 0, "last connected with a basic capsule");
        });
        helper.succeed();
    }

    /** The diamond license is sold when the advanced capsule shows up in the place {@code own} puts it; one test per place. */
    private static void diamondLicenseWith(GameTestHelper helper, Consumer<ServerPlayer> own, String where) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, RICH);
            own.accept(player);
            Outcome outcome = LicenseShop.buy(player, CapsuleTier.DIAMOND, 15000L, C);
            assertResult(helper, Result.OK, outcome, where);
            helper.assertTrue(outcome.won() == 15000L && AccountService.won(player) == 85_000L, where + ": 15.000 Won charged");
            helper.assertTrue(count(player, ModItems.DIAMOND_CAPSULE_LICENSE.get()) == 1, where + ": the license is delivered");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsSoldWithAnAdvancedCapsuleInTheInventory(GameTestHelper helper) {
        diamondLicenseWith(helper, p -> p.getInventory().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE.get())), "an advanced capsule in the inventory");
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsSoldWithADiamondCapsuleInTheInventory(GameTestHelper helper) {
        diamondLicenseWith(helper, p -> p.getInventory().setItem(0, new ItemStack(ModItems.DIAMOND_CAPSULE.get())), "a diamond capsule in the inventory");
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsSoldWithAnAdvancedCapsuleInTheOffHand(GameTestHelper helper) {
        diamondLicenseWith(helper, p -> p.getInventory().offhand.set(0, new ItemStack(ModItems.ADVANCED_CAPSULE.get())), "an advanced capsule in the off hand");
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsSoldWithAnAdvancedCapsuleInTheCraftingGrid(GameTestHelper helper) {
        diamondLicenseWith(helper, p -> p.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE.get())),
                "an advanced capsule in the crafting grid");
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseIsSoldAfterConnectingWithAnAdvancedCapsule(GameTestHelper helper) {
        diamondLicenseWith(helper, p -> p.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.ADVANCED), "last connected with an advanced capsule, none carried");
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondLicenseDoesNotCareAboutTheDebt(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, new Account(100_000L, 500L, DebtState.ACTIVE));
            player.getInventory().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE.get()));
            assertResult(helper, Result.OK, LicenseShop.buy(player, CapsuleTier.DIAMOND, 15000L, C), "with an advanced capsule and an active debt");
        });
        helper.succeed();
    }

    // ---- Stale orders, double clicks and a full inventory ----

    @GameTest(template = TestSupport.EMPTY)
    public static void aPriceThatChangedIsStale(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 2000L, C), Result.STALE, SETTLED, 0, 0, "a wrong seen price");
            CapsuleBalance dearer = new CapsuleBalance(55, 8, 75, 6, 4000L, 15000L, 10, 10, 0.9, 30);
            CapsuleBalance.withOverride(dearer, () ->
                    assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, CapsuleBalance.current()), Result.STALE, SETTLED, 0, 0, "a price changed by the configuration"));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDoubleClickChargesOnce(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            assertResult(helper, Result.OK, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), "first click");
            Account after = AccountService.get(player);
            Outcome second = LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C);
            assertRefused(helper, player, second, Result.ALREADY_HAVE, after, 1, 0, "second click");
            helper.assertTrue(after.won() == 97_000L, "charged once");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLicenseOnTheCursorCountsAsCarried(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            player.containerMenu.setCarried(new ItemStack(ModItems.ADVANCED_CAPSULE_LICENSE.get()));
            try {
                assertResult(helper, Result.ALREADY_HAVE, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), "license in the cursor");
            } finally {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aFullInventoryRefusesTheOrderBeforeCharging(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            try {
                for (int slot = 0; slot < 36; slot++) {
                    player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
                }
                int before = looseItems(helper, player).size();
                Outcome outcome = LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C);
                assertRefused(helper, player, outcome, Result.NO_ROOM, SETTLED, 0, 0, "full inventory");
                helper.assertTrue(count(player, Items.STONE) == 36 * 64, "the inventory is untouched");
                helper.assertTrue(looseItems(helper, player).size() == before, "nothing was dropped on the ground");
                // A second click does not charge either.
                assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.NO_ROOM, SETTLED, 0, 0, "second click");
                // With one free slot it is bought.
                player.getInventory().setItem(10, ItemStack.EMPTY);
                Outcome bought = LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C);
                assertResult(helper, Result.OK, bought, "one free slot");
                helper.assertTrue(bought.won() == 3000L && AccountService.won(player) == 97_000L, "and 3.000 Won are charged once");
            } finally {
                looseItems(helper, player).forEach(ItemEntity::discard);
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLicenseInTheCraftingGridCountsAsCarried(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            player.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE_LICENSE.get()));
            try {
                assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.ALREADY_HAVE, SETTLED, 0, 0, "license in the crafting grid");
                List<LicenseOffer> offers = LicenseShop.offers(player, C);
                helper.assertTrue(offers.get(0).lock() == LicenseLock.ALREADY_HAVE, "and the offer says so: " + offers.get(0));
            } finally {
                player.inventoryMenu.getCraftSlots().clearContent();
            }
        });
        helper.succeed();
    }

    private static List<ItemEntity> looseItems(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4.0));
    }

    // ---- Where it can be bought ----

    /** Runs {@code body} with the terminal window 7 open for {@code player}, closing it after. */
    private static void withTerminalOpen(ServerPlayer player, Runnable body) {
        player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
        try {
            body.run();
        } finally {
            player.containerMenu = player.inventoryMenu;
        }
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void inSatisfyNothingIsBought(GameTestHelper helper) {
        ServerPlayer player = buyer(helper, SETTLED);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)), () -> CapsuleBalance.withOverride(C, () ->
                withTerminalOpen(player, () -> {
                    assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L)),
                            Result.IN_SATISFY, SETTLED, 0, 0, "in Satisfy");
                    assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.IN_SATISFY, SETTLED, 0, 0, "in Satisfy, direct");
                })));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutTheRightWindowNothingIsBought(GameTestHelper helper) {
        ServerPlayer player = buyer(helper, SETTLED);
        real(helper, () -> {
            assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L)),
                    Result.NO_TERMINAL, SETTLED, 0, 0, "no window open");
            withTerminalOpen(player, () -> assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(8, CapsuleTier.ADVANCED, 3000L)),
                    Result.NO_TERMINAL, SETTLED, 0, 0, "another window"));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theBasicTierHasNoLicenseToBuy(GameTestHelper helper) {
        ServerPlayer player = buyer(helper, SETTLED);
        real(helper, () -> withTerminalOpen(player, () ->
                assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.BASIC, 0L)),
                        Result.INVALID_TIER, SETTLED, 0, 0, "the basic tier has no license")));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theRightWindowBuysTheLicense(GameTestHelper helper) {
        ServerPlayer player = buyer(helper, SETTLED);
        real(helper, () -> withTerminalOpen(player, () -> {
            Outcome outcome = LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L));
            assertResult(helper, Result.OK, outcome, "the right window");
            helper.assertTrue(outcome.won() == 3000L && AccountService.won(player) == 97_000L, "3.000 Won charged");
        }));
        helper.succeed();
    }

    // ---- The order of the checks ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theChecksRunInTheirOrder(GameTestHelper helper) {
        real(helper, () -> {
            // The basic tier is refused before its (non-existent) price is looked at, whatever price was seen.
            ServerPlayer basic = buyer(helper, SETTLED);
            assertRefused(helper, basic, LicenseShop.buy(basic, CapsuleTier.BASIC, 3000L, C), Result.INVALID_TIER, SETTLED, 0, 0, "basic tier with a seen price of 3000");

            // A stale price wins over an active debt.
            Account active = new Account(10_000L, 500L, DebtState.ACTIVE);
            ServerPlayer staleAndIndebted = buyer(helper, active);
            assertRefused(helper, staleAndIndebted, LicenseShop.buy(staleAndIndebted, CapsuleTier.ADVANCED, 2999L, C), Result.STALE, active, 0, 0, "wrong price, active debt");

            // An active debt wins over having no Won.
            Account brokeAndIndebted = new Account(0L, 500L, DebtState.ACTIVE);
            ServerPlayer broke = buyer(helper, brokeAndIndebted);
            assertRefused(helper, broke, LicenseShop.buy(broke, CapsuleTier.ADVANCED, 3000L, C), Result.DEBT_ACTIVE, brokeAndIndebted, 0, 0, "active debt, no Won");

            // A lock wins over having no Won also for the diamond one.
            Account poor = new Account(0L, 0L, DebtState.SETTLED);
            ServerPlayer poorNoCapsule = buyer(helper, poor);
            assertRefused(helper, poorNoCapsule, LicenseShop.buy(poorNoCapsule, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, poor, 0, 0, "no advanced capsule, no Won");

            // Having it wins over having no Won.
            ServerPlayer carrying = buyer(helper, poor);
            carrying.getInventory().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE_LICENSE.get()));
            assertRefused(helper, carrying, LicenseShop.buy(carrying, CapsuleTier.ADVANCED, 3000L, C), Result.ALREADY_HAVE, poor, 1, 0, "already carried, no Won");

            // No Won wins over no room (nothing is charged either way).
            ServerPlayer brokeAndFull = buyer(helper, poor);
            for (int slot = 0; slot < 36; slot++) {
                brokeAndFull.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
            assertRefused(helper, brokeAndFull, LicenseShop.buy(brokeAndFull, CapsuleTier.ADVANCED, 3000L, C), Result.NOT_ENOUGH_WON, poor, 0, 0, "no Won, full inventory");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theOfferShowsThePriceAndTheLockOfEachLicense(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, new Account(0L, 500L, DebtState.ACTIVE));
            List<LicenseOffer> offers = LicenseShop.offers(player, C);
            helper.assertTrue(offers.size() == 2, "two licenses on sale: " + offers.size());
            helper.assertTrue(offers.get(0).tier() == CapsuleTier.ADVANCED && offers.get(0).price() == 3000L && offers.get(0).lock() == LicenseLock.DEBT_ACTIVE, "advanced: " + offers.get(0));
            helper.assertTrue(offers.get(1).tier() == CapsuleTier.DIAMOND && offers.get(1).price() == 15000L && offers.get(1).lock() == LicenseLock.NEEDS_ADVANCED, "diamond: " + offers.get(1));
            player.setData(ModAttachments.ACCOUNT.get(), SETTLED);
            player.getInventory().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE_LICENSE.get()));
            player.getInventory().setItem(1, new ItemStack(ModItems.ADVANCED_CAPSULE.get()));
            offers = LicenseShop.offers(player, C);
            helper.assertTrue(offers.get(0).lock() == LicenseLock.ALREADY_HAVE && offers.get(1).lock() == LicenseLock.OPEN, "carried and open: " + offers);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void everyResultThatSpeaksHasAMessage(GameTestHelper helper) {
        for (Result result : Result.values()) {
            Optional<net.minecraft.network.chat.Component> message = LicenseShop.message(new Outcome(result, CapsuleTier.ADVANCED, 3000L));
            helper.assertTrue(message.isPresent() == (result != Result.NO_TERMINAL), result + (message.isPresent() ? " speaks" : " is silent"));
            message.ifPresent(m -> helper.assertTrue(m.getContents() instanceof TranslatableContents, result + " is a translatable message"));
        }
        helper.succeed();
    }

    // ---- The recipes ----

    private static Optional<ItemStack> craft(GameTestHelper helper, int width, int height, ItemStack... grid) {
        CraftingInput input = CraftingInput.of(width, height, List.of(grid));
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()));
    }

    private static ItemStack s(Item item) {
        return new ItemStack(item);
    }

    private static List<ItemStack> advancedGrid(ItemStack license, ItemStack capsule) {
        ItemStack d = s(Items.DIAMOND);
        ItemStack g = s(Items.GOLD_BLOCK);
        return List.of(d, license, d.copy(), g, capsule, g.copy(), d.copy(), ItemStack.EMPTY, d.copy());
    }

    private static ItemStack[] grid(List<ItemStack> items) {
        return items.toArray(ItemStack[]::new);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAdvancedRecipeNeedsTheLicenseAndTheBasicCapsule(GameTestHelper helper) {
        ItemStack license = s(ModItems.ADVANCED_CAPSULE_LICENSE.get());
        ItemStack basic = s(ModItems.CAPSULE.get());
        Optional<ItemStack> made = craft(helper, 3, 3, grid(advancedGrid(license, basic)));
        helper.assertTrue(made.isPresent() && made.get().is(ModItems.ADVANCED_CAPSULE.get()), "license + basic capsule + materials: an advanced capsule");
        helper.assertTrue(made.isPresent() && made.get().getCount() == 1, "it makes exactly one");
        helper.assertFalse(craft(helper, 3, 3, grid(advancedGrid(ItemStack.EMPTY, basic))).isPresent(), "without the license: nothing");
        helper.assertFalse(craft(helper, 3, 3, grid(advancedGrid(license, ItemStack.EMPTY))).isPresent(), "without the basic capsule: nothing");
        helper.assertFalse(craft(helper, 3, 3, grid(advancedGrid(license, s(ModItems.ADVANCED_CAPSULE.get())))).isPresent(), "an advanced capsule is not a basic one");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAdvancedRecipeRefusesTheDiamondLicense(GameTestHelper helper) {
        helper.assertFalse(craft(helper, 3, 3, grid(advancedGrid(s(ModItems.DIAMOND_CAPSULE_LICENSE.get()), s(ModItems.CAPSULE.get())))).isPresent(),
                "the diamond license does not upgrade a basic capsule");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAdvancedRecipeNeedsAllItsMaterials(GameTestHelper helper) {
        ItemStack license = s(ModItems.ADVANCED_CAPSULE_LICENSE.get());
        ItemStack basic = s(ModItems.CAPSULE.get());
        List<ItemStack> threeDiamonds = new java.util.ArrayList<>(advancedGrid(license, basic));
        threeDiamonds.set(8, ItemStack.EMPTY);
        helper.assertFalse(craft(helper, 3, 3, grid(threeDiamonds)).isPresent(), "3 diamonds instead of 4: nothing");
        List<ItemStack> oneBlock = new java.util.ArrayList<>(advancedGrid(license, basic));
        oneBlock.set(5, ItemStack.EMPTY);
        helper.assertFalse(craft(helper, 3, 3, grid(oneBlock)).isPresent(), "1 gold block instead of 2: nothing");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondRecipeNeedsTheLicenseAndTheAdvancedCapsule(GameTestHelper helper) {
        ItemStack star = s(Items.NETHER_STAR);
        ItemStack block = s(Items.NETHERITE_BLOCK);
        ItemStack license = s(ModItems.DIAMOND_CAPSULE_LICENSE.get());
        ItemStack advanced = s(ModItems.ADVANCED_CAPSULE.get());
        Optional<ItemStack> made = craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, advanced, block);
        helper.assertTrue(made.isPresent() && made.get().is(ModItems.DIAMOND_CAPSULE.get()), "license + advanced capsule + materials: a diamond capsule");
        helper.assertTrue(made.isPresent() && made.get().getCount() == 1, "it makes exactly one");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, ItemStack.EMPTY, advanced, block).isPresent(), "without the license: nothing");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, ItemStack.EMPTY, block).isPresent(), "without the advanced capsule: nothing");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, s(ModItems.CAPSULE.get()), block).isPresent(), "a basic capsule is not enough");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, license, advanced, block).isPresent(), "without the nether star: nothing");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, advanced, ItemStack.EMPTY).isPresent(), "without the netherite block: nothing");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondRecipeRefusesTheAdvancedLicense(GameTestHelper helper) {
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, s(Items.NETHER_STAR), ItemStack.EMPTY, s(ModItems.ADVANCED_CAPSULE_LICENSE.get()),
                s(ModItems.ADVANCED_CAPSULE.get()), s(Items.NETHERITE_BLOCK)).isPresent(), "the advanced license does not upgrade an advanced capsule");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theUpgradesUseEverythingUpAndWorkInTheRealWorld(GameTestHelper helper) {
        ItemStack license = s(ModItems.ADVANCED_CAPSULE_LICENSE.get());
        CraftingInput input = CraftingInput.of(3, 3, advancedGrid(license, s(ModItems.CAPSULE.get())));
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        helper.assertTrue(holder.value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "nothing is left behind, not even the license");
        helper.assertFalse(holder.value() instanceof SatisfyShapedRecipe, "it is not a Satisfy-only recipe: it is made in the real world");
        helper.succeed();
    }
}
