package com.orijua.overgearedmod.feature.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.quest.goal.Goal;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.Map;

/**
 * A title from data/<namespace>/overgearedmod/title/<id>.json: earned once its goal is met, its bonuses apply forever.
 * A {@code rare} one is announced with the grand system window.
 */
public record TitleDefinition(Component name, Component description, Goal goal, Map<StatType, Integer> bonusStats, boolean rare) {
    public static final Codec<TitleDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(TitleDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(TitleDefinition::description),
            Goal.CODEC.fieldOf("goal").forGetter(TitleDefinition::goal),
            Codec.unboundedMap(StatType.CODEC, Codec.INT).optionalFieldOf("bonus_stats", Map.of()).forGetter(TitleDefinition::bonusStats),
            Codec.BOOL.optionalFieldOf("rare", false).forGetter(TitleDefinition::rare)
    ).apply(instance, TitleDefinition::new));
}
