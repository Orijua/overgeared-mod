package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModMenus;
import com.orijua.overgearedmod.registry.ModRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Smithing anvil, two jobs in one menu:
 * <ul>
 *     <li>Forging: material (top) + handle (bottom) → result slot. Rating, options and requirements are
 *     rolled on the server only when the result is taken.</li>
 *     <li>Enhancing: equipment (top) + enhancement stone (bottom) → "Enhance" button (+1, may fail).</li>
 * </ul>
 * Slot positions match vanilla's furnace texture, which the screen reuses as a placeholder.
 */
public class SmithingAnvilMenu extends AbstractContainerMenu {
    public static final int TOP_SLOT = 0;
    public static final int BOTTOM_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int ENHANCE_BUTTON = 0;
    private static final int INVENTORY_START = 3;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmithingAnvilMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private RecipeHolder<ForgingRecipe> currentRecipe;

    /** Client-side constructor, called by the MenuType when the server opens the menu. */
    public SmithingAnvilMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SmithingAnvilMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.SMITHING_ANVIL.get(), containerId);
        this.access = access;
        this.player = inventory.player;

        addSlot(new Slot(inputs, TOP_SLOT, 56, 17));
        addSlot(new Slot(inputs, BOTTOM_SLOT, 56, 53));
        addSlot(new ResultSlot(result, 0, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    // ---- Forging ----

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        access.execute((level, pos) -> updateResult(level));
    }

    /** Server only (access is NULL on the client); the result slot then syncs by itself. */
    private void updateResult(Level level) {
        ForgingRecipeInput input = new ForgingRecipeInput(inputs.getItem(TOP_SLOT), inputs.getItem(BOTTOM_SLOT));
        Optional<RecipeHolder<ForgingRecipe>> recipe = level.getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(), input, level);
        currentRecipe = recipe.orElse(null);
        result.setItem(0, recipe.map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY));
    }

    private void stampForged(ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer && currentRecipe != null) {
            EquipmentService.stampForged(serverPlayer, stack, currentRecipe.value().requirements());
        }
    }

    /** Consumes the inputs and rewards the smith. Runs on both sides; rewards only on the server. */
    private void finishForging(ItemStack forged) {
        int materialCount = currentRecipe != null ? currentRecipe.value().materialCount() : 1;
        int smithingXp = currentRecipe != null ? currentRecipe.value().smithingXp() : 0;
        inputs.removeItem(TOP_SLOT, materialCount);
        if (!inputs.getItem(BOTTOM_SLOT).isEmpty()) {
            inputs.removeItem(BOTTOM_SLOT, 1);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ProgressionService.addSmithingXp(serverPlayer, smithingXp);
            ItemRating rating = forged.get(ModDataComponents.ITEM_RATING);
            if (rating != null) {
                serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.forged", forged.getHoverName(), rating.displayName()), true);
            }
            access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f));
        }
    }

    private class ResultSlot extends Slot {
        ResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        /** Called with the very stack the player now holds, so stamping it here sticks. */
        @Override
        public void onTake(Player player, ItemStack stack) {
            stampForged(stack);
            finishForging(stack);
            super.onTake(player, stack);
        }
    }

    // ---- Enhancing ----

    public boolean canEnhance() {
        return inputs.getItem(BOTTOM_SLOT).is(ModItems.ENHANCEMENT_STONE.get())
                && EquipmentService.canEnhance(inputs.getItem(TOP_SLOT))
                && currentEnhancement() < EquipmentRules.MAX_ENHANCEMENT;
    }

    public int currentEnhancement() {
        return inputs.getItem(TOP_SLOT).getOrDefault(ModDataComponents.ENHANCEMENT, 0);
    }

    /** Sent by the screen's button through vanilla's container-button packet; runs on the server. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != ENHANCE_BUTTON || !canEnhance() || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        EquipmentService.enhance(serverPlayer, inputs.getItem(TOP_SLOT));
        inputs.removeItem(BOTTOM_SLOT, 1); // the stone is used up whether it worked or not
        return true;
    }

    // ---- Shared container plumbing ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == RESULT_SLOT) {
            // moveItemStackTo copies the stack, so it must be stamped before moving.
            stampForged(stack);
            ItemStack forged = stack.copy();
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                slot.set(original);
                return ItemStack.EMPTY;
            }
            finishForging(forged); // refreshes the preview; vanilla repeats while ingredients last
            return original;
        }

        boolean moved = index < INVENTORY_START
                ? moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)
                : moveItemStackTo(stack, TOP_SLOT, BOTTOM_SLOT + 1, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SMITHING_ANVIL.get());
    }
}
