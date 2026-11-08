package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.feature.character.Characters;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;

/**
 * The real world (overworld, Nether, End, any other dimension) and Satisfy are separate worlds: whatever tries to go
 * from one to the other is cancelled, except while the capsule connects/disconnects or the debug goto teleports.
 * Inside the same category nothing changes.
 */
public final class WorldSeparation {
    private static final ThreadLocal<Boolean> ALLOWED = ThreadLocal.withInitial(() -> false);

    private WorldSeparation() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(WorldSeparation::onTravel);
        gameBus.addListener(WorldSeparation::onSetSpawn);
    }

    /**
     * The way a player changes world: closes their menu in the world they leave, teleports with crossing allowed, and
     * then makes them carry the character of the world they are in ({@link Characters#settle}). If the trip did not
     * happen (another mod cancelled it) the world is the same and nothing is swapped.
     */
    public static void cross(ServerPlayer player, Runnable teleport) {
        Characters.prepareCrossing(player);
        crossing(teleport);
        Characters.settle(player);
    }

    /** Runs {@code teleport} with crossing between the two worlds allowed; always restored afterwards. */
    public static void crossing(Runnable teleport) {
        boolean before = ALLOWED.get();
        ALLOWED.set(true);
        try {
            teleport.run();
        } finally {
            ALLOWED.set(before);
        }
    }

    public static boolean crossingAllowed() {
        return ALLOWED.get();
    }

    private static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (CapsuleRules.crossingBlocked(Satisfy.is(entity.level()), Satisfy.is(event.getDimension()), crossingAllowed())) {
            event.setCanceled(true);
            ServerPlayer toTell = entity instanceof ServerPlayer player ? player
                    : entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner ? owner : null;
            if (toTell != null) {
                toTell.displayClientMessage(Component.translatable("message.overgearedmod.capsule.crossing_blocked"), true);
            }
        }
    }

    /**
     * A bed (or anchor) in Satisfy must not set the respawn point of the real world: dying in Satisfy sends you back to
     * the real world's respawn, and the respawn itself does not fire the travel event. Resets (no new position) carry
     * the overworld, so they pass.
     */
    private static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (Satisfy.is(event.getSpawnLevel())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.bed_ignored"), true);
            }
        }
    }
}
