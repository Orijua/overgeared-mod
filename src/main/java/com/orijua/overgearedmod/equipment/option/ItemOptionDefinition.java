package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** An option that forged items can roll, from data/<namespace>/overgearedmod/item_option/<id>.json. */
public record ItemOptionDefinition(Component name, int weight, float min, float max, OptionEffect effect) {

    public static final Codec<ItemOptionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(ItemOptionDefinition::name),
            Codec.intRange(1, 1000).optionalFieldOf("weight", 10).forGetter(ItemOptionDefinition::weight),
            Codec.FLOAT.fieldOf("min").forGetter(ItemOptionDefinition::min),
            Codec.FLOAT.fieldOf("max").forGetter(ItemOptionDefinition::max),
            OptionEffect.CODEC.fieldOf("effect").forGetter(ItemOptionDefinition::effect)
    ).apply(instance, ItemOptionDefinition::new));
}
