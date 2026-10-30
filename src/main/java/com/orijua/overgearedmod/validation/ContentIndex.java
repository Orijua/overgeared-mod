package com.orijua.overgearedmod.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * What {@link ContentValidator} needs to know about the loaded content: which ids exist and which ids
 * each definition points to. Built from the real registries by {@link ContentScanner}, or by hand in tests.
 *
 * @param advancedClasses classes with {@code "selectable": false}: only a quest's {@code class_reward} can grant them
 * @param rewardedClasses every class some quest grants with {@code class_reward}
 */
public record ContentIndex(
        Set<ResourceLocation> skills,
        Set<ResourceLocation> classes,
        Set<ResourceLocation> advancedClasses,
        Set<ResourceLocation> rewardedClasses,
        List<Reference> references) {

    public enum Kind {
        SKILL,
        CLASS
    }

    /** "{@code from} needs the {@code kind} {@code target} to exist", e.g. class warrior → skill pagma_link. */
    public record Reference(String from, Kind kind, ResourceLocation target) {
    }
}
