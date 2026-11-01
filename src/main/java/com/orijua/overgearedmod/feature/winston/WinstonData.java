package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What happened to Winston for the whole world, saved with the overworld (data/overgearedmod_winston.dat): whether
 * Khan died because "The Blacksmith's Rage" failed in a single-player world.
 */
final class WinstonData extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_winston";
    private static final String KHAN_DEAD = "khan_dead";

    private boolean khanDead;

    static WinstonData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WinstonData::new, WinstonData::load), NAME);
    }

    private static WinstonData load(CompoundTag tag, HolderLookup.Provider registries) {
        WinstonData data = new WinstonData();
        data.khanDead = tag.getBoolean(KHAN_DEAD);
        return data;
    }

    boolean khanDead() {
        return khanDead;
    }

    void setKhanDead() {
        khanDead = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(KHAN_DEAD, khanDead);
        return tag;
    }
}
