package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.feature.equipment.EquipmentService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import com.orijua.overgearedmod.feature.npc.client.ApprenticeScreen;
import com.orijua.overgearedmod.feature.progression.ProgressionRules;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.smithing.ForgingRecipe;
import com.orijua.overgearedmod.feature.smithing.HammerRules;
import com.orijua.overgearedmod.feature.smithing.SmithingHammers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An apprentice blacksmith (own design inspired by the canon, caps. 622, 624 and 925). He works for the smith who
 * hired him, at a smithing anvil within {@link #STATION_REACH} blocks: he takes the materials of his order from the
 * chests touching the anvil and leaves each piece in a barrel touching it, unless his master assigned other containers
 * (within {@link #ASSIGN_REACH} blocks of the anvil). One piece every {@link ApprenticeRules#ticksPerPiece} ticks, rated
 * a bit worse than his master would, sometimes botched (the materials are lost). Every piece teaches him; his level
 * never reaches his master's. Without his daily wage he stops.
 * <p>
 * His master deals with him by right-clicking: empty-handed, opens his window (status, wage, containers, dismiss); with
 * emeralds, pays his wage; with an item, orders it (any forging recipe without a class). What the window shows is synced
 * to the client through the entity data.
 */
public class ApprenticeSmith extends PathfinderMob {
    static final int STATION_REACH = 4;
    /** How far from his anvil an assigned container may be. */
    public static final int ASSIGN_REACH = 8;
    /** How close his master must stand to use his window. */
    public static final double WINDOW_REACH = QuestService.GIVER_REACH;
    private static final int WORK_STEP = 20;
    private static final List<String> NAMES = List.of("Bran", "Tomas", "Gerd", "Ilse", "Marek", "Rolf", "Hilda", "Otto", "Lena", "Ivo");

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(ApprenticeSmith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATUS = SynchedEntityData.defineId(ApprenticeSmith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ORDER = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_DAYS_PAID = SynchedEntityData.defineId(ApprenticeSmith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_MATERIALS = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_OUTPUT = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_GRADE = SynchedEntityData.defineId(ApprenticeSmith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CRAFTSMAN = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.BOOLEAN);
    /** How far the piece on the anvil is, in percent. */
    private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(ApprenticeSmith.class, EntityDataSerializers.INT);
    /** What he lacks to start the next piece (with the count missing), or empty. */
    private static final EntityDataAccessor<ItemStack> DATA_MISSING = SynchedEntityData.defineId(ApprenticeSmith.class,
            EntityDataSerializers.ITEM_STACK);

    /** Why he isn't working, shown in his window. */
    public enum Status { WORKING, NO_ORDER, NO_ANVIL, UNPAID, NO_MATERIALS, BARREL_FULL, NO_OUTPUT;

        /** Stopped on his own, with an order: what the "!" over his head and the chat warning are for. */
        public boolean stopped() {
            return this != WORKING && this != NO_ORDER;
        }
    }

    /** The containers his master can assign him. */
    public enum Slot { MATERIALS, OUTPUT }

    private String baseName = "";
    private int smithingXp = ApprenticeRules.STARTING_XP;
    private int masterLevel = ApprenticeRules.FIRST_APPRENTICE_LEVEL;
    private Item order;
    private BlockPos anvil;
    private long paidUntil;
    private int workTicks;
    /** The materials of the piece on the anvil, taken when he started it; empty between pieces. */
    private final List<ItemStack> onAnvil = new ArrayList<>();

    public ApprenticeSmith(EntityType<? extends ApprenticeSmith> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_LEVEL, 0);
        builder.define(DATA_STATUS, Status.NO_ORDER.ordinal());
        builder.define(DATA_ORDER, ItemStack.EMPTY);
        builder.define(DATA_DAYS_PAID, 0);
        builder.define(DATA_MATERIALS, Optional.empty());
        builder.define(DATA_OUTPUT, Optional.empty());
        builder.define(DATA_GRADE, NpcGrade.NORMAL.ordinal());
        builder.define(DATA_CRAFTSMAN, false);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_MISSING, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /**
     * A new apprentice for {@code master}, with a name from the village and a growth grade rolled now. A named one (1 %)
     * shows it at once: his name is gold, and the system tells his master.
     */
    public void hireBy(ServerPlayer master) {
        entityData.set(DATA_OWNER, Optional.of(master.getUUID()));
        masterLevel = ProgressionService.effectiveSmithingLevel(master);
        paidUntil = level().getGameTime() + ApprenticeRules.DAY_TICKS; // the first day is on Smith
        baseName = NAMES.get(getRandom().nextInt(NAMES.size()));
        setCustomNameVisible(true);
        setGrade(NpcGrade.roll(getRandom().nextDouble()));
        findAnvil();
        sync();
        Workshop.report(master, this);
        if (grade() == NpcGrade.NAMED) {
            NotificationEvents.send(master, SystemNotification.Kind.APPRENTICE,
                    List.of(Component.translatable("notification.overgearedmod.apprentice.named", getDisplayName())));
        }
    }

    /** Works on both sides: the owner is synced. */
    public boolean isOwnedBy(Player player) {
        return entityData.get(DATA_OWNER).filter(player.getUUID()::equals).isPresent();
    }

    /** Synced (the window will show it by Insight later). */
    public NpcGrade grade() {
        return NpcGrade.values()[entityData.get(DATA_GRADE)];
    }

    /** Set at hiring; public for tests and debugging. */
    public void setGrade(NpcGrade grade) {
        entityData.set(DATA_GRADE, grade.ordinal());
        refreshName();
        sync();
    }

    /** His name without colour or title. */
    public String baseName() {
        return baseName;
    }

    /** Synced: whether he has been recognised as a Craftsman. */
    public boolean isCraftsman() {
        return entityData.get(DATA_CRAFTSMAN);
    }

    /** His name: gold if named, followed by "Craftsman" once he is one. */
    private void refreshName() {
        MutableComponent name = Component.literal(baseName);
        if (grade() == NpcGrade.NAMED) {
            name.withStyle(ChatFormatting.GOLD);
        }
        if (isCraftsman()) {
            name.append(Component.translatable("entity.overgearedmod.apprentice_smith.craftsman_suffix").withStyle(ChatFormatting.YELLOW));
        }
        setCustomName(name);
    }

    /** Server side: his working level, from his experience, below his master's and within his grade's cap. */
    public int smithingLevel() {
        return ApprenticeRules.workingLevel(ProgressionRules.smithingLevel(smithingXp), masterLevel, grade());
    }

    /** Server side: how far he has mastered the trade. */
    public MasteryStage stage() {
        return MasteryStage.of(smithingLevel());
    }

    public int smithingXp() {
        return smithingXp;
    }

    /** Public for tests and debugging. */
    public void setSmithingXp(int xp) {
        smithingXp = Math.max(0, xp);
        sync();
    }

    /** Server side: whole days of wage still paid ahead. */
    public int daysPaid() {
        return (int) Math.max(0, (paidUntil - level().getGameTime() + ApprenticeRules.DAY_TICKS - 1) / ApprenticeRules.DAY_TICKS);
    }

    // What his window shows; synced, so these work on the client too.

    public int shownLevel() {
        return entityData.get(DATA_LEVEL);
    }

    public MasteryStage shownStage() {
        return MasteryStage.of(shownLevel());
    }

    public Status status() {
        return Status.values()[entityData.get(DATA_STATUS)];
    }

    public ItemStack shownOrder() {
        return entityData.get(DATA_ORDER);
    }

    public int shownDaysPaid() {
        return entityData.get(DATA_DAYS_PAID);
    }

    /** How far the piece on the anvil is, 0-100 (0 with none). */
    public int shownProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    /** What he lacks to start the next piece, with the count missing (empty when nothing). */
    public ItemStack shownMissing() {
        return entityData.get(DATA_MISSING);
    }

    /**
     * His status in words, as his window and the chat warning show it: what he lacks when it is materials, and how far
     * the piece is while he works.
     */
    public Component statusText() {
        ItemStack missing = shownMissing();
        return Workshop.statusText(status(), shownProgress(), missing.getCount(), missing.isEmpty() ? null : missing.getItem().getDescription());
    }

    /** The container assigned to {@code slot}, if any; without one he uses those touching the anvil. */
    public Optional<BlockPos> assigned(Slot slot) {
        return entityData.get(accessor(slot));
    }

    private static EntityDataAccessor<Optional<BlockPos>> accessor(Slot slot) {
        return slot == Slot.MATERIALS ? DATA_MATERIALS : DATA_OUTPUT;
    }

    public Optional<Item> order() {
        return Optional.ofNullable(order);
    }

    /** Pushes what his window shows to the clients watching him. */
    private void sync() {
        entityData.set(DATA_LEVEL, smithingLevel());
        entityData.set(DATA_ORDER, order == null ? ItemStack.EMPTY : new ItemStack(order));
        entityData.set(DATA_DAYS_PAID, daysPaid());
        // Same steps for his window and the Workshop tab: the workshop's snapshot copies this value as it is.
        entityData.set(DATA_PROGRESS, onAnvil.isEmpty() ? 0 : WorkshopEntry.quantize(workTicks * 100 / ApprenticeRules.ticksPerPiece(smithingLevel())));
    }

    private void setStatus(Status status) {
        entityData.set(DATA_STATUS, status.ordinal());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            if (isOwnedBy(player) && held.isEmpty()) {
                // Client only: the screen class is never loaded on a dedicated server.
                ApprenticeScreen.open(this);
            }
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer master)) {
            return InteractionResult.CONSUME;
        }
        if (!isOwnedBy(master)) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_not_yours", getDisplayName()), true);
        } else if (held.is(Items.EMERALD)) {
            pay(master, held);
        } else if (!held.isEmpty()) {
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
        addPaidDays(master, days);
    }

    /** From his window: one day of wage, taken from anywhere in the inventory. */
    public boolean payDay(ServerPlayer master) {
        if (ApprenticeRules.daysBought(ApprenticeRules.DAILY_WAGE, daysPaid()) == 0) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_paid_enough", getDisplayName()), true);
            return false;
        }
        if (master.getInventory().countItem(Items.EMERALD) < ApprenticeRules.DAILY_WAGE) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_no_emeralds", ApprenticeRules.DAILY_WAGE), true);
            return false;
        }
        master.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.EMERALD), ApprenticeRules.DAILY_WAGE,
                master.inventoryMenu.getCraftSlots());
        addPaidDays(master, 1);
        return true;
    }

    private void addPaidDays(ServerPlayer master, int days) {
        paidUntil = Math.max(paidUntil, level().getGameTime()) + days * ApprenticeRules.DAY_TICKS;
        sync();
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_paid", getDisplayName(), days, daysPaid()), true);
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
    }

    /** Orders {@code item}, if a forging recipe without a class makes it and he has mastered its material. */
    public boolean takeOrder(ServerPlayer master, Item item) {
        Optional<ForgingRecipe> recipe = recipeFor(item);
        if (recipe.isEmpty()) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_cant_make", getDisplayName(),
                    item.getDescription()), true);
            return false;
        }
        MasteryStage needed = ApprenticeMaterials.requiredStage(recipe.get().material());
        if (!stage().atLeast(needed)) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_stage_too_low", getDisplayName(),
                    Component.translatable("npc.overgearedmod.stage." + needed.key())), true);
            return false;
        }
        if (order != item) {
            returnMaterials(master);
        }
        order = item;
        findAnvil();
        sync();
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_order", getDisplayName(), item.getDescription()), true);
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }

    public void cancelOrder(ServerPlayer master) {
        if (order == null) {
            return;
        }
        returnMaterials(master);
        order = null;
        entityData.set(DATA_MISSING, ItemStack.EMPTY);
        setStatus(Status.NO_ORDER);
        sync();
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_order_cancelled", getDisplayName()), true);
    }

    /**
     * Assigns the container at {@code pos} to {@code slot}: it has to hold items and stand within {@link #ASSIGN_REACH}
     * blocks of his anvil (of himself while he has none).
     */
    public boolean assign(ServerPlayer master, Slot slot, BlockPos pos) {
        if (HopperBlockEntity.getContainerAt(level(), pos) == null) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_assign_not_container"), true);
            return false;
        }
        BlockPos workplace = anvil != null ? anvil : blockPosition();
        if (pos.distSqr(workplace) > ASSIGN_REACH * ASSIGN_REACH) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_assign_far", getDisplayName(), ASSIGN_REACH), true);
            return false;
        }
        entityData.set(accessor(slot), Optional.of(pos.immutable()));
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_assigned_" + slot.name().toLowerCase(),
                getDisplayName(), level().getBlockState(pos).getBlock().getName()), true);
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }

    /** Back to the containers touching the anvil. */
    public void clearAssignments(ServerPlayer master) {
        entityData.set(DATA_MATERIALS, Optional.empty());
        entityData.set(DATA_OUTPUT, Optional.empty());
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_assign_cleared", getDisplayName()), true);
    }

    /** From his window: he takes the best smithing hammer his master carries (handing back the one he had). */
    public boolean giveHammer(ServerPlayer master) {
        int slot = SmithingHammers.bestSlot(master.registryAccess(), master.getInventory());
        if (slot < 0) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_no_hammer"), true);
            return false;
        }
        ItemStack hammer = master.getInventory().removeItemNoUpdate(slot);
        returnHammer(master);
        setItemSlot(EquipmentSlot.MAINHAND, hammer);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_hammer_given", getDisplayName(), hammer.getHoverName()), true);
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0f, 1.0f);
        return true;
    }

    /** From his window: his hammer goes back to his master. */
    public void takeHammer(ServerPlayer master) {
        if (returnHammer(master)) {
            master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_hammer_taken", getDisplayName()), true);
        }
    }

    private boolean returnHammer(ServerPlayer master) {
        ItemStack hammer = getMainHandItem();
        if (hammer.isEmpty()) {
            return false;
        }
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (!master.getInventory().add(hammer)) {
            master.drop(hammer, false);
        }
        return true;
    }

    public void dismiss(ServerPlayer master) {
        returnHammer(master);
        returnMaterials(master);
        List<UUID> hired = new ArrayList<>(master.getData(ModAttachments.APPRENTICES));
        hired.remove(getUUID());
        master.setData(ModAttachments.APPRENTICES, List.copyOf(hired));
        Workshop.forget(master, getUUID());
        master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_dismissed", getDisplayName()), false);
        discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % WORK_STEP != 0 || !(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer master = master();
        if (master != null) {
            masterLevel = ProgressionService.effectiveSmithingLevel(master);
        }
        Status status = readyStatus();
        if (status == Status.WORKING) {
            status = work(level);
        }
        setStatus(status);
        sync();
        if (master != null) {
            Workshop.report(master, this);
        }
    }

    /**
     * One work step: with no piece on the anvil he starts one if he can (otherwise he waits, silent, and tries again next
     * step); with one, he hammers on and finishes it when its time is up.
     */
    private Status work(ServerLevel level) {
        if (onAnvil.isEmpty()) {
            Status started = startPiece();
            if (started != Status.WORKING) {
                return started;
            }
        }
        int needed = ApprenticeRules.ticksPerPiece(smithingLevel());
        if (workTicks < needed) {
            workTicks += WORK_STEP;
            if (workTicks % 100 == 0) {
                swing(InteractionHand.MAIN_HAND);
                level.playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.2f);
            }
        }
        return workTicks >= needed ? finishPiece(getRandom().nextDouble()) : Status.WORKING;
    }

    /** His master, if online in this world. */
    private ServerPlayer master() {
        return level() instanceof ServerLevel level
                && entityData.get(DATA_OWNER).map(level::getPlayerByUUID).orElse(null) instanceof ServerPlayer online ? online : null;
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
     * A whole piece of his order at once (for tests): starts one if none is on the anvil and finishes it, with
     * {@code roll01} deciding whether he botches it. Returns what happened.
     */
    public Status forgeOne(double roll01) {
        Status started = onAnvil.isEmpty() ? startPiece() : Status.WORKING;
        return started != Status.WORKING ? started : finishPiece(roll01);
    }

    /**
     * Starts a piece: it needs somewhere to go and all its materials, which leave their containers now. Otherwise he
     * says what stops him (and, for materials, which item he lacks). Public for tests.
     */
    public Status startPiece() {
        Optional<ForgingRecipe> found = order().flatMap(this::recipeFor);
        if (found.isEmpty() || anvil == null) {
            return Status.NO_ORDER;
        }
        ForgingRecipe recipe = found.get();
        Status room = outputStatus();
        if (room != Status.WORKING) {
            return room;
        }
        List<Container> sources = sources();
        ItemStack missing = missing(sources, recipe.material(), recipe.materialCount());
        if (missing.isEmpty() && !recipe.handle().isEmpty()) {
            missing = missing(sources, recipe.handle(), 1);
        }
        entityData.set(DATA_MISSING, missing);
        if (!missing.isEmpty()) {
            return Status.NO_MATERIALS;
        }
        onAnvil.addAll(take(sources, recipe.material(), recipe.materialCount()));
        if (!recipe.handle().isEmpty()) {
            onAnvil.addAll(take(sources, recipe.handle(), 1));
        }
        workTicks = 0;
        return Status.WORKING;
    }

    /**
     * Finishes the piece on the anvil: either it is botched ({@code roll01} under his fail chance; the materials are
     * lost and his master hears of it) or it lands in the output container, rated by his level. Either way he learns.
     * With the output full or gone, the piece waits on the anvil.
     */
    private Status finishPiece(double roll01) {
        Optional<ForgingRecipe> found = order().flatMap(this::recipeFor);
        if (found.isEmpty() || anvil == null) {
            return Status.NO_ORDER;
        }
        ForgingRecipe recipe = found.get();
        Status room = outputStatus();
        if (room != Status.WORKING) {
            return room;
        }
        Container output = output().orElseThrow();
        onAnvil.clear();
        workTicks = 0;
        ItemStack hammer = getMainHandItem();
        double hammerSkill = SmithingHammers.skillBonus(level().registryAccess(), hammer);
        int xp = HammerRules.smithingXp(recipe.smithingXp(), SmithingHammers.xpBonus(level().registryAccess(), hammer));
        smithingXp += ApprenticeRules.xpGained(xp, grade());
        SmithingHammers.wear(this, EquipmentSlot.MAINHAND);
        sync();
        if (roll01 < ApprenticeRules.failChance(smithingLevel())) {
            level().playSound(null, anvil, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.6f, 1.0f);
            ServerPlayer master = master();
            if (master != null) {
                master.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_botched", getDisplayName(),
                        recipe.result().getHoverName()), true);
            }
            return Status.WORKING;
        }
        ItemStack piece = recipe.result().copy();
        double penalty = isCraftsman() ? ApprenticeRules.CRAFTSMAN_RATING_PENALTY : ApprenticeRules.RATING_PENALTY;
        EquipmentService.stampForged(level(), piece, recipe.requirements(), recipe.extras(), 0, smithingLevel(), penalty + hammerSkill, 0,
                getRandom());
        put(output, piece);
        level().playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        ItemRating rating = piece.get(ModDataComponents.ITEM_RATING);
        if (rating != null) {
            judgePiece(rating, true);
        }
        return Status.WORKING;
    }

    /** WORKING if a piece has somewhere to go; otherwise whether the output is missing or full. */
    private Status outputStatus() {
        Optional<Container> output = output();
        if (output.isEmpty()) {
            return Status.NO_OUTPUT;
        }
        return hasRoom(output.get()) ? Status.WORKING : Status.BARREL_FULL;
    }

    private List<Container> sources() {
        return assignedContainer(Slot.MATERIALS).map(List::of).orElseGet(() -> touching(ChestBlockEntity.class));
    }

    private Optional<Container> output() {
        return assignedContainer(Slot.OUTPUT).or(() -> touching(BarrelBlockEntity.class).stream().findFirst());
    }

    /**
     * Gives the materials of the unfinished piece back to {@code master} (an order cancelled or changed, a dismissal), so
     * they aren't lost.
     */
    private void returnMaterials(ServerPlayer master) {
        for (ItemStack stack : onAnvil) {
            if (!master.getInventory().add(stack)) {
                master.drop(stack, false);
            }
        }
        onAnvil.clear();
        workTicks = 0;
    }

    /**
     * A piece he forged ({@code ownPiece}) or saw his master forge: if he is a Master and it is extraordinary enough
     * (see {@link ApprenticeRules#earnsCraftsman}), he becomes a Craftsman. Returns whether he did.
     */
    public boolean judgePiece(ItemRating rating, boolean ownPiece) {
        if (isCraftsman() || !ApprenticeRules.earnsCraftsman(stage(), rating, ownPiece)) {
            return false;
        }
        entityData.set(DATA_CRAFTSMAN, true);
        refreshName();
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 0.8f);
        if (level() instanceof ServerLevel level && entityData.get(DATA_OWNER).map(level::getPlayerByUUID).orElse(null) instanceof ServerPlayer master) {
            NotificationEvents.send(master, SystemNotification.Kind.APPRENTICE, List.of(Component.translatable(
                    ownPiece ? "notification.overgearedmod.apprentice.craftsman" : "notification.overgearedmod.apprentice.illuminated", baseName)));
        }
        return true;
    }

    /** The assigned container, while it is still there (a broken one is forgotten). */
    private Optional<Container> assignedContainer(Slot slot) {
        Optional<BlockPos> pos = assigned(slot);
        if (pos.isEmpty()) {
            return Optional.empty();
        }
        Container container = HopperBlockEntity.getContainerAt(level(), pos.get());
        if (container == null) {
            entityData.set(accessor(slot), Optional.empty());
        }
        return Optional.ofNullable(container);
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

    /** What is short of {@code count} of {@code ingredient} in the containers (one of its items, with the count missing), or empty. */
    private static ItemStack missing(List<Container> containers, Ingredient ingredient, int count) {
        int found = 0;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (ingredient.test(stack)) {
                    found += stack.getCount();
                }
            }
        }
        if (found >= count) {
            return ItemStack.EMPTY;
        }
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? new ItemStack(Items.BARRIER, count - found) : options[0].copyWithCount(count - found);
    }

    /** Takes {@code count} of {@code ingredient} from the containers; returns what was taken. */
    private static List<ItemStack> take(List<Container> containers, Ingredient ingredient, int count) {
        List<ItemStack> taken = new ArrayList<>();
        int missing = count;
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize() && missing > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (ingredient.test(stack)) {
                    ItemStack removed = container.removeItem(slot, Math.min(missing, stack.getCount()));
                    missing -= removed.getCount();
                    taken.add(removed);
                }
            }
            container.setChanged();
        }
        return taken;
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
        entityData.get(DATA_OWNER).ifPresent(owner -> tag.putUUID("Owner", owner));
        tag.putString("BaseName", baseName);
        tag.putString("Grade", grade().name());
        tag.putBoolean("Craftsman", isCraftsman());
        tag.putInt("SmithingXp", smithingXp);
        tag.putInt("MasterLevel", masterLevel);
        if (order != null) {
            tag.putString("Order", BuiltInRegistries.ITEM.getKey(order).toString());
        }
        if (anvil != null) {
            tag.put("Anvil", NbtUtils.writeBlockPos(anvil));
        }
        assigned(Slot.MATERIALS).ifPresent(pos -> tag.put("Materials", NbtUtils.writeBlockPos(pos)));
        assigned(Slot.OUTPUT).ifPresent(pos -> tag.put("Output", NbtUtils.writeBlockPos(pos)));
        tag.putLong("PaidUntil", paidUntil);
        tag.putInt("WorkTicks", workTicks);
        ListTag pieceMaterials = new ListTag();
        onAnvil.forEach(stack -> pieceMaterials.add(stack.save(registryAccess())));
        tag.put("OnAnvil", pieceMaterials);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        // Apprentices hired before grades existed: the name they had, normal grade.
        baseName = tag.contains("BaseName") ? tag.getString("BaseName") : getCustomName() == null ? "" : getCustomName().getString();
        entityData.set(DATA_GRADE, tag.contains("Grade") ? gradeNamed(tag.getString("Grade")).ordinal() : NpcGrade.NORMAL.ordinal());
        entityData.set(DATA_CRAFTSMAN, tag.getBoolean("Craftsman"));
        refreshName();
        smithingXp = tag.getInt("SmithingXp");
        masterLevel = tag.getInt("MasterLevel");
        order = tag.contains("Order") ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(tag.getString("Order"))).orElse(null) : null;
        anvil = NbtUtils.readBlockPos(tag, "Anvil").orElse(null);
        entityData.set(DATA_MATERIALS, NbtUtils.readBlockPos(tag, "Materials"));
        entityData.set(DATA_OUTPUT, NbtUtils.readBlockPos(tag, "Output"));
        paidUntil = tag.getLong("PaidUntil");
        workTicks = tag.getInt("WorkTicks");
        onAnvil.clear();
        tag.getList("OnAnvil", Tag.TAG_COMPOUND).forEach(saved -> ItemStack.parse(registryAccess(), saved).ifPresent(onAnvil::add));
        if (!level().isClientSide) {
            sync();
        }
    }

    private static NpcGrade gradeNamed(String name) {
        for (NpcGrade grade : NpcGrade.values()) {
            if (grade.name().equals(name)) {
                return grade;
            }
        }
        return NpcGrade.NORMAL;
    }
}
