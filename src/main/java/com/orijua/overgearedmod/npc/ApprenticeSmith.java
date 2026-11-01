package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An apprentice blacksmith (own design inspired by the canon, caps. 622, 624 and 925). He works for the smith who
 * hired him, at a smithing anvil within {@link #STATION_REACH} blocks: he takes the materials of his order from a
 * chest touching the anvil and leaves each piece in a barrel touching it. One piece every
 * {@link ApprenticeRules#ticksPerPiece} ticks, rated a bit worse than his master would, sometimes botched (the materials
 * are lost). Every piece teaches him; his level never reaches his master's. Without his daily wage he stops.
 * <p>
 * His master deals with him by right-clicking: with emeralds, pays his wage; with an item, orders it (any forging
 * recipe without a class); empty-handed, asks how it goes; sneaking empty-handed, dismisses him.
 */
public class ApprenticeSmith extends PathfinderMob {
    static final int STATION_REACH = 4;
    private static final int WORK_STEP = 20;
    private static final List<String> NAMES = List.of("Bran", "Tomas", "Gerd", "Ilse", "Marek", "Rolf", "Hilda", "Otto", "Lena", "Ivo");

    /** Why he isn't working, shown when his master asks. */
    public enum Status { WORKING, NO_ORDER, NO_ANVIL, UNPAID, NO_MATERIALS, BARREL_FULL }

    private UUID owner;
    private int smithingXp = ApprenticeRules.STARTING_XP;
    private int masterLevel = ApprenticeRules.FIRST_APPRENTICE_LEVEL;
    private Item order;
    private BlockPos anvil;
    private long paidUntil;
    private int workTicks;
    private Status status = Status.NO_ORDER;
    /** What stopped the last piece (no materials, full barrel), or null if it went through. */
    private Status blocked;

    public ApprenticeSmith(EntityType<? extends ApprenticeSmith> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** A new apprentice for {@code master}, with a name from the village. */
    public void hireBy(ServerPlayer master) {
        owner = master.getUUID();
        masterLevel = ProgressionService.effectiveSmithingLevel(master);
        paidUntil = level().getGameTime() + ApprenticeRules.DAY_TICKS; // the first day is on Smith
        setCustomName(Component.literal(NAMES.get(getRandom().nextInt(NAMES.size()))));
        setCustomNameVisible(true);
        findAnvil();
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public int smithingLevel() {
        return ApprenticeRules.workingLevel(ProgressionRules.smithingLevel(smithingXp), masterLevel);
    }

    public int smithingXp() {
        return smithingXp;
    }

    public Status status() {
        return status;
    }

    public Optional<Item> order() {
        return Optional.ofNullable(order);
    }

    public int daysPaid() {
        return (int) Math.max(0, (paidUntil - level().getGameTime() + ApprenticeRules.DAY_TICKS - 1) / ApprenticeRules.DAY_TICKS);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide || !(player instanceof ServerPlayer master)) {
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(master)) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_not_yours", getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        ItemStack held = master.getItemInHand(hand);
        if (held.isEmpty() && master.isShiftKeyDown()) {
            dismiss(master);
        } else if (held.isEmpty()) {
            report(master);
        } else if (held.is(Items.EMERALD)) {
            pay(master, held);
        } else {
            takeOrder(master, held.getItem());
        }
        return InteractionResult.CONSUME;
    }

    /** Pays as many days as the emeralds in hand buy (up to {@link ApprenticeRules#MAX_PAID_DAYS} ahead). */
    public void pay(ServerPlayer master, ItemStack emeralds) {
        int days = ApprenticeRules.daysBought(emeralds.getCount(), daysPaid());
        if (days == 0) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_paid_enough", getDisplayName()), true);
            return;
        }
        emeralds.shrink(days * ApprenticeRules.DAILY_WAGE);
        paidUntil = Math.max(paidUntil, level().getGameTime()) + days * ApprenticeRules.DAY_TICKS;
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_paid", getDisplayName(), days, daysPaid()), true);
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
    }

    /** Orders {@code item}, if a forging recipe without a class makes it. */
    public boolean takeOrder(ServerPlayer master, Item item) {
        if (recipeFor(item).isEmpty()) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_cant_make", getDisplayName(),
                    item.getDescription()), true);
            return false;
        }
        order = item;
        workTicks = 0;
        findAnvil();
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_order", getDisplayName(), item.getDescription()), true);
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }

    private void report(ServerPlayer master) {
        Component orderName = order().map(Item::getDescription).orElse(Component.translatable("message.overgearedmod.apprentice_no_order_name"));
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_report", getDisplayName(), smithingLevel(),
                orderName, daysPaid(), Component.translatable("message.overgearedmod.apprentice_status." + status.name().toLowerCase())), false);
    }

    private void dismiss(ServerPlayer master) {
        List<UUID> hired = new ArrayList<>(master.getData(ModAttachments.APPRENTICES));
        hired.remove(getUUID());
        master.setData(ModAttachments.APPRENTICES, List.copyOf(hired));
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_dismissed", getDisplayName()), false);
        discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % WORK_STEP != 0 || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer master) {
            masterLevel = ProgressionService.effectiveSmithingLevel(master);
        }
        status = readyStatus();
        if (status != Status.WORKING) {
            return;
        }
        if (blocked != null) {
            status = blocked;
        }
        workTicks += WORK_STEP;
        if (workTicks % 100 == 0) {
            swing(InteractionHand.MAIN_HAND);
            level.playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
        if (workTicks >= ApprenticeRules.ticksPerPiece(smithingLevel())) {
            workTicks = 0;
            forgeOne(getRandom().nextDouble());
        }
    }

    /** Whether he can work right now, or why not. Needs his order, an anvil near him and his wage paid. */
    private Status readyStatus() {
        if (order == null) {
            return Status.NO_ORDER;
        }
        if (anvil == null || !level().getBlockState(anvil).is(ModBlocks.SMITHING_ANVIL.get()) || anvil.distSqr(blockPosition()) > STATION_REACH * STATION_REACH) {
            findAnvil();
            if (anvil == null) {
                return Status.NO_ANVIL;
            }
        }
        if (level().getGameTime() >= paidUntil) {
            return Status.UNPAID;
        }
        return Status.WORKING;
    }

    /**
     * One piece of his order: the materials leave the chest, and then either the piece is botched ({@code roll01} under
     * his fail chance) or it lands in the barrel, rated by his level. Either way he learns. Returns what happened.
     */
    public Status forgeOne(double roll01) {
        Optional<ForgingRecipe> found = order().flatMap(this::recipeFor);
        if (found.isEmpty() || anvil == null) {
            return blocked = Status.NO_ORDER;
        }
        ForgingRecipe recipe = found.get();
        List<Container> chests = touching(ChestBlockEntity.class);
        Optional<Container> barrel = touching(BarrelBlockEntity.class).stream().findFirst();
        if (barrel.isEmpty() || !hasRoom(barrel.get())) {
            return blocked = Status.BARREL_FULL;
        }
        if (!has(chests, recipe.material(), recipe.materialCount()) || !recipe.handle().isEmpty() && !has(chests, recipe.handle(), 1)) {
            return blocked = Status.NO_MATERIALS;
        }
        blocked = null;
        take(chests, recipe.material(), recipe.materialCount());
        if (!recipe.handle().isEmpty()) {
            take(chests, recipe.handle(), 1);
        }
        smithingXp += recipe.smithingXp();
        if (roll01 < ApprenticeRules.failChance(smithingLevel())) {
            level().playSound(null, anvil, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.6f, 1.0f);
            return Status.WORKING;
        }
        ItemStack piece = recipe.result().copy();
        EquipmentService.stampForged(level(), piece, recipe.requirements(), recipe.extras(), 0, smithingLevel(), ApprenticeRules.RATING_PENALTY, 0,
                getRandom());
        put(barrel.get(), piece);
        level().playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        return Status.WORKING;
    }

    /** The forging recipe that makes {@code item}, if it has no class requirement (no blueprints for apprentices). */
    private Optional<ForgingRecipe> recipeFor(Item item) {
        return level().getRecipeManager().getAllRecipesFor(ModRecipes.FORGING.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.result().is(item) && recipe.extras().requiredClass().isEmpty())
                .findFirst();
    }

    /** The nearest smithing anvil within reach, or none. */
    private void findAnvil() {
        anvil = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-STATION_REACH, -1, -STATION_REACH),
                blockPosition().offset(STATION_REACH, 1, STATION_REACH))) {
            if (level().getBlockState(pos).is(ModBlocks.SMITHING_ANVIL.get()) && pos.distSqr(blockPosition()) < best) {
                best = pos.distSqr(blockPosition());
                anvil = pos.immutable();
            }
        }
    }

    /** The containers of type {@code type} touching the anvil (any side). */
    private <T extends BlockEntity & Container> List<Container> touching(Class<T> type) {
        List<Container> found = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockEntity entity = level().getBlockEntity(anvil.relative(side));
            if (type.isInstance(entity)) {
                found.add((Container) entity);
            }
        }
        return found;
    }

    private static boolean has(List<Container> containers, Ingredient ingredient, int count) {
        int found = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (ingredient.test(stack)) {
                    found += stack.getCount();
                }
            }
        }
        return found >= count;
    }

    private static void take(List<Container> containers, Ingredient ingredient, int count) {
        int missing = count;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize() && missing > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (ingredient.test(stack)) {
                    int taken = Math.min(missing, stack.getCount());
                    container.removeItem(slot, taken);
                    missing -= taken;
                }
            }
            container.setChanged();
        }
    }

    private static boolean hasRoom(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void put(Container container, ItemStack piece) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, piece);
                container.setChanged();
                return;
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return NpcRules.isInvulnerableTo(source);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("SmithingXp", smithingXp);
        tag.putInt("MasterLevel", masterLevel);
        if (order != null) {
            tag.putString("Order", BuiltInRegistries.ITEM.getKey(order).toString());
        }
        if (anvil != null) {
            tag.put("Anvil", NbtUtils.writeBlockPos(anvil));
        }
        tag.putLong("PaidUntil", paidUntil);
        tag.putInt("WorkTicks", workTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        smithingXp = tag.getInt("SmithingXp");
        masterLevel = tag.getInt("MasterLevel");
        order = tag.contains("Order") ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(tag.getString("Order"))).orElse(null) : null;
        anvil = NbtUtils.readBlockPos(tag, "Anvil").orElse(null);
        paidUntil = tag.getLong("PaidUntil");
        workTicks = tag.getInt("WorkTicks");
    }
}
