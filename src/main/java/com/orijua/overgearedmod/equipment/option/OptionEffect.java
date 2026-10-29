package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * What a rolled item option does. Each type overrides only the hooks it needs; the JSON "type"
 * field picks the implementation (see {@link OptionEffectType}).
 */
public interface OptionEffect {
    Codec<OptionEffect> CODEC = OptionEffectType.CODEC.dispatch("type", OptionEffect::type, OptionEffectType::codec);

    OptionEffectType type();

    /** Tooltip value, e.g. "+2.5" or "4%". */
    Component formatValue(float value);

    default void addModifiers(ItemAttributeModifierEvent event, ResourceLocation modifierId, float value, EquipmentSlotGroup slot) {
    }

    default void onHit(ServerPlayer attacker, LivingEntity target, float damageDealt, float value) {
    }
}
