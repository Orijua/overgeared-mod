package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The Satisfy dimension's structure sites and Bairan's landing spot, saved with the overworld (the server's main level)
 * so they survive restarts. Worlds from before the dimension have no such file, so no Satisfy sites: new worlds only.
 */
final class WorldSitesData extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_satisfy_sites";
    private static final String SITES = "sites";
    private static final String LANDING = "landing";
    private static final String CHOSEN = "chosen";

    private final Map<String, ChunkPos> sites = new LinkedHashMap<>();
    private BlockPos landing;
    private boolean chosen;

    /** {@code overworld} is where the file lives, whatever dimension the sites are in. */
    static WorldSitesData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldSitesData::new, WorldSitesData::load), NAME);
    }

    static WorldSitesData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldSitesData data = new WorldSitesData();
        data.chosen = tag.getBoolean(CHOSEN);
        CompoundTag saved = tag.getCompound(SITES);
        for (String key : saved.getAllKeys()) {
            data.sites.put(key, new ChunkPos(saved.getLong(key)));
        }
        if (tag.contains(LANDING)) {
            data.landing = BlockPos.of(tag.getLong(LANDING));
        }
        // Worlds saved before the flag existed that already have sites count as chosen.
        data.chosen |= !data.sites.isEmpty();
        return data;
    }

    /** Whether the site search already ran for this world (even if it found nothing). */
    boolean chosen() {
        return chosen;
    }

    void markChosen() {
        chosen = true;
        setDirty();
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

    Optional<BlockPos> landing() {
        return Optional.ofNullable(landing);
    }

    void setLanding(BlockPos landing) {
        this.landing = landing;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag saved = new CompoundTag();
        sites.forEach((name, site) -> saved.putLong(name, site.toLong()));
        tag.put(SITES, saved);
        tag.putBoolean(CHOSEN, chosen);
        if (landing != null) {
            tag.putLong(LANDING, landing.asLong());
        }
        return tag;
    }
}
