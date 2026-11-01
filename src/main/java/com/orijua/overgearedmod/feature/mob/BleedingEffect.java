package com.orijua.overgearedmod.feature.mob;

import com.orijua.overgearedmod.core.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Bleeding: 1 damage per second per level (amplifier + 1). Its damage type ignores armor. */
public class BleedingEffect extends MobEffect {
    public static final int INTERVAL_TICKS = 20;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x9A1010);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        DamageSource source = new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModEffects.BLEEDING_DAMAGE));
        entity.hurt(source, amplifier + 1);
        return true;
    }
}
