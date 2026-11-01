package com.orijua.overgearedmod.pagma;

import com.orijua.overgearedmod.client.NpcQuestScreen;
import com.orijua.overgearedmod.rumor.Rumors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Earl Ashur (canon, caps. 1-2): a noble of Patrian and a great magician on his third class change. He gives the secret
 * request for Pagma's book from his manor near Bairan; when the book is appraised he comes for it with his knights. As
 * an enemy he keeps his distance and casts lines of fangs from the ground, every few casts slowing his target, with a
 * boss bar for the players near. Killing him earns "Noble Killer" and ends his anger.
 */
public class EarlAshur extends AshurRetinue {
    public static final int CANON_LEVEL = 300;
    /** Ticks between two spells, and every how many spells the target is also slowed. */
    static final int CAST_INTERVAL = 50;
    private static final int SLOW_EVERY = 3;
    private static final double BAR_RANGE = 32.0;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    public EarlAshur(EntityType<? extends EarlAshur> type, Level level) {
        super(type, level);
        xpReward = 500;
        ItemStack robe = new ItemStack(Items.LEATHER_CHESTPLATE);
        robe.set(DataComponents.DYED_COLOR, new DyedItemColor(0x5B2C83, false));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, robe);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    @Override
    protected int canonLevel() {
        return CANON_LEVEL;
    }

    @Override
    protected void registerAttackGoal() {
        goalSelector.addGoal(2, new CastGoal());
    }

    /** In his manor, his quest window; to the thief he hunts, only his anger. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (PagmaStory.isAshursEnemy(player)) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.overgearedmod.ashur_thief"), true);
            }
        } else if (level().isClientSide) {
            // Only runs on the client, so the client-only class is never loaded on a server.
            NpcQuestScreen.open(this, false);
        } else if (player instanceof ServerPlayer serverPlayer) {
            AshurBetrayal.talk(serverPlayer);
            Rumors.hear(serverPlayer, getType());
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** A line of fangs from the ground towards the target, like an evoker's (canon: a great magician). */
    void castAt(LivingEntity target, boolean slow) {
        float angle = (float) Mth.atan2(target.getZ() - getZ(), target.getX() - getX());
        double y = Math.min(getY(), target.getY());
        for (int i = 0; i < 10; i++) {
            double distance = 1.25 * (i + 1);
            level().addFreshEntity(new EvokerFangs(level(), getX() + Mth.cos(angle) * distance, y, getZ() + Mth.sin(angle) * distance,
                    angle, i, this));
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5f, 0.8f);
        if (slow) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 == 0) {
            updateBarViewers();
            if (!isSummoned() && level() instanceof ServerLevel level
                    && !level.getPlayers(player -> distanceToSqr(player) < 32 * 32 && PagmaStory.killedAshur(player)).isEmpty()) {
                discard(); // the player near killed him elsewhere: he is dead
            }
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    /** The bar shows only while he hunts someone, to the players near. */
    private void updateBarViewers() {
        Set<ServerPlayer> near = new HashSet<>();
        if (getTarget() != null) {
            near.addAll(((ServerLevel) level()).getPlayers(player -> player.isAlive() && distanceToSqr(player) <= BAR_RANGE * BAR_RANGE));
        }
        for (ServerPlayer viewer : List.copyOf(bossEvent.getPlayers())) {
            if (!near.contains(viewer)) {
                bossEvent.removePlayer(viewer);
            }
        }
        near.forEach(bossEvent::addPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /** Keeps 6-12 blocks from the target and casts every {@link #CAST_INTERVAL} ticks while he sees it. */
    private class CastGoal extends Goal {
        private int cooldown = 20;
        private int casts;

        CastGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            double distance = distanceTo(target);
            if (distance > 12) {
                getNavigation().moveTo(target, 1.0);
            } else if (distance < 6) {
                getNavigation().stop();
            }
            if (--cooldown <= 0 && distance < 20 && hasLineOfSight(target)) {
                casts++;
                castAt(target, casts % SLOW_EVERY == 0);
                cooldown = CAST_INTERVAL;
            }
        }
    }
}
