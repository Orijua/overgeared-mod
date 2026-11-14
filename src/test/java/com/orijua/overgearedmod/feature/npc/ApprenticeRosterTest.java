package com.orijua.overgearedmod.feature.npc;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprenticeRosterTest {
    @Test
    void theLostApprenticesSurviveASaveAndLoad() {
        UUID master = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        ApprenticeRoster roster = new ApprenticeRoster();
        roster.markLost(first, master);
        roster.markLost(second, master);
        roster.markLost(third, other);

        ApprenticeRoster loaded = ApprenticeRoster.load(roster.save(new CompoundTag(), null), null);

        assertEquals(3, loaded.size());
        assertEquals(List.of(first, second), loaded.lostOf(master));
        assertEquals(List.of(third), loaded.lostOf(other));
    }

    @Test
    void forgettingOneLeavesTheOthers() {
        UUID master = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ApprenticeRoster roster = new ApprenticeRoster();
        roster.markLost(first, master);
        roster.markLost(second, master);
        roster.forget(first);
        assertEquals(List.of(second), roster.lostOf(master));
    }

    @Test
    void anEmptyOrDamagedTagLoadsEmpty() {
        assertTrue(ApprenticeRoster.load(new CompoundTag(), null).lostOf(UUID.randomUUID()).isEmpty());
        CompoundTag damaged = new CompoundTag();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(new CompoundTag()); // an entry without its UUIDs
        damaged.put("lost", list);
        assertEquals(0, ApprenticeRoster.load(damaged, null).size());
    }
}
