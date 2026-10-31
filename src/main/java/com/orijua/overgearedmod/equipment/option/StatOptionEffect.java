package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.StatModifier;
import com.orijua.overgearedmod.progression.StatSource;
import com.orijua.overgearedmod.progression.StatType;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Adds points to one of the player's stats while worn, e.g. {"type": "stat", "stat": "agility"}. The value is the points. */
public record StatOptionEffect(StatType stat) implements OptionEffect {
    public static final MapCodec<StatOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StatType.CODEC.fieldOf("stat").forGetter(StatOptionEffect::stat)
    ).apply(instance, StatOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.STAT;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("+%.0f", value));
    }

    @Override
    public void statModifiers(StatSource source, float value, Consumer<StatModifier> add) {
        add.accept(StatModifier.add(source, stat, value));
    }
}
