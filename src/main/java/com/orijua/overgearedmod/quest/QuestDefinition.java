package com.orijua.overgearedmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.goal.Goal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * A quest from data/<namespace>/overgearedmod/quest/<id>.json: accept it, meet its goal, claim the rewards.
 * {@code required_class} limits who can accept it; {@code class_reward} changes the player's class when claimed.
 * {@code requires_quest} chains quests: this one unlocks once that one has been completed.
 * {@code discover_when} makes it a hidden quest: invisible until the player meets that secret goal
 * (counted over their whole life, like titles), then it shows up and can be accepted.
 * {@code giver} is the NPC (entity type) that gives it: such a quest is only accepted, delivered to and claimed at an NPC
 * of that type, from its quest window; the Quests tab lists it with a hint to go and talk to them.
 * {@code loaned_items} are lent on accepting it and taken back (from the inventory) when it is claimed or abandoned.
 */
public record QuestDefinition(
        Component name,
        Component description,
        Goal goal,
        Optional<ResourceLocation> requiredClass,
        Optional<ResourceLocation> requiresQuest,
        Optional<Goal> discoverWhen,
        int xpReward,
        List<ItemStack> itemRewards,
        Optional<ResourceLocation> classReward,
        boolean repeatable,
        Optional<EntityType<?>> giver,
        List<ItemStack> loanedItems) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(QuestDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(QuestDefinition::description),
            Goal.CODEC.fieldOf("goal").forGetter(QuestDefinition::goal),
            ResourceLocation.CODEC.optionalFieldOf("required_class").forGetter(QuestDefinition::requiredClass),
            ResourceLocation.CODEC.optionalFieldOf("requires_quest").forGetter(QuestDefinition::requiresQuest),
            Goal.CODEC.optionalFieldOf("discover_when").forGetter(QuestDefinition::discoverWhen),
            Codec.intRange(0, 1_000_000).optionalFieldOf("xp_reward", 0).forGetter(QuestDefinition::xpReward),
            ItemStack.CODEC.listOf().optionalFieldOf("item_rewards", List.of()).forGetter(QuestDefinition::itemRewards),
            ResourceLocation.CODEC.optionalFieldOf("class_reward").forGetter(QuestDefinition::classReward),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(QuestDefinition::repeatable),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("giver").forGetter(QuestDefinition::giver),
            ItemStack.CODEC.listOf().optionalFieldOf("loaned_items", List.of()).forGetter(QuestDefinition::loanedItems)
    ).apply(instance, QuestDefinition::new));

    public boolean allowsClass(Optional<ResourceLocation> playerClass) {
        return requiredClass.isEmpty() || requiredClass.equals(playerClass);
    }

    /** No previous quest in the chain, or the player has completed it. */
    public boolean isUnlockedBy(PlayerQuests quests) {
        return requiresQuest.map(quests::isCompleted).orElse(true);
    }

    public boolean isGivenBy(EntityType<?> type) {
        return giver.isPresent() && giver.get() == type;
    }

    /** Where to go for it, for the Quests tab: "quest.&lt;namespace&gt;.giver.&lt;npc&gt;" ("Talk to Smith in Bairan"). */
    public Optional<Component> giverHint() {
        return giver.map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .map(id -> Component.translatable("quest." + id.getNamespace() + ".giver." + id.getPath()));
    }

    public boolean isHidden() {
        return discoverWhen.isPresent();
    }

    /** Visible quests are listed and can be accepted; a hidden one only once {@code quests} has discovered it. */
    public boolean isVisibleTo(ResourceLocation id, PlayerQuests quests) {
        return !isHidden() || quests.isDiscovered(id);
    }
}
