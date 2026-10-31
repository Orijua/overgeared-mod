package com.orijua.overgearedmod.structure;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The world's structure sites, saved with the overworld so they survive restarts. The file keeps its first name
 * (data/overgearedmod_starting_village.dat), and Bairan its old "site" key, so worlds from before the cave keep their Bairan.
 */
final class WorldSitesData extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_starting_village";
    private static final String LEGACY_BAIRAN = "site";

    private final Map<String, ChunkPos> sites = new LinkedHashMap<>();

    static WorldSitesData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldSitesData::new, WorldSitesData::load), NAME);
    }

    private static WorldSitesData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldSitesData data = new WorldSitesData();
        for (String key : tag.getAllKeys()) {
            data.sites.put(key.equals(LEGACY_BAIRAN) ? WorldSites.BAIRAN : key, new ChunkPos(tag.getLong(key)));
        }
        return data;
    }

    Map<String, ChunkPos> sites() {
        return Map.copyOf(sites);
    }

    Optional<ChunkPos> site(String name) {
        return Optional.ofNullable(sites.get(name));
    }

    void setSite(String name, ChunkPos site) {
        sites.put(name, site);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        sites.forEach((name, site) -> tag.putLong(name.equals(WorldSites.BAIRAN) ? LEGACY_BAIRAN : name, site.toLong()));
        return tag;
    }
}
