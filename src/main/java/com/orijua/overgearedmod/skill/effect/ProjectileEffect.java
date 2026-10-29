package com.orijua.overgearedmod.skill.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/** Shoots small fireballs where the player looks, spread a little when {@code count} > 1. */
public record ProjectileEffect(int count, double speed) implements SkillEffect {

    public static final MapCodec<ProjectileEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 10).optionalFieldOf("count", 1).forGetter(ProjectileEffect::count),
            Codec.doubleRange(0.1, 5).optionalFieldOf("speed", 1.0).forGetter(ProjectileEffect::speed)
    ).apply(instance, ProjectileEffect::new));

    @Override
    public SkillEffectType type() {
        return SkillEffectType.PROJECTILE;
    }

    @Override
    public void apply(ServerPlayer player, PlayerStats stats) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look);
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) / 2.0) * 0.08;
            Vec3 direction = look.add(-look.z * spread, 0, look.x * spread).normalize().scale(speed);
            SmallFireball fireball = new SmallFireball(player.level(), player, direction);
            fireball.setPos(start);
            player.level().addFreshEntity(fireball);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
