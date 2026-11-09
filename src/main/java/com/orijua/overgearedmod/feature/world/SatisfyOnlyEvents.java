package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.feature.progression.ProgressionService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Keeps the mod's objects inert in the real world (see {@link SatisfyOnly}): they can't be placed or used with right
 * click (nor can its entities; the capsule, terminal and phone are the exceptions), and the passive bonuses are refreshed when
 * the player changes dimension. Also tells the game-test server
 * which level plays Satisfy by default.
 */
public final class SatisfyOnlyEvents {
    private SatisfyOnlyEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SatisfyOnlyEvents::onServerStarting);
        gameBus.addListener(SatisfyOnlyEvents::onServerStopped);
        gameBus.addListener(SatisfyOnlyEvents::onPlace);
        gameBus.addListener(SatisfyOnlyEvents::onRightClickItem);
        gameBus.addListener(SatisfyOnlyEvents::onRightClickBlock);
        gameBus.addListener(SatisfyOnlyEvents::onEntityInteract);
        gameBus.addListener(SatisfyOnlyEvents::onEntityInteractSpecific);
        gameBus.addListener(SatisfyOnlyEvents::onChangedDimension);
    }

    /** The game-test server drops the datapack's dimensions: there the overworld plays Satisfy unless a test says otherwise. */
    private static void onServerStarting(ServerStartingEvent event) {
        if (event.getServer() instanceof GameTestServer server) {
            Satisfy.testDefault(server.overworld());
            Satisfy.restoreStandIn();
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        Satisfy.testDefault(null);
        Satisfy.standIn(null);
    }

    /** A block of the mod placed by an entity in the real world is cancelled (the player is told). */
    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity placer = event.getEntity();
        if (placer == null || placer.level().isClientSide() || !SatisfyOnly.active(placer)
                || !SatisfyOnly.blocksPlacement(BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()))) {
            return;
        }
        event.setCanceled(true);
        if (placer instanceof ServerPlayer player) {
            SatisfyOnly.warn(player);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (inert(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            warnOnServer(event.getEntity());
        }
    }

    /**
     * Only the item's use is denied (a mod block can't be placed, a mod tool does nothing); the block clicked still
     * reacts, so a chest opens even with a mod item in the hand.
     */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (inert(event.getEntity(), event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
            warnOnServer(event.getEntity());
        }
    }

    /** The mod's entities (NPCs) don't answer to right click in the real world; fires on both sides, so no window opens. */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (refuses(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            warnOnServer(event.getEntity());
        }
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (refuses(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            warnOnServer(event.getEntity());
        }
    }

    private static boolean refuses(Player player, Entity target) {
        return SatisfyOnly.active(player) && SatisfyOnly.isSatisfyEntity(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
    }

    private static boolean inert(Player player, ItemStack stack) {
        return !stack.isEmpty() && SatisfyOnly.active(player) && SatisfyOnly.isSatisfyItem(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static void warnOnServer(Player player) {
        if (player instanceof ServerPlayer && !player.level().isClientSide()) {
            SatisfyOnly.warn(player);
        }
    }

    /** Going to or from Satisfy turns the passive bonuses off or on. */
    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionService.refreshEffects(player);
        }
    }
}
