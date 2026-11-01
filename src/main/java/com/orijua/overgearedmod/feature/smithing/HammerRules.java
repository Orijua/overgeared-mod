package com.orijua.overgearedmod.feature.smithing;

import com.orijua.overgearedmod.feature.equipment.ItemRating;

/**
 * Pure rules of the smithing hammer (canon: production tools matter, cap. 65; Grid's golden hammer, cap. 893). A
 * hammer adds skill points to the rating roll, like a good manual forge: its material sets a base, its rating, its
 * enhancement (+1 per level) and its "precision" options add more.
 */
public final class HammerRules {
    /** Extra skill points by the hammer's own rating, in {@link ItemRating} order. */
    private static final double[] RATING_BONUS = {0, 2, 4, 7, 11, 16};
    public static final double PER_ENHANCEMENT = 1;

    private HammerRules() {
    }

    public static double ratingBonus(ItemRating rating) {
        return RATING_BONUS[rating.ordinal()];
    }

    /** All the skill points a hammer gives: material, rating, enhancement and options. */
    public static double skillBonus(int materialBonus, ItemRating rating, int enhancement, double optionSkill) {
        return materialBonus + ratingBonus(rating) + enhancement * PER_ENHANCEMENT + optionSkill;
    }

    /** Smithing experience with a hammer whose options teach {@code xpBonus} more (0.1 = +10 %). */
    public static int smithingXp(int baseXp, double xpBonus) {
        return (int) Math.round(baseXp * (1 + xpBonus));
    }
}
