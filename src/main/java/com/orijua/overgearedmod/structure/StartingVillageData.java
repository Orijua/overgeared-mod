package com.orijua.overgearedmod.structure;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;

/** The starting village's site, saved with the overworld (data/overgearedmod_starting_village.dat) so it survives restarts. */
final class StartingVillageData extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_starting_village";
    private static final String SITE = "site";

    private ChunkPos site;

    static StartingVillageData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(StartingVillageData::new, StartingVillageData::load), NAME);
    }

    private static StartingVillageData load(CompoundTag tag, HolderLookup.Provider registries) {
        StartingVillageData data = new StartingVillageData();
        if (tag.contains(SITE)) {
            data.site = new ChunkPos(tag.getLong(SITE));
        }
        return data;
    }

    Optional<ChunkPos> site() {
        return Optional.ofNullable(site);
    }

    void setSite(ChunkPos site) {
        this.site = site;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (site != null) {
            tag.putLong(SITE, site.toLong());
        }
        return tag;
    }
}
