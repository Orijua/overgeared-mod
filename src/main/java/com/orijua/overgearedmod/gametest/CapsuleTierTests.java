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

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    private static CapsuleTier tier(ServerPlayer player) {
        return player.getData(ModAttachments.CAPSULE_TIER.get());
    }

    private static int rested(ServerPlayer player) {
        return FatigueService.restedSeconds(player);
    }

    /** A connection from a block of {@code tier} with {@code energy}; returns the player. */
    private static ServerPlayer connected(GameTestHelper helper, CapsuleTier tier, double energy) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.setBlock(CAPSULE, block(tier));
        FatigueService.setEnergy(player, energy);
        helper.assertTrue(CapsuleService.connect(player, at(helper)), "it connects");
        return player;
    }

    // ---- The tier of the connection ----

    @GameTest(template = TestSupport.EMPTY)
    public static void connectingFromEachBlockSetsTheTier(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            for (CapsuleTier tier : CapsuleTier.values()) {
                ServerPlayer player = connected(helper, tier, 1.0);
                helper.assertTrue(tier(player) == tier, "connecting from the " + tier + " capsule: " + tier(player));
            }
        });
        helper.succeed();
    }

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
            com.orijua.overgearedmod.feature.world.WorldSites.forgetLanding(helper.getLevel().getSeed());
            helper.assertFalse(CapsuleService.connect(player, at(helper)), "nowhere to land");
            helper.assertTrue(tier(player) == CapsuleTier.ADVANCED, "the tier is as it was");
            helper.assertTrue(rested(player) == 0, "and nothing is rested");
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
    public static void everyTierCanStartTheWait(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            for (CapsuleTier tier : CapsuleTier.values()) {
                ServerPlayer player = TestSupport.fakePlayer(helper);
                helper.setBlock(CAPSULE, block(tier));
                CapsuleService.start(player, at(helper));
                CapsuleTests.tickMany(player, CapsuleRules.CONNECT_TICKS);
                helper.assertTrue(CapsuleService.state(player).isConnected(), "the " + tier + " capsule connects after the wait");
                helper.assertTrue(tier(player) == tier, "with its tier");
            }
        });
        helper.succeed();
    }

    // ---- Session and recovery ----

    @GameTest(template = TestSupport.EMPTY)
    public static void sixtySecondsOfPlayCostLessWithABetterCapsule(GameTestHelper helper) {
        inSatisfy(helper, () -> {
            double[] lost = new double[CapsuleTier.values().length];
            for (CapsuleTier tier : CapsuleTier.values()) {
                ServerPlayer player = TestSupport.fakePlayer(helper);
                player.setData(ModAttachments.CAPSULE_TIER.get(), tier);
                FatigueService.setEnergy(player, 1.0);
                FatigueService.tickSecond(player);
                if (tier == CapsuleTier.ADVANCED) {
                    helper.assertTrue(near(1.0 - FatigueService.energy(player), 1.0 / (55 * 60)), "the first second costs 1/(55*60): " + FatigueService.energy(player));
                }
                for (int i = 1; i < 60; i++) {
                    FatigueService.tickSecond(player);
                }
                lost[tier.ordinal()] = 1.0 - FatigueService.energy(player);
            }
            helper.assertTrue(near(lost[CapsuleTier.BASIC.ordinal()] / lost[CapsuleTier.ADVANCED.ordinal()], 55.0 / 40.0), "advanced lasts 55/40 of basic");
            helper.assertTrue(near(lost[CapsuleTier.BASIC.ordinal()] / lost[CapsuleTier.DIAMOND.ordinal()], 75.0 / 40.0), "diamond lasts 75/40 of basic");
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
        inRealWorld(helper, () -> {
            for (int i = 0; i < 4 * 60; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertTrue(near(FatigueService.energy(player), 0.5), "four minutes refill half of eight: " + FatigueService.energy(player));
            for (int i = 0; i < 4 * 60; i++) {
                FatigueService.tickSecond(player);
            }
            helper.assertTrue(near(FatigueService.energy(player), 1.0), "eight minutes refill it: " + FatigueService.energy(player));
        });
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
                helper.assertTrue(near(FatigueService.energy(player), 0.7), "four minutes at the advanced rate refill half: " + FatigueService.energy(player));
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
            FatigueService.onConnected(player, CapsuleTier.DIAMOND);
            helper.assertTrue(rested(player) == 600, "it is set, not added: " + rested(player));
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
    public static void restedRaisesLevelXpButNotSmithingXp(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        inSatisfy(helper, () -> {
            int xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 10, "not rested: 10");
            player.setData(ModAttachments.RESTED_SECONDS.get(), 60);
            xp = ProgressionService.get(player).xp();
            ProgressionService.awardXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).xp() == xp + 11, "rested: 11 (+10 %)");
            int smithing = ProgressionService.get(player).smithingXp();
            ProgressionService.addSmithingXp(player, 10);
            helper.assertTrue(ProgressionService.get(player).smithingXp() == smithing + 10, "smithing XP is the same rested or not");
        });
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
}
