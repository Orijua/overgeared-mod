package com.orijua.overgearedmod.feature.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The system won't let anyone take what lies in the Northern End Cave (canon, cap. 1: the gear is piled like a mountain,
 * but Grid can't obtain any of it). Survival players can't break its blocks or touch the armored stands, and explosions
 * leave both alone. Only the book on the golden table can be taken. Creative players may still build and break.
 */
public final class NorthEndCaveProtection {
    private NorthEndCaveProtection() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NorthEndCaveProtection::onBreak);
        gameBus.addListener(NorthEndCaveProtection::onInteract);
        gameBus.addListener(NorthEndCaveProtection::onInteractAt);
        gameBus.addListener(NorthEndCaveProtection::onAttack);
        gameBus.addListener(NorthEndCaveProtection::onExplosion);
    }

    /** Whether a block belongs to one of the cave's pieces. */
    public static boolean inCave(LevelAccessor level, BlockPos pos) {
        return level instanceof ServerLevel server && server.structureManager().getStructureWithPieceAt(pos, structure -> structure.is(NorthEndCave.KEY)).isValid();
    }

    public static boolean isRelic(Entity entity) {
        return entity.getTags().contains(NorthEndCave.RELIC_TAG);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().isCreative() && inCave(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            refuse(event.getPlayer());
        }
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (guarded(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (guarded(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private static void onAttack(AttackEntityEvent event) {
        if (guarded(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> inCave(level, pos));
        event.getAffectedEntities().removeIf(NorthEndCaveProtection::isRelic);
    }

    /** A survival player reaching for a relic: refused, with the system's message (once, on the server). */
    private static boolean guarded(Player player, Entity target) {
        if (!isRelic(target) || player.isCreative()) {
            return false;
        }
        if (!player.level().isClientSide) {
            refuse(player);
        }
        return true;
    }

    private static void refuse(Player player) {
        player.displayClientMessage(Component.translatable("message.overgearedmod.cave_relic"), true);
    }
}
