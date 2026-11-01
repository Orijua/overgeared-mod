package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What happened to Winston for the whole world, saved with the overworld (data/overgearedmod_winston.dat): whether
 * Khan died because "The Blacksmith's Rage" failed in a single-player world, and whether his smithy was rebuilt after
 * the contest was won.
 */
final class WinstonData extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_winston";
    private static final String KHAN_DEAD = "khan_dead";
    private static final String SMITHY_RESTORED = "smithy_restored";

    private boolean khanDead;
    private boolean smithyRestored;

    static WinstonData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WinstonData::new, WinstonData::load), NAME);
    }

    private static WinstonData load(CompoundTag tag, HolderLookup.Provider registries) {
        WinstonData data = new WinstonData();
        data.khanDead = tag.getBoolean(KHAN_DEAD);
        data.smithyRestored = tag.getBoolean(SMITHY_RESTORED);
        return data;
    }

    boolean khanDead() {
        return khanDead;
    }

    void setKhanDead() {
        khanDead = true;
        setDirty();
    }

    boolean smithyRestored() {
        return smithyRestored;
    }

    void setSmithyRestored() {
        smithyRestored = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(KHAN_DEAD, khanDead);
        tag.putBoolean(SMITHY_RESTORED, smithyRestored);
        return tag;
    }
}
