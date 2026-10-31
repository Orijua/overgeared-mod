package com.orijua.overgearedmod.smelting;

import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The smelter's screen: metal (top), fuel (bottom) and ingots (right, take only), its heat synced from the block
 * entity, and three buttons. <b>Fire</b> lights or puts out the fire; <b>Auto</b> starts or stops melting once hot;
 * <b>Bellows</b> starts the manual minigame on the client,
 * whose precision comes back through {@link #finishManual}, refused when it arrives implausibly fast or late.
 */
public class SmelterMenu extends AbstractContainerMenu {
    public static final int AUTO_BUTTON = 0;
    public static final int MANUAL_BUTTON = 1;
    public static final int FIRE_BUTTON = 2;
    private static final int SLOTS = 3;
    private static final int INVENTORY_END = SLOTS + 36;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    @Nullable
    private final SmelterBlockEntity smelter;
    /** Server only: when the manual minigame began (game time), -1 when none is running. */
    private long manualStart = -1;

    /** Client-side constructor, called by the MenuType when the server opens the menu. */
    public SmelterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(SmelterBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL, null);
    }

    public SmelterMenu(int containerId, Inventory inventory, SmelterBlockEntity smelter, ContainerLevelAccess access) {
        this(containerId, inventory, smelter.items(), smelter.data(), access, smelter);
    }

    private SmelterMenu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access,
                        @Nullable SmelterBlockEntity smelter) {
        super(ModMenus.SMELTER.get(), containerId);
        this.container = container;
        this.data = data;
        this.access = access;
        this.smelter = smelter;

        addSlot(new Slot(container, SmelterBlockEntity.INPUT, 56, 17));
        addSlot(new Slot(container, SmelterBlockEntity.FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SmelterBlockEntity.isFuel(stack);
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int temperature() {
        return data.get(SmelterBlockEntity.DATA_TEMPERATURE);
    }

    /** How hot the fuel that burns (or last burned) can get the smelter. */
    public int fuelHeat() {
        return data.get(SmelterBlockEntity.DATA_FUEL_HEAT);
    }

    /** The melting temperature of the metal in the input, 0 when it melts nothing. */
    public int meltingTemperature() {
        return data.get(SmelterBlockEntity.DATA_MIN_TEMPERATURE);
    }

    public boolean fireOn() {
        return data.get(SmelterBlockEntity.DATA_FIRE_ON) == 1;
    }

    public boolean melting() {
        return data.get(SmelterBlockEntity.DATA_MELTING) == 1;
    }

    /** Fuel left in the current burn, 0-1. */
    public float burnLeft() {
        int total = data.get(SmelterBlockEntity.DATA_BURN_TOTAL);
        return total <= 0 ? 0 : data.get(SmelterBlockEntity.DATA_BURN) / (float) total;
    }

    /** Automatic melting progress of the current piece, 0-1. */
    public float meltProgress() {
        int time = data.get(SmelterBlockEntity.DATA_TIME);
        return time <= 0 ? 0 : data.get(SmelterBlockEntity.DATA_PROGRESS) / (float) time;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer smith) || smelter == null) {
            return false;
        }
        return switch (id) {
            case AUTO_BUTTON -> smelter.toggleAuto(smith);
            case FIRE_BUTTON -> smelter.toggleFire(smith);
            case MANUAL_BUTTON -> {
                if (!smelter.canStartManual(smith)) {
                    yield false;
                }
                manualStart = smith.level().getGameTime();
                yield true;
            }
            default -> false;
        };
    }

    /** The minigame's result arrived: believed only for a minigame started here, after a plausible time. */
    public boolean finishManual(ServerPlayer smith, double precision) {
        if (smelter == null || manualStart < 0) {
            return false;
        }
        long elapsed = smith.level().getGameTime() - manualStart;
        manualStart = -1;
        return SmelterRules.plausibleManualDuration(elapsed) && smelter.finishManual(smith, precision);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From the inventory: fuel to the fuel slot, anything else to the metal slot.
            boolean fueled = SmelterBlockEntity.isFuel(stack)
                    && moveItemStackTo(stack, SmelterBlockEntity.FUEL, SmelterBlockEntity.FUEL + 1, false);
            if (!fueled && !moveItemStackTo(stack, SmelterBlockEntity.INPUT, SmelterBlockEntity.INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SMELTER.get());
    }
}
