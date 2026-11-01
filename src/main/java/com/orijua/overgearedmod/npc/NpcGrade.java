package com.orijua.overgearedmod.npc;

/**
 * An NPC's growth grade (canon: NPC wiki, cap. 1187). It says how an NPC grows, never what trade he has or how well he
 * works it: normal NPCs stop early, semi-named ones grow further but still stop, and named ones (gold name, like Ashur
 * or Piaro) grow without limit, like a player. Super named (the legends) stay out of reach for now.
 * <p>
 * For apprentices it is rolled at hiring: {@link #NAMED_CHANCE} named, {@link #SEMI_NAMED_CHANCE} semi-named, the
 * rest normal. It only touches growth: how much each piece teaches him and the highest level he can reach.
 */
public enum NpcGrade {
    NORMAL(1.0, 40),
    SEMI_NAMED(1.5, 70),
    NAMED(2.0, Integer.MAX_VALUE);

    public static final double NAMED_CHANCE = 0.01;
    public static final double SEMI_NAMED_CHANCE = 0.09;

    private final double xpMultiplier;
    private final int levelCap;

    NpcGrade(double xpMultiplier, int levelCap) {
        this.xpMultiplier = xpMultiplier;
        this.levelCap = levelCap;
    }

    /** Experience gained is multiplied by this. */
    public double xpMultiplier() {
        return xpMultiplier;
    }

    /** The highest level he can reach ({@link Integer#MAX_VALUE}: none). */
    public int levelCap() {
        return levelCap;
    }

    /** The grade a roll in [0, 1) gives: 1 % named, 9 % semi-named, 90 % normal. */
    public static NpcGrade roll(double roll01) {
        if (roll01 < NAMED_CHANCE) {
            return NAMED;
        }
        return roll01 < NAMED_CHANCE + SEMI_NAMED_CHANCE ? SEMI_NAMED : NORMAL;
    }
}
