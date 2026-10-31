package com.orijua.overgearedmod.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
                case QUEST -> content.quests();
                case OPTION -> content.itemOptions();
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
        for (ResourceLocation quest : content.visibleClassQuests()) {
            problems.add("quest " + quest + ": grants a class but is visible to everyone; give it a discover_when");
        }
        for (ResourceLocation quest : content.questRequirements().keySet()) {
            if (loopsBack(quest, content.questRequirements())) {
                problems.add("quest " + quest + ": its requires_quest chain loops back to it, so it can never be accepted");
            }
        }
        content.questGivers().forEach((quest, giver) -> {
            if (!content.questGiverNpcs().contains(giver)) {
                problems.add("quest " + quest + ": giver " + giver + " has no quest window, so nobody can accept it");
            }
        });
        for (String owner : content.deliveriesWithoutGiver()) {
            problems.add(owner + ": a deliver goal needs a quest with a giver to hand the items to");
        }
        problems.sort(null);
        return problems;
    }

    /** Follows requires_quest from {@code start}; true if the chain comes back to it. */
    private static boolean loopsBack(ResourceLocation start, Map<ResourceLocation, ResourceLocation> requirements) {
        Set<ResourceLocation> seen = new HashSet<>();
        ResourceLocation current = requirements.get(start);
        while (current != null && seen.add(current)) {
            if (current.equals(start)) {
                return true;
            }
            current = requirements.get(current);
        }
        return false;
    }
}
