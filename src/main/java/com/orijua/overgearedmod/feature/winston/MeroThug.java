package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.mob.BossMob;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import com.orijua.overgearedmod.feature.mob.MobLevelService;
import com.orijua.overgearedmod.feature.quest.QuestService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * One of the Mero Company's thugs who harass Khan in his smithy (canon, cap. 24: "the Mero Company's minions", level 35
 * or more). They come for one player, the one who accepted "The Blacksmith's Rage" ({@link #owner}): they only attack
 * that player and only that player can hurt them (or a creative player, or /kill). They stay around the smithy and
 * leave once that player's quest is over. The last of them to fall drops the smithy transfer contract.
 */
public class MeroThug extends PathfinderMob implements BossMob {
    public static final int CANON_LEVEL = 35;
    /** How far from the smithy they wander. */
    static final int HOME_RADIUS = 10;
    /** How far the rest of the gang is looked for when one falls. */
    static final double GANG_RANGE = 48.0;

    @Nullable
    private UUID owner;

    public MeroThug(EntityType<? extends MeroThug> type, Level level) {
        super(type, level);
        xpReward = 20;
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isOwner));
    }

    /** Sent for {@code player}, to stay around {@code smithy}. */
    public void sendFor(ServerPlayer player, BlockPos smithy) {
        owner = player.getUUID();
        restrictTo(smithy, HOME_RADIUS);
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    /** The player they came for, in survival or adventure. */
    boolean isOwner(@Nullable Entity entity) {
        return entity instanceof Player player && player.getUUID().equals(owner) && !player.isCreative() && !player.isSpectator();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!MobLevelService.get(this).isLeveled()) {
            setData(ModAttachments.MOB_LEVEL, new MobLevel(CANON_LEVEL, false));
        }
        if (tickCount % 20 == 0 && questOver()) {
            discard(); // their job is done, one way or another
        }
    }

    /** No player to come for, or that player is online and no longer on the quest (completed or failed). */
    private boolean questOver() {
        if (owner == null) {
            return true;
        }
        ServerPlayer player = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(owner);
        return player != null && !QuestService.get(player).isActive(WinstonStory.KHANS_RAGE);
    }

    /** Only the player they came for (or a creative player, or /kill) can hurt them. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return !(source.getEntity() instanceof Player player && (player.isCreative() || isOwner(player)));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof MeroThug || super.isAlliedTo(other);
    }

    /** The last of the gang to fall drops the transfer contract, which only the player they came for can pick up. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (owner != null && WinstonStory.gang(level, owner, getBoundingBox().getCenter(), GANG_RANGE).stream().noneMatch(other -> other != this)) {
            ItemEntity contract = new ItemEntity(level, getX(), getY() + 0.5, getZ(), new ItemStack(ModItems.TRANSFER_CONTRACT.get()));
            contract.setTarget(owner);
            contract.setUnlimitedLifetime();
            level.addFreshEntity(contract);
        }
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
        if (hasRestriction()) {
            tag.putLong("Home", getRestrictCenter().asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("Home")) {
            restrictTo(BlockPos.of(tag.getLong("Home")), HOME_RADIUS);
        }
    }
}
