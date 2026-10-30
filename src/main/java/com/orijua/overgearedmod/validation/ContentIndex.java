package com.orijua.overgearedmod.validation;

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
 */
public record ContentIndex(
        Set<ResourceLocation> skills,
        Set<ResourceLocation> classes,
        Set<ResourceLocation> quests,
        Set<ResourceLocation> advancedClasses,
        Set<ResourceLocation> rewardedClasses,
        Map<ResourceLocation, ResourceLocation> questRequirements,
        Set<ResourceLocation> visibleClassQuests,
        List<Reference> references) {

    public enum Kind {
        SKILL,
        CLASS,
        QUEST
    }

    /** "{@code from} needs the {@code kind} {@code target} to exist", e.g. class warrior → skill pagma_link. */
    public record Reference(String from, Kind kind, ResourceLocation target) {
    }
}
