package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;

/** On a smithing hammer: skill points added to the rating roll of each piece forged with it. The value is those points. */
public record ForgingSkillOptionEffect() implements OptionEffect {
    public static final MapCodec<ForgingSkillOptionEffect> CODEC = MapCodec.unit(new ForgingSkillOptionEffect());

    @Override
    public OptionEffectType type() {
        return OptionEffectType.FORGING_SKILL;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("+%.1f", value));
    }

    @Override
    public double forgingSkill(float value) {
        return value;
    }
}
