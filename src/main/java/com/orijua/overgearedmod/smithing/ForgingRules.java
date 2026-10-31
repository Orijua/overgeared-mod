package com.orijua.overgearedmod.smithing;

/**
 * Pure rules of the two ways to forge. Canon: Satisfy has automatic production (press the button, it takes
 * longer for unskilled smiths) and manual production (more chance of a high rating and extra options).
 */
public final class ForgingRules {
    /** Automatic forging takes between 1 and 4 seconds. */
    public static final int MIN_AUTO_TICKS = 20;
    public static final int MAX_AUTO_TICKS = 80;

    /** Skill points a manual forge adds at 100 % precision, and takes away at 0 %. At 50 % it equals the automatic one. */
    public static final double MANUAL_SKILL_SWING = 40.0;
    /** From this precision on, a manual forge may add one extra option. */
    public static final double EXTRA_OPTION_FROM = 0.8;
    /** Chance of that extra option at 100 % precision. */
    public static final double MAX_EXTRA_OPTION_CHANCE = 0.5;
    /** Manual forging grows the smith's craft faster. */
    public static final int MANUAL_XP_MULTIPLIER = 2;

    /** A real minigame can't be finished faster than this, nor left open longer; outside it the result is refused. */
    public static final int MIN_MANUAL_TICKS = 40;
    public static final int MAX_MANUAL_TICKS = 20 * 120;

    /** Batch forging (own design): how many items one press of Auto makes. "All" is as many as the materials allow. */
    public static final int BATCH_ALL = Integer.MAX_VALUE;
    public static final int[] BATCH_SIZES = {1, 5, 10, BATCH_ALL};
    /** Smithing level (with the class bonus) that unlocks each of {@link #BATCH_SIZES}. */
    public static final int[] BATCH_UNLOCK_LEVELS = {0, 10, 20, 30};

    private ForgingRules() {
    }

    /** Whether the batch size at {@code index} of {@link #BATCH_SIZES} is unlocked at this smithing level. */
    public static boolean batchUnlocked(int index, int smithingLevel) {
        return index >= 0 && index < BATCH_SIZES.length && smithingLevel >= BATCH_UNLOCK_LEVELS[index];
    }

    /** The next unlocked batch size after {@code index}, wrapping back to ×1. */
    public static int nextBatchIndex(int index, int smithingLevel) {
        int next = index + 1;
        return batchUnlocked(next, smithingLevel) ? next : 0;
    }

    /**
     * How many items a batch really makes: the chosen size, capped by the materials. Each item takes
     * {@code perItem} materials and, if the recipe uses a handle, one handle ({@code handles} &lt; 0 means none).
     */
    public static int batchTotal(int chosen, int materials, int perItem, int handles) {
        int affordable = perItem <= 0 ? 0 : materials / perItem;
        if (handles >= 0) {
            affordable = Math.min(affordable, handles);
        }
        return Math.max(0, Math.min(chosen, affordable));
    }

    /** 4 s for a novice, shorter with every smithing level and DEX point, never under 1 s. */
    public static int autoForgeTicks(int dexterity, int smithingLevel) {
        int ticks = MAX_AUTO_TICKS - 4 * Math.max(0, smithingLevel) - Math.max(0, dexterity) / 2;
        return Math.clamp(ticks, MIN_AUTO_TICKS, MAX_AUTO_TICKS);
    }

    /** Clamps whatever the client sent into 0-1. */
    public static double clampPrecision(double precision) {
        return Double.isNaN(precision) ? 0 : Math.clamp(precision, 0, 1);
    }

    /** Skill bonus for the rating roll: -40 at 0 %, 0 at 50 %, +40 at 100 %. */
    public static double manualSkillBonus(double precision) {
        return (clampPrecision(precision) - 0.5) * 2 * MANUAL_SKILL_SWING;
    }

    /** 0 below 80 % precision, then rising to 50 % at a perfect forge. */
    public static double extraOptionChance(double precision) {
        double p = clampPrecision(precision);
        if (p < EXTRA_OPTION_FROM) {
            return 0;
        }
        return (p - EXTRA_OPTION_FROM) / (1 - EXTRA_OPTION_FROM) * MAX_EXTRA_OPTION_CHANCE;
    }

    /** Whether a manual forge that started {@code elapsedTicks} ago can be a real, honest minigame. */
    public static boolean plausibleManualDuration(long elapsedTicks) {
        return elapsedTicks >= MIN_MANUAL_TICKS && elapsedTicks <= MAX_MANUAL_TICKS;
    }
}
