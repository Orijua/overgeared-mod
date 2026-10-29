package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Heals the attacker by a fraction of the damage dealt. The rolled value is that fraction (0.05 = 5%). */
public record LifeStealOptionEffect() implements OptionEffect {
    public static final MapCodec<LifeStealOptionEffect> CODEC = MapCodec.unit(new LifeStealOptionEffect());

    @Override
    public OptionEffectType type() {
        return OptionEffectType.LIFE_STEAL;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("%.0f%%", value * 100));
    }

    @Override
    public void onHit(ServerPlayer attacker, LivingEntity target, float damageDealt, float value) {
        attacker.heal(damageDealt * value);
    }
}
