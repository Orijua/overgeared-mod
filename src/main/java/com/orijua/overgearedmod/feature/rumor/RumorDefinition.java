package com.orijua.overgearedmod.feature.rumor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Optional;

/**
 * A rumor from data/<namespace>/overgearedmod/rumor/<id>.json: a hint (not a quest) that the NPCs in {@code givers} drop
 * in their window, one a day. It stays quiet until {@code requires_quest} is completed, and is put to rest once its
 * secret is found: any quest in {@code solved_by.quests} discovered, accepted or completed, or any milestone in
 * {@code solved_by.milestones} reached.
 */
public record RumorDefinition(List<EntityType<?>> givers, Component text, Optional<ResourceLocation> requiresQuest, Secret solvedBy) {
    public record Secret(List<ResourceLocation> quests, List<ResourceLocation> milestones) {
        public static final Codec<Secret> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("quests", List.of()).forGetter(Secret::quests),
                ResourceLocation.CODEC.listOf().optionalFieldOf("milestones", List.of()).forGetter(Secret::milestones)
        ).apply(instance, Secret::new));
    }

    public static final Codec<RumorDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().listOf().fieldOf("givers").forGetter(RumorDefinition::givers),
            ComponentSerialization.CODEC.fieldOf("text").forGetter(RumorDefinition::text),
            ResourceLocation.CODEC.optionalFieldOf("requires_quest").forGetter(RumorDefinition::requiresQuest),
            Secret.CODEC.fieldOf("solved_by").forGetter(RumorDefinition::solvedBy)
    ).apply(instance, RumorDefinition::new));

    public boolean isToldBy(EntityType<?> type) {
        return givers.contains(type);
    }
}
