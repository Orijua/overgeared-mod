package com.orijua.overgearedmod.playerclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.progression.StatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** A class as written in data/<namespace>/overgearedmod/player_class/<id>.json. */
public record PlayerClassDefinition(
        Component name,
        Component description,
        Map<StatType, Integer> bonusStats,
        int smithingBonus,
        List<ResourceLocation> skills) {

    public static final Codec<PlayerClassDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(PlayerClassDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(PlayerClassDefinition::description),
            Codec.unboundedMap(StatType.CODEC, Codec.INT).optionalFieldOf("bonus_stats", Map.of()).forGetter(PlayerClassDefinition::bonusStats),
            Codec.intRange(0, 100).optionalFieldOf("smithing_bonus", 0).forGetter(PlayerClassDefinition::smithingBonus),
            ResourceLocation.CODEC.listOf().optionalFieldOf("skills", List.of()).forGetter(PlayerClassDefinition::skills)
    ).apply(instance, PlayerClassDefinition::new));
}
