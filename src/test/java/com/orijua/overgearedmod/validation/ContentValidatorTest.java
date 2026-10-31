package com.orijua.overgearedmod.validation;

import com.orijua.overgearedmod.validation.ContentIndex.Kind;
import com.orijua.overgearedmod.validation.ContentIndex.Reference;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentValidatorTest {
    private static final ResourceLocation SLASH = id("slash");
    private static final ResourceLocation WARRIOR = id("warrior");
    private static final ResourceLocation HERO = id("hero");
    private static final ResourceLocation TRIAL = id("trial");
    private static final ResourceLocation HUNT = id("hunt");
    private static final ResourceLocation FINALE = id("finale");

    @Test
    void consistentContentHasNoProblems() {
        ContentIndex content = new ContentIndex(Set.of(SLASH), Set.of(WARRIOR, HERO), Set.of(TRIAL, HUNT), Set.of(HERO), Set.of(HERO),
                Map.of(HUNT, TRIAL), Set.of(), List.of(
                new Reference("class test:warrior", Kind.SKILL, SLASH),
                new Reference("quest test:trial", Kind.CLASS, WARRIOR),
                new Reference("quest test:trial", Kind.CLASS, HERO),
                new Reference("quest test:hunt", Kind.QUEST, TRIAL)));
        assertEquals(List.of(), ContentValidator.validate(content));
    }

    @Test
    void reportsAMissingSkill() {
        ContentIndex content = references(new Reference("class test:warrior", Kind.SKILL, id("slsh")));
        assertEquals(List.of("class test:warrior: skill test:slsh does not exist"), ContentValidator.validate(content));
    }

    @Test
    void reportsAMissingClass() {
        ContentIndex content = references(new Reference("quest test:trial", Kind.CLASS, id("mage")));
        assertEquals(List.of("quest test:trial: class test:mage does not exist"), ContentValidator.validate(content));
    }

    @Test
    void reportsAMissingPreviousQuest() {
        ContentIndex content = references(new Reference("quest test:hunt", Kind.QUEST, id("trail")));
        assertEquals(List.of("quest test:hunt: quest test:trail does not exist"), ContentValidator.validate(content));
    }

    @Test
    void reportsAnAdvancedClassNoQuestGrants() {
        ContentIndex content = new ContentIndex(Set.of(), Set.of(WARRIOR, HERO), Set.of(), Set.of(HERO), Set.of(), Map.of(), Set.of(), List.of());
        assertEquals(List.of("class test:hero: can't be chosen and no quest grants it, so nobody can get it"),
                ContentValidator.validate(content));
    }

    @Test
    void reportsAClassQuestVisibleToEveryone() {
        ContentIndex content = new ContentIndex(Set.of(), Set.of(HERO), Set.of(TRIAL), Set.of(), Set.of(HERO), Map.of(), Set.of(TRIAL), List.of());
        assertEquals(List.of("quest test:trial: grants a class but is visible to everyone; give it a discover_when"),
                ContentValidator.validate(content));
    }

    @Test
    void reportsEveryQuestOfAChainThatLoops() {
        // trial → finale → hunt → trial loops; the extra quest only leads into the loop and is fine on its own.
        ResourceLocation extra = id("extra");
        Map<ResourceLocation, ResourceLocation> requirements = Map.of(TRIAL, FINALE, FINALE, HUNT, HUNT, TRIAL, extra, TRIAL);
        ContentIndex content = new ContentIndex(Set.of(), Set.of(), Set.of(TRIAL, HUNT, FINALE, extra), Set.of(), Set.of(),
                requirements, Set.of(), List.of());
        assertEquals(List.of(
                "quest test:finale: its requires_quest chain loops back to it, so it can never be accepted",
                "quest test:hunt: its requires_quest chain loops back to it, so it can never be accepted",
                "quest test:trial: its requires_quest chain loops back to it, so it can never be accepted"),
                ContentValidator.validate(content));
    }

    @Test
    void reportsEveryProblemSorted() {
        ContentIndex content = references(
                new Reference("quest test:b", Kind.SKILL, SLASH),
                new Reference("class test:a", Kind.SKILL, SLASH));
        assertEquals(List.of(
                "class test:a: skill test:slash does not exist",
                "quest test:b: skill test:slash does not exist"), ContentValidator.validate(content));
    }

    @Test
    void npcQuestsNeedAGiverWithAQuestWindow() {
        ResourceLocation smith = id("smith");
        ResourceLocation cow = ResourceLocation.withDefaultNamespace("cow");
        ContentIndex content = new ContentIndex(Set.of(), Set.of(), Set.of(TRIAL, HUNT), Set.of(), Set.of(), Map.of(), Set.of(), List.of(),
                Map.of(TRIAL, smith, HUNT, cow), Set.of(smith), Set.of("quest test:finale", "title test:hoarder"));
        assertEquals(List.of(
                "quest test:finale: a deliver goal needs a quest with a giver to hand the items to",
                "quest test:hunt: giver minecraft:cow has no quest window, so nobody can accept it",
                "title test:hoarder: a deliver goal needs a quest with a giver to hand the items to"), ContentValidator.validate(content));
    }

    @Test
    void aRecipeWithAMissingFixedOptionIsReported() {
        ResourceLocation agility = id("agility");
        ContentIndex content = new ContentIndex(Set.of(), Set.of(HERO), Set.of(), Set.of(), Set.of(), Map.of(), Set.of(), List.of(
                new Reference("forging recipe test:fracaso", Kind.CLASS, HERO),
                new Reference("forging recipe test:fracaso", Kind.OPTION, agility),
                new Reference("forging recipe test:fracaso", Kind.OPTION, id("agilty"))),
                Map.of(), Set.of(), Set.of(), Set.of(agility));
        assertEquals(List.of("forging recipe test:fracaso: option test:agilty does not exist"), ContentValidator.validate(content));
    }

    /** Content with warrior and the trial quest but no skills, plus the given references. */
    private static ContentIndex references(Reference... references) {
        return new ContentIndex(Set.of(), Set.of(WARRIOR), Set.of(TRIAL), Set.of(), Set.of(), Map.of(), Set.of(), List.of(references));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
