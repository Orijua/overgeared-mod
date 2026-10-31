package com.orijua.overgearedmod.equipment.option;

import com.mojang.serialization.Codec;
import com.orijua.overgearedmod.progression.StatModifier;
import com.orijua.overgearedmod.progression.StatSource;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    /** Points added to the wearer's stats (STR, AGI...) while the item is worn, tagged with the item as their source. */
    default void statModifiers(StatSource source, float value, Consumer<StatModifier> add) {
    }

    default void onHit(ServerPlayer attacker, LivingEntity target, float damageDealt, float value) {
    }

    /** The wearer is about to take damage; may cancel it (a block) or change its amount. */
    default void onWearerHurt(ServerPlayer wearer, LivingIncomingDamageEvent event, float value) {
    }

    /** The damage the wearer's attack deals to {@code target}, before armor; returns {@code amount} unchanged by default. */
    default float outgoingDamage(ServerPlayer attacker, LivingEntity target, float amount, float value) {
        return amount;
    }

    /** Runs once a second while the item is worn. */
    default void tickWorn(ServerPlayer wearer, float value) {
    }

    /** The skill this option lets the wearer cast with the weapon-skill key, if any. */
    default Optional<ResourceLocation> grantedSkill() {
        return Optional.empty();
    }
}
