package com.orijua.overgearedmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.goal.Goal;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * A quest from data/<namespace>/overgearedmod/quest/<id>.json: accept it, meet its goal, claim the rewards.
 * {@code required_class} limits who can accept it; {@code class_reward} changes the player's class when claimed.
 * {@code requires_quest} chains quests: this one unlocks once that one has been completed.
 */
public record QuestDefinition(
        Component name,
        Component description,
        Goal goal,
        Optional<ResourceLocation> requiredClass,
        Optional<ResourceLocation> requiresQuest,
        int xpReward,
        List<ItemStack> itemRewards,
        Optional<ResourceLocation> classReward,
        boolean repeatable) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(QuestDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(QuestDefinition::description),
            Goal.CODEC.fieldOf("goal").forGetter(QuestDefinition::goal),
            ResourceLocation.CODEC.optionalFieldOf("required_class").forGetter(QuestDefinition::requiredClass),
            ResourceLocation.CODEC.optionalFieldOf("requires_quest").forGetter(QuestDefinition::requiresQuest),
            Codec.intRange(0, 1_000_000).optionalFieldOf("xp_reward", 0).forGetter(QuestDefinition::xpReward),
            ItemStack.CODEC.listOf().optionalFieldOf("item_rewards", List.of()).forGetter(QuestDefinition::itemRewards),
            ResourceLocation.CODEC.optionalFieldOf("class_reward").forGetter(QuestDefinition::classReward),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(QuestDefinition::repeatable)
    ).apply(instance, QuestDefinition::new));

    public boolean allowsClass(Optional<ResourceLocation> playerClass) {
        return requiredClass.isEmpty() || requiredClass.equals(playerClass);
    }

    /** No previous quest in the chain, or the player has completed it. */
    public boolean isUnlockedBy(PlayerQuests quests) {
        return requiresQuest.map(quests::isCompleted).orElse(true);
    }
}
