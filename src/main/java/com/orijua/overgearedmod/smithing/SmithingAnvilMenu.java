package com.orijua.overgearedmod.smithing;

import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ItemForgedEvent;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * Smithing anvil, two jobs in one menu:
 * <ul>
 *     <li>Forging: material (top) + handle (bottom). The result slot only previews the item; it is forged with
 *     one of two buttons and goes straight to the inventory. <b>Automatic:</b> takes 1-4 s by smithing level
 *     and DEX (progress synced to the screen); changing the inputs or closing the anvil cancels it, spending
 *     nothing. <b>Manual:</b> the client plays the minigame and sends its precision ({@link #finishManual});
 *     the server clamps it and refuses results that came back implausibly fast or late.</li>
 *     <li>Enhancing: equipment (top) + enhancement stone (bottom) → "Enhance" button (+1, may fail).</li>
 * </ul>
 * Slot positions follow vanilla's furnace layout; the screen draws its own Satisfy-styled slots around them.
 */
public class SmithingAnvilMenu extends AbstractContainerMenu {
    public static final int TOP_SLOT = 0;
    public static final int BOTTOM_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int ENHANCE_BUTTON = 0;
    public static final int AUTO_FORGE_BUTTON = 1;
    public static final int MANUAL_FORGE_BUTTON = 2;
    private static final int INVENTORY_START = 3;
    private static final int INVENTORY_END = INVENTORY_START + 36;
    /** Synced to the client: automatic forging progress and total, in ticks (total 0 = not forging). */
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_TOTAL = 1;

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
    private final ContainerData forging = new SimpleContainerData(2);
    private RecipeHolder<ForgingRecipe> currentRecipe;
    // Server only: when the automatic or manual forge began (game time), and the recipe the automatic one is for.
    private long autoStart = -1;
    private RecipeHolder<ForgingRecipe> autoRecipe;
    private long manualStart = -1;

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
        addDataSlots(forging);
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

    /** Whether the inputs make something (the preview slot holds it); synced, so the screen can use it. */
    public boolean canForge() {
        return getSlot(RESULT_SLOT).hasItem();
    }

    public boolean autoForging() {
        return forging.get(DATA_TOTAL) > 0;
    }

    /** Automatic forging progress, 0-1. */
    public float autoProgress() {
        int total = forging.get(DATA_TOTAL);
        return total <= 0 ? 0 : forging.get(DATA_PROGRESS) / (float) total;
    }

    private boolean startAutoForge(ServerPlayer smith) {
        if (currentRecipe == null || autoForging()) {
            return false;
        }
        int dexterity = ProgressionService.effectiveStats(smith).dexterity();
        forging.set(DATA_TOTAL, ForgingRules.autoForgeTicks(dexterity, ProgressionService.effectiveSmithingLevel(smith)));
        forging.set(DATA_PROGRESS, 0);
        autoStart = smith.level().getGameTime();
        autoRecipe = currentRecipe;
        return true;
    }

    private void stopAutoForge() {
        forging.set(DATA_TOTAL, 0);
        forging.set(DATA_PROGRESS, 0);
        autoStart = -1;
        autoRecipe = null;
    }

    /** Runs every server tick while the menu is open (ServerPlayer.tick calls broadcastChanges). */
    private void tickAutoForge(ServerPlayer smith) {
        if (currentRecipe == null || !currentRecipe.id().equals(autoRecipe.id())) {
            stopAutoForge(); // inputs changed: cancel, nothing spent
            return;
        }
        long elapsed = smith.level().getGameTime() - autoStart;
        int total = forging.get(DATA_TOTAL);
        forging.set(DATA_PROGRESS, (int) Math.min(elapsed, total));
        if (elapsed >= total) {
            stopAutoForge();
            forge(smith, 0, 0, 1);
        }
    }

    /** The client asked to play the minigame: remember when, to judge the result it sends back. */
    private boolean startManualForge(ServerPlayer smith) {
        if (currentRecipe == null || autoForging()) {
            return false;
        }
        manualStart = smith.level().getGameTime();
        return true;
    }

    /**
     * The minigame's result arrived. Its precision is clamped to 0-1, and refused if no minigame was started,
     * the inputs no longer make anything, or it came back too fast (or too late) to be a real game.
     */
    public boolean finishManual(ServerPlayer smith, double precision) {
        if (manualStart < 0 || currentRecipe == null || autoForging()) {
            return false;
        }
        long elapsed = smith.level().getGameTime() - manualStart;
        manualStart = -1;
        if (!ForgingRules.plausibleManualDuration(elapsed)) {
            return false;
        }
        forge(smith, ForgingRules.manualSkillBonus(precision), ForgingRules.extraOptionChance(precision), ForgingRules.MANUAL_XP_MULTIPLIER);
        return true;
    }

    /** Makes the previewed item for real: rolls it, spends the inputs, rewards the smith and gives it to them. */
    private void forge(ServerPlayer smith, double skillBonus, double extraOptionChance, int xpMultiplier) {
        ForgingRecipe recipe = currentRecipe.value();
        ItemStack forged = result.getItem(0).copy();
        EquipmentService.stampForged(smith, forged, recipe.requirements(), skillBonus, extraOptionChance);
        inputs.removeItem(TOP_SLOT, recipe.materialCount());
        if (!inputs.getItem(BOTTOM_SLOT).isEmpty()) {
            inputs.removeItem(BOTTOM_SLOT, 1);
        }
        ProgressionService.addSmithingXp(smith, recipe.smithingXp() * xpMultiplier);
        ItemRating rating = forged.get(ModDataComponents.ITEM_RATING);
        if (rating != null) {
            NeoForge.EVENT_BUS.post(new ItemForgedEvent(smith, forged, rating));
            smith.displayClientMessage(Component.translatable("message.overgearedmod.forged", forged.getHoverName(), rating.displayName()), true);
        }
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f));
        if (!smith.getInventory().add(forged)) {
            smith.drop(forged, false);
        }
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer smith && autoForging()) {
            tickAutoForge(smith);
        }
        super.broadcastChanges();
    }

    /** Only shows what the inputs would make; forging happens with the Auto and Manual buttons. */
    private static class ResultSlot extends Slot {
        ResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
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

    private boolean enhance(ServerPlayer smith) {
        if (!canEnhance()) {
            return false;
        }
        EquipmentService.enhance(smith, inputs.getItem(TOP_SLOT));
        inputs.removeItem(BOTTOM_SLOT, 1); // the stone is used up whether it worked or not
        return true;
    }

    /** The screen's buttons arrive through vanilla's container-button packet; runs on the server. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer smith)) {
            return false;
        }
        return switch (id) {
            case ENHANCE_BUTTON -> enhance(smith);
            case AUTO_FORGE_BUTTON -> startAutoForge(smith);
            case MANUAL_FORGE_BUTTON -> startManualForge(smith);
            default -> false;
        };
    }

    // ---- Shared container plumbing ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index == RESULT_SLOT) {
            return ItemStack.EMPTY; // the result is only a preview: it can't be shift-clicked out
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

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
