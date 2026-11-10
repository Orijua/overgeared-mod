package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.capsule.CapsuleRules;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.CombatClock;
import com.orijua.overgearedmod.feature.smelting.SmelterMenu;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilMenu;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.Activity;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.SessionModifiers;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.world.Satisfy;
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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerLevel;
import java.util.Set;
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
    /** Server tick of the last block each player broke in Satisfy: mining is work, not rest. */
    private static final Map<UUID, Long> LAST_BREAK = new ConcurrentHashMap<>();
    /** Server tick until which each player is protected after a forced disconnection. Only in memory (10 seconds). */
    private static final Map<UUID, Long> PROTECTED_UNTIL = new ConcurrentHashMap<>();

    /** The operators who asked for the debug panel. Only in memory. */
    private static final Set<UUID> DEBUG_VIEWERS = ConcurrentHashMap.newKeySet();

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
        gameBus.addListener(FatigueService::onSleepFinished);
        gameBus.addListener(FatigueService::onBlockBroken);
    }

    /** Everyone who slept through the night in the real world rests a bit. */
    private static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.isSleepingLongEnough()) {
                    onSlept(player);
                }
            }
        }
    }

    /** The player slept through the night: gives back a share of the energy (not all of it, so it cannot be abused). */
    public static void onSlept(ServerPlayer player) {
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled() || WorldSide.of(player.level()) != WorldSide.REAL) {
            return;
        }
        double before = energy(player);
        setEnergy(player, before + balance.sleepRecovery());
        long gained = Math.round((energy(player) - before) * 100);
        if (gained > 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.fatigue.slept", gained), true);
        }
    }

    // ---- debug panel ----

    /** Turns the debug panel of this operator on or off; returns whether it is on now. */
    public static boolean toggleDebug(ServerPlayer player) {
        if (DEBUG_VIEWERS.remove(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, FatigueDebugPayload.HIDDEN);
            return false;
        }
        DEBUG_VIEWERS.add(player.getUUID());
        sendDebug(player);
        return true;
    }

    public static boolean isDebugging(ServerPlayer player) {
        return DEBUG_VIEWERS.contains(player.getUUID());
    }

    /** What the panel shows right now. */
    public static FatigueDebugPayload debugSnapshot(ServerPlayer player) {
        FatigueBalance balance = FatigueBalance.current();
        boolean satisfy = WorldSide.of(player.level()) == WorldSide.SATISFY;
        Vec3 here = player.position();
        Vec3 before = LAST_POSITION.get(player.getUUID());
        Activity activity = activity(player, here, before);
        double drain = satisfy ? FatigueRules.drainPerSecond(balance, activity, modifiers(player)) : 0.0;
        OptionalDouble minutes = satisfy ? FatigueRules.minutesLeft(balance, energy(player), activity, modifiers(player)) : OptionalDouble.empty();
        var last = CombatClock.last(player);
        int sinceCombat = last.isPresent() ? (int) Math.max(0, (player.server.getTickCount() - last.getAsLong()) / 20) : -1;
        return new FatigueDebugPayload(true, energy(player), activity.ordinal(), (float) drain,
                drain > 0 ? (int) Math.ceil(energy(player) / drain) : -1, graceLeft(player), sinceCombat, isProtected(player));
    }

    private static void sendDebug(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, debugSnapshot(player));
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
        LAST_BREAK.clear();
        PROTECTED_UNTIL.clear();
        DEBUG_VIEWERS.clear();
    }

    private static void onServerStarting(ServerStartingEvent event) {
        LAST_POSITION.clear();
        LAST_BREAK.clear();
        PROTECTED_UNTIL.clear();
        DEBUG_VIEWERS.clear();
    }

    /** During the grace period, and for a few seconds after reconnecting from it, nothing can hurt the player. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isProtected(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A block broken in Satisfy marks the player as working for a few seconds (see FatigueRules.activity). */
    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && WorldSide.of(player.level()) == WorldSide.SATISFY) {
            LAST_BREAK.put(player.getUUID(), (long) player.server.getTickCount());
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
        setEnergy(player, FatigueRules.afterForge(balance, before, modifiers(player)));
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

    /**
     * How this player's capsule changes the session: their last connection's tier (the basic one, neutral, if none).
     * The rested seconds of a connection are on the attachment, not here.
     */
    public static SessionModifiers modifiers(ServerPlayer player) {
        return CapsuleRules.modifiers(player.getData(ModAttachments.CAPSULE_TIER.get()), FatigueBalance.current(), CapsuleBalance.current());
    }

    /** Seconds of the rested bonus the player has left. */
    public static int restedSeconds(ServerPlayer player) {
        return player.getData(ModAttachments.RESTED_SECONDS.get());
    }

    private static void setRested(ServerPlayer player, int seconds) {
        if (seconds != restedSeconds(player)) {
            player.setData(ModAttachments.RESTED_SECONDS.get(), seconds);
        }
    }

    /** One second of the rested bonus goes by, in Satisfy only (whatever the fatigue or the game mode). */
    private static void tickRested(ServerPlayer player) {
        if (player.isAlive() && Satisfy.is(player.level()) && restedSeconds(player) > 0) {
            setRested(player, restedSeconds(player) - 1);
        }
    }

    /**
     * The player has just connected to Satisfy: the rested bonus is set (from the tier already stored on the player)
     * and, after a forced disconnection, they start protected.
     */
    public static void onConnected(ServerPlayer player) {
        setRested(player, CapsuleRules.restedOnConnect(modifiers(player), energy(player), CapsuleBalance.current()));
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
        LAST_BREAK.remove(player.getUUID());
        PROTECTED_UNTIL.remove(player.getUUID());
        DEBUG_VIEWERS.remove(player.getUUID());
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
        setEnergy(player, FatigueRules.afterRecovery(balance, energy(player), modifiers(player), seconds));
    }

    // ---- the second ----

    /**
     * One second of play: drains in Satisfy (survival and adventure only) and refills in the real world. In Satisfy it
     * also warns when a threshold is crossed and, with no energy left, runs the grace period that ends in the forced
     * disconnection.
     */
    public static void tickSecond(ServerPlayer player) {
        tickRested(player);
        update(player);
        if (DEBUG_VIEWERS.contains(player.getUUID())) {
            sendDebug(player);
        }
    }

    private static void update(ServerPlayer player) {
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
            setEnergy(player, FatigueRules.afterRecovery(balance, before, modifiers(player), 1.0));
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            endGrace(player);
            return;
        }
        setEnergy(player, FatigueRules.afterDrain(balance, before, activityOf(player), modifiers(player), 1.0));
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
        return activity(player, now, before);
    }

    /** The rule fed with the player's state; reads only, so the debug panel can call it too. */
    private static Activity activity(ServerPlayer player, Vec3 now, Vec3 before) {
        long tick = player.server.getTickCount();
        Long lastBreak = LAST_BREAK.get(player.getUUID());
        return FatigueRules.activity(FatigueRules.inCombat(tick, CombatClock.last(player)), atWorkstation(player), tick,
                lastBreak == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(lastBreak),
                before != null, before == null ? 0.0 : now.distanceToSqr(before));
    }

    /** Working at an anvil or a smelter: the window is open. */
    static boolean atWorkstation(ServerPlayer player) {
        return player.containerMenu instanceof SmithingAnvilMenu || player.containerMenu instanceof SmelterMenu;
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
