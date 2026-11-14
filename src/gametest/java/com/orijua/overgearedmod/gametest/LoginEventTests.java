package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.save.SaveMigrations;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

/**
 * Logging in and out through the real events ({@code PlayerLoggedInEvent} / {@code PlayerLoggedOutEvent} posted on the
 * game bus), not by calling the services by hand: what matters here is that the listeners are registered, that they
 * run in the right order and that each one only touches its own player. Posting the login event runs every listener
 * of the mod (phone, starter capsule, terminal...), so each test only looks at what it is about.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class LoginEventTests {
    private LoginEventTests() {
    }

    private static void logIn(ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
    }

    private static void logOut(ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }

    private static GlobalPos capsule(GameTestHelper helper) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(0, 1, 0)));
    }

    /** The save migrations run at the highest priority: a listener of any other priority already sees the data brought up to date. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theMigrationRunsBeforeTheOtherLoginListeners(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.SAVE_VERSION.get(), SaveMigrations.BASELINE - 1); // an older save than any step knows
        int[] seen = {-1};
        for (EventPriority priority : new EventPriority[] {EventPriority.HIGH, EventPriority.NORMAL, EventPriority.LOWEST}) {
            seen[0] = -1;
            player.setData(ModAttachments.SAVE_VERSION.get(), SaveMigrations.BASELINE - 1);
            Consumer<PlayerEvent.PlayerLoggedInEvent> spy = event -> {
                if (event.getEntity() == player) {
                    seen[0] = player.getData(ModAttachments.SAVE_VERSION.get());
                }
            };
            NeoForge.EVENT_BUS.addListener(priority, spy);
            try {
                logIn(player);
            } finally {
                NeoForge.EVENT_BUS.unregister(spy);
            }
            helper.assertTrue(seen[0] == SaveMigrations.CURRENT, "a " + priority + " listener saw save version " + seen[0]
                    + " instead of " + SaveMigrations.CURRENT);
        }
        helper.assertTrue(player.getData(ModAttachments.SAVE_VERSION.get()) == SaveMigrations.CURRENT, "stamped with the current version");
        helper.succeed();
    }

    /** Logging in outside Satisfy still "connected" (the server went down in there): the body is released and the rested bonus is gone. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aLoginOutsideSatisfyReleasesTheBody(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GlobalPos capsule = capsule(helper);
        player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY.connected(capsule, capsule));
        player.setData(ModAttachments.RESTED_SECONDS.get(), 120);
        CapsuleTests.outsideSatisfy(helper, () -> {
            logIn(player);
            helper.assertFalse(CapsuleService.state(player).isConnected(), "the body leaves the capsule");
            helper.assertTrue(player.getData(ModAttachments.RESTED_SECONDS.get()) == 0, "and the rested bonus does not outlive it");
        });
        helper.succeed();
    }

    /** Leaving the server cancels the connection and the logout countdown of that player, and of nobody else. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aLogoutClearsOnlyThatPlayersPendingThings(GameTestHelper helper) {
        ServerPlayer leaving = TestSupport.fakePlayer(helper);
        ServerPlayer staying = TestSupport.fakePlayer(helper);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            try {
                CapsuleService.start(leaving, capsule(helper));
                CapsuleService.start(staying, capsule(helper));
                helper.assertTrue(LogoutService.start(leaving) == LogoutService.Start.STARTED, "the countdown of the one who leaves");
                helper.assertTrue(LogoutService.start(staying) == LogoutService.Start.STARTED, "the countdown of the other");

                logOut(leaving);

                helper.assertFalse(CapsuleService.isConnecting(leaving), "his connection is gone");
                helper.assertFalse(LogoutService.isCountingDown(leaving), "and his countdown");
                helper.assertTrue(CapsuleService.isConnecting(staying), "the other one is still connecting");
                helper.assertTrue(LogoutService.isCountingDown(staying), "and still counting down");
            } finally {
                CapsuleService.cancel(leaving, false);
                CapsuleService.cancel(staying, false);
                LogoutService.forget(leaving);
                LogoutService.forget(staying);
            }
        });
        helper.succeed();
    }

    /** Dropping out in Satisfy and coming back: the state is the same, nothing is lost and nobody is moved. */
    @GameTest(template = TestSupport.EMPTY)
    public static void anAbruptExitInSatisfyLeavesAConsistentState(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            try {
                helper.assertTrue(CapsuleService.connect(player, capsule(helper)), "connects");
                CapsuleState connected = CapsuleService.state(player);
                logOut(player);
                logIn(player);
                helper.assertTrue(CapsuleService.state(player).equals(connected), "connected in Satisfy: the body stays in its capsule");

                player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY);
                logOut(player);
                logIn(player);
                helper.assertTrue(CapsuleService.state(player).equals(CapsuleState.EMPTY), "without a body in Satisfy it is only reported, nothing changes");
            } finally {
                LogoutService.forget(player);
                CapsuleService.cancel(player, false);
            }
        });
        helper.succeed();
    }
}
