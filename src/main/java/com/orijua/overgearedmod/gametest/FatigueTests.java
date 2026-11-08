package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules;
import com.orijua.overgearedmod.feature.fatigue.FatigueService;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The fatigue, step 2: the energy value, the per-second update (drain in Satisfy, refill in the real world), the
 * switches that turn it off and the debug command. Fake players receive no tick events and do not move, so the tests
 * call {@link FatigueService#tickSecond} by hand, as the tick listener would once per second.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class FatigueTests {
    private FatigueTests() {
    }

    private static final double SECOND_OF_PLAY = FatigueRules.drainPerSecond(FatigueBalance.DEFAULT, FatigueRules.Activity.NORMAL, FatigueRules.SessionModifiers.NEUTRAL);
    private static final double SECOND_OF_REST = FatigueRules.recoverPerSecond(FatigueBalance.DEFAULT, FatigueRules.SessionModifiers.NEUTRAL);

    private static void in(GameTestHelper helper, boolean satisfy, Runnable body) {
        try {
            Satisfy.standIn(satisfy ? helper.getLevel() : null);
            body.run();
        } finally {
            Satisfy.restoreStandIn();
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-5;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aNewPlayerIsRested(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(FatigueService.energy(player) == 1.0f, "full energy to begin with");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theEnergySurvivesRespawn(GameTestHelper helper) {
        ServerPlayer old = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(old, 0.3);
        ServerPlayer fresh = TestSupport.fakePlayer(helper);
        helper.assertTrue(FatigueService.energy(fresh) == 1.0f, "a new player is rested");
        fresh.restoreFrom(old, false);
        helper.assertTrue(near(FatigueService.energy(fresh), 0.3), "dying does not refill the body");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSecondInSatisfyDrainsAndAnotherInTheRealWorldRefills(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.5);
        in(helper, true, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), 0.5 - SECOND_OF_PLAY), "one second of play costs one second of the session");
        });
        double afterPlay = FatigueService.energy(player);
        in(helper, false, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), afterPlay + SECOND_OF_REST), "one second in the real world refills");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theEnergyStaysBetweenZeroAndOne(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        in(helper, false, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.energy(player) == 1.0f, "a rested player does not go above full");
        });
        FatigueService.setEnergy(player, 0.0);
        in(helper, true, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.energy(player) == 0.0f, "nor below empty");
        });
        FatigueService.setEnergy(player, 7.0);
        helper.assertTrue(FatigueService.energy(player) == 1.0f, "setting is clamped too");
        FatigueService.setEnergy(player, -2.0);
        helper.assertTrue(FatigueService.energy(player) == 0.0f, "from below as well");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void creativeAndSpectatorPlayersAreNotTiredInSatisfy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.5);
        try {
            in(helper, true, () -> {
                player.setGameMode(GameType.CREATIVE);
                FatigueService.tickSecond(player);
                helper.assertTrue(near(FatigueService.energy(player), 0.5), "creative does not drain");
                player.setGameMode(GameType.SPECTATOR);
                FatigueService.tickSecond(player);
                helper.assertTrue(near(FatigueService.energy(player), 0.5), "nor does spectator");
                player.setGameMode(GameType.ADVENTURE);
                FatigueService.tickSecond(player);
                helper.assertTrue(FatigueService.energy(player) < 0.5f, "adventure does");
            });
        } finally {
            player.setGameMode(GameType.SURVIVAL);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withTheFatigueOffNothingChanges(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.5);
        FatigueBalance off = new FatigueBalance(false, 40, 0.5, 2, 20, 10, 0.1, 15, List.of(0.25));
        FatigueBalance.withOverride(off, () -> {
            in(helper, true, () -> FatigueService.tickSecond(player));
            in(helper, false, () -> FatigueService.tickSecond(player));
        });
        helper.assertTrue(near(FatigueService.energy(player), 0.5), "off: neither drains nor refills");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resetAllRefillsTheEnergy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.2);
        ModAttachments.resetAll(player);
        helper.assertTrue(FatigueService.energy(player) == 1.0f, "reset all leaves the body rested");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebugCommandSetsTheEnergy(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        dispatcher.execute("satisfy debug set fatigue 30", player.createCommandSourceStack().withPermission(4));
        helper.assertTrue(near(FatigueService.energy(player), 0.30), "30 % of the energy");
        dispatcher.execute("satisfy debug set fatigue 0", player.createCommandSourceStack().withPermission(4));
        helper.assertTrue(FatigueService.energy(player) == 0.0f, "and empty");
        boolean refused;
        try {
            dispatcher.execute("satisfy debug set fatigue 101", player.createCommandSourceStack().withPermission(4));
            refused = false;
        } catch (CommandSyntaxException e) {
            refused = true;
        }
        helper.assertTrue(refused, "over 100 % is refused");
        boolean needsOp;
        try {
            dispatcher.execute("satisfy debug set fatigue 50", player.createCommandSourceStack().withPermission(0));
            needsOp = false;
        } catch (CommandSyntaxException e) {
            needsOp = true;
        }
        helper.assertTrue(needsOp, "the command is for operators");
        helper.succeed();
    }

    // ---- Step 3: warnings, grace period, forced disconnection, refusal to connect ----

    private static final int GRACE = FatigueBalance.DEFAULT.graceSeconds();

    private static net.minecraft.core.GlobalPos capsuleAt(GameTestHelper helper) {
        return net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0)));
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void runningOutOfEnergyStartsTheGracePeriod(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.0);
        in(helper, true, () -> {
            helper.assertFalse(FatigueService.isInGrace(player), "no grace before the first second at zero");
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.isInGrace(player) && FatigueService.graceLeft(player) == GRACE, "the grace period starts at its full length");
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.graceLeft(player) == GRACE - 1, "and counts down one second at a time");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theGracePeriodEndsInAForcedDisconnection(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.core.BlockPos bairan = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        CapsuleTests.inSatisfy(helper, bairan, () -> {
            helper.assertTrue(CapsuleService.connect(player, capsuleAt(helper)), "connected");
            FatigueService.setEnergy(player, 0.0);
            for (int i = 0; i < GRACE; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertTrue(CapsuleService.state(player).isConnected(), "still connected one second before the end (" + FatigueService.graceLeft(player) + " left)");
            FatigueService.tickSecond(player);
            helper.assertFalse(CapsuleService.state(player).isConnected(), "the capsule took the player out");
            helper.assertTrue(CapsuleService.state(player).lastSave().isPresent(), "and remembers where they were");
            helper.assertFalse(FatigueService.isInGrace(player), "the grace period is over");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void gettingEnergyBackCancelsTheGracePeriod(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.0);
        in(helper, true, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.isInGrace(player), "in grace");
            FatigueService.setEnergy(player, 0.5);
            FatigueService.tickSecond(player);
            helper.assertFalse(FatigueService.isInGrace(player), "with energy again there is nothing to wait for");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void thereIsNoGracePeriodInTheRealWorldNorForCreative(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.0);
        in(helper, false, () -> {
            FatigueService.tickSecond(player);
            helper.assertFalse(FatigueService.isInGrace(player), "empty in the real world just refills");
        });
        try {
            in(helper, true, () -> {
                player.setGameMode(GameType.CREATIVE);
                FatigueService.tickSecond(player);
                helper.assertFalse(FatigueService.isInGrace(player), "creative is never thrown out");
            });
        } finally {
            player.setGameMode(GameType.SURVIVAL);
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void quittingKeepsTheGracePeriodAndRespawningEndsIt(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.0);
        in(helper, true, () -> {
            FatigueService.tickSecond(player);
            FatigueService.tickSecond(player);
            int left = FatigueService.graceLeft(player);
            FatigueService.onLogout(player);
            helper.assertTrue(FatigueService.graceLeft(player) == left, "quitting does not stretch the grace period: " + left + " seconds left");
            FatigueService.onLogin(player);
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.graceLeft(player) == left - 1, "coming back it goes on from where it was");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(player, false));
            helper.assertFalse(FatigueService.isInGrace(player), "respawning ends it");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLogoutCountdownDoesNotRaceTheForcedDisconnection(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.core.BlockPos bairan = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        CapsuleTests.inSatisfy(helper, bairan, () -> {
            helper.assertTrue(CapsuleService.connect(player, capsuleAt(helper)), "connected");
            helper.assertTrue(LogoutService.start(player) == LogoutService.Start.STARTED, "a logout countdown is running");
            FatigueService.setEnergy(player, 0.0);
            for (int i = 0; i <= GRACE; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertFalse(LogoutService.isCountingDown(player), "the forced disconnection cancelled the logout");
            helper.assertFalse(CapsuleService.state(player).isConnected(), "and the player left exactly once");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theCapsuleRefusesToConnectBelowTheMinimumEnergy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.server.level.ServerLevel other = CapsuleTests.otherLevel(helper);
        if (other == null) {
            helper.fail("BLOCKED: no second level to play Satisfy");
            return;
        }
        net.minecraft.core.BlockPos spot = helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 1));
        try {
            Satisfy.standIn(other);
            FatigueService.setEnergy(player, 0.05);
            helper.assertTrue(CapsuleService.begin(player, spot) == CapsuleService.Begin.TOO_TIRED, "below the minimum: refused");
            helper.assertFalse(CapsuleService.isConnecting(player), "and nothing started");
            FatigueService.setEnergy(player, 0.10);
            helper.assertTrue(CapsuleService.begin(player, spot) == CapsuleService.Begin.STARTED, "exactly the minimum: allowed");
            CapsuleService.cancel(player, false);
            FatigueBalance.withOverride(new FatigueBalance(false, 40, 0.5, 2, 20, 10, 0.1, 15, List.of(0.25)), () -> {
                FatigueService.setEnergy(player, 0.0);
                helper.assertTrue(CapsuleService.begin(player, spot) == CapsuleService.Begin.STARTED, "with the fatigue off nobody is refused");
                CapsuleService.cancel(player, false);
            });
        } finally {
            CapsuleService.cancel(player, false);
            Satisfy.restoreStandIn();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void crossingAWarningThresholdDoesNotBreakTheSecond(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        in(helper, true, () -> {
            double drain = SECOND_OF_PLAY;
            FatigueService.setEnergy(player, 0.25 + drain / 2);
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.energy(player) <= 0.25f && !FatigueService.isInGrace(player), "crossed 25 % and carried on");
            FatigueService.setEnergy(player, 0.00001);
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.energy(player) == 0.0f, "reached zero");
            helper.assertTrue(FatigueService.isInGrace(player) && FatigueService.graceLeft(player) == GRACE,
                    "the grace period starts in the very second the energy reaches zero, at its full length");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withNoGracePeriodTheDisconnectionIsImmediate(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.core.BlockPos bairan = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        FatigueBalance noGrace = new FatigueBalance(true, 40, 0.5, 2, 20, 10, 0.1, 0, List.of(0.25));
        CapsuleTests.inSatisfy(helper, bairan, () -> {
            helper.assertTrue(CapsuleService.connect(player, capsuleAt(helper)), "connected");
            FatigueService.setEnergy(player, 0.0);
            FatigueBalance.withOverride(noGrace, () -> FatigueService.tickSecond(player));
            helper.assertFalse(CapsuleService.state(player).isConnected(), "out in the very second it ran out");
            helper.assertFalse(FatigueService.isInGrace(player), "with nothing left over");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void changingToCreativeDuringTheGracePeriodEndsIt(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        FatigueService.setEnergy(player, 0.0);
        try {
            in(helper, true, () -> {
                FatigueService.tickSecond(player);
                helper.assertTrue(FatigueService.isInGrace(player), "in grace");
                player.setGameMode(GameType.CREATIVE);
                FatigueService.tickSecond(player);
                helper.assertFalse(FatigueService.isInGrace(player), "creative ends it");
                player.setGameMode(GameType.SURVIVAL);
                FatigueService.tickSecond(player);
                helper.assertTrue(FatigueService.graceLeft(player) == GRACE, "and back in survival it starts again from the full length");
            });
        } finally {
            player.setGameMode(GameType.SURVIVAL);
        }
        helper.succeed();
    }

    // ---- Step 4: activity, forge, protection, away from the game ----

    /** Whether the damage would be cancelled: posts the event as a hit would (a fake player is immune to a real hit while it spawns). */
    private static boolean damageIsCancelled(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
        var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player, new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 5.0f));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    private static double drainOf(FatigueRules.Activity activity) {
        return FatigueRules.drainPerSecond(FatigueBalance.DEFAULT, activity, FatigueRules.SessionModifiers.NEUTRAL);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fightingDrainsDoubleAndStandingStillHalf(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        in(helper, true, () -> {
            FatigueService.setEnergy(player, 0.5);
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), 0.5 - drainOf(FatigueRules.Activity.NORMAL)), "the first second has nowhere to compare: normal play");
            double afterFirst = FatigueService.energy(player);
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), afterFirst - drainOf(FatigueRules.Activity.REST)), "standing still costs half");
            double afterRest = FatigueService.energy(player);
            com.orijua.overgearedmod.feature.capsule.CombatClock.mark(player, helper.getLevel().getServer().getTickCount());
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), afterRest - drainOf(FatigueRules.Activity.COMBAT)), "fighting costs double");
            com.orijua.overgearedmod.feature.capsule.CombatClock.forget(player);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void movingCountsAsNormalPlay(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        in(helper, true, () -> {
            FatigueService.setEnergy(player, 0.5);
            FatigueService.tickSecond(player);
            double before = FatigueService.energy(player);
            player.setPos(player.getX() + 3.0, player.getY(), player.getZ());
            FatigueService.tickSecond(player);
            helper.assertTrue(near(FatigueService.energy(player), before - drainOf(FatigueRules.Activity.NORMAL)), "walking is normal play");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void forgingCostsTwentySecondsOfPlayInSatisfyOnly(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        double cost = FatigueBalance.DEFAULT.forgeCostSeconds() * drainOf(FatigueRules.Activity.NORMAL);
        in(helper, true, () -> {
            FatigueService.setEnergy(player, 0.5);
            TestSupport.forge(player, com.orijua.overgearedmod.feature.equipment.ItemRating.NORMAL);
            helper.assertTrue(near(FatigueService.energy(player), 0.5 - cost), "a forged item costs 20 seconds of play");
        });
        in(helper, false, () -> {
            FatigueService.setEnergy(player, 0.5);
            TestSupport.forge(player, com.orijua.overgearedmod.feature.equipment.ItemRating.NORMAL);
            helper.assertTrue(near(FatigueService.energy(player), 0.5), "in the real world it costs nothing");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void nothingCanHurtThePlayerDuringTheGracePeriod(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        in(helper, true, () -> {
            helper.assertFalse(damageIsCancelled(player, player.damageSources().generic()), "before the grace period damage goes through");
            FatigueService.setEnergy(player, 0.0);
            FatigueService.tickSecond(player);
            helper.assertTrue(FatigueService.isProtected(player), "protected in the grace period");
            helper.assertTrue(damageIsCancelled(player, player.damageSources().generic()), "ordinary damage is cancelled");
            helper.assertTrue(damageIsCancelled(player, player.damageSources().mobAttack(null)), "so is a mob's");
            helper.assertFalse(damageIsCancelled(player, player.damageSources().fellOutOfWorld()), "but the void still gets through");
            helper.assertFalse(damageIsCancelled(player, player.damageSources().genericKill()), "and so does /kill");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aForcedDisconnectionIsFollowedByProtectionWhenConnectingAgain(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        net.minecraft.core.BlockPos bairan = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        CapsuleTests.inSatisfy(helper, bairan, () -> {
            helper.assertTrue(CapsuleService.connect(player, capsuleAt(helper)), "connected");
            helper.assertFalse(FatigueService.isProtected(player), "a normal connection brings no protection");
            FatigueService.setEnergy(player, 0.0);
            for (int i = 0; i <= GRACE; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertFalse(CapsuleService.state(player).isConnected(), "forced out");
            helper.assertFalse(FatigueService.isInGrace(player), "grace over");
            FatigueService.setEnergy(player, 0.5);
            helper.assertTrue(CapsuleService.connect(player, capsuleAt(helper)), "connected again");
            helper.assertTrue(FatigueService.isProtected(player), "a few seconds of protection after a forced disconnection");
            helper.assertTrue(damageIsCancelled(player, player.damageSources().generic()), "they cannot be hurt");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theRealWorldRefillsTheEnergyWhileTheGameIsClosed(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long[] now = {1_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            in(helper, false, () -> {
                FatigueService.setEnergy(player, 0.2);
                FatigueService.onLogout(player);
                now[0] += 5 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(near(FatigueService.energy(player), 0.7), "five minutes away from the real world refill half of ten: " + FatigueService.energy(player));
                FatigueService.onLogout(player);
                now[0] += 60 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(FatigueService.energy(player) == 1.0f, "an hour away leaves it full, not above");
                FatigueService.onLogin(player);
                helper.assertTrue(FatigueService.energy(player) == 1.0f, "a login with no logout before changes nothing");
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theEnergyIsFrozenWhileTheGameIsClosedInsideSatisfy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long[] now = {1_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            in(helper, true, () -> {
                FatigueService.setEnergy(player, 0.3);
                FatigueService.onLogout(player);
                now[0] += 60 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(near(FatigueService.energy(player), 0.3), "quitting inside Satisfy neither drains nor refills");
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theClockGoingBackwardsDoesNotBreakTheEnergy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long[] now = {5_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            in(helper, false, () -> {
                FatigueService.setEnergy(player, 0.4);
                FatigueService.onLogout(player);
                now[0] -= 10_000L;
                FatigueService.onLogin(player);
                helper.assertTrue(near(FatigueService.energy(player), 0.4), "a negative time away counts as none");
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }
}
