package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An option that forged items can roll, from data/<namespace>/overgearedmod/item_option/<id>.json.
 * {@code applies_to} (an item tag) limits which items can roll it; without it, any item can. A {@code weight} of 0
 * keeps it out of random rolls: only a recipe's {@code fixed_options} can give it (Fracaso's canon effects).
 */
public record ItemOptionDefinition(Component name, int weight, float min, float max, Optional<TagKey<Item>> appliesTo, OptionEffect effect) {

    public static final Codec<ItemOptionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(ItemOptionDefinition::name),
            Codec.intRange(0, 1000).optionalFieldOf("weight", 10).forGetter(ItemOptionDefinition::weight),
            Codec.FLOAT.fieldOf("min").forGetter(ItemOptionDefinition::min),
            Codec.FLOAT.fieldOf("max").forGetter(ItemOptionDefinition::max),
            TagKey.codec(Registries.ITEM).optionalFieldOf("applies_to").forGetter(ItemOptionDefinition::appliesTo),
            OptionEffect.CODEC.fieldOf("effect").forGetter(ItemOptionDefinition::effect)
    ).apply(instance, ItemOptionDefinition::new));

    public boolean canRollOn(ItemStack stack) {
        return appliesTo.map(stack::is).orElse(true);
    }
}
