package com.orijua.overgearedmod.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.skill.effect.SkillEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** A skill as written in data/<namespace>/overgearedmod/skill/<id>.json. */
public record SkillDefinition(
        Component name,
        Component description,
        float manaCost,
        int cooldownTicks,
        Optional<TagKey<Item>> requiredItem,
        SkillEffect effect) {

    public static final Codec<SkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillDefinition::description),
            Codec.floatRange(0, 1000).fieldOf("mana_cost").forGetter(SkillDefinition::manaCost),
            Codec.intRange(0, 72000).fieldOf("cooldown_ticks").forGetter(SkillDefinition::cooldownTicks),
            TagKey.codec(Registries.ITEM).optionalFieldOf("required_item").forGetter(SkillDefinition::requiredItem),
            SkillEffect.CODEC.fieldOf("effect").forGetter(SkillDefinition::effect)
    ).apply(instance, SkillDefinition::new));

    public boolean canUseWith(ItemStack mainHand) {
        return requiredItem.map(mainHand::is).orElse(true);
    }
}
