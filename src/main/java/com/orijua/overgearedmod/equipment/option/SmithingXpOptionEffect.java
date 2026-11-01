package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;

/** On a smithing hammer: more smithing experience per piece. The value is the extra fraction (0.1 = +10 %). */
public record SmithingXpOptionEffect() implements OptionEffect {
    public static final MapCodec<SmithingXpOptionEffect> CODEC = MapCodec.unit(new SmithingXpOptionEffect());

    @Override
    public OptionEffectType type() {
        return OptionEffectType.SMITHING_XP;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("+%.0f%%", value * 100));
    }

    @Override
    public double smithingXp(float value) {
        return value;
    }
}
