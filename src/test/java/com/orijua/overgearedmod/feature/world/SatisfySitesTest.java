package com.orijua.overgearedmod.feature.world;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fixed sites of the West Continent ({@code satisfy_map/west/sites.json}) and the rules the search gave them. */
class SatisfySitesTest {
    private static final SatisfySites SITES = SatisfySites.west();
    private static final SatisfyMap MAP = new SatisfyMap(SatisfyMapData.west());

    private static ChunkPos site(String id) {
        return SITES.get(id).orElseThrow(() -> new AssertionError("no " + id + " in sites.json"));
    }

    @Test
    void theFourPlacesAreThereAndNothingElse() {
        assertEquals(List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR, WorldSites.WINSTON),
                List.copyOf(SITES.all().keySet()));
    }

    @Test
    void bairanIsAtItsPixel() {
        assertEquals(new ChunkPos(0, 0), site(WorldSites.BAIRAN), "the map's origin is Bairan's pixel");
    }

    /** The rules of the search ({@link StartingVillage#search}) hold for the fixed sites. */
    @Test
    void theSitesKeepTheRulesOfTheSearch() {
        ChunkPos bairan = site(WorldSites.BAIRAN);
        ChunkPos cave = site(WorldSites.NORTH_END_CAVE);
        ChunkPos manor = site(WorldSites.ASHUR_MANOR);
        ChunkPos winston = site(WorldSites.WINSTON);
        for (ChunkPos chunk : SITES.all().values()) {
            assertTrue(MAP.isLand(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()), chunk + " is on the map's land");
            // The shore is at relief 2.5, the border of a coast cell; the cave stands on the coast (3), the rest inland.
            assertTrue(MAP.reliefAt(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()) > SatisfyMapData.COAST_RELIEF - 0.5,
                    chunk + " is above the shore");
        }
        assertEquals(SatisfyMapData.PLAINS, MAP.regionAt(bairan.getMiddleBlockX(), bairan.getMiddleBlockZ()), "Bairan on the plains");
        assertTrue(bairan.z - cave.z >= NorthEndCave.MIN_DISTANCE, "the cave far north of Bairan: " + cave);
        assertTrue(MAP.reliefAt(cave.getMiddleBlockX(), cave.getMiddleBlockZ()) < NorthEndCave.MAX_MAP_RELIEF, "the cave not on a mountain");
        ChunkPos glacier = NorthEndCave.mapOffset(SatisfyMapData.west());
        assertTrue(cave.getChessboardDistance(glacier) <= 15, "the cave near the Arctic Glacier label: " + cave + " vs " + glacier);
        int toManor = bairan.getChessboardDistance(manor);
        assertTrue(toManor >= AshurManor.MIN_DISTANCE && toManor <= AshurManor.MAX_DISTANCE, "the manor " + toManor + " chunks from Bairan");
        ChunkPos winstonOnTheMap = Winston.mapOffset(SatisfyMapData.west());
        assertTrue(winston.getChessboardDistance(winstonOnTheMap) <= 1, "Winston at its place on the map: " + winston + " vs " + winstonOnTheMap);
        for (ChunkPos other : List.of(bairan, cave, manor)) {
            assertTrue(winston.getChessboardDistance(other) >= Winston.KEEP_AWAY, "Winston away from " + other);
        }
    }

    private static SatisfySites read(String json) throws IOException {
        return SatisfySites.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aGoodFileIsRead() throws IOException {
        SatisfySites sites = read("{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [3, -4]}, {\"id\": \"b_2\", \"chunk\": [-1, 0], \"note\": \"x\"}]}");
        assertEquals(Map.of("a", new ChunkPos(3, -4), "b_2", new ChunkPos(-1, 0)), sites.all());
        assertTrue(sites.get("c").isEmpty());
    }

    @Test
    void badFilesAreRejected() {
        for (String bad : List.of("{\"version\": 2, \"sites\": []}",
                "{\"sites\": []}",
                "{\"version\": 1}",
                "{\"version\": 1, \"sites\": [{\"chunk\": [0, 0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"A\", \"chunk\": [0, 0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [0.5, 0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [\"0\", 0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [9999999, 0]}]}",
                "{\"version\": 1, \"sites\": [{\"id\": \"a\", \"chunk\": [0, 0]}, {\"id\": \"a\", \"chunk\": [1, 1]}]}",
                "{\"version\": 1, \"sites\": [")) {
            assertThrows(IOException.class, () -> read(bad), bad);
        }
    }
}
