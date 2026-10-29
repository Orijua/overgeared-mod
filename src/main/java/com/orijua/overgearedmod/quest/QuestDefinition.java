package com.orijua.overgearedmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.goal.Goal;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A quest from data/<namespace>/overgearedmod/quest/<id>.json: accept it, meet its goal, claim the rewards. */
public record QuestDefinition(
        Component name,
        Component description,
        Goal goal,
        int xpReward,
        List<ItemStack> itemRewards,
        boolean repeatable) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(QuestDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(QuestDefinition::description),
            Goal.CODEC.fieldOf("goal").forGetter(QuestDefinition::goal),
            Codec.intRange(0, 1_000_000).optionalFieldOf("xp_reward", 0).forGetter(QuestDefinition::xpReward),
            ItemStack.CODEC.listOf().optionalFieldOf("item_rewards", List.of()).forGetter(QuestDefinition::itemRewards),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(QuestDefinition::repeatable)
    ).apply(instance, QuestDefinition::new));
}
