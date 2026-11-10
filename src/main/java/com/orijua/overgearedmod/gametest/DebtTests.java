package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.DebtBalance;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.StarterCapsule;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.DebtService;
import com.orijua.overgearedmod.feature.economy.DebtState;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
import com.orijua.overgearedmod.feature.economy.TerminalPayDebtPayload;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalState;
import com.orijua.overgearedmod.feature.economy.WorkOrder;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The starter debt (economy, step 5): taken with the starter capsule, paid by the quota of every shift and by hand from
 * the terminal, saved with the player, and the debug commands that set it. The services are called directly; the window
 * is built by hand because a fake player has no real connection to open one.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class DebtTests {
    private static final BlockPos LANDING = new BlockPos(1, 1, 1);
    private static final WorkOrder COAL_32 = new WorkOrder(ResourceLocation.withDefaultNamespace("coals"), true, 32);
    private static final TerminalBalance COAL_ONLY = new TerminalBalance(100, 1000L, List.of(COAL_32));
    private static final DebtBalance DEBT = DebtBalance.DEFAULT;

    private DebtTests() {
    }

    private static void real(GameTestHelper helper, Runnable body) {
        CapsuleTests.outsideSatisfy(helper, body);
    }

    /** The player carries the real world's character, with a phone (so a hand-built window stays valid) and the coal of the order. */
    private static ServerPlayer worker(GameTestHelper helper, Account account) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
        player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
        player.getInventory().setItem(1, new ItemStack(Items.COAL, 32));
        player.setData(ModAttachments.ACCOUNT.get(), account);
        return player;
    }

    private static void withWindow(ServerPlayer player, Runnable body) {
        player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
        try {
            body.run();
        } finally {
            player.containerMenu = player.inventoryMenu;
        }
    }

    private static String key(Optional<Component> message) {
        return message.map(component -> ((TranslatableContents) component.getContents()).getKey()).orElse("");
    }

    private static void assertAccount(GameTestHelper helper, ServerPlayer player, Account expected) {
        Account actual = AccountService.get(player);
        helper.assertTrue(actual.equals(expected), "the account should be " + expected + " but is " + actual);
    }

    private static void assertResult(GameTestHelper helper, DebtService.Result expected, DebtService.Outcome actual, String what) {
        helper.assertTrue(actual.result() == expected, what + ": expected " + expected + " but got " + actual);
    }

    /** The players the milestone was announced to while {@code body} runs (nothing is sent to them). */
    private static List<ServerPlayer> announced(Runnable body) {
        List<ServerPlayer> told = new ArrayList<>();
        DebtService.withAnnouncer(told::add, body);
        return told;
    }

    private static int run(GameTestHelper helper, ServerPlayer player, String command, int permission, List<Component> answers) throws CommandSyntaxException {
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
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static boolean refused(GameTestHelper helper, ServerPlayer player, String command, int permission) {
        try {
            run(helper, player, command, permission, new ArrayList<>());
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    // ---- Taking the debt ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theStarterCapsuleBringsTheStarterDebtOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(StarterCapsule.giveIfFirstTime(player), "the capsule is given");
        assertAccount(helper, player, new Account(0, DebtBalance.current().starterDebt(), DebtState.ACTIVE));
        AccountService.payWork(player, 1000, 25);
        Account paying = AccountService.get(player);
        helper.assertFalse(StarterCapsule.giveIfFirstTime(player), "never again");
        helper.assertTrue(AccountService.get(player).equals(paying), "the second call changes nothing");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theStarterDebtIsTheConfiguredOne(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        DebtBalance.withOverride(new DebtBalance(1234, 25), () -> helper.assertTrue(StarterCapsule.giveIfFirstTime(player), "the capsule is given"));
        assertAccount(helper, player, new Account(0, 1234, DebtState.ACTIVE));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theCapsuleKeepsTheBalanceThePlayerAlreadyHad(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(500, 0, DebtState.UNASSIGNED));
        helper.assertTrue(StarterCapsule.giveIfFirstTime(player), "the capsule is given");
        assertAccount(helper, player, new Account(500, DebtBalance.current().starterDebt(), DebtState.ACTIVE));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theCapsuleDoesNotReassignADebtThatIsNotUnassigned(GameTestHelper helper) {
        Account settled = new Account(900, 0, DebtState.SETTLED);
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), settled);
        helper.assertTrue(StarterCapsule.giveIfFirstTime(player), "the capsule is given");
        assertAccount(helper, player, settled);

        Account owing = new Account(10, 50, DebtState.ACTIVE);
        ServerPlayer other = TestSupport.fakePlayer(helper);
        other.setData(ModAttachments.ACCOUNT.get(), owing);
        helper.assertTrue(StarterCapsule.giveIfFirstTime(other), "the capsule is given");
        assertAccount(helper, other, owing);

        helper.assertFalse(AccountService.assignStarterDebt(player, 100), "a settled debt is not assigned again");
        helper.assertFalse(AccountService.assignStarterDebt(other, 100), "an active debt is not assigned again");
        ServerPlayer fresh = TestSupport.fakePlayer(helper);
        helper.assertTrue(AccountService.assignStarterDebt(fresh, 100), "an unassigned one is");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPlayerWhoAlreadyHadTheCapsuleOwesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.STARTER_CAPSULE_GIVEN.get(), true);
        helper.assertFalse(StarterCapsule.giveIfFirstTime(player), "nothing is given");
        assertAccount(helper, player, Account.EMPTY);
        helper.succeed();
    }

    // ---- The quota of the shift ----

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftSendsItsQuotaToTheDebt(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(0, 5900, DebtState.ACTIVE));
        real(helper, () -> {
            TerminalService.Outcome outcome = TerminalService.work(player, COAL_32, 5L, COAL_ONLY, DEBT);
            helper.assertTrue(outcome.result() == TerminalService.Result.OK && outcome.won() == 750L && outcome.toDebt() == 250L
                    && !outcome.settled(), "the outcome: " + outcome);
            assertAccount(helper, player, new Account(750, 5650, DebtState.ACTIVE));
            helper.assertTrue(key(TerminalService.message(outcome)).equals("message.overgearedmod.terminal.worked_debt"), "the message tells the split");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftThatSettlesTheDebtAnnouncesItOnceAndAPartialOneDoesNot(GameTestHelper helper) {
        ServerPlayer paying = worker(helper, new Account(0, 5900, DebtState.ACTIVE));
        ServerPlayer last = worker(helper, new Account(0, 100, DebtState.ACTIVE));
        real(helper, () -> {
            helper.assertTrue(announced(() -> TerminalService.work(paying, COAL_32, 5L, COAL_ONLY, DEBT)).isEmpty(), "a partial shift announces nothing");
            List<ServerPlayer> told = announced(() -> TerminalService.work(last, COAL_32, 5L, COAL_ONLY, DEBT));
            helper.assertTrue(told.size() == 1 && told.get(0) == last, "the settling shift announces once, to its player: " + told.size());
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftThatFailsTouchesNeitherTheDebtNorTheBalance(GameTestHelper helper) {
        Account start = new Account(30, 100, DebtState.ACTIVE);
        ServerPlayer noCoal = worker(helper, start);
        noCoal.getInventory().setItem(1, ItemStack.EMPTY);
        ServerPlayer worked = worker(helper, start);
        worked.setData(ModAttachments.WORK_DAY.get(), 5L);
        ServerPlayer stale = worker(helper, start);
        WorkOrder other = new WorkOrder(ResourceLocation.withDefaultNamespace("coals"), true, 16);
        real(helper, () -> {
            List<ServerPlayer> told = announced(() -> {
                helper.assertTrue(TerminalService.work(noCoal, COAL_32, 5L, COAL_ONLY, DEBT).result() == TerminalService.Result.NOT_ENOUGH, "no item");
                helper.assertTrue(TerminalService.work(worked, COAL_32, 5L, COAL_ONLY, DEBT).result() == TerminalService.Result.ALREADY_WORKED, "already worked");
                helper.assertTrue(TerminalService.work(stale, other, 5L, COAL_ONLY, DEBT).result() == TerminalService.Result.STALE, "stale order");
            });
            helper.assertTrue(told.isEmpty(), "nothing is announced");
        });
        assertAccount(helper, noCoal, start);
        assertAccount(helper, worked, start);
        assertAccount(helper, stale, start);
        ServerPlayer inSatisfy = worker(helper, start);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(LANDING), () ->
                helper.assertTrue(TerminalService.work(inSatisfy, COAL_32, 5L, COAL_ONLY, DEBT).result() == TerminalService.Result.IN_SATISFY, "in Satisfy"));
        assertAccount(helper, inSatisfy, start);
        helper.assertTrue(inSatisfy.getInventory().countItem(Items.COAL) == 32, "and the coal stays");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theLastQuotaSettlesTheDebt(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(0, 100, DebtState.ACTIVE));
        real(helper, () -> {
            TerminalService.Outcome outcome = TerminalService.work(player, COAL_32, 5L, COAL_ONLY, DEBT);
            helper.assertTrue(outcome.won() == 900L && outcome.toDebt() == 100L && outcome.settled(), "the outcome: " + outcome);
            assertAccount(helper, player, new Account(900, 0, DebtState.SETTLED));
            helper.assertTrue(key(TerminalService.message(outcome)).equals("message.overgearedmod.terminal.worked_settled"), "the line carries the amount of the shift");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutAnActiveDebtTheWholePayArrives(GameTestHelper helper) {
        for (DebtState state : new DebtState[]{DebtState.UNASSIGNED, DebtState.SETTLED}) {
            ServerPlayer player = worker(helper, new Account(0, 0, state));
            real(helper, () -> {
                TerminalService.Outcome outcome = TerminalService.work(player, COAL_32, 5L, COAL_ONLY, DEBT);
                helper.assertTrue(outcome.won() == 1000L && outcome.toDebt() == 0L && !outcome.settled(), state + ": " + outcome);
                assertAccount(helper, player, new Account(1000, 0, state));
                helper.assertTrue(key(TerminalService.message(outcome)).equals("message.overgearedmod.terminal.worked"), state + " message");
            });
        }
        helper.succeed();
    }

    // ---- Paying by hand ----

    @GameTest(template = TestSupport.EMPTY)
    public static void payingByHandTakesFromTheBalance(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(1000, 6400, DebtState.ACTIVE));
        real(helper, () -> withWindow(player, () -> {
            DebtService.Outcome outcome = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 300, 6400));
            helper.assertTrue(outcome.result() == DebtService.Result.OK && outcome.paid() == 300 && outcome.left() == 6100 && !outcome.settled(),
                    "the outcome: " + outcome);
            assertAccount(helper, player, new Account(700, 6100, DebtState.ACTIVE));
            helper.assertTrue(key(DebtService.message(outcome)).equals("message.overgearedmod.terminal.debt_paid"), "the message");
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentWithoutTheTerminalWindowChangesNothing(GameTestHelper helper) {
        Account start = new Account(100, 6400, DebtState.ACTIVE);
        ServerPlayer player = worker(helper, start);
        real(helper, () -> {
            DebtService.Outcome noWindow = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 50, 6400));
            assertResult(helper, DebtService.Result.NO_TERMINAL, noWindow, "no window");
            helper.assertTrue(DebtService.message(noWindow).isEmpty(), "and it says nothing");
            withWindow(player, () -> {
                DebtService.Outcome other = DebtService.payDebt(player, new TerminalPayDebtPayload(8, 50, 6400));
                assertResult(helper, DebtService.Result.NO_TERMINAL, other, "another container id");
                helper.assertTrue(DebtService.message(other).isEmpty(), "and it says nothing either");
            });
        });
        assertAccount(helper, player, start);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentWithABadAmountChangesNothing(GameTestHelper helper) {
        Account start = new Account(100, 6400, DebtState.ACTIVE);
        ServerPlayer player = worker(helper, start);
        real(helper, () -> withWindow(player, () -> {
            assertResult(helper, DebtService.Result.INVALID_AMOUNT, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 0, 6400)), "zero");
            assertResult(helper, DebtService.Result.INVALID_AMOUNT, DebtService.payDebt(player, new TerminalPayDebtPayload(7, -5, 6400)), "negative");
            assertResult(helper, DebtService.Result.INVALID_AMOUNT, DebtService.payDebt(player, new TerminalPayDebtPayload(7, Long.MIN_VALUE, 6400)), "the smallest");
            // 6401 is above the debt and above the balance at once: the amount check comes first.
            DebtService.Outcome both = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 6401, 6400));
            assertResult(helper, DebtService.Result.INVALID_AMOUNT, both, "above the debt and the balance");
            helper.assertTrue(key(DebtService.message(both)).equals("message.overgearedmod.terminal.invalid_amount"), "its message");
        }));
        assertAccount(helper, player, start);

        Account rich = new Account(10_000, 6400, DebtState.ACTIVE);
        player.setData(ModAttachments.ACCOUNT.get(), rich);
        real(helper, () -> withWindow(player, () ->
                assertResult(helper, DebtService.Result.INVALID_AMOUNT, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 6401, 6400)), "above the debt only")));
        assertAccount(helper, player, rich);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentWithTooLittleBalanceChangesNothing(GameTestHelper helper) {
        Account start = new Account(100, 6400, DebtState.ACTIVE);
        ServerPlayer player = worker(helper, start);
        real(helper, () -> withWindow(player, () -> {
            DebtService.Outcome outcome = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 101, 6400));
            assertResult(helper, DebtService.Result.NOT_ENOUGH_WON, outcome, "more than the balance");
            helper.assertTrue(key(DebtService.message(outcome)).equals("message.overgearedmod.terminal.not_enough_won"), "its message");
        }));
        assertAccount(helper, player, start);

        Account broke = new Account(0, 6400, DebtState.ACTIVE);
        player.setData(ModAttachments.ACCOUNT.get(), broke);
        real(helper, () -> withWindow(player, () ->
                assertResult(helper, DebtService.Result.NOT_ENOUGH_WON, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 1, 6400)), "no Won at all")));
        assertAccount(helper, player, broke);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentWithAnOldDebtChangesNothing(GameTestHelper helper) {
        Account start = new Account(100, 6400, DebtState.ACTIVE);
        ServerPlayer player = worker(helper, start);
        real(helper, () -> withWindow(player, () -> {
            DebtService.Outcome stale = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 50, 6000));
            assertResult(helper, DebtService.Result.STALE, stale, "stale debt");
            helper.assertTrue(key(DebtService.message(stale)).equals("message.overgearedmod.terminal.stale"), "its message");
            // The documented order: NO_DEBT, STALE, INVALID_AMOUNT, NOT_ENOUGH_WON.
            assertResult(helper, DebtService.Result.STALE, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 0, 6000)), "stale and a zero amount");
            assertResult(helper, DebtService.Result.STALE, DebtService.payDebt(player, new TerminalPayDebtPayload(7, -3, 1)), "stale and a negative amount");
            assertResult(helper, DebtService.Result.STALE, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 9999, 6000)), "stale and too much");
        }));
        assertAccount(helper, player, start);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentWithoutAnActiveDebtChangesNothing(GameTestHelper helper) {
        ServerPlayer player = worker(helper, Account.EMPTY);
        for (DebtState state : new DebtState[]{DebtState.UNASSIGNED, DebtState.SETTLED}) {
            Account none = new Account(100, 0, state);
            player.setData(ModAttachments.ACCOUNT.get(), none);
            real(helper, () -> withWindow(player, () -> {
                DebtService.Outcome outcome = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 50, 0));
                assertResult(helper, DebtService.Result.NO_DEBT, outcome, state + " owes nothing");
                helper.assertTrue(key(DebtService.message(outcome)).equals("message.overgearedmod.terminal.no_debt"), "its message");
                assertResult(helper, DebtService.Result.NO_DEBT, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 0, 5)), state + ", old and a zero amount");
            }));
            assertAccount(helper, player, none);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentInSatisfyChangesNothing(GameTestHelper helper) {
        Account start = new Account(100, 6400, DebtState.ACTIVE);
        ServerPlayer player = worker(helper, start);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(LANDING), () -> withWindow(player, () ->
                assertResult(helper, DebtService.Result.IN_SATISFY, DebtService.payDebt(player, new TerminalPayDebtPayload(7, 50, 6400)), "in Satisfy")));
        assertAccount(helper, player, start);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void payingExactlyTheBalancePaysAndLeavesNothing(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(300, 6400, DebtState.ACTIVE));
        real(helper, () -> withWindow(player, () -> {
            DebtService.Outcome outcome = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 300, 6400));
            helper.assertTrue(outcome.result() == DebtService.Result.OK && outcome.paid() == 300 && outcome.left() == 6100 && !outcome.settled(), "the outcome: " + outcome);
        }));
        assertAccount(helper, player, new Account(0, 6100, DebtState.ACTIVE));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPaymentThatSettlesAnnouncesTheMilestoneAndAPartialOneDoesNot(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(1000, 400, DebtState.ACTIVE));
        real(helper, () -> withWindow(player, () -> {
            helper.assertTrue(announced(() -> DebtService.payDebt(player, new TerminalPayDebtPayload(7, 100, 400))).isEmpty(), "a partial payment announces nothing");
            helper.assertTrue(announced(() -> DebtService.payDebt(player, new TerminalPayDebtPayload(7, 5000, 300))).isEmpty(), "a refused one announces nothing");
            List<ServerPlayer> told = announced(() -> DebtService.payDebt(player, new TerminalPayDebtPayload(7, 300, 300)));
            helper.assertTrue(told.size() == 1 && told.get(0) == player, "the settling payment announces once, to its player: " + told.size());
            helper.assertTrue(announced(() -> DebtService.payDebt(player, new TerminalPayDebtPayload(7, 1, 0))).isEmpty(), "and nothing after");
        }));
        assertAccount(helper, player, new Account(600, 0, DebtState.SETTLED));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDoubleClickPaysOnce(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(1000, 6400, DebtState.ACTIVE));
        real(helper, () -> withWindow(player, () -> {
            TerminalPayDebtPayload click = new TerminalPayDebtPayload(7, 300, 6400);
            helper.assertTrue(DebtService.payDebt(player, click).result() == DebtService.Result.OK, "the first click pays");
            helper.assertTrue(DebtService.payDebt(player, click).result() == DebtService.Result.STALE, "the second saw an old debt");
            assertAccount(helper, player, new Account(700, 6100, DebtState.ACTIVE));
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void payingTheWholeDebtSettlesItAndThenThereIsNothingToPay(GameTestHelper helper) {
        ServerPlayer player = worker(helper, new Account(500, 400, DebtState.ACTIVE));
        real(helper, () -> withWindow(player, () -> {
            DebtService.Outcome outcome = DebtService.payDebt(player, new TerminalPayDebtPayload(7, 400, 400));
            helper.assertTrue(outcome.result() == DebtService.Result.OK && outcome.settled() && outcome.left() == 0, "the outcome: " + outcome);
            assertAccount(helper, player, new Account(100, 0, DebtState.SETTLED));
            helper.assertTrue(key(DebtService.message(outcome)).equals("message.overgearedmod.terminal.debt_paid_off"), "the short line, not Grid's phrase again");
            helper.assertTrue(DebtService.payDebt(player, new TerminalPayDebtPayload(7, 1, 0)).result() == DebtService.Result.NO_DEBT, "then no debt");
        }));
        helper.succeed();
    }

    // ---- Saving ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebtIsSavedKeptOnDeathAndClearedByAReset(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Account settled = new Account(77, 0, DebtState.SETTLED);
        player.setData(ModAttachments.ACCOUNT.get(), settled);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer other = TestSupport.fakePlayer(helper);
        other.load(saved);
        assertAccount(helper, other, settled);

        Account owing = new Account(5, 6000, DebtState.ACTIVE);
        ServerPlayer old = TestSupport.fakePlayer(helper);
        old.setData(ModAttachments.ACCOUNT.get(), owing);
        ServerPlayer fresh = TestSupport.fakePlayer(helper);
        fresh.restoreFrom(old, false);
        assertAccount(helper, fresh, owing);
        ModAttachments.resetAll(fresh);
        assertAccount(helper, fresh, Account.EMPTY);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theAccountSurvivesDyingAndComingBackFromTheEnd(GameTestHelper helper) {
        for (Account account : new Account[]{new Account(5, 6000, DebtState.ACTIVE), new Account(77, 0, DebtState.SETTLED), new Account(9, 0, DebtState.UNASSIGNED)}) {
            for (boolean alive : new boolean[]{false, true}) {
                ServerPlayer old = TestSupport.fakePlayer(helper);
                old.setData(ModAttachments.ACCOUNT.get(), account);
                ServerPlayer fresh = TestSupport.fakePlayer(helper);
                fresh.restoreFrom(old, alive);
                assertAccount(helper, fresh, account);
            }
        }
        helper.succeed();
    }

    // ---- Network ----

    @GameTest(template = TestSupport.EMPTY)
    public static void thePayloadAndTheStateRoundTrip(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        TerminalPayDebtPayload.STREAM_CODEC.encode(buf, new TerminalPayDebtPayload(3, 1_500_000_000L, 6_400L));
        TerminalPayDebtPayload back = TerminalPayDebtPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.containerId() == 3 && back.amount() == 1_500_000_000L && back.seenDebt() == 6_400L, "the payload: " + back);

        real(helper, () -> DebtBalance.withOverride(new DebtBalance(6400, 40), () -> {
            TerminalState state = TerminalService.state(player, Optional.empty());
            helper.assertTrue(state.debtQuotaPercent() == 40, "the state carries the quota: " + state.debtQuotaPercent());
            RegistryFriendlyByteBuf stateBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            TerminalState.STREAM_CODEC.encode(stateBuf, state);
            TerminalState decoded = TerminalState.STREAM_CODEC.decode(stateBuf);
            helper.assertTrue(decoded.debtQuotaPercent() == 40 && decoded.withMessage(Optional.empty()).debtQuotaPercent() == 40, "and it arrives");
        }));
        helper.succeed();
    }

    // ---- Commands ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebtCommandsShowSetAndClear(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(50, 0, DebtState.UNASSIGNED));
        List<Component> shown = new ArrayList<>();
        helper.assertTrue(run(helper, player, "satisfy debug debt", 2, shown) == 1, "it answers");
        helper.assertTrue(shown.size() == 1 && shown.get(0).getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("message.overgearedmod.debug_debt") && "0".equals(contents.getArgs()[0]),
                "it shows the debt: " + shown);

        run(helper, player, "satisfy debug set debt 300", 2, new ArrayList<>());
        assertAccount(helper, player, new Account(50, 300, DebtState.ACTIVE));
        helper.assertTrue(refused(helper, player, "satisfy debug set debt 5", 1), "level 1 is refused");
        helper.assertTrue(refused(helper, player, "satisfy debug debt clear", 1), "clear needs level 2");
        assertAccount(helper, player, new Account(50, 300, DebtState.ACTIVE));

        run(helper, player, "satisfy debug set debt 0", 2, new ArrayList<>());
        assertAccount(helper, player, new Account(50, 0, DebtState.SETTLED));

        run(helper, player, "satisfy debug set debt 40", 2, new ArrayList<>());
        run(helper, player, "satisfy debug debt clear", 2, new ArrayList<>());
        assertAccount(helper, player, new Account(50, 0, DebtState.UNASSIGNED));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void setDebtRefusesNegativeAndHugeAmounts(GameTestHelper helper) {
        Account start = new Account(50, 300, DebtState.ACTIVE);
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), start);
        helper.assertTrue(refused(helper, player, "satisfy debug set debt -1", 2), "negative");
        helper.assertTrue(refused(helper, player, "satisfy debug set debt 9223372036854775808", 2), "one more than the largest is not a number");
        helper.assertTrue(refused(helper, player, "satisfy debug set debt 99999999999999999999", 2), "huge");
        helper.assertTrue(refused(helper, player, "satisfy debug set debt abc", 2), "not a number");
        assertAccount(helper, player, start);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void setDebtOverASettledDebtMakesItActiveAgain(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.ACCOUNT.get(), new Account(50, 0, DebtState.SETTLED));
        run(helper, player, "satisfy debug set debt 120", 2, new ArrayList<>());
        assertAccount(helper, player, new Account(50, 120, DebtState.ACTIVE));
        helper.succeed();
    }

    /** The terminal commands hang from debug, not from set (a misplaced parenthesis once moved them). */
    @GameTest(template = TestSupport.EMPTY)
    public static void theTerminalCommandsHangFromDebug(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        for (String command : List.of("satisfy debug terminal", "satisfy debug terminal offer", "satisfy debug terminal offer reset",
                "satisfy debug terminal work reset")) {
            helper.assertTrue(run(helper, player, command, 2, new ArrayList<>()) == 1, command + " works");
        }
        helper.assertTrue(refused(helper, player, "satisfy debug set terminal", 2), "set terminal does not exist");
        for (String command : List.of("satisfy debug set fatigue 50", "satisfy debug set won 10", "satisfy debug set insight 3")) {
            helper.assertTrue(run(helper, player, command, 2, new ArrayList<>()) == 1, command + " still hangs from set");
        }
        helper.succeed();
    }
}
