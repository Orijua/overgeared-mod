package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.CombatClock;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.Activity;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.SessionModifiers;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification.Kind;
import com.orijua.overgearedmod.feature.smithing.ItemForgedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Applies the {@link FatigueRules} to the players: once per real second the energy drains in Satisfy and refills in
 * the real world. The energy is one value per player (the body's), saved with them and kept on death; it does not
 * change with the character swap.
 *
 * <p>Away from the game: a player who leaves while in the real world keeps resting (the energy refills with the real
 * time that passes, at the normal rate); one who leaves inside Satisfy finds it exactly as it was.
 */
public final class FatigueService {
    /** Ticks between two updates (one second). */
    private static final int TICKS_PER_SECOND = 20;

    /** Where each player was a second ago, to tell resting from moving. Only in memory. */
    private static final Map<UUID, Vec3> LAST_POSITION = new ConcurrentHashMap<>();
    /** Server tick until which each player is protected after a forced disconnection. Only in memory (10 seconds). */
    private static final Map<UUID, Long> PROTECTED_UNTIL = new ConcurrentHashMap<>();

    private static final LongSupplier SYSTEM_CLOCK = System::currentTimeMillis;
    private static volatile LongSupplier clock = SYSTEM_CLOCK;

    private FatigueService() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(FatigueService::onTick);
        gameBus.addListener(FatigueService::onLoggedIn);
        gameBus.addListener(FatigueService::onLoggedOut);
        gameBus.addListener(FatigueService::onRespawn);
        gameBus.addListener(FatigueService::onChangedDimension);
        gameBus.addListener(FatigueService::onServerStopping);
        gameBus.addListener(FatigueService::onServerStarting);
        gameBus.addListener(FatigueService::onIncomingDamage);
        gameBus.addListener(FatigueService::onForged);
    }

    /** For tests: the wall clock the away time is measured with. */
    public static void setClock(LongSupplier replacement) {
        clock = replacement;
    }

    public static void restoreClock() {
        clock = SYSTEM_CLOCK;
    }

    // ---- events ----

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % TICKS_PER_SECOND == 0) {
            tickSecond(player);
        }
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            onLogin(player);
        }
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            onLogout(player);
        }
    }

    /** Dying ends the grace period (the player comes back in the real world). */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endGrace(player);
            PROTECTED_UNTIL.remove(player.getUUID());
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endGrace(player);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        LAST_POSITION.clear();
        PROTECTED_UNTIL.clear();
    }

    private static void onServerStarting(ServerStartingEvent event) {
        LAST_POSITION.clear();
        PROTECTED_UNTIL.clear();
    }

    /** During the grace period, and for a few seconds after reconnecting from it, nothing can hurt the player. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isProtected(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** Each forged item costs a fixed number of seconds of play. */
    private static void onForged(ItemForgedEvent event) {
        ServerPlayer player = event.player();
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || !player.isAlive() || WorldSide.of(player.level()) != WorldSide.SATISFY
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        double before = energy(player);
        setEnergy(player, FatigueRules.afterForge(balance, before, SessionModifiers.NEUTRAL));
        warnIfCrossed(player, balance.warnAt(), before, energy(player));
    }

    // ---- state ----

    /** The energy of the player, 1 (rested) to 0 (spent). */
    public static float energy(ServerPlayer player) {
        return player.getData(ModAttachments.FATIGUE.get());
    }

    /** Sets the energy (clamped to 0..1); only writes, and so syncs, when the value changes. */
    public static void setEnergy(ServerPlayer player, double energy) {
        float value = (float) FatigueRules.clamp(energy);
        if (value != energy(player)) {
            player.setData(ModAttachments.FATIGUE.get(), value);
        }
    }

    /** Whether the player is in the grace period (for the tests and the HUD). */
    public static boolean isInGrace(ServerPlayer player) {
        return graceLeft(player) >= 0;
    }

    /** Seconds left of the grace period, or -1 if it is not running. Saved with the player. */
    public static int graceLeft(ServerPlayer player) {
        return player.getData(ModAttachments.FATIGUE_GRACE.get());
    }

    private static void setGrace(ServerPlayer player, int seconds) {
        if (seconds != graceLeft(player)) {
            player.setData(ModAttachments.FATIGUE_GRACE.get(), seconds);
        }
    }

    private static void endGrace(ServerPlayer player) {
        setGrace(player, -1);
    }

    /** Whether nothing can hurt the player now: in the grace period, or just reconnected after a forced disconnection. */
    public static boolean isProtected(ServerPlayer player) {
        if (isInGrace(player)) {
            return true;
        }
        Long until = PROTECTED_UNTIL.get(player.getUUID());
        return until != null && player.server.getTickCount() < until;
    }

    /** The player has just connected to Satisfy: after a forced disconnection they start protected. */
    public static void onConnected(ServerPlayer player) {
        if (player.getData(ModAttachments.FATIGUE_FORCED_OUT.get())) {
            player.setData(ModAttachments.FATIGUE_FORCED_OUT.get(), false);
            PROTECTED_UNTIL.put(player.getUUID(), player.server.getTickCount() + (long) FatigueRules.PROTECTION_TICKS);
        }
    }

    // ---- away from the game ----

    /** The player leaves the server: remembers when, so the real world can refill the energy meanwhile. */
    public static void onLogout(ServerPlayer player) {
        player.setData(ModAttachments.FATIGUE_AWAY_SINCE.get(), clock.getAsLong());
        LAST_POSITION.remove(player.getUUID());
        PROTECTED_UNTIL.remove(player.getUUID());
    }

    /**
     * The player comes back. If they left while in the real world they have rested all that time (at the normal rate,
     * up to full); if they left inside Satisfy the energy is as they left it. Either way the clock starts again.
     */
    public static void onLogin(ServerPlayer player) {
        long since = player.getData(ModAttachments.FATIGUE_AWAY_SINCE.get());
        if (since <= 0) {
            return;
        }
        player.setData(ModAttachments.FATIGUE_AWAY_SINCE.get(), 0L);
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || WorldSide.of(player.level()) != WorldSide.REAL) {
            return;
        }
        double seconds = Math.max(0.0, (clock.getAsLong() - since) / 1000.0);
        setEnergy(player, FatigueRules.afterRecovery(balance, energy(player), SessionModifiers.NEUTRAL, seconds));
    }

    // ---- the second ----

    /**
     * One second of play: drains in Satisfy (survival and adventure only) and refills in the real world. In Satisfy it
     * also warns when a threshold is crossed and, with no energy left, runs the grace period that ends in the forced
     * disconnection.
     */
    public static void tickSecond(ServerPlayer player) {
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || !player.isAlive()) {
            endGrace(player);
            LAST_POSITION.remove(player.getUUID());
            return;
        }
        double before = energy(player);
        if (WorldSide.of(player.level()) != WorldSide.SATISFY) {
            endGrace(player);
            LAST_POSITION.remove(player.getUUID());
            setEnergy(player, FatigueRules.afterRecovery(balance, before, SessionModifiers.NEUTRAL, 1.0));
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            endGrace(player);
            return;
        }
        setEnergy(player, FatigueRules.afterDrain(balance, before, activityOf(player), SessionModifiers.NEUTRAL, 1.0));
        double after = energy(player);
        if (after > 0.0) {
            endGrace(player);
            warnIfCrossed(player, balance.warnAt(), before, after);
        } else {
            runGrace(player, balance);
        }
    }

    /** What the player has been doing for the last second: fighting, standing still, or playing normally. */
    static Activity activityOf(ServerPlayer player) {
        Vec3 now = player.position();
        Vec3 before = LAST_POSITION.put(player.getUUID(), now);
        if (FatigueRules.inCombat(player.server.getTickCount(), CombatClock.last(player))) {
            return Activity.COMBAT;
        }
        return before != null && FatigueRules.isResting(now.distanceToSqr(before)) ? Activity.REST : Activity.NORMAL;
    }

    private static void warnIfCrossed(ServerPlayer player, List<Double> warnAt, double before, double after) {
        OptionalDouble crossed = FatigueRules.crossedWarning(before, after, warnAt);
        if (crossed.isPresent() && after > 0.0) {
            NotificationEvents.send(player, Kind.FATIGUE_WARNING,
                    List.of(Component.translatable("notification.overgearedmod.fatigue_warning.body", Math.round(after * 100))));
        }
    }

    /**
     * No energy left. The first second announces the safety protocol; each following one counts down on the action bar;
     * at the end the capsule takes the player out. Nothing can hurt them meanwhile (see {@link #isProtected}), and
     * combat does not pause it. The seconds left are saved, so quitting does not stretch it.
     */
    private static void runGrace(ServerPlayer player, FatigueBalance balance) {
        int running = graceLeft(player);
        int left;
        if (running < 0) {
            left = balance.graceSeconds();
            setGrace(player, left);
            if (left > 0) {
                NotificationEvents.send(player, Kind.FATIGUE_EXHAUSTED,
                        List.of(Component.translatable("notification.overgearedmod.fatigue_exhausted.body")));
            }
        } else {
            left = Math.max(running - 1, 0);
            setGrace(player, left);
        }
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.fatigue.countdown", left), true);
            return;
        }
        forceDisconnect(player);
    }

    /** The end of the grace period. Whatever else was about to take the player out (the logout) is cancelled first. */
    private static void forceDisconnect(ServerPlayer player) {
        endGrace(player);
        LogoutService.cancel(player, false);
        player.setData(ModAttachments.FATIGUE_FORCED_OUT.get(), true);
        Overgearedmod.LOGGER.info("{} ran out of energy: the capsule disconnects them", player.getGameProfile().getName());
        if (!CapsuleService.disconnect(player, CapsuleService.Reason.FATIGUE)) {
            player.setData(ModAttachments.FATIGUE_FORCED_OUT.get(), false);
            Overgearedmod.LOGGER.warn("The forced disconnection of {} did not happen: the grace period starts again", player.getGameProfile().getName());
        }
    }
}
