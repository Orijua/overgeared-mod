package com.orijua.overgearedmod.feature.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Chance on hit to give the target a status effect, e.g. {"type": "apply_effect", "effect": "overgearedmod:bleeding",
 * "duration": 100}. The rolled value is the chance (0.1 = 10%).
 */
public record ApplyEffectOptionEffect(Holder<MobEffect> effect, int duration, int amplifier) implements OptionEffect {
    public static final MapCodec<ApplyEffectOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(ApplyEffectOptionEffect::effect),
            Codec.intRange(1, 72_000).fieldOf("duration").forGetter(ApplyEffectOptionEffect::duration),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(ApplyEffectOptionEffect::amplifier)
    ).apply(instance, ApplyEffectOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.APPLY_EFFECT;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("%.0f%%", value * 100));
    }

    @Override
    public void onHit(ServerPlayer attacker, LivingEntity target, float damageDealt, float value) {
        if (target.isAlive() && attacker.getRandom().nextFloat() < value) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier), attacker);
        }
    }
}
