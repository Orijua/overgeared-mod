package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.insight.TalentSearch;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.npc.ApprenticeRules;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The economy, step 1: the account (Won and debt), the Satisfy emerald and the shop of Smith, which trades in it.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class EconomyTests {
    private EconomyTests() {
    }

    private static boolean refused(GameTestHelper helper, ServerPlayer player, String command, int permission) {
        try {
            helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute(command, player.createCommandSourceStack().withPermission(permission));
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    private static MerchantOffer offerFor(BlacksmithNpc smith, net.minecraft.world.item.Item result) {
        return smith.getOffers().stream().filter(offer -> offer.getResult().is(result)).findFirst().orElseThrow();
    }

    private static long count(MerchantOffers offers, net.minecraft.world.item.Item result) {
        return offers.stream().filter(offer -> offer.getResult().is(result)).count();
    }

    /** Runs a command as {@code player} with the permission level, collecting what it answers; throws if refused. */
    private static List<Component> run(GameTestHelper helper, ServerPlayer player, String command, int permission) throws CommandSyntaxException {
        List<Component> answers = new ArrayList<>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                answers.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = player.createCommandSourceStack().withPermission(permission).withSource(sink);
        helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        return answers;
    }

    private static boolean sameOffer(MerchantOffer a, MerchantOffer b) {
        return ItemStack.isSameItemSameComponents(a.getResult(), b.getResult()) && a.getResult().getCount() == b.getResult().getCount()
                && ItemStack.isSameItemSameComponents(a.getBaseCostA(), b.getBaseCostA()) && a.getBaseCostA().getCount() == b.getBaseCostA().getCount()
                && ItemStack.isSameItemSameComponents(a.getCostB(), b.getCostB()) && a.getCostB().getCount() == b.getCostB().getCount();
    }

    /** The shop after a restock is exactly the catalog of a new Smith: every offer once, none missing, none extra. */
    private static void assertFullCatalog(GameTestHelper helper, BlacksmithNpc restocked) {
        BlacksmithNpc fresh = helper.spawn(ModEntities.BLACKSMITH.get(), 3, 1, 1);
        MerchantOffers expected = fresh.getOffers();
        MerchantOffers actual = restocked.getOffers();
        helper.assertTrue(actual.size() == expected.size(), "the catalog has " + expected.size() + " offers, not " + actual.size());
        for (MerchantOffer offer : expected) {
            long matches = actual.stream().filter(other -> sameOffer(offer, other)).count();
            helper.assertTrue(matches == 1, "offer for " + offer.getResult() + " appears " + matches + " times");
        }
        helper.assertTrue(count(actual, ModItems.ENHANCEMENT_STONE.get()) == 1, "one enhancement stone");
        helper.assertTrue(count(actual, ModItems.RESPEC_SCROLL.get()) == 1, "one respec scroll");
        helper.assertTrue(actual.stream().filter(offer -> offer.getBaseCostA().is(ModItems.MITHRIL_INGOT.get())).count() == 1, "one mithril offer");
        helper.assertTrue(count(actual, ModItems.SATISFY_EMERALD.get()) == 1, "one offer gives Satisfy emeralds");
        helper.assertTrue(count(actual, ModItems.EYE_OF_PALLALIAN.get()) == 1, "one Eye");
        helper.assertTrue(count(actual, ModItems.APPRENTICE_CONTRACT.get()) == 1, "one contract");
        helper.assertTrue(count(actual, ModItems.SKILL_BOOK.get()) == count(expected, ModItems.SKILL_BOOK.get()), "all the books");
        MerchantOffer mithril = actual.stream().filter(offer -> offer.getBaseCostA().is(ModItems.MITHRIL_INGOT.get())).findFirst().orElseThrow();
        helper.assertTrue(mithril.getBaseCostA().getCount() == 4 && mithril.getResult().is(ModItems.SATISFY_EMERALD.get())
                && mithril.getResult().getCount() == 3, "the mithril gives three Satisfy emeralds");
        MerchantOffer contract = offerFor(restocked, ModItems.APPRENTICE_CONTRACT.get());
        helper.assertTrue(contract.getBaseCostA().is(ModItems.SATISFY_EMERALD.get())
                && contract.getBaseCostA().getCount() == ApprenticeRules.CONTRACT_PRICE, "the contract costs its price in Satisfy emeralds");
        fresh.discard();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAccountSurvivesRespawn(GameTestHelper helper) {
        ServerPlayer old = TestSupport.fakePlayer(helper);
        AccountService.setWon(old, 700);
        old.setData(ModAttachments.ACCOUNT.get(), new Account(700, 20));
        ServerPlayer fresh = TestSupport.fakePlayer(helper);
        helper.assertTrue(AccountService.get(fresh).equals(Account.EMPTY), "a new player has an empty account");
        fresh.restoreFrom(old, false);
        helper.assertTrue(AccountService.get(fresh).equals(new Account(700, 20)), "dying does not touch the money");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAccountIsSavedWithThePlayer(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(321, 9));
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer other = TestSupport.fakePlayer(helper);
        other.load(saved);
        helper.assertTrue(AccountService.get(other).equals(new Account(321, 9)), "the account is read back from the saved player");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resetAllClearsTheAccountButNotTheVault(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
        CharacterVault vault = CharacterVault.of(WorldSide.SATISFY, CharacterSnapshot.capture(player));
        player.setData(ModAttachments.CHARACTER_VAULT.get(), vault);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(500, 40));
        player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY.disconnected(player.blockPosition()));

        ModAttachments.resetAll(player);

        helper.assertTrue(AccountService.get(player).equals(Account.EMPTY), "reset all empties Won and debt");
        helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "and the vault does not change");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebugCommandsSetAndShowTheAccount(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        dispatcher.execute("satisfy debug set won 500", player.createCommandSourceStack().withPermission(4));
        helper.assertTrue(AccountService.won(player) == 500, "500 Won");
        helper.assertTrue(refused(helper, player, "satisfy debug set won -1", 4), "a negative amount is refused");
        helper.assertTrue(AccountService.won(player) == 500, "and nothing changed");
        helper.assertTrue(refused(helper, player, "satisfy debug set won 10", 0), "the command is for operators");
        helper.assertTrue(AccountService.won(player) == 500, "and nothing changed either");
        helper.assertTrue(dispatcher.execute("satisfy debug account", player.createCommandSourceStack().withPermission(4)) == 1, "account answers");
        helper.assertTrue(refused(helper, player, "satisfy debug account", 0), "account is for operators too");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void noOfferOfSmithUsesTheVanillaEmerald(GameTestHelper helper) {
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        for (MerchantOffer offer : smith.getOffers()) {
            helper.assertFalse(offer.getBaseCostA().is(Items.EMERALD), "cost A is not the vanilla emerald");
            helper.assertFalse(offer.getCostB().is(Items.EMERALD), "cost B is not the vanilla emerald");
            helper.assertFalse(offer.getResult().is(Items.EMERALD), "the result is not the vanilla emerald");
        }
        MerchantOffer mithril = offerFor(smith, ModItems.SATISFY_EMERALD.get());
        helper.assertTrue(mithril.getBaseCostA().is(ModItems.MITHRIL_INGOT.get()) && mithril.getBaseCostA().getCount() == 4,
                "the mithril costs four ingots");
        helper.assertTrue(mithril.getResult().getCount() == 3, "and gives three Satisfy emeralds");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void smithTakesTheSatisfyEmeraldAndNotTheVanillaOne(GameTestHelper helper) {
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        MerchantOffer stone = offerFor(smith, ModItems.ENHANCEMENT_STONE.get());
        helper.assertTrue(stone.satisfiedBy(new ItemStack(ModItems.SATISFY_EMERALD.get(), 6), ItemStack.EMPTY), "six Satisfy emeralds buy the stone");
        helper.assertFalse(stone.satisfiedBy(new ItemStack(Items.EMERALD, 6), ItemStack.EMPTY), "six vanilla emeralds do not");
        MerchantOffer contract = offerFor(smith, ModItems.APPRENTICE_CONTRACT.get());
        helper.assertTrue(contract.satisfiedBy(new ItemStack(ModItems.SATISFY_EMERALD.get(), ApprenticeRules.CONTRACT_PRICE), ItemStack.EMPTY),
                "the contract is paid in Satisfy emeralds");
        helper.assertFalse(contract.satisfiedBy(new ItemStack(Items.EMERALD, ApprenticeRules.CONTRACT_PRICE), ItemStack.EMPTY),
                "and not in vanilla ones");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anOldSmithIsRestockedInTheNewCurrency(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        smith.getOffers().clear();
        smith.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(ModItems.ENHANCEMENT_STONE.get()), 16, 0, 0.05f));
        smith.getOffers().add(new MerchantOffer(new ItemCost(ModItems.MITHRIL_INGOT.get(), 4), new ItemStack(Items.EMERALD, 3), 16, 0, 0.05f));

        smith.startTrading(player);
        MerchantOffers offers = smith.getOffers();
        helper.assertTrue(offers.stream().noneMatch(offer -> offer.getBaseCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD)
                || offer.getResult().is(Items.EMERALD)), "no vanilla emerald is left in the shop");
        helper.assertTrue(count(offers, ModItems.EYE_OF_PALLALIAN.get()) == 1, "the Eye appears once");
        helper.assertTrue(count(offers, ModItems.APPRENTICE_CONTRACT.get()) == 1, "and the contract too");
        int size = offers.size();
        smith.setTradingPlayer(null);
        smith.startTrading(player);
        helper.assertTrue(smith.getOffers().size() == size, "a second opening changes nothing");
        helper.assertTrue(count(smith.getOffers(), ModItems.EYE_OF_PALLALIAN.get()) == 1, "still one Eye");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theSatisfyEmeraldIsNotAVanillaOne(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.SATISFY_EMERALD.get());
        helper.assertFalse(stack.is(Items.EMERALD), "it is not the vanilla emerald");
        helper.assertFalse(stack.is(Tags.Items.GEMS_EMERALD), "nor is it in the emerald gem tag");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bairansQuestPaysSatisfyEmeraldsAndNoVanillaOnes(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.world.entity.Entity chief = helper.spawn(ModEntities.VILLAGE_CHIEF.get(), 1, 1, 2);
        TestSupport.completed(player, "watch_commander");
        var quest = Overgearedmod.id("bairan_talents");
        helper.assertTrue(QuestService.accept(player, quest, chief) == QuestService.Result.SUCCESS, "accepted at the chief");
        for (int i = 0; i < 3; i++) {
            GoalTracker.milestone(player, TalentSearch.MILESTONE);
        }
        helper.assertTrue(QuestService.claim(player, quest, chief) == QuestService.Result.SUCCESS, "claimed");
        helper.assertTrue(player.getInventory().countItem(ModItems.SATISFY_EMERALD.get()) == 16, "sixteen Satisfy emeralds");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 0, "and no vanilla emerald");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void depositSaturatesAtTheMaximum(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        AccountService.setWon(player, Long.MAX_VALUE - 5);
        AccountService.deposit(player, 100);
        helper.assertTrue(AccountService.won(player) == Long.MAX_VALUE, "the balance stops at the maximum");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withdrawingMoreThanTheBalanceLeavesZero(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        AccountService.setWon(player, 40);
        AccountService.withdraw(player, 100);
        helper.assertTrue(AccountService.won(player) == 0, "the balance does not go negative");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void depositAndWithdrawKeepTheDebt(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(100, 33));
        AccountService.deposit(player, 50);
        helper.assertTrue(AccountService.get(player).equals(new Account(150, 33)), "deposit keeps the debt");
        AccountService.withdraw(player, 70);
        helper.assertTrue(AccountService.get(player).equals(new Account(80, 33)), "withdraw keeps the debt");
        AccountService.setWon(player, -5);
        helper.assertTrue(AccountService.get(player).equals(new Account(0, 33)), "a negative balance becomes 0, still with the debt");
        helper.succeed();
    }

    /** The same value is not written again: the stored account is still the very same object (a write would store a new one). */
    @GameTest(template = TestSupport.EMPTY)
    public static void settingTheSameWonDoesNotWriteAgain(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        AccountService.setWon(player, 25);
        Account before = AccountService.get(player);
        AccountService.setWon(player, 25);
        helper.assertTrue(AccountService.get(player) == before, "the same value does not rewrite the account");
        AccountService.setWon(player, 26);
        helper.assertTrue(AccountService.get(player) != before && AccountService.won(player) == 26, "a different value does");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebugCommandPermissionIsLevelTwo(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        for (String command : new String[]{"satisfy debug set won 5", "satisfy debug set won 0", "satisfy debug account"}) {
            helper.assertTrue(refused(helper, player, command, 1), "level 1 is refused: " + command);
            run(helper, player, command, 2);
        }
        helper.assertTrue(AccountService.won(player) == 0, "level 2 ran the last 'set won 0' and the balance is 0");
        run(helper, player, "satisfy debug set won 5", 2);
        helper.assertTrue(AccountService.won(player) == 5, "level 2 may set the balance");
        run(helper, player, "satisfy debug set won 0", 2);
        helper.assertTrue(AccountService.won(player) == 0, "zero is valid");
        run(helper, player, "satisfy debug set won 9223372036854775807", 2);
        helper.assertTrue(AccountService.won(player) == Long.MAX_VALUE, "the largest amount is valid");
        helper.assertTrue(refused(helper, player, "satisfy debug set won 9223372036854775808", 4), "one more than the largest is not a number");
        helper.assertTrue(AccountService.won(player) == Long.MAX_VALUE, "and nothing changed");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAccountCommandAnswersWithWonAndDebt(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(1234, 56));
        List<Component> answers = run(helper, player, "satisfy debug account", 2);
        helper.assertTrue(answers.size() == 1, "one answer");
        helper.assertTrue(answers.get(0).getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("message.overgearedmod.debug_account")
                && contents.getArgs().length == 2 && Long.valueOf(1234).equals(contents.getArgs()[0]) && Long.valueOf(56).equals(contents.getArgs()[1]),
                "it carries the Won and the debt: " + answers.get(0));
        List<Component> set = run(helper, player, "satisfy debug set won 77", 2);
        helper.assertTrue(set.size() == 1 && set.get(0).getContents() instanceof TranslatableContents confirm
                && confirm.getKey().equals("message.overgearedmod.debug_won") && Long.valueOf(77).equals(confirm.getArgs()[0]),
                "set won confirms the amount");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anOldOfferWithTheVanillaEmeraldAsCostBIsRestocked(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        smith.getOffers().add(new MerchantOffer(new ItemCost(ModItems.SATISFY_EMERALD.get(), 6), Optional.of(new ItemCost(Items.EMERALD, 2)),
                new ItemStack(ModItems.RESPEC_SCROLL.get()), 4, 0, 0.05f));
        smith.startTrading(player);
        helper.assertTrue(smith.getOffers().stream().noneMatch(offer -> offer.getBaseCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD)
                || offer.getResult().is(Items.EMERALD)), "no vanilla emerald is left");
        assertFullCatalog(helper, smith);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theVanillaEyeOfPallalianBecomesTheSatisfyOne(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        smith.getOffers().clear();
        smith.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 32), Optional.of(new ItemCost(Items.DIAMOND, 2)),
                new ItemStack(ModItems.EYE_OF_PALLALIAN.get()), 2, 0, 0.05f));
        smith.startTrading(player);
        MerchantOffer eye = offerFor(smith, ModItems.EYE_OF_PALLALIAN.get());
        helper.assertTrue(eye.getBaseCostA().is(ModItems.SATISFY_EMERALD.get()) && eye.getBaseCostA().getCount() == 32, "32 Satisfy emeralds");
        helper.assertTrue(eye.getCostB().is(Items.DIAMOND) && eye.getCostB().getCount() == 2, "and the two diamonds stay");
        assertFullCatalog(helper, smith);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSpentOldOfferComesBackWithoutUses(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlacksmithNpc smith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        smith.getOffers().clear();
        MerchantOffer spent = new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(ModItems.ENHANCEMENT_STONE.get()), 16, 0, 0.05f);
        spent.setToOutOfStock();
        helper.assertTrue(spent.isOutOfStock() && spent.getUses() == spent.getMaxUses(), "the old offer is spent");
        smith.getOffers().add(spent);
        smith.startTrading(player);
        helper.assertTrue(smith.getOffers().stream().allMatch(offer -> offer.getUses() == 0 && !offer.isOutOfStock()), "every offer is fresh");
        helper.assertTrue(offerFor(smith, ModItems.ENHANCEMENT_STONE.get()).getBaseCostA().is(ModItems.SATISFY_EMERALD.get()), "and the stone costs Satisfy emeralds");
        assertFullCatalog(helper, smith);
        helper.succeed();
    }
}
