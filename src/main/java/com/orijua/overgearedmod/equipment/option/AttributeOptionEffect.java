package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/** Adds a vanilla attribute modifier while the item is equipped, e.g. +attack damage or +max health. */
public record AttributeOptionEffect(Holder<Attribute> attribute, AttributeModifier.Operation operation) implements OptionEffect {

    public static final MapCodec<AttributeOptionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(AttributeOptionEffect::attribute),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                    .forGetter(AttributeOptionEffect::operation)
    ).apply(instance, AttributeOptionEffect::new));

    @Override
    public OptionEffectType type() {
        return OptionEffectType.ATTRIBUTE;
    }

    @Override
    public Component formatValue(float value) {
        return operation == AttributeModifier.Operation.ADD_VALUE
                ? Component.literal(String.format("+%.1f", value))
                : Component.literal(String.format("+%.0f%%", value * 100));
    }

    @Override
    public void addModifiers(ItemAttributeModifierEvent event, ResourceLocation modifierId, float value, EquipmentSlotGroup slot) {
        event.addModifier(attribute, new AttributeModifier(modifierId, value, operation), slot);
    }
}
