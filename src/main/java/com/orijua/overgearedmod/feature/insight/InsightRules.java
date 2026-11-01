package com.orijua.overgearedmod.feature.insight;

/**
 * Pure rules of Insight (canon: a stat that can't be allocated and grows by observing; with it, Character Observation
 * shows a target's abilities, maximum stats and skills). Here it grows by observing: sneaking (the concentration) while
 * looking at an NPC or a monster for {@link #OBSERVE_TICKS} gives one point, once per target and Minecraft day. The
 * more Insight, the more an NPC's window shows ({@link Detail}).
 */
public final class InsightRules {
    /** Ten seconds of steady observation. */
    public static final int OBSERVE_TICKS = 200;
    /** How far you can study someone. */
    public static final double REACH = 16.0;
    /** Insight needed for each level of detail. */
    public static final int MEDIUM = 15;
    public static final int HIGH = 40;
    public static final long DAY_TICKS = 24000;
    /** Insight that halves the time to observe; it never drops under {@link #FASTEST_OBSERVE_TICKS}. */
    public static final int HALVING_INSIGHT = 50;
    public static final int FASTEST_OBSERVE_TICKS = 40;

    /** How much Character Observation reveals. */
    public enum Detail { LOW, MEDIUM, HIGH }

    private InsightRules() {
    }

    public static Detail detail(int insight) {
        if (insight >= HIGH) {
            return Detail.HIGH;
        }
        return insight >= MEDIUM ? Detail.MEDIUM : Detail.LOW;
    }

    /** The Insight needed for the next detail, or -1 when everything is visible. */
    public static int nextThreshold(int insight) {
        return switch (detail(insight)) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> -1;
        };
    }

    /**
     * Ticks of steady looking for a point, from {@code baseTicks} (ten seconds, or a sword of observation's): the more
     * Insight, the faster the bar fills. 0 Insight: the whole base; {@link #HALVING_INSIGHT}: half; never under
     * {@link #FASTEST_OBSERVE_TICKS}.
     */
    public static int observeTicks(int baseTicks, int insight) {
        int ticks = (int) Math.round(baseTicks * HALVING_INSIGHT / (double) (HALVING_INSIGHT + Math.max(0, insight)));
        return Math.max(Math.min(baseTicks, FASTEST_OBSERVE_TICKS), ticks);
    }

    public static long day(long gameTime) {
        return gameTime / DAY_TICKS;
    }
}
