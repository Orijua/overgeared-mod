package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /satisfy logout}: a 3-second countdown inside Satisfy that ends in {@link CapsuleService#disconnect}. It is
 * refused after recent combat (damage dealt or taken) and cancelled by damage taken, death, respawn, leaving the
 * server or changing dimension. Damage dealt during the countdown does not cancel it.
 */
public final class LogoutService {
    /** Server tick of the last effective damage dealt or taken, by player. */
    private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();
    /** Ticks left of the countdown, by player. */
    private static final Map<UUID, Integer> COUNTDOWN = new ConcurrentHashMap<>();

    /** What asking to log out did. */
    public enum Start { STARTED, ALREADY_RUNNING, NOT_IN_SATISFY, IN_DANGER }

    private LogoutService() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LogoutService::onTick);
        gameBus.addListener(LogoutService::onDamaged);
        gameBus.addListener(LogoutService::onDeath);
        gameBus.addListener(LogoutService::onRespawn);
        gameBus.addListener(LogoutService::onChangedDimension);
        gameBus.addListener(LogoutService::onLoggedOut);
        gameBus.addListener(LogoutService::onServerStopping);
        gameBus.addListener(LogoutService::onServerStarting);
        gameBus.addListener(LogoutService::onRegisterCommands);
    }

    public static Start start(ServerPlayer player) {
        if (!Satisfy.is(player.level())) {
            return Start.NOT_IN_SATISFY;
        }
        if (isCountingDown(player)) {
            return Start.ALREADY_RUNNING;
        }
        Long last = LAST_COMBAT.get(player.getUUID());
        if (LogoutRules.inDanger(player.server.getTickCount(), last == null ? OptionalLong.empty() : OptionalLong.of(last))) {
            return Start.IN_DANGER;
        }
        COUNTDOWN.put(player.getUUID(), LogoutRules.COUNTDOWN_TICKS);
        announce(player, LogoutRules.COUNTDOWN_TICKS);
        return Start.STARTED;
    }

    public static boolean isCountingDown(ServerPlayer player) {
        return COUNTDOWN.containsKey(player.getUUID());
    }

    /** One tick of the player's countdown, if any. */
    public static void tick(ServerPlayer player) {
        Integer left = COUNTDOWN.get(player.getUUID());
        if (left == null) {
            return;
        }
        if (!player.isAlive() || !Satisfy.is(player.level())) {
            cancel(player, false);
            return;
        }
        int next = left - 1;
        if (next == 0) {
            // Remove first: disconnect changes dimension, and that would otherwise cancel us half way.
            COUNTDOWN.remove(player.getUUID());
            CapsuleService.disconnect(player, CapsuleService.Reason.LOGOUT);
            return;
        }
        COUNTDOWN.put(player.getUUID(), next);
        announce(player, next);
    }

    private static void announce(ServerPlayer player, int ticksLeft) {
        OptionalInt seconds = LogoutRules.announcement(ticksLeft);
        if (seconds.isPresent()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.logout_countdown", seconds.getAsInt()), true);
        }
    }

    /** Stops the countdown; the "cancelled" message only if {@code tell}. */
    public static void cancel(ServerPlayer player, boolean tell) {
        if (COUNTDOWN.remove(player.getUUID()) != null && tell) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.logout_cancelled"), true);
        }
    }

    public static void markCombat(ServerPlayer player, long tick) {
        LAST_COMBAT.put(player.getUUID(), tick);
    }

    /** Forgets everything about this player. */
    public static void forget(ServerPlayer player) {
        COUNTDOWN.remove(player.getUUID());
        LAST_COMBAT.remove(player.getUUID());
    }

    public static void forgetAll() {
        COUNTDOWN.clear();
        LAST_COMBAT.clear();
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            markCombat(victim, victim.server.getTickCount());
            if (isCountingDown(victim)) {
                cancel(victim, true);
            }
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker && source != event.getEntity()) {
            markCombat(attacker, attacker.server.getTickCount());
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, false);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, false);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, true);
        }
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        forgetAll();
    }

    private static void onServerStarting(ServerStartingEvent event) {
        forgetAll();
    }

    /** /satisfy logout: for everyone (the op-only parts of /satisfy are their own literals). */
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("satisfy")
                .then(Commands.literal("logout")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Start result = start(player);
                            switch (result) {
                                case NOT_IN_SATISFY -> player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.in_real_world"), true);
                                case ALREADY_RUNNING -> player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.logout_in_progress"), true);
                                case IN_DANGER -> player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.logout_in_danger"), true);
                                case STARTED -> {
                                }
                            }
                            return result == Start.STARTED ? 1 : 0;
                        })));
    }
}
