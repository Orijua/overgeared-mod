package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.Activity;
import com.orijua.overgearedmod.feature.fatigue.FatigueRules.SessionModifiers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

    private FatigueService() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(FatigueService::onTick);
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

    /** One second of play: drains in Satisfy (survival and adventure only), refills in the real world. */
    public static void tickSecond(ServerPlayer player) {
        FatigueBalance balance = FatigueBalance.current();
        if (!balance.enabled()) {
            return;
        }
        double before = energy(player);
        if (WorldSide.of(player.level()) == WorldSide.SATISFY) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            setEnergy(player, FatigueRules.afterDrain(balance, before, activityOf(player), SessionModifiers.NEUTRAL, 1.0));
        } else {
            setEnergy(player, FatigueRules.afterRecovery(balance, before, SessionModifiers.NEUTRAL, 1.0));
        }
    }

    /** What the player is doing. Step 4 tells rest and combat apart; for now every second is normal play. */
    static Activity activityOf(ServerPlayer player) {
        return Activity.NORMAL;
    }
}
