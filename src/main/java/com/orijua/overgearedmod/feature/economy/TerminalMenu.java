package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.core.registry.ModMenus;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The terminal's window: no slots, only the state the server sends. It stays open while the player is alive, in the
 * real world, and either near the terminal block that opened it or carrying a phone. The server pushes a new state when
 * the game day changes while the window is open (the order of the day changes with it).
 */
public class TerminalMenu extends AbstractContainerMenu {
    /** Squared distance (8 blocks) from the terminal block at which the window closes. */
    public static final double RANGE_SQR = 64.0;

    private final Optional<BlockPos> terminal;
    /** The server-side owner; null on the client, where nothing is pushed. */
    private final ServerPlayer owner;
    /** The game day of the last state sent to the client. */
    private long sentDay = Long.MIN_VALUE;

    /** Server-side: {@code terminal} is the block that opened it, empty when it was opened with a phone. */
    public TerminalMenu(int containerId, Inventory inventory, Optional<BlockPos> terminal) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.owner = inventory.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    /** Client-side constructor, called by the MenuType when the server opens the menu: reads the first state. */
    public TerminalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, Optional.empty());
        ClientTerminal.update(containerId, TerminalState.STREAM_CODEC.decode(buf));
    }

    /** Records the game day of the state just sent to the client. */
    public void markSent(long day) {
        this.sentDay = day;
    }

    public long sentDay() {
        return sentDay;
    }

    /** Called every tick by the server for the open menu: if the day changed since the last state, sends the new one. */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (owner != null && TerminalService.today(owner) != sentDay) {
            TerminalService.sendState(owner, this, Optional.empty());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (Satisfy.is(player.level()) || !player.isAlive()) {
            return false;
        }
        if (terminal.isPresent()) {
            BlockPos pos = terminal.get();
            if (player.level().getBlockState(pos).is(ModBlocks.ACCOUNT_TERMINAL.get())
                    && player.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE_SQR) {
                return true;
            }
        }
        return player.getInventory().contains(stack -> stack.is(ModItems.PHONE.get()));
    }
}
