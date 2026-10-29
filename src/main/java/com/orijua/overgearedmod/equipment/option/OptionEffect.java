package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.BiConsumer;

/**
 * What a rolled item option does. Each type overrides only the hooks it needs; the JSON "type"
 * field picks the implementation (see {@link OptionEffectType}).
 */
public interface OptionEffect {
    Codec<OptionEffect> CODEC = OptionEffectType.CODEC.dispatch("type", OptionEffect::type, OptionEffectType::codec);

    OptionEffectType type();

    /** Tooltip value, e.g. "+2.5" or "4%". */
    Component formatValue(float value);

    /** Attribute modifiers granted while the item is worn (and its requirements are met). */
    default void modifiers(ResourceLocation modifierId, float value, BiConsumer<Holder<Attribute>, AttributeModifier> add) {
    }

    default void onHit(ServerPlayer attacker, LivingEntity target, float damageDealt, float value) {
    }
}
