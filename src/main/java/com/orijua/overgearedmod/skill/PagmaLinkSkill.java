package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Inspired by canon: "Pagma's Swordsmanship" is the sword style Grid inherits; "Link" is one of its
 * techniques (verify the exact name/chapter in the novel before calling it canon).
 * The effect below (frontal sweep scaling with STR/DEX) is project design.
 */
final class PagmaLinkSkill implements Skill {
    private static final double RANGE = 4.5;
    /** cos of the half-angle of the cone in front of the player (~70 degrees). */
    private static final double CONE_COS = 0.35;

    @Override
    public String id() {
        return "pagma_link";
    }

    @Override
    public Component displayName() {
        return Component.translatable("skill.overgearedmod.pagma_link");
    }

    @Override
    public float manaCost() {
        return 10.0f;
    }

    @Override
    public int cooldownTicks() {
        return 100; // 5 seconds
    }

    @Override
    public boolean canUseWith(ItemStack mainHand) {
        return mainHand.getItem() instanceof SwordItem;
    }

    @Override
    public void execute(ServerPlayer player, PlayerStats stats) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Includes the weapon's damage, its rating bonus and the STR modifier.
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (1.0 + 0.03 * stats.strength() + 0.02 * stats.dexterity());

        AABB area = new AABB(eye, eye).inflate(RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target ->
                target != player && target.isAlive() && !target.isAlliedTo(player) && isInCone(eye, look, target));

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), (float) damage);
            target.knockback(0.5, -look.x, -look.z);
        }

        for (int i = 1; i <= 3; i++) {
            Vec3 p = eye.add(look.scale(i * 1.2));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y - 0.3, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private static boolean isInCone(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
        double distance = toTarget.length();
        return distance <= RANGE && (distance < 1.0 || toTarget.normalize().dot(look) >= CONE_COS);
    }
}
