package com.orijua.overgearedmod.validation;

import com.orijua.overgearedmod.validation.ContentIndex.Kind;
import com.orijua.overgearedmod.validation.ContentIndex.Reference;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentValidatorTest {
    private static final ResourceLocation SLASH = id("slash");
    private static final ResourceLocation WARRIOR = id("warrior");
    private static final ResourceLocation HERO = id("hero");

    @Test
    void consistentContentHasNoProblems() {
        ContentIndex content = new ContentIndex(Set.of(SLASH), Set.of(WARRIOR, HERO), Set.of(HERO), Set.of(HERO), List.of(
                new Reference("class test:warrior", Kind.SKILL, SLASH),
                new Reference("quest test:trial", Kind.CLASS, WARRIOR),
                new Reference("quest test:trial", Kind.CLASS, HERO)));
        assertEquals(List.of(), ContentValidator.validate(content));
    }

    @Test
    void reportsAMissingSkill() {
        ContentIndex content = new ContentIndex(Set.of(SLASH), Set.of(WARRIOR), Set.of(), Set.of(), List.of(
                new Reference("class test:warrior", Kind.SKILL, id("slsh"))));
        assertEquals(List.of("class test:warrior: skill test:slsh does not exist"), ContentValidator.validate(content));
    }

    @Test
    void reportsAMissingClass() {
        ContentIndex content = new ContentIndex(Set.of(), Set.of(WARRIOR), Set.of(), Set.of(), List.of(
                new Reference("quest test:trial", Kind.CLASS, id("mage"))));
        assertEquals(List.of("quest test:trial: class test:mage does not exist"), ContentValidator.validate(content));
    }

    @Test
    void reportsAnAdvancedClassNoQuestGrants() {
        ContentIndex content = new ContentIndex(Set.of(), Set.of(WARRIOR, HERO), Set.of(HERO), Set.of(), List.of());
        assertEquals(List.of("class test:hero: can't be chosen and no quest grants it, so nobody can get it"),
                ContentValidator.validate(content));
    }

    @Test
    void reportsEveryProblemSorted() {
        ContentIndex content = new ContentIndex(Set.of(), Set.of(), Set.of(), Set.of(), List.of(
                new Reference("quest test:b", Kind.SKILL, SLASH),
                new Reference("class test:a", Kind.SKILL, SLASH)));
        assertEquals(List.of(
                "class test:a: skill test:slash does not exist",
                "quest test:b: skill test:slash does not exist"), ContentValidator.validate(content));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
