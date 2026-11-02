package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.feature.equipment.ItemRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HammerRulesTest {
    @Test
    void aBetterHammerAddsMoreSkill() {
        assertEquals(0, HammerRules.ratingBonus(ItemRating.NORMAL));
        assertEquals(16, HammerRules.ratingBonus(ItemRating.MYTH));
        assertEquals(5, HammerRules.skillBonus(5, ItemRating.NORMAL, 0, 0), "a plain iron hammer");
        assertEquals(10 + 4 + 2 + 3.5, HammerRules.skillBonus(10, ItemRating.EPIC, 2, 3.5), "material + rating + enhancement + options");
    }

    @Test
    void everyBetterRatingGivesMoreSkill() {
        ItemRating[] ratings = ItemRating.values();
        for (int i = 1; i < ratings.length; i++) {
            assertTrue(HammerRules.ratingBonus(ratings[i]) > HammerRules.ratingBonus(ratings[i - 1]), ratings[i] + " beats " + ratings[i - 1]);
        }
    }

    @Test
    void experienceRoundsAndNeverDropsBelowTheBase() {
        assertEquals(2, HammerRules.smithingXp(1, 0.5), "1.5 rounds up");
        assertEquals(0, HammerRules.smithingXp(0, 0.5));
        assertEquals(10, HammerRules.smithingXp(10, 0));
    }

    @Test
    void learningOptionsGiveMoreExperience() {
        assertEquals(20, HammerRules.smithingXp(20, 0));
        assertEquals(23, HammerRules.smithingXp(20, 0.15));
    }
}
