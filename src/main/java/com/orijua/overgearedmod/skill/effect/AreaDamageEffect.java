package com.orijua.overgearedmod.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hits every enemy in a cone in front of the player, scaling the weapon's damage with STR/DEX.
 * Optional {@code target_effects} are given to each enemy hit, e.g. {"id": "overgearedmod:bleeding", "duration": 100}.
 */
public record AreaDamageEffect(double range, double coneDegrees, double damageMultiplier,
                               double strengthScaling, double dexterityScaling,
                               List<MobEffectInstance> targetEffects) implements SkillEffect {

    public static final MapCodec<AreaDamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(0.5, 16).fieldOf("range").forGetter(AreaDamageEffect::range),
            Codec.doubleRange(1, 360).optionalFieldOf("cone_degrees", 140.0).forGetter(AreaDamageEffect::coneDegrees),
            Codec.doubleRange(0, 100).optionalFieldOf("damage_multiplier", 1.0).forGetter(AreaDamageEffect::damageMultiplier),
            Codec.DOUBLE.optionalFieldOf("strength_scaling", 0.0).forGetter(AreaDamageEffect::strengthScaling),
            Codec.DOUBLE.optionalFieldOf("dexterity_scaling", 0.0).forGetter(AreaDamageEffect::dexterityScaling),
            MobEffectInstance.CODEC.listOf().optionalFieldOf("target_effects", List.of()).forGetter(AreaDamageEffect::targetEffects)
    ).apply(instance, AreaDamageEffect::new));

    @Override
    public SkillEffectType type() {
        return SkillEffectType.AREA_DAMAGE;
    }

    @Override
    public void apply(ServerPlayer player, PlayerStats stats) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(Math.toRadians(coneDegrees / 2));
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier
                * (1.0 + strengthScaling * stats.strength() + dexterityScaling * stats.dexterity()));

        level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range),
                        target -> target != player && target.isAlive() && !target.isAlliedTo(player)
                                && inCone(eye, look, target, minDot))
                .forEach(target -> {
                    target.hurt(player.damageSources().playerAttack(player), damage);
                    target.knockback(0.5, -look.x, -look.z);
                    targetEffects.forEach(effect -> target.addEffect(new MobEffectInstance(effect), player));
                });

        for (int i = 1; i <= 3; i++) {
            Vec3 p = eye.add(look.scale(i * range / 4));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y - 0.3, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private boolean inCone(Vec3 eye, Vec3 look, LivingEntity target, double minDot) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
        double distance = toTarget.length();
        return distance <= range && (distance < 1.0 || toTarget.normalize().dot(look) >= minDot);
    }
}
