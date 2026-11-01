package com.orijua.overgearedmod.feature.rumor;

import java.util.UUID;

/** Which of an NPC's open rumors a player hears today: the same all day, another tomorrow, different for each player. */
public final class RumorRules {
    public static final long DAY_TICKS = 24000L;

    private RumorRules() {
    }

    public static long day(long dayTime) {
        return Math.floorDiv(dayTime, DAY_TICKS);
    }

    /** An index into {@code count} open rumors (sorted by id), or -1 when there are none. */
    public static int pick(int count, long day, UUID player, String giver) {
        if (count <= 0) {
            return -1;
        }
        // Consecutive days walk through the list, from a start that depends on the player and the NPC.
        long start = player.getMostSignificantBits() ^ player.getLeastSignificantBits() ^ giver.hashCode();
        return (int) Math.floorMod(start + day, (long) count);
    }
}
