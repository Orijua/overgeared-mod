package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.Activity;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.SessionModifiers;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification.Kind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the {@link FatigueRules} to the players: once per real second the energy drains in Satisfy and refills in
 * the real world. The energy is one value per player (the body's), saved with them and kept on death; it does not
 * change with the character swap.
 *
 * <p>The time only runs while the player is on the server: when they are away the energy is frozen, in both worlds.
 */
public final class FatigueService {
    /** Ticks between two updates (one second). */
    private static final int TICKS_PER_SECOND = 20;

    /**
     * Seconds left of the grace period between running out of energy and the forced disconnection, by player. Only in
     * memory: it is deduced again from the state (in Satisfy with no energy), so saving and quitting during it, or a
     * server restart, simply starts it from the beginning.
     */
    private static final Map<UUID, Integer> GRACE = new ConcurrentHashMap<>();

    private FatigueService() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(FatigueService::onTick);
        gameBus.addListener(FatigueService::onLoggedOut);
        gameBus.addListener(FatigueService::onRespawn);
        gameBus.addListener(FatigueService::onChangedDimension);
        gameBus.addListener(FatigueService::onServerStopping);
        gameBus.addListener(FatigueService::onServerStarting);
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GRACE.remove(player.getUUID());
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GRACE.remove(player.getUUID());
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GRACE.remove(player.getUUID());
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        GRACE.clear();
    }

    private static void onServerStarting(ServerStartingEvent event) {
        GRACE.clear();
    }

    /** Whether the player is in the grace period (for the tests and the HUD). */
    public static boolean isInGrace(ServerPlayer player) {
        return GRACE.containsKey(player.getUUID());
    }

    /** Seconds left of the grace period, or -1 if it is not running. */
    public static int graceLeft(ServerPlayer player) {
        return GRACE.getOrDefault(player.getUUID(), -1);
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % TICKS_PER_SECOND == 0) {
            tickSecond(player);
        }
    }

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

    /**
     * One second of play: drains in Satisfy (survival and adventure only) and refills in the real world. In Satisfy it
     * also warns when a threshold is crossed and, with no energy left, runs the grace period that ends in the forced
     * disconnection.
     */
    public static void tickSecond(ServerPlayer player) {
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || !player.isAlive()) {
            GRACE.remove(player.getUUID());
            return;
        }
        double before = energy(player);
        if (WorldSide.of(player.level()) != WorldSide.SATISFY) {
            GRACE.remove(player.getUUID());
            setEnergy(player, FatigueRules.afterRecovery(balance, before, SessionModifiers.NEUTRAL, 1.0));
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            GRACE.remove(player.getUUID());
            return;
        }
        setEnergy(player, FatigueRules.afterDrain(balance, before, activityOf(player), SessionModifiers.NEUTRAL, 1.0));
        double after = energy(player);
        if (after > 0.0) {
            GRACE.remove(player.getUUID());
            warnIfCrossed(player, balance.warnAt(), before, after);
        } else {
            runGrace(player, balance);
        }
    }

    private static void warnIfCrossed(ServerPlayer player, List<Double> warnAt, double before, double after) {
        OptionalDouble crossed = FatigueRules.crossedWarning(before, after, warnAt);
        if (crossed.isPresent()) {
            NotificationEvents.send(player, Kind.FATIGUE_WARNING,
                    List.of(Component.translatable("notification.overgearedmod.fatigue_warning.body", Math.round(after * 100))));
        }
    }

    /**
     * No energy left. The first second announces the safety protocol; each following one counts down on the action bar;
     * at the end the capsule takes the player out. Combat does not pause it (the warnings before are the protection).
     */
    private static void runGrace(ServerPlayer player, FatigueBalance balance) {
        Integer running = GRACE.get(player.getUUID());
        int left;
        if (running == null) {
            left = balance.graceSeconds();
            GRACE.put(player.getUUID(), left);
            NotificationEvents.send(player, Kind.FATIGUE_EXHAUSTED,
                    List.of(Component.translatable("notification.overgearedmod.fatigue_exhausted.body")));
        } else {
            left = running - 1;
            GRACE.put(player.getUUID(), left);
        }
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.fatigue.countdown", left), true);
            return;
        }
        forceDisconnect(player);
    }

    /** The end of the grace period. Whatever else was about to take the player out (the logout) is cancelled first. */
    private static void forceDisconnect(ServerPlayer player) {
        GRACE.remove(player.getUUID());
        LogoutService.cancel(player, false);
        Overgearedmod.LOGGER.info("{} ran out of energy: the capsule disconnects them", player.getGameProfile().getName());
        CapsuleService.disconnect(player, CapsuleService.Reason.FATIGUE);
    }

    /** What the player is doing. Step 4 tells rest and combat apart; for now every second is normal play. */
    static Activity activityOf(ServerPlayer player) {
        return Activity.NORMAL;
    }
}
