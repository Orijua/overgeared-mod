package com.orijua.overgearedmod.smelting;

import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModBlockEntities;
import com.orijua.overgearedmod.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The smelter's state: its three slots (metal, fuel, ingots) and its heat, kept while nobody looks.
 * <p>
 * Every tick the fuel burns (only while there is metal it can melt), pulling the temperature up towards the fuel's
 * heat ({@link #fuelHeat}); without fuel it cools. <b>Automatic:</b> a smith starts it (if their smithing level is
 * enough) and it melts one piece after another while it is hot enough, each with a chance of an extra ingot by that
 * smith's level. <b>Manual:</b> the smith plays the bellows minigame; its precision decides an extra ingot or slag
 * ({@link #finishManual}). Smithing XP goes to the smith who started it.
 */
public class SmelterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;
    /** What the menu syncs to the screen. */
    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_FUEL_HEAT = 1;
    public static final int DATA_BURN = 2;
    public static final int DATA_BURN_TOTAL = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_TIME = 5;
    public static final int DATA_MIN_TEMPERATURE = 6;
    public static final int DATA_MELTING = 7;
    public static final int DATA_COUNT = 8;
    private static final Component TITLE = Component.translatable("container.overgearedmod.smelter");

    private final SimpleContainer items = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmelterBlockEntity.this.setChanged();
        }
    };
    private int temperature = SmelterRules.AMBIENT;
    private int fuelHeat;
    private int burnTime;
    private int burnTotal;
    private int progress;
    private boolean melting;
    @Nullable
    private UUID melter;
    private int melterLevel;
    @Nullable
    private RecipeHolder<MeltingRecipe> currentRecipe;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TEMPERATURE -> temperature;
                case DATA_FUEL_HEAT -> fuelHeat;
                case DATA_BURN -> burnTime;
                case DATA_BURN_TOTAL -> burnTotal;
                case DATA_PROGRESS -> progress;
                case DATA_TIME -> currentRecipe == null ? 0 : currentRecipe.value().time();
                case DATA_MIN_TEMPERATURE -> currentRecipe == null ? 0 : currentRecipe.value().minTemperature();
                case DATA_MELTING -> melting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMELTER.get(), pos, state);
    }

    /** How hot a fuel can get the smelter: lava 1600, blaze rods 1500, coal 1050, wood and the rest 700. */
    public static int fuelHeat(ItemStack fuel) {
        if (fuel.is(Items.LAVA_BUCKET)) {
            return 1600;
        }
        if (fuel.is(Items.BLAZE_ROD) || fuel.is(Items.BLAZE_POWDER)) {
            return 1500;
        }
        if (fuel.is(Items.COAL_BLOCK)) {
            return 1150;
        }
        if (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL)) {
            return 1050;
        }
        return 700;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public SimpleContainer items() {
        return items;
    }

    public ContainerData data() {
        return data;
    }

    public int temperature() {
        return temperature;
    }

    public boolean melting() {
        return melting;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity smelter) {
        smelter.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        currentRecipe = findRecipe(level);
        if (burnTime > 0) {
            burnTime--;
        }
        if (burnTime <= 0 && currentRecipe != null) {
            burnFuel();
        }
        boolean burning = burnTime > 0;
        temperature = SmelterRules.nextTemperature(temperature, fuelHeat, burning);
        if (melting) {
            meltStep(level);
        }
        if (state.getValue(SmelterBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(SmelterBlock.LIT, burning), 3);
        }
        setChanged();
    }

    @Nullable
    private RecipeHolder<MeltingRecipe> findRecipe(Level level) {
        ItemStack metal = items.getItem(INPUT);
        if (metal.isEmpty()) {
            return null;
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.MELTING.get(), new SingleRecipeInput(metal), level).orElse(null);
    }

    private void burnFuel() {
        ItemStack fuel = items.getItem(FUEL);
        int burn = fuel.getBurnTime(RecipeType.SMELTING);
        if (burn <= 0) {
            return;
        }
        fuelHeat = fuelHeat(fuel);
        burnTime = burn;
        burnTotal = burn;
        if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
            items.setItem(FUEL, fuel.getCraftingRemainingItem()); // the lava bucket's empty bucket
        } else {
            fuel.shrink(1);
        }
    }

    /** Automatic melting: waits for heat and room, stops when the metal runs out or needs more skill. */
    private void meltStep(ServerLevel level) {
        if (currentRecipe == null || currentRecipe.value().smithingLevel() > melterLevel) {
            stopMelting();
            return;
        }
        MeltingRecipe recipe = currentRecipe.value();
        if (temperature < recipe.minTemperature() || !hasRoomFor(recipe, 2)) {
            return;
        }
        if (++progress >= recipe.time()) {
            progress = 0;
            int ingots = level.random.nextDouble() < SmelterRules.autoExtraChance(melterLevel) ? 2 : 1;
            ServerPlayer smith = melter == null ? null : level.getServer().getPlayerList().getPlayer(melter);
            meltOne(level, recipe, ingots, 1, smith);
        }
    }

    private void stopMelting() {
        melting = false;
        progress = 0;
        melter = null;
    }

    private boolean hasRoomFor(MeltingRecipe recipe, int ingots) {
        ItemStack output = items.getItem(OUTPUT);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, recipe.result())
                && output.getCount() + ingots <= output.getMaxStackSize();
    }

    /** Spends one piece of metal and puts {@code ingots} (0 for slag) in the output; {@code smith}, if any, gets the XP. */
    private void meltOne(ServerLevel level, MeltingRecipe recipe, int ingots, int xpMultiplier, @Nullable ServerPlayer smith) {
        items.getItem(INPUT).shrink(1);
        if (ingots > 0) {
            ItemStack output = items.getItem(OUTPUT);
            if (output.isEmpty()) {
                items.setItem(OUTPUT, recipe.result().copyWithCount(ingots));
            } else {
                output.grow(ingots);
            }
        }
        if (smith != null) {
            ProgressionService.addSmithingXp(smith, recipe.smithingXp() * xpMultiplier);
        }
        level.playSound(null, worldPosition, ingots > 0 ? SoundEvents.LAVA_POP : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
        items.setChanged();
    }

    /** The Auto button: starts melting if the smith knows enough, or stops it. */
    public boolean toggleAuto(ServerPlayer smith) {
        if (melting) {
            stopMelting();
            return true;
        }
        if (!checkCanMelt(smith)) {
            return false;
        }
        melting = true;
        progress = 0;
        melter = smith.getUUID();
        melterLevel = ProgressionService.effectiveSmithingLevel(smith);
        return true;
    }

    /** Whether the smith can tend a manual melt now: metal, skill, room, no automatic melt running, and enough heat. */
    public boolean canStartManual(ServerPlayer smith) {
        if (melting || !checkCanMelt(smith)) {
            return false;
        }
        MeltingRecipe recipe = currentRecipe.value();
        if (temperature < recipe.minTemperature()) {
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_too_cold", temperature,
                    recipe.minTemperature()), true);
            return false;
        }
        return true;
    }

    /** Metal the smelter can melt, the smith's smithing level, and room for the ingots; says what is missing. */
    private boolean checkCanMelt(ServerPlayer smith) {
        currentRecipe = findRecipe(smith.level());
        if (currentRecipe == null) {
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_nothing"), true);
            return false;
        }
        MeltingRecipe recipe = currentRecipe.value();
        if (ProgressionService.effectiveSmithingLevel(smith) < recipe.smithingLevel()) {
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_level", recipe.smithingLevel(),
                    recipe.result().getHoverName()), true);
            return false;
        }
        if (!hasRoomFor(recipe, 2)) {
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_full"), true);
            return false;
        }
        return true;
    }

    /**
     * The bellows minigame's result: one piece of metal melts, with a chance of an extra ingot for a good hand or of
     * slag for a bad one, and double smithing XP. The precision is clamped; the menu already checked the timing.
     */
    public boolean finishManual(ServerPlayer smith, double precision) {
        currentRecipe = findRecipe(smith.level());
        if (currentRecipe == null || melting || !hasRoomFor(currentRecipe.value(), 2)) {
            return false;
        }
        ServerLevel level = smith.serverLevel();
        double roll = level.random.nextDouble();
        int ingots;
        if (roll < SmelterRules.slagChance(precision)) {
            ingots = 0;
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_slag"), true);
        } else if (roll < SmelterRules.manualExtraChance(precision)) {
            ingots = 2;
            smith.displayClientMessage(Component.translatable("message.overgearedmod.smelter_extra"), true);
        } else {
            ingots = 1;
        }
        meltOne(level, currentRecipe.value(), ingots, SmelterRules.MANUAL_XP_MULTIPLIER, smith);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        tag.putInt("temperature", temperature);
        tag.putInt("fuel_heat", fuelHeat);
        tag.putInt("burn_time", burnTime);
        tag.putInt("burn_total", burnTotal);
        tag.putInt("progress", progress);
        tag.putBoolean("melting", melting);
        if (melter != null) {
            tag.putUUID("melter", melter);
        }
        tag.putInt("melter_level", melterLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> loaded = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, loaded, registries);
        for (int slot = 0; slot < loaded.size(); slot++) {
            items.setItem(slot, loaded.get(slot));
        }
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : SmelterRules.AMBIENT;
        fuelHeat = tag.getInt("fuel_heat");
        burnTime = tag.getInt("burn_time");
        burnTotal = tag.getInt("burn_total");
        progress = tag.getInt("progress");
        melting = tag.getBoolean("melting");
        melter = tag.hasUUID("melter") ? tag.getUUID("melter") : null;
        melterLevel = tag.getInt("melter_level");
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SmelterMenu(containerId, inventory, this, ContainerLevelAccess.create(level, worldPosition));
    }
}
