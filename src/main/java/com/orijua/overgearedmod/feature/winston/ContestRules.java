package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.core.config.Balance.ContestLoss;
import com.orijua.overgearedmod.feature.equipment.ItemRating;

/**
 * Pure rules of the item production contest against Euphemina in Winston's square (canon, caps. 36-40: Grid saves
 * Khan's smithy in a production contest against the smith the Mero Company hires). How it is scored is our own design:
 * one piece forged by hand within {@link #TIME_LIMIT_TICKS}, worth {@link #POINTS_PER_RATING} per rating step above
 * Normal plus the minigame's precision in percent, against Euphemina's roll of {@link #RIVAL_MIN}-{@link #RIVAL_MAX}
 * (between a fair Rare and a perfect Epic).
 */
public final class ContestRules {
    /** Five minutes of game time to forge the piece. */
    public static final int TIME_LIMIT_TICKS = 20 * 60 * 5;
    public static final int POINTS_PER_RATING = 100;
    public static final int RIVAL_MIN = 150;
    public static final int RIVAL_MAX = 300;

    private ContestRules() {
    }

    /** A piece's score: 100 per rating step above Normal plus the precision (0-1) in whole percent. Epic at 87 % is 287. */
    public static int score(ItemRating rating, double precision) {
        double p = Double.isNaN(precision) ? 0 : Math.clamp(precision, 0, 1);
        return rating.ordinal() * POINTS_PER_RATING + (int) Math.round(p * 100);
    }

    /** Euphemina's score for a {@code roll} in 0-1. */
    public static int rivalScore(double roll) {
        double r = Double.isNaN(roll) ? 0 : Math.clamp(roll, 0, 1);
        return RIVAL_MIN + (int) Math.round(r * (RIVAL_MAX - RIVAL_MIN));
    }

    /** The player has to beat her: a tie goes to Euphemina. */
    public static boolean wins(int score, int rivalScore) {
        return score > rivalScore;
    }

    /** Whether losing closes the quest for good, or allows a rematch the next day. */
    public static boolean failsForGood(ContestLoss loss, boolean wholeWorld) {
        return switch (loss) {
            case FAIL -> true;
            case REMATCH -> false;
            case AUTO -> wholeWorld;
        };
    }

    /** Game ticks left of a contest that began at {@code startedAt}; never negative. */
    public static long ticksLeft(long startedAt, long now) {
        return Math.max(0, TIME_LIMIT_TICKS - (now - startedAt));
    }

    /** A player who lost on {@code lostDay} (negative: never lost) can try again from the next day. */
    public static boolean canRematch(long lostDay, long today) {
        return lostDay < 0 || today > lostDay;
    }
}
