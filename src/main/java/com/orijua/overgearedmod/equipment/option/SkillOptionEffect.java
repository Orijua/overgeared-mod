package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Lets the wearer cast a skill with the weapon-skill key while holding the item, without learning it,
 * e.g. {"type": "skill", "skill": "overgearedmod:bisection"}. The value is unused.
 */
public record SkillOptionEffect(ResourceLocation skill) implements OptionEffect {
    public static final MapCodec<SkillOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("skill").forGetter(SkillOptionEffect::skill)
    ).apply(instance, SkillOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.SKILL;
    }

    @Override
    public Component formatValue(float value) {
        return Component.empty();
    }

    @Override
    public Optional<ResourceLocation> grantedSkill() {
        return Optional.of(skill);
    }
}
