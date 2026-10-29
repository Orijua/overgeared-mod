package com.orijua.overgearedmod.skill.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/** Applies vanilla potion effects to the caster, e.g. {"id": "minecraft:resistance", "amplifier": 1, "duration": 200}. */
public record SelfBuffEffect(List<MobEffectInstance> effects) implements SkillEffect {

    public static final MapCodec<SelfBuffEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(SelfBuffEffect::effects)
    ).apply(instance, SelfBuffEffect::new));

    @Override
    public SkillEffectType type() {
        return SkillEffectType.SELF_BUFF;
    }

    @Override
    public void apply(ServerPlayer player, PlayerStats stats) {
        effects.forEach(effect -> player.addEffect(new MobEffectInstance(effect)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
