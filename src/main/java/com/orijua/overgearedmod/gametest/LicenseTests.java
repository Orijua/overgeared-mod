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

/**
 * The capsule licenses sold at the terminal and the crafting recipes that use them up. A rejected order must leave the
 * Won and the inventory exactly as they were.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class LicenseTests {
    private static final CapsuleBalance C = CapsuleBalance.DEFAULT;
    private static final Account SETTLED = new Account(100_000L, 0L, DebtState.SETTLED);

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
    public static void theDiamondLicenseNeedsAnAdvancedCapsule(GameTestHelper helper) {
        real(helper, () -> {
            Account rich = new Account(100_000L, 0L, DebtState.SETTLED);
            ServerPlayer none = buyer(helper, rich);
            assertRefused(helper, none, LicenseShop.buy(none, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, rich, 0, 0, "no advanced capsule, debt settled");

            ServerPlayer basic = buyer(helper, rich);
            basic.getInventory().setItem(0, new ItemStack(ModItems.CAPSULE.get()));
            assertRefused(helper, basic, LicenseShop.buy(basic, CapsuleTier.DIAMOND, 15000L, C), Result.NEEDS_ADVANCED, rich, 0, 0, "a basic capsule is not enough");

            ServerPlayer held = buyer(helper, rich);
            held.getInventory().setItem(0, new ItemStack(ModItems.ADVANCED_CAPSULE.get()));
            Outcome outcome = LicenseShop.buy(held, CapsuleTier.DIAMOND, 15000L, C);
            assertResult(helper, Result.OK, outcome, "an advanced capsule in the inventory");
            helper.assertTrue(AccountService.won(held) == 85_000L && count(held, ModItems.DIAMOND_CAPSULE_LICENSE.get()) == 1, "15.000 Won for the license");

            ServerPlayer offHand = buyer(helper, rich);
            offHand.getInventory().offhand.set(0, new ItemStack(ModItems.DIAMOND_CAPSULE.get()));
            assertResult(helper, Result.OK, LicenseShop.buy(offHand, CapsuleTier.DIAMOND, 15000L, C), "a diamond capsule in the off hand");

            ServerPlayer connected = buyer(helper, rich);
            connected.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.ADVANCED);
            assertResult(helper, Result.OK, LicenseShop.buy(connected, CapsuleTier.DIAMOND, 15000L, C), "last connected with an advanced capsule, none carried");
        });
        helper.succeed();
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
            CapsuleBalance dearer = new CapsuleBalance(55, 8, 75, 6, 4000L, 15000L, 10, 10, 0.9);
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
    public static void aFullInventoryDropsTheLicenseAtTheFeet(GameTestHelper helper) {
        real(helper, () -> {
            ServerPlayer player = buyer(helper, SETTLED);
            for (int slot = 0; slot < 36; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
            Outcome outcome = LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C);
            assertResult(helper, Result.OK_DROPPED, outcome, "full inventory");
            helper.assertTrue(AccountService.won(player) == 97_000L, "the Won is charged anyway");
            helper.assertTrue(count(player, ModItems.ADVANCED_CAPSULE_LICENSE.get()) == 0, "nothing fitted");
            List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4.0),
                    entity -> entity.getItem().is(ModItems.ADVANCED_CAPSULE_LICENSE.get()));
            helper.assertTrue(dropped.size() == 1, "the license is on the ground near the player: " + dropped.size());
            dropped.forEach(ItemEntity::discard);
        });
        helper.succeed();
    }

    // ---- Where it can be bought ----

    @GameTest(template = TestSupport.EMPTY)
    public static void inSatisfyAndWithTheWrongTierOrWindowNothingHappens(GameTestHelper helper) {
        ServerPlayer player = buyer(helper, SETTLED);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)), () -> CapsuleBalance.withOverride(C, () -> {
            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L)),
                        Result.IN_SATISFY, SETTLED, 0, 0, "in Satisfy");
                assertRefused(helper, player, LicenseShop.buy(player, CapsuleTier.ADVANCED, 3000L, C), Result.IN_SATISFY, SETTLED, 0, 0, "in Satisfy, direct");
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
        }));
        real(helper, () -> {
            assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L)),
                    Result.NO_TERMINAL, SETTLED, 0, 0, "no window open");
            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(8, CapsuleTier.ADVANCED, 3000L)),
                        Result.NO_TERMINAL, SETTLED, 0, 0, "another window");
                assertRefused(helper, player, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.BASIC, 0L)),
                        Result.INVALID_TIER, SETTLED, 0, 0, "the basic tier has no license");
                assertResult(helper, Result.OK, LicenseShop.buy(player, new TerminalBuyLicensePayload(7, CapsuleTier.ADVANCED, 3000L)), "the right window");
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
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

    @GameTest(template = TestSupport.EMPTY)
    public static void theAdvancedRecipeNeedsTheLicenseAndTheBasicCapsule(GameTestHelper helper) {
        ItemStack license = s(ModItems.ADVANCED_CAPSULE_LICENSE.get());
        ItemStack basic = s(ModItems.CAPSULE.get());
        Optional<ItemStack> made = craft(helper, 3, 3, advancedGrid(license, basic).toArray(ItemStack[]::new));
        helper.assertTrue(made.isPresent() && made.get().is(ModItems.ADVANCED_CAPSULE.get()), "license + basic capsule + materials: an advanced capsule");
        helper.assertFalse(craft(helper, 3, 3, advancedGrid(ItemStack.EMPTY, basic).toArray(ItemStack[]::new)).isPresent(), "without the license: nothing");
        helper.assertFalse(craft(helper, 3, 3, advancedGrid(license, ItemStack.EMPTY).toArray(ItemStack[]::new)).isPresent(), "without the basic capsule: nothing");
        helper.assertFalse(craft(helper, 3, 3, advancedGrid(license, s(ModItems.ADVANCED_CAPSULE.get())).toArray(ItemStack[]::new)).isPresent(), "an advanced capsule is not a basic one");
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
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, ItemStack.EMPTY, advanced, block).isPresent(), "without the license: nothing");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, ItemStack.EMPTY, block).isPresent(), "without the advanced capsule: nothing");
        helper.assertFalse(craft(helper, 3, 2, ItemStack.EMPTY, star, ItemStack.EMPTY, license, s(ModItems.CAPSULE.get()), block).isPresent(), "a basic capsule is not enough");
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
