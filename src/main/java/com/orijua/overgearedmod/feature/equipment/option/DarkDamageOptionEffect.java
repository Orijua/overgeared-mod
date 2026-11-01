package com.orijua.overgearedmod.feature.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * More damage while the attacker stands in the dark: light level {@code max_light} or less, counting the sky at
 * night. Canon: blue orichalcum is stronger in darkness. The rolled value is the bonus (0.2 = +20%).
 */
public record DarkDamageOptionEffect(int maxLight) implements OptionEffect {
    public static final MapCodec<DarkDamageOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 15).optionalFieldOf("max_light", 7).forGetter(DarkDamageOptionEffect::maxLight)
    ).apply(instance, DarkDamageOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.DARK_DAMAGE;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("+%.0f%%", value * 100));
    }

    @Override
    public float outgoingDamage(ServerPlayer attacker, LivingEntity target, float amount, float value) {
        boolean dark = attacker.level().getMaxLocalRawBrightness(attacker.blockPosition()) <= maxLight;
        return dark ? amount * (1 + value) : amount;
    }
}
