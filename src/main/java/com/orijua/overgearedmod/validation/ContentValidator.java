package com.orijua.overgearedmod.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Cross-checks the JSON content: Minecraft loads each file on its own, so a misspelled id in one file
 * only shows up in play (a class that silently grants no skill). Pure: no Minecraft state here.
 */
public final class ContentValidator {
    private ContentValidator() {
    }

    /** Every problem found, as a readable sentence, sorted; empty when the content is consistent. */
    public static List<String> validate(ContentIndex content) {
        List<String> problems = new ArrayList<>();
        for (ContentIndex.Reference reference : content.references()) {
            Set<ResourceLocation> existing = switch (reference.kind()) {
                case SKILL -> content.skills();
                case CLASS -> content.classes();
            };
            if (!existing.contains(reference.target())) {
                problems.add(reference.from() + ": " + reference.kind().name().toLowerCase() + " " + reference.target() + " does not exist");
            }
        }
        for (ResourceLocation advanced : content.advancedClasses()) {
            if (!content.rewardedClasses().contains(advanced)) {
                problems.add("class " + advanced + ": can't be chosen and no quest grants it, so nobody can get it");
            }
        }
        problems.sort(null);
        return problems;
    }
}
