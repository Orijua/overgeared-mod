package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.mob.MobLevelService;
import com.orijua.overgearedmod.progression.ProgressionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

/**
 * Monsters far below the wearer's level are struck with fear: slowed and weakened while they stay within
 * {@code radius} blocks. Canon: Fracaso makes much weaker enemies afraid. The rolled value is how many levels
 * below the wearer a monster must be (an unleveled monster counts as level 1).
 */
public record FearOptionEffect(double radius, int duration) implements OptionEffect {
    public static final MapCodec<FearOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(1, 32).optionalFieldOf("radius", 8.0).forGetter(FearOptionEffect::radius),
            Codec.intRange(20, 600).optionalFieldOf("duration", 60).forGetter(FearOptionEffect::duration)
    ).apply(instance, FearOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.FEAR;
    }

    @Override
    public Component formatValue(float value) {
        return Component.translatable("option.overgearedmod.level_gap", Math.round(value));
    }

    @Override
    public void tickWorn(ServerPlayer wearer, float value) {
        int wearerLevel = ProgressionService.effectiveStats(wearer).level();
        for (Monster monster : wearer.level().getEntitiesOfClass(Monster.class, wearer.getBoundingBox().inflate(radius))) {
            int monsterLevel = Math.max(1, MobLevelService.get(monster).level());
            if (wearerLevel - monsterLevel >= value) {
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1), wearer);
                monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0), wearer);
            }
        }
    }
}
