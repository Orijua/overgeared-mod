package com.orijua.overgearedmod.feature.insight;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hidden talents of villagers, for Talent Search (canon, cap. 157: Grid searches Bairan's residents and finds, among
 * others, a miner). Bairan's residents always hide one; elsewhere one villager in {@link #ONE_IN} does. Which talent is
 * fixed by the villager's UUID, so it never changes.
 */
public final class Talents {
    /** Tag carried by the villagers Bairan places. */
    public static final String BAIRAN_RESIDENT = Overgearedmod.MODID + ".bairan_resident";
    static final int ONE_IN = 4;
    static final List<String> KINDS = List.of("mining", "smithing", "carpentry", "cooking", "swordsmanship", "magic");

    private Talents() {
    }

    public static Optional<String> of(Villager villager) {
        return of(villager.getUUID(), villager.getTags().contains(BAIRAN_RESIDENT));
    }

    /** Pure part: the talent a villager with this UUID hides, if any. */
    static Optional<String> of(UUID uuid, boolean bairanResident) {
        if (!bairanResident && Math.floorMod(uuid.getMostSignificantBits(), ONE_IN) != 0) {
            return Optional.empty();
        }
        return Optional.of(KINDS.get(Math.floorMod(uuid.getLeastSignificantBits(), KINDS.size())));
    }
}
