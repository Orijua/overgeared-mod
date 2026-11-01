package com.orijua.overgearedmod.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Assigning a container to an apprentice from his window: the window's Assign button arms it, and the master's next
 * right-click on a block (within {@link #TIMEOUT_TICKS}) picks that container instead of opening it.
 */
public final class ApprenticeAssignments {
    /** 30 seconds to click the container. */
    static final long TIMEOUT_TICKS = 600;

    private record Pending(UUID apprentice, ApprenticeSmith.Slot slot, long until) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ApprenticeAssignments() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ApprenticeAssignments::onRightClickBlock);
        gameBus.addListener(ApprenticeAssignments::onLogout);
    }

    /** Arms the next right-click of {@code master} to assign {@code slot} of {@code apprentice}. */
    public static void start(ServerPlayer master, ApprenticeSmith apprentice, ApprenticeSmith.Slot slot) {
        PENDING.put(master.getUUID(), new Pending(apprentice.getUUID(), slot, master.level().getGameTime() + TIMEOUT_TICKS));
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_assign_" + slot.name().toLowerCase(),
                apprentice.getDisplayName(), ApprenticeSmith.ASSIGN_REACH), true);
    }

    /** Uses the armed assignment on the block at {@code pos}. Returns whether one was armed (and the click is taken). */
    public static boolean click(ServerPlayer master, BlockPos pos) {
        Pending pending = PENDING.remove(master.getUUID());
        if (pending == null || master.level().getGameTime() > pending.until()) {
            return false;
        }
        if (master.serverLevel().getEntity(pending.apprentice()) instanceof ApprenticeSmith apprentice && apprentice.isOwnedBy(master)) {
            apprentice.assign(master, pending.slot(), pos);
        }
        return true;
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer master && click(master, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}
