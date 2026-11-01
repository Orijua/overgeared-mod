package com.orijua.overgearedmod.core.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What {@link ContentValidator} needs to know about the loaded content: which ids exist and which ids
 * each definition points to. Built from the real registries by {@link ContentScanner}, or by hand in tests.
 *
 * @param advancedClasses   classes with {@code "selectable": false}: only a quest's {@code class_reward} can grant them
 * @param rewardedClasses   every class some quest grants with {@code class_reward}
 * @param questRequirements quest → the quest its {@code requires_quest} points to, to find chains that loop
 * @param visibleClassQuests quests with a {@code class_reward} but no {@code discover_when}: in Satisfy every
 *                           class quest is found by the player, never listed for everyone
 * @param questGivers       quest → the NPC (entity type) in its {@code giver}
 * @param questGiverNpcs    the NPCs with a quest window, the only ones a {@code giver} can name
 * @param deliveriesWithoutGiver where a deliver goal sits with no giver to hand the items to ("quest x", "title y")
 * @param itemOptions       the item options that exist, for recipes' fixed options
 */
public record ContentIndex(
        Set<ResourceLocation> skills,
        Set<ResourceLocation> classes,
        Set<ResourceLocation> quests,
        Set<ResourceLocation> advancedClasses,
        Set<ResourceLocation> rewardedClasses,
        Map<ResourceLocation, ResourceLocation> questRequirements,
        Set<ResourceLocation> visibleClassQuests,
        List<Reference> references,
        Map<ResourceLocation, ResourceLocation> questGivers,
        Set<ResourceLocation> questGiverNpcs,
        Set<String> deliveriesWithoutGiver,
        Set<ResourceLocation> itemOptions) {

    /** Content without NPC quests, as most tests build it. */
    public ContentIndex(Set<ResourceLocation> skills, Set<ResourceLocation> classes, Set<ResourceLocation> quests,
                        Set<ResourceLocation> advancedClasses, Set<ResourceLocation> rewardedClasses,
                        Map<ResourceLocation, ResourceLocation> questRequirements, Set<ResourceLocation> visibleClassQuests,
                        List<Reference> references) {
        this(skills, classes, quests, advancedClasses, rewardedClasses, questRequirements, visibleClassQuests, references,
                Map.of(), Set.of(), Set.of());
    }

    /** Content without recipes' fixed options. */
    public ContentIndex(Set<ResourceLocation> skills, Set<ResourceLocation> classes, Set<ResourceLocation> quests,
                        Set<ResourceLocation> advancedClasses, Set<ResourceLocation> rewardedClasses,
                        Map<ResourceLocation, ResourceLocation> questRequirements, Set<ResourceLocation> visibleClassQuests,
                        List<Reference> references, Map<ResourceLocation, ResourceLocation> questGivers,
                        Set<ResourceLocation> questGiverNpcs, Set<String> deliveriesWithoutGiver) {
        this(skills, classes, quests, advancedClasses, rewardedClasses, questRequirements, visibleClassQuests, references,
                questGivers, questGiverNpcs, deliveriesWithoutGiver, Set.of());
    }

    public enum Kind {
        SKILL,
        CLASS,
        QUEST,
        OPTION
    }

    /** "{@code from} needs the {@code kind} {@code target} to exist", e.g. class warrior → skill pagma_link. */
    public record Reference(String from, Kind kind, ResourceLocation target) {
    }
}
