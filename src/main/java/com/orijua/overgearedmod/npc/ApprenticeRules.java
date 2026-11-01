package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.equipment.ItemRating;

/**
 * Pure rules of the apprentice blacksmiths (own design inspired by the canon, caps. 622, 624 and 925: Grid trains a
 * hundred smiths into a production system). They forge alone, slowly and not as well as their master, can botch a piece,
 * get better with every piece and cost a daily wage.
 */
public final class ApprenticeRules {
    /** The first apprentice comes at this effective smithing level, one more every {@link #LEVELS_PER_APPRENTICE}. */
    public static final int FIRST_APPRENTICE_LEVEL = 50;
    public static final int LEVELS_PER_APPRENTICE = 10;
    public static final int MAX_APPRENTICES = 5;
    /** What a new apprentice already knows: smithing level 5. */
    public static final int STARTING_XP = 250;
    /** Rating roll penalty: a bit worse than the master's own automatic forging. */
    public static final double RATING_PENALTY = -10;
    /** Chance of botching a piece (the materials are lost): high for a beginner, never below the minimum. */
    public static final double MAX_FAIL_CHANCE = 0.30;
    public static final double MIN_FAIL_CHANCE = 0.05;
    private static final double FAIL_DROP_PER_LEVEL = 0.004;
    /** Ticks per piece: a minute for a beginner, down to 20 s. */
    public static final int SLOWEST_TICKS = 1200;
    public static final int FASTEST_TICKS = 400;
    private static final int TICKS_SAVED_PER_LEVEL = 10;
    /** Emeralds per Minecraft day, and how many days can be paid ahead. */
    public static final int DAILY_WAGE = 3;
    public static final int MAX_PAID_DAYS = 14;
    public static final long DAY_TICKS = 24000;
    /** What Smith asks for each contract after the first, which he gives. */
    public static final int CONTRACT_PRICE = 64;

    private ApprenticeRules() {
    }

    /** How many apprentices a smith of this effective level may have: 0 below 50, then 1 at 50, 2 at 60... up to 5. */
    public static int allowedApprentices(int smithingLevel) {
        if (smithingLevel < FIRST_APPRENTICE_LEVEL) {
            return 0;
        }
        return Math.min(MAX_APPRENTICES, 1 + (smithingLevel - FIRST_APPRENTICE_LEVEL) / LEVELS_PER_APPRENTICE);
    }

    /** The level needed for the next apprentice once you have {@code owned}, or -1 past the maximum. */
    public static int levelForApprentice(int owned) {
        return owned >= MAX_APPRENTICES ? -1 : FIRST_APPRENTICE_LEVEL + owned * LEVELS_PER_APPRENTICE;
    }

    /** A Craftsman's pieces come out as good as his master's automatic forging. */
    public static final double CRAFTSMAN_RATING_PENALTY = 0;
    /** How close to his master a forged Legendary must be to enlighten him. */
    public static final double ILLUMINATION_REACH = 8.0;

    /** The apprentice's working level: what his experience gives, below his master's and within his grade's cap. */
    public static int workingLevel(int levelFromXp, int masterLevel, NpcGrade grade) {
        return Math.max(0, Math.min(Math.min(levelFromXp, masterLevel - 1), grade.levelCap()));
    }

    /** Experience a piece teaches him: the recipe's, scaled by his grade. */
    public static int xpGained(int recipeXp, NpcGrade grade) {
        return (int) Math.round(recipeXp * grade.xpMultiplier());
    }

    /**
     * Whether a piece makes him a Craftsman (canon: the recognition comes from extraordinary work; Khan reached it through
     * Grid, cap. 768). Only a Master can; then his own Unique or better piece is enough, or watching his master forge a
     * Legendary (illumination).
     */
    public static boolean earnsCraftsman(MasteryStage stage, ItemRating piece, boolean ownPiece) {
        if (stage != MasteryStage.MASTER) {
            return false;
        }
        return piece.ordinal() >= (ownPiece ? ItemRating.UNIQUE : ItemRating.LEGENDARY).ordinal();
    }

    public static double failChance(int level) {
        return Math.clamp(MAX_FAIL_CHANCE - level * FAIL_DROP_PER_LEVEL, MIN_FAIL_CHANCE, MAX_FAIL_CHANCE);
    }

    public static int ticksPerPiece(int level) {
        return Math.max(FASTEST_TICKS, SLOWEST_TICKS - level * TICKS_SAVED_PER_LEVEL);
    }

    /** Days a payment of {@code emeralds} buys, given the days already paid ahead. */
    public static int daysBought(int emeralds, int daysAlreadyPaid) {
        return Math.max(0, Math.min(emeralds / DAILY_WAGE, MAX_PAID_DAYS - daysAlreadyPaid));
    }
}
