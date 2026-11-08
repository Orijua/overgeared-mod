package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
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
}
