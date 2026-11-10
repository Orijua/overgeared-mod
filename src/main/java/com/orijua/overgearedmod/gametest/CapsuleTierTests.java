package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.feature.capsule.CapsuleRules;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.fatigue.FatigueService;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The capsule tiers: the tier of the block a connection starts from sets the session and recovery times, and the diamond
 * one grants the "rested" bonus. As in {@link CapsuleTests}, the test overworld plays Satisfy, so the connection is
 * checked through the saved state and the attachments.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class CapsuleTierTests {
    private static final BlockPos CAPSULE = new BlockPos(0, 1, 1);

    private CapsuleTierTests() {
    }

    private static Block block(CapsuleTier tier) {
        return switch (tier) {
            case BASIC -> ModBlocks.CAPSULE.get();
            case ADVANCED -> ModBlocks.ADVANCED_CAPSULE.get();
            case DIAMOND -> ModBlocks.DIAMOND_CAPSULE.get();
        };
    }

    private static GlobalPos at(GameTestHelper helper) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(CAPSULE));
    }

    /** Runs {@code body} in Satisfy, with the default numbers (a world keeps the numbers of its saved config). */
    private static void inSatisfy(GameTestHelper helper, Runnable body) {
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)),
                () -> FatigueBalance.withOverride(FatigueBalance.DEFAULT, () -> CapsuleBalance.withOverride(CapsuleBalance.DEFAULT, body)));
    }

    private static void inRealWorld(GameTestHelper helper, Runnable body) {
        CapsuleTests.outsideSatisfy(helper,
                () -> FatigueBalance.withOverride(FatigueBalance.DEFAULT, () -> CapsuleBalance.withOverride(CapsuleBalance.DEFAULT, body)));
    }

    /**
     * The energy is saved as a float and rewritten every second, so a sum of seconds is exact only up to the float's
     * rounding (below 2e-6 after 60 writes, so 1e-5 is plenty and still tells the tiers apart by far).
     */
    private static boolean nearEnergy(double a, double b) {
        return nearEnergy(a, b, 1e-5);
    }

    private static boolean nearEnergy(double a, double b, double tolerance) {
        return Math.abs(a - b) < tolerance;
    }

    private static CapsuleTier tier(ServerPlayer player) {
        return player.getData(ModAttachments.CAPSULE_TIER.get());
    }

    private static int rested(ServerPlayer player) {
        return FatigueService.restedSeconds(player);
    }

    /** A connection from a block of {@code tier} with {@code energy}; returns the player. */
    private static int cooldown(ServerPlayer player) {
        return player.getData(ModAttachments.RESTED_COOLDOWN.get());
    }

    private static ServerPlayer connected(GameTestHelper helper, CapsuleTier tier, double energy) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.setBlock(CAPSULE, block(tier));
        FatigueService.setEnergy(player, energy);
        helper.assertTrue(CapsuleService.connect(player, at(helper)), "it connects");
        return player;
    }

    // ---- The tier of the connection ----

    @GameTest(template = TestSupport.EMPTY)
    public static void aPlayerWithoutConnectionsIsBasic(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(tier(player) == CapsuleTier.BASIC, "the default tier is the basic one");
        helper.assertTrue(rested(player) == 0, "and nobody starts rested");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPositionWithoutCapsuleCountsAsBasic(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.ADVANCED);
        helper.setBlock(CAPSULE, Blocks.AIR);
        inSatisfy(helper, () -> {
            helper.assertTrue(CapsuleService.connect(player, at(helper)), "it connects");
            helper.assertTrue(tier(player) == CapsuleTier.BASIC, "no capsule block there: the basic tier");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aFailedTripLeavesTheTierAlone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.ADVANCED);
        helper.setBlock(CAPSULE, block(CapsuleTier.DIAMOND));
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            // No Satisfy landing known: the connection is refused before anything is written.
            long seed = helper.getLevel().getSeed();
            var landingBefore = com.orijua.overgearedmod.feature.world.WorldSites.landingFor(seed);
            com.orijua.overgearedmod.feature.world.WorldSites.forgetLanding(seed);
            try {
                helper.assertFalse(CapsuleService.connect(player, at(helper)), "nowhere to land");
                helper.assertTrue(tier(player) == CapsuleTier.ADVANCED, "the tier is as it was");
                helper.assertTrue(rested(player) == 0, "and nothing is rested");
            } finally {
                landingBefore.ifPresent(pos -> com.orijua.overgearedmod.feature.world.WorldSites.rememberLanding(seed, pos));
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void breakingAnAdvancedCapsuleWhileConnectingCancels(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.setBlock(CAPSULE, block(CapsuleTier.ADVANCED));
        inSatisfy(helper, () -> {
            CapsuleService.start(player, at(helper));
            helper.assertTrue(CapsuleService.isConnecting(player), "lying down starts the wait");
            helper.setBlock(CAPSULE, Blocks.AIR);
            CapsuleService.tick(player);
            helper.assertFalse(CapsuleService.isConnecting(player), "the capsule gone cancels");
            helper.assertTrue(CapsuleService.state(player).capsule().isEmpty(), "and nothing connected");
            helper.assertTrue(tier(player) == CapsuleTier.BASIC, "the tier did not change");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void everyBlockConnectsWithItsTierAfterTheWait(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            for (CapsuleTier tier : CapsuleTier.values()) {
                ServerPlayer player = TestSupport.fakePlayer(helper);
                helper.setBlock(CAPSULE, block(tier));
                CapsuleService.start(player, at(helper));
                CapsuleTests.tickMany(player, CapsuleRules.CONNECT_TICKS);
                helper.assertTrue(CapsuleService.state(player).isConnected(), "the " + tier + " capsule connects after the wait");
                helper.assertTrue(tier(player) == tier, "the " + tier + " capsule sets its tier: " + tier(player));
            }
        });
        helper.succeed();
    }

    // ---- Session and recovery ----

    /**
     * Energy lost by a player who plays for 60 seconds: the first second is normal play (no earlier position to compare),
     * the other 59 are standing still (rest, 0.8 of the drain).
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void sixtySecondsOfPlayCostLessWithABetterCapsule(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            double[] sessionMinutes = {40.0, 55.0, 75.0};
            double[] lost = new double[CapsuleTier.values().length];
            for (CapsuleTier tier : CapsuleTier.values()) {
                double perSecond = 1.0 / (sessionMinutes[tier.ordinal()] * 60.0);
                ServerPlayer player = TestSupport.fakePlayer(helper);
                player.setData(ModAttachments.CAPSULE_TIER.get(), tier);
                FatigueService.setEnergy(player, 1.0);
                FatigueService.tickSecond(player);
                helper.assertTrue(nearEnergy(1.0 - FatigueService.energy(player), perSecond),
                        tier + ": the first second costs 1/(" + sessionMinutes[tier.ordinal()] + "*60) = " + perSecond + " but cost " + (1.0 - FatigueService.energy(player)));
                for (int i = 1; i < 60; i++) {
                    FatigueService.tickSecond(player);
                }
                lost[tier.ordinal()] = 1.0 - FatigueService.energy(player);
                double expected = (1.0 + 59 * 0.8) * perSecond;
                helper.assertTrue(nearEnergy(lost[tier.ordinal()], expected),
                        tier + ": 60 seconds cost " + expected + " (1 normal + 59 resting) but cost " + lost[tier.ordinal()]);
            }
            helper.assertTrue(lost[0] > lost[1] && lost[1] > lost[2], "a better capsule loses less in the same time: " + java.util.Arrays.toString(lost));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void afterDisconnectingTheTierStaysAndRecoversInEightMinutes(GameTestHelper helper) {
        ServerPlayer[] holder = new ServerPlayer[1];
        inSatisfy(helper, () -> {
            holder[0] = connected(helper, CapsuleTier.ADVANCED, 0.0);
            helper.assertTrue(CapsuleService.disconnect(holder[0], CapsuleService.Reason.MANUAL), "it disconnects");
            helper.assertTrue(tier(holder[0]) == CapsuleTier.ADVANCED, "the tier stays after disconnecting");
        });
        ServerPlayer player = holder[0];
        FatigueService.setEnergy(player, 0.0);
        // The recovery by the second must not read the wall clock: a clock that fails when asked proves it.
        FatigueService.setClock(() -> {
            throw new IllegalStateException("tickSecond must not read the wall clock");
        });
        try {
            inRealWorld(helper, () -> {
                for (int i = 0; i < 4 * 60; i++) {
                    FatigueService.tickSecond(player);
                }
                helper.assertTrue(nearEnergy(FatigueService.energy(player), 0.5), "four minutes refill half of eight: " + FatigueService.energy(player));
                for (int i = 0; i < 4 * 60; i++) {
                    FatigueService.tickSecond(player);
                }
                helper.assertTrue(nearEnergy(FatigueService.energy(player), 1.0, 5e-5), "eight minutes refill it: " + FatigueService.energy(player));
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void awayFromTheGameTheRealWorldUsesTheLastTier(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.ADVANCED);
        long[] now = {1_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            inRealWorld(helper, () -> {
                FatigueService.setEnergy(player, 0.2);
                FatigueService.onLogout(player);
                now[0] += 4 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(nearEnergy(FatigueService.energy(player), 0.7), "four minutes at the advanced rate refill half: " + FatigueService.energy(player));
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    // ---- Rested ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondCapsuleRestsOnlyWithEnoughEnergy(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            helper.assertTrue(rested(connected(helper, CapsuleTier.DIAMOND, 0.95)) == 600, "0.95 energy: ten minutes");
            helper.assertTrue(rested(connected(helper, CapsuleTier.DIAMOND, 0.85)) == 0, "0.85 energy: none");
            helper.assertTrue(rested(connected(helper, CapsuleTier.DIAMOND, 1.0)) == 600, "full energy: ten minutes");
            helper.assertTrue(rested(connected(helper, CapsuleTier.ADVANCED, 1.0)) == 0, "advanced: never");
            helper.assertTrue(rested(connected(helper, CapsuleTier.BASIC, 1.0)) == 0, "basic: never");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void restedCountsDownInSatisfyAndNotInTheRealWorld(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_SECONDS.get(), 5);
        inRealWorld(helper, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(rested(player) == 5, "it does not run in the real world");
        });
        inSatisfy(helper, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(rested(player) == 4, "one second in Satisfy: " + rested(player));
            for (int i = 0; i < 10; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertTrue(rested(player) == 0, "it ends at zero, never below: " + rested(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void restedCountsDownEvenWithTheFatigueOff(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_SECONDS.get(), 5);
        FatigueBalance off = new FatigueBalance(false, 40.0, 0.8, 2.0, 20.0, 10.0, 0.25, 15, java.util.List.of(0.15, 0.08, 0.02), 0.30);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> FatigueBalance.withOverride(off, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(rested(player) == 4, "the bonus runs out whatever the fatigue does: " + rested(player));
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void disconnectingEndsTheRested(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            helper.assertTrue(rested(player) == 600, "rested after connecting");
            helper.assertTrue(CapsuleService.disconnect(player, CapsuleService.Reason.MANUAL), "it disconnects");
            helper.assertTrue(rested(player) == 0, "disconnecting ends it");
            helper.assertTrue(tier(player) == CapsuleTier.DIAMOND, "the tier is kept");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void dyingInSatisfyEndsTheRestedForTheNewBody(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer old = connected(helper, CapsuleTier.DIAMOND, 1.0);
            CapsuleService.died(old);
            helper.assertTrue(rested(old) == 0, "the old one is no longer rested");
            old.setData(ModAttachments.RESTED_SECONDS.get(), 300);
            ServerPlayer fresh = TestSupport.fakePlayer(helper);
            fresh.restoreFrom(old, false);
            helper.assertTrue(rested(fresh) == 0, "the rested bonus is not carried over a death");
            helper.assertTrue(tier(fresh) == CapsuleTier.DIAMOND, "but the tier is");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void connectingAgainDoesNotAddRestedSeconds(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
            player.setData(ModAttachments.RESTED_COOLDOWN.get(), 0);
            FatigueService.onConnected(player);
            helper.assertTrue(rested(player) == 600, "it is set, not added: " + rested(player));
            player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
            player.setData(ModAttachments.RESTED_COOLDOWN.get(), 100);
            FatigueService.onConnected(player);
            helper.assertTrue(rested(player) == 0 && cooldown(player) == 100, "with a wait running it grants nothing: " + rested(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void restedSurvivesLeavingTheGameInsideSatisfy(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long[] now = {1_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            inSatisfy(helper, () -> {
                player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
                FatigueService.onLogout(player);
                now[0] += 60 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(rested(player) == 300, "quitting inside Satisfy keeps the rested seconds: " + rested(player));
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void turningUpOutsideStillConnectedEndsTheRested(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GlobalPos capsule = at(helper);
        GlobalPos exit = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 0)));
        player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
        inRealWorld(helper, () -> {
            player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY.connected(capsule, exit));
            CapsuleService.reconcile(player);
            helper.assertTrue(rested(player) == 0, "connected but outside Satisfy: no longer rested");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutRestedTheLevelXpIsAsAwarded(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        inSatisfy(helper, () -> {
            int xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 10, "not rested: 10");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void restedRaisesLevelXpByThePercentAndRounds(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        inSatisfy(helper, () -> {
            player.setData(ModAttachments.RESTED_SECONDS.get(), 60);
            int xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 11, "rested: 10 becomes 11 (+10 %)");
            xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 15);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 17, "rested: 15 becomes 17 (16.5 rounds up)");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void restedXpFollowsTheConfiguredPercentAndSkipsSmithingXp(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CapsuleBalance half = new CapsuleBalance(55, 8, 75, 6, 3000, 15000, 10, 50, 0.9, 30);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> CapsuleBalance.withOverride(half, () -> {
            player.setData(ModAttachments.RESTED_SECONDS.get(), 60);
            int xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 15, "rested at +50 %: 10 becomes 15");
            int smithing = ProgressionService.get(player).smithingXp();
            ProgressionService.addSmithingXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).smithingXp() == smithing + 10, "smithing XP is the same rested or not");
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void dyingInTheRealWorldKeepsTheRestedSeconds(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
        inRealWorld(helper, () -> {
            CapsuleService.died(player);
            helper.assertTrue(rested(player) == 300, "only a death in Satisfy ends the rested: " + rested(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void zeroRestedMinutesGrantsNothingEvenToTheDiamondCapsule(GameTestHelper helper) {
        CapsuleBalance none = new CapsuleBalance(55, 8, 75, 6, 3000, 15000, 0, 10, 0.9, 30);
        inSatisfy(helper, () -> CapsuleBalance.withOverride(none, () ->
                helper.assertTrue(rested(connected(helper, CapsuleTier.DIAMOND, 1.0)) == 0, "restedMinutes 0: no rested")));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theFatigueOffStillGrantsTheRestedOnConnecting(GameTestHelper helper) {
        FatigueBalance off = new FatigueBalance(false, 40.0, 0.8, 2.0, 20.0, 10.0, 0.25, 15, java.util.List.of(0.15, 0.08, 0.02), 0.30);
        inSatisfy(helper, () -> FatigueBalance.withOverride(off, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            helper.assertTrue(rested(player) == 600, "fatigue.enabled=false does not stop the bonus: " + rested(player));
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resettingEverythingClearsTheTierAndTheRested(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CAPSULE_TIER.get(), CapsuleTier.DIAMOND);
        player.setData(ModAttachments.RESTED_SECONDS.get(), 300);
        ModAttachments.resetAll(player);
        helper.assertTrue(tier(player) == CapsuleTier.BASIC && rested(player) == 0, "back to basic and not rested");
        helper.succeed();
    }

    // ---- The wait for the next rested bonus ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theDiamondCapsuleGrantsTheRestedAndOpensTheWait(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            helper.assertTrue(rested(player) == 600, "rested 600: " + rested(player));
            helper.assertTrue(cooldown(player) == 1800, "wait 1800: " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void disconnectingAndConnectingAgainDoesNotRestartTheRested(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            helper.assertTrue(CapsuleService.disconnect(player, CapsuleService.Reason.MANUAL), "it disconnects");
            helper.assertTrue(cooldown(player) == 1800, "the wait survives disconnecting: " + cooldown(player));
            FatigueService.setEnergy(player, 1.0);
            helper.assertTrue(CapsuleService.connect(player, at(helper)), "it connects again");
            helper.assertTrue(rested(player) == 0, "no new rested: " + rested(player));
            helper.assertTrue(cooldown(player) == 1800, "the wait goes on: " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theWaitCountsDownInSatisfyOnlyAndNotWhenDead(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_COOLDOWN.get(), 100);
        inRealWorld(helper, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(cooldown(player) == 100, "it does not run in the real world: " + cooldown(player));
        });
        inSatisfy(helper, () -> {
            FatigueService.tickSecond(player);
            helper.assertTrue(cooldown(player) == 99, "one second in Satisfy: " + cooldown(player));
            player.setHealth(0.0f);
            FatigueService.tickSecond(player);
            helper.assertTrue(cooldown(player) == 99, "dead: it does not run: " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void whenTheWaitEndsConnectingGrantsTheRestedAgain(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            player.setData(ModAttachments.RESTED_COOLDOWN.get(), 1);
            FatigueService.tickSecond(player);
            helper.assertTrue(cooldown(player) == 0, "the wait ended: " + cooldown(player));
            FatigueService.tickSecond(player);
            helper.assertTrue(cooldown(player) == 0, "and never goes below 0: " + cooldown(player));
            FatigueService.onConnected(player);
            helper.assertTrue(rested(player) == 600 && cooldown(player) == 1800, "rested again: " + rested(player) + ", " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aWaitOfZeroMinutesAlwaysGrantsTheRested(GameTestHelper helper) {
        CapsuleBalance noWait = new CapsuleBalance(55, 8, 75, 6, 3000, 15000, 10, 10, 0.9, 0);
        inSatisfy(helper, () -> CapsuleBalance.withOverride(noWait, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 1.0);
            helper.assertTrue(rested(player) == 600 && cooldown(player) == 0, "no wait is opened: " + cooldown(player));
            helper.assertTrue(CapsuleService.disconnect(player, CapsuleService.Reason.MANUAL), "it disconnects");
            FatigueService.setEnergy(player, 1.0);
            helper.assertTrue(CapsuleService.connect(player, at(helper)), "it connects again");
            helper.assertTrue(rested(player) == 600, "rested again at once: " + rested(player));
        }));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void connectingWithLowEnergyOpensNoWait(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer player = connected(helper, CapsuleTier.DIAMOND, 0.85);
            helper.assertTrue(rested(player) == 0 && cooldown(player) == 0, "nothing granted, no wait: " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void dyingInSatisfyKeepsTheWait(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            ServerPlayer old = connected(helper, CapsuleTier.DIAMOND, 1.0);
            CapsuleService.died(old);
            ServerPlayer fresh = TestSupport.fakePlayer(helper);
            fresh.restoreFrom(old, false);
            helper.assertTrue(cooldown(fresh) == 1800, "the wait is carried over a death: " + cooldown(fresh));
            helper.assertTrue(rested(fresh) == 0, "the bonus is not");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void leavingTheGameInsideSatisfyKeepsTheWait(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long[] now = {1_000_000L};
        FatigueService.setClock(() -> now[0]);
        try {
            inSatisfy(helper, () -> {
                player.setData(ModAttachments.RESTED_COOLDOWN.get(), 900);
                FatigueService.onLogout(player);
                now[0] += 60 * 60 * 1000L;
                FatigueService.onLogin(player);
                helper.assertTrue(cooldown(player) == 900, "the wait is kept: " + cooldown(player));
            });
        } finally {
            FatigueService.restoreClock();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void turningUpOutsideStillConnectedKeepsTheWait(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GlobalPos capsule = at(helper);
        GlobalPos exit = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 0)));
        player.setData(ModAttachments.RESTED_COOLDOWN.get(), 900);
        inRealWorld(helper, () -> {
            player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY.connected(capsule, exit));
            CapsuleService.reconcile(player);
            helper.assertTrue(cooldown(player) == 900, "reconciling keeps the wait: " + cooldown(player));
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void loweringTheConfiguredWaitShortensARunningOne(GameTestHelper helper) {
        CapsuleBalance fiveMinutes = new CapsuleBalance(55, 8, 75, 6, 3000, 15000, 10, 10, 0.9, 5);
        CapsuleBalance noWait = new CapsuleBalance(55, 8, 75, 6, 3000, 15000, 10, 10, 0.9, 0);
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_COOLDOWN.get(), 1800);
        inSatisfy(helper, () -> {
            CapsuleBalance.withOverride(fiveMinutes, () -> {
                FatigueService.tickSecond(player);
                helper.assertTrue(cooldown(player) == 299, "clamped to 300 and one second gone: " + cooldown(player));
            });
            CapsuleBalance.withOverride(noWait, () -> {
                FatigueService.tickSecond(player);
                helper.assertTrue(cooldown(player) == 0, "0 minutes frees it: " + cooldown(player));
            });
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resettingEverythingClearsTheWait(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.RESTED_COOLDOWN.get(), 900);
        ModAttachments.resetAll(player);
        helper.assertTrue(cooldown(player) == 0, "resetAll clears the wait: " + cooldown(player));
        helper.succeed();
    }
}
