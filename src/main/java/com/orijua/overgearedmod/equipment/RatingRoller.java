package com.orijua.overgearedmod.equipment;

/**
 * Pure game rule (no Minecraft state): picks the rating of a freshly forged item.
 * Project design, inspired by Grid's forging getting better with dexterity and smithing skill.
 */
public final class RatingRoller {
    /** Base chance weights for each tier, in {@link ItemRating} order. Sum = 100. */
    private static final double[] BASE_WEIGHTS = {60.0, 25.0, 10.0, 4.0, 0.9, 0.1};

    /** Skill points needed to double the odds of each step up in tier. */
    private static final double SKILL_SCALE = 40.0;

    private RatingRoller() {
    }

    /**
     * @param dexterity     the smith's dexterity stat
     * @param smithingLevel the smith's smithing level
     * @param random01      a uniform random number in [0, 1)
     */
    public static ItemRating roll(int dexterity, int smithingLevel, double random01) {
        double[] weights = weights(dexterity, smithingLevel);
        double total = 0;
        for (double w : weights) {
            total += w;
        }

        double target = random01 * total;
        ItemRating[] tiers = ItemRating.values();
        for (int i = 0; i < tiers.length; i++) {
            target -= weights[i];
            if (target < 0) {
                return tiers[i];
            }
        }
        return tiers[tiers.length - 1];
    }

    /** Higher tiers get boosted more: tier i is multiplied by factor^i. */
    static double[] weights(int dexterity, int smithingLevel) {
        double skill = Math.max(0, dexterity) + 3.0 * Math.max(0, smithingLevel);
        double factor = 1.0 + skill / SKILL_SCALE;
        double[] weights = new double[BASE_WEIGHTS.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = BASE_WEIGHTS[i] * Math.pow(factor, i);
        }
        return weights;
    }
}
