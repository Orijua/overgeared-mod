package com.orijua.overgearedmod.feature.equipment.option;

import com.orijua.overgearedmod.feature.equipment.ItemRating;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedOptionTest {
    private static final ResourceLocation AGILITY = ResourceLocation.fromNamespaceAndPath("test", "agility");
    private static final ResourceLocation BISECTION = ResourceLocation.fromNamespaceAndPath("test", "bisection");
    private static final List<FixedOption> FRACASO = List.of(
            new FixedOption(AGILITY, 30),
            new FixedOption(BISECTION, 1, ItemRating.LEGENDARY));
    private static final List<FixedOption> GRADED_AGILITY = List.of(
            new FixedOption(AGILITY, 30, ItemRating.NORMAL, ItemRating.UNIQUE),
            new FixedOption(AGILITY, 50, ItemRating.LEGENDARY));

    @Test
    void optionsWithoutMinimumAlwaysCome() {
        assertEquals(List.of(new RolledOption(AGILITY, 30)), FixedOption.forRating(FRACASO, ItemRating.UNIQUE));
    }

    @Test
    void anOptionWithAMinimumNeedsThatRatingOrBetter() {
        List<RolledOption> expected = List.of(new RolledOption(AGILITY, 30), new RolledOption(BISECTION, 1));
        assertEquals(expected, FixedOption.forRating(FRACASO, ItemRating.LEGENDARY));
        assertEquals(expected, FixedOption.forRating(FRACASO, ItemRating.MYTH));
    }

    @Test
    void withoutAMaximumAnOptionComesAtEveryRatingFromItsMinimum() {
        assertEquals(List.of(new RolledOption(AGILITY, 30)), FixedOption.forRating(FRACASO, ItemRating.NORMAL));
        assertEquals(List.of(new RolledOption(AGILITY, 30)), FixedOption.forRating(FRACASO, ItemRating.MYTH).subList(0, 1));
    }

    @Test
    void anOptionWithAMaximumStopsAfterThatRating() {
        assertEquals(List.of(new RolledOption(AGILITY, 30)), FixedOption.forRating(GRADED_AGILITY, ItemRating.UNIQUE));
        assertEquals(List.of(new RolledOption(AGILITY, 50)), FixedOption.forRating(GRADED_AGILITY, ItemRating.LEGENDARY));
        assertEquals(List.of(new RolledOption(AGILITY, 50)), FixedOption.forRating(GRADED_AGILITY, ItemRating.MYTH));
    }
}
