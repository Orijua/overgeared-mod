package com.orijua.overgearedmod.feature.equipment;

/**
 * Pure game rule (no Minecraft state): picks the rating of a freshly forged item.
 * Project design, inspired by Grid's forging getting better with dexterity and smithing skill.
 */
public final class RatingRoller {
    /** Base chance weights for each tier, in {@link ItemRating} order. Sum = 100. */
    private static final double[] BASE_WEIGHTS = {60.0, 25.0, 10.0, 4.0, 0.9, 0.1};

    /** Skill points needed to double the odds of each step up in tier. */
    private static final double SKILL_SCALE = 40.0;
    /** A negative skill (a botched manual forge) can push the odds down, but never below this factor. */
    private static final double MIN_FACTOR = 0.25;

    private RatingRoller() {
    }

    /**
     * @param dexterity     the smith's dexterity stat
     * @param smithingLevel the smith's smithing level
     * @param random01      a uniform random number in [0, 1)
     */
    public static ItemRating roll(int dexterity, int smithingLevel, double random01) {
        return roll(dexterity, smithingLevel, 0, random01);
    }

    /** As {@link #roll(int, int, double)}, plus {@code skillBonus} (negative or positive), e.g. from a manual forge. */
    public static ItemRating roll(int dexterity, int smithingLevel, double skillBonus, double random01) {
        return pick(weights(dexterity, smithingLevel, skillBonus), random01);
    }

    /**
     * As {@link #roll(int, int, double, double)}, but only tiers from {@code minimum} to {@code maximum} can come out,
     * keeping their relative odds. Fracaso (canon) always comes out Unique or Legendary.
     */
    public static ItemRating roll(int dexterity, int smithingLevel, double skillBonus, ItemRating minimum, ItemRating maximum,
                                  double random01) {
        return pick(limit(weights(dexterity, smithingLevel, skillBonus), minimum, maximum), random01);
    }

    /**
     * Rating of an item found as loot: {@code luck} plays the part of the smith's skill (0 = base odds)
     * and tiers below {@code minimum} can't come out, so a boss can guarantee at least Rare.
     */
    public static ItemRating rollLoot(int luck, ItemRating minimum, double random01) {
        return pick(limit(weights(luck, 0), minimum, ItemRating.MYTH), random01);
    }

    /** Zeroes every tier outside {@code minimum}-{@code maximum}. */
    private static double[] limit(double[] weights, ItemRating minimum, ItemRating maximum) {
        for (int i = 0; i < weights.length; i++) {
            if (i < minimum.ordinal() || i > maximum.ordinal()) {
                weights[i] = 0;
            }
        }
        return weights;
    }

    private static ItemRating pick(double[] weights, double random01) {
        double total = 0;
        for (double w : weights) {
            total += w;
        }

        // Keep to the [0, 1) contract: NaN counts as 0 and anything else is clamped.
        double clamped = Double.isNaN(random01) ? 0 : Math.max(0, Math.min(random01, Math.nextDown(1.0)));
        double target = clamped * total;
        ItemRating[] tiers = ItemRating.values();
        for (int i = 0; i < tiers.length; i++) {
            target -= weights[i];
            if (target < 0) {
                return tiers[i];
            }
        }
        // Rounding left us past the end: the last tier that is actually allowed, not blindly Myth.
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (weights[i] > 0) {
                return tiers[i];
            }
        }
        return tiers[tiers.length - 1];
    }

    /**
     * The chance of each rating when forging, in {@link ItemRating} order, as fractions that add up to 1.
     * Exactly what {@link #roll} uses, so the anvil can show the player their real odds.
     */
    public static double[] odds(int dexterity, int smithingLevel) {
        return odds(dexterity, smithingLevel, 0);
    }

    /** As {@link #odds(int, int)}, plus {@code skillBonus}, e.g. to show what a perfect manual forge would give. */
    public static double[] odds(int dexterity, int smithingLevel, double skillBonus) {
        return odds(dexterity, smithingLevel, skillBonus, ItemRating.NORMAL, ItemRating.MYTH);
    }

    /** As {@link #odds(int, int, double)}, for a recipe that only gives {@code minimum}-{@code maximum}. */
    public static double[] odds(int dexterity, int smithingLevel, double skillBonus, ItemRating minimum, ItemRating maximum) {
        double[] weights = limit(weights(dexterity, smithingLevel, skillBonus), minimum, maximum);
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= total;
        }
        return weights;
    }

    static double[] weights(int dexterity, int smithingLevel) {
        return weights(dexterity, smithingLevel, 0);
    }

    /** Higher tiers get boosted more: tier i is multiplied by factor^i (a factor below 1 favours low tiers). */
    static double[] weights(int dexterity, int smithingLevel, double skillBonus) {
        double skill = Math.max(0, dexterity) + 3.0 * Math.max(0, smithingLevel) + skillBonus;
        double factor = Math.max(MIN_FACTOR, 1.0 + skill / SKILL_SCALE);
        double[] weights = new double[BASE_WEIGHTS.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = BASE_WEIGHTS[i] * Math.pow(factor, i);
        }
        return weights;
    }
}
