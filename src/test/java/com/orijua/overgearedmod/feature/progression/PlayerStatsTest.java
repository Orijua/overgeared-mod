package com.orijua.overgearedmod.feature.progression;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The saved shape of the player's stats: it must read back what it wrote, whatever the values (a save can hold odd ones). */
class PlayerStatsTest {
    private static PlayerStats roundTrip(PlayerStats stats) {
        Tag saved = PlayerStats.CODEC.encodeStart(NbtOps.INSTANCE, stats).getOrThrow();
        return PlayerStats.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    }

    @Test
    void theDefaultRoundTrips() {
        assertEquals(PlayerStats.DEFAULT, roundTrip(PlayerStats.DEFAULT));
    }

    @Test
    void aFilledInValueRoundTrips() {
        PlayerStats stats = new PlayerStats(12, 340, 5, 7, 6, 4, 3, 9, 1500);
        assertEquals(stats, roundTrip(stats));
    }

    @Test
    void levelZeroAndNegativeValuesAreKeptAsSaved() {
        PlayerStats zero = PlayerStats.DEFAULT.withProgress(0, 0, 0);
        PlayerStats negative = new PlayerStats(-3, -10, -1, -1, -1, -1, -1, -1, -5);
        assertEquals(zero, roundTrip(zero));
        assertEquals(negative, roundTrip(negative));
    }

    @Test
    void theMaximumValuesRoundTrip() {
        int max = Integer.MAX_VALUE;
        PlayerStats stats = new PlayerStats(max, max, max, max, max, max, max, max, max);
        assertEquals(stats, roundTrip(stats));
    }

    @Test
    void smithingXpIsOptionalInOlderSaves() {
        Tag saved = PlayerStats.CODEC.encodeStart(NbtOps.INSTANCE, PlayerStats.DEFAULT).getOrThrow();
        ((net.minecraft.nbt.CompoundTag) saved).remove("smithing_xp");
        PlayerStats loaded = PlayerStats.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(0, loaded.smithingXp());
        assertTrue(loaded.level() == 1);
    }
}
