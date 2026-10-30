package com.orijua.overgearedmod.boss;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.mob.BossMob;
import com.orijua.overgearedmod.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The Bone Lord, the first boss: a big sword-wielding skeleton with a boss bar and two phases.
 * Phase 1 is a plain (but tough) melee fight. Below half health it enrages once: faster, glowing,
 * its hits cause Bleeding, and it calls {@link #MINIONS} skeletons. Its loot table guarantees an
 * Epic-or-better piece. Uses the wither skeleton model until it gets its own texture.
 */
public class BoneLord extends AbstractSkeleton implements BossMob {
    public static final int MINIONS = 2;
    private static final ResourceLocation ENRAGED_SPEED = Overgearedmod.id("bone_lord_enraged");

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean phaseTwo;

    public BoneLord(EntityType<? extends BoneLord> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                // The wither skeleton renderer already draws it 1.2x: 1.25 on top makes it 1.5x a skeleton.
                .add(Attributes.SCALE, 1.25);
    }

    /** Like a skeleton's goals, minus fleeing the sun and wolves: a boss stands its ground. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** A sword instead of the skeleton's bow; it never drops (the loot table gives the rated reward). */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !phaseTwo && isAlive() && getHealth() <= getMaxHealth() / 2) {
            enterPhaseTwo();
        }
        return hurt;
    }

    private void enterPhaseTwo() {
        phaseTwo = true;
        applyEnragedSpeed();
        setGlowingTag(true);
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.7f);
        for (ServerPlayer player : bossEvent.getPlayers()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.bone_lord_enraged", getDisplayName()), true);
        }
        summonMinions((ServerLevel) level());
    }

    private void applyEnragedSpeed() {
        getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
                new AttributeModifier(ENRAGED_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private void summonMinions(ServerLevel level) {
        for (int i = 0; i < MINIONS; i++) {
            Skeleton minion = EntityType.SKELETON.create(level);
            if (minion == null) {
                continue;
            }
            double side = i == 0 ? -2.0 : 2.0;
            minion.moveTo(getX() + side, getY(), getZ(), getYRot(), 0.0f);
            // Through the event hook so minions get a mob level like any other spawn.
            EventHooks.finalizeMobSpawn(minion, level, level.getCurrentDifficultyAt(BlockPos.containing(minion.position())),
                    MobSpawnType.MOB_SUMMONED, null);
            minion.setTarget(getTarget());
            level.addFreshEntity(minion);
        }
    }

    /** In phase two its hits also make the target bleed. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && phaseTwo && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 100, 1), this);
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", phaseTwo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phaseTwo = tag.getBoolean("PhaseTwo");
        if (phaseTwo) {
            applyEnragedSpeed();
            bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    /** A boss never despawns and doesn't burn in daylight. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WITHER_SKELETON_STEP;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(getStepSound(), 0.3f, 0.8f);
    }
}
