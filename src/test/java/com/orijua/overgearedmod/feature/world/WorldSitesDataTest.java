package com.orijua.overgearedmod.feature.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSitesDataTest {
    @Test
    void sitesAndLandingRoundTrip() {
        WorldSitesData data = new WorldSitesData();
        data.setSite(WorldSites.BAIRAN, new ChunkPos(-3, 5));
        data.setSite(WorldSites.NORTH_END_CAVE, new ChunkPos(10, -200));
        data.setSite(WorldSites.ASHUR_MANOR, new ChunkPos(-40, -7));
        data.setSite(WorldSites.WINSTON, new ChunkPos(33, 41));
        data.setLanding(new BlockPos(-12, 70, 85));
        data.markChosen();

        WorldSitesData loaded = WorldSitesData.load(data.save(new CompoundTag(), null), null);

        assertEquals(Map.copyOf(data.sites()), loaded.sites());
        assertEquals(4, loaded.sites().size());
        assertEquals(new BlockPos(-12, 70, 85), loaded.landing().orElseThrow());
        assertTrue(loaded.chosen());
    }

    @Test
    void noLandingWritesNoKey() {
        WorldSitesData data = new WorldSitesData();
        data.setSite(WorldSites.BAIRAN, new ChunkPos(1, 2));
        CompoundTag tag = data.save(new CompoundTag(), null);
        assertFalse(tag.contains("landing"));
        assertTrue(WorldSitesData.load(tag, null).landing().isEmpty());
    }

    @Test
    void emptyTagHasNothing() {
        WorldSitesData loaded = WorldSitesData.load(new CompoundTag(), null);
        assertTrue(loaded.sites().isEmpty());
        assertTrue(loaded.landing().isEmpty());
        assertFalse(loaded.chosen());
    }

    @Test
    void chosenWithoutSitesSurvives() {
        WorldSitesData data = new WorldSitesData();
        data.markChosen();
        WorldSitesData loaded = WorldSitesData.load(data.save(new CompoundTag(), null), null);
        assertTrue(loaded.chosen());
        assertTrue(loaded.sites().isEmpty());
    }

    @Test
    void oldSaveWithSitesCountsAsChosen() {
        WorldSitesData data = new WorldSitesData();
        data.setSite(WorldSites.BAIRAN, new ChunkPos(4, 4));
        CompoundTag old = data.save(new CompoundTag(), null);
        old.remove("chosen");
        assertTrue(WorldSitesData.load(old, null).chosen());
    }
}
