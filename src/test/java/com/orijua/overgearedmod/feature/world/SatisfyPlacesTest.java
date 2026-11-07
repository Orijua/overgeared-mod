package com.orijua.overgearedmod.feature.world;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyPlacesTest {
    private static final SatisfyMapData MAP = SatisfyMapData.west();
    private static final SatisfyPlaces PLACES = SatisfyPlaces.west();

    @Test
    void theRegistryHasTheCanonPlaces() {
        assertEquals(placesInTheFile(), PLACES.all().size(), "every entry of places.json is loaded: " + PLACES.ids());
        for (String id : List.of("bairan", "winston", "reidan", "patrian", "lilton_desert", "cogro_island", "titan", "rebeca",
                "behen_archipelago", "elf_forest", "eternal_mountains", "travian_sierra", "saharan_empire", "war_god_ruins")) {
            assertTrue(PLACES.get(id).isPresent(), id);
        }
    }

    @Test
    void idsAreUnique() {
        Set<String> seen = new HashSet<>();
        PLACES.all().forEach(place -> assertTrue(seen.add(place.id()), "twice: " + place.id()));
        String twice = """
                {"version": 1, "places": [
                  {"id": "a", "name": "A", "type": "city", "px": [1, 1], "certainty": "mapa", "land": true},
                  {"id": "a", "name": "B", "type": "city", "px": [2, 2], "certainty": "mapa", "land": true}]}""";
        assertThrows(IOException.class, () -> SatisfyPlaces.read(new ByteArrayInputStream(twice.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void everyPlaceIsOnTheMap() {
        for (SatisfyPlaces.Place place : PLACES.all()) {
            assertTrue(SatisfyPlaces.onMap(MAP, place), place.id() + " is off the map");
            int[] block = SatisfyPlaces.block(MAP, place);
            assertEquals(place.pixelX() / MAP.cellPx(), MAP.colAtBlock(block[0]), place.id());
            assertEquals(place.pixelZ() / MAP.cellPx(), MAP.rowAtBlock(block[1]), place.id());
        }
    }

    @Test
    void bairanIsTheOrigin() {
        assertArrayEquals(new int[]{0, 0}, SatisfyPlaces.block(MAP, PLACES.get("bairan").orElseThrow()));
        int[] winston = SatisfyPlaces.block(MAP, PLACES.get("winston").orElseThrow());
        assertTrue(winston[0] < 0 && winston[1] < 0, "Winston is north-west of Bairan");
    }

    @Test
    void landPlacesFallOnLand() {
        for (SatisfyPlaces.Place place : PLACES.all()) {
            if (!place.land()) {
                continue;
            }
            int col = place.pixelX() / MAP.cellPx();
            int row = place.pixelZ() / MAP.cellPx();
            assertTrue(MAP.isLandCell(col, row), place.id() + " is in the sea: relief " + MAP.reliefAtCell(col, row));
            assertTrue(MAP.regionAtCell(col, row) != SatisfyMapData.SEA, place.id() + " has the sea region");
            int[] landing = SatisfyPlaces.landing(MAP, place).orElseThrow(() -> new AssertionError(place.id() + ": no landing"));
            int landingCol = MAP.colAtBlock(landing[0]);
            int landingRow = MAP.rowAtBlock(landing[1]);
            assertTrue(MAP.isLandCell(landingCol, landingRow), place.id() + " lands on land");
            assertTrue(Math.abs(landingCol - col) <= SatisfyPlaces.SEARCH_CELLS && Math.abs(landingRow - row) <= SatisfyPlaces.SEARCH_CELLS,
                    place.id() + " lands near it");
            assertTrue(MAP.relief(landing[0], landing[1]) > 2.5, place.id() + " lands above the shore: " + MAP.relief(landing[0], landing[1]));
        }
    }

    @Test
    void seaPlacesHaveNoLanding() {
        SatisfyPlaces.Place sea = PLACES.get("red_sea").orElseThrow();
        assertFalse(sea.land());
        assertTrue(SatisfyPlaces.landing(MAP, sea).isEmpty());
        // A place said to be land but in open sea, far from any coast, has no landing either.
        SatisfyPlaces.Place lost = new SatisfyPlaces.Place("lost", "Lost", "island", sea.pixelX(), sea.pixelZ(), "mapa", true);
        assertTrue(SatisfyPlaces.landing(MAP, lost).isEmpty());
    }

    private static int placesInTheFile() {
        try (java.io.InputStream in = SatisfyPlaces.class.getResourceAsStream(SatisfyMapData.WEST + SatisfyPlaces.FILE)) {
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("places").size();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String place(String px, String land) {
        return "{\"version\": 1, \"places\": [{\"id\": \"x\", \"name\": \"x\", \"type\": \"city\", \"px\": " + px
                + ", \"certainty\": \"mapa\", \"land\": " + land + "}]}";
    }

    @Test
    void badFilesAreRefused() {
        Map<String, String> bad = new java.util.LinkedHashMap<>();
        bad.put("version", "{\"version\": 2, \"places\": []}");
        bad.put("id", "{\"version\": 1, \"places\": [{\"id\": \"Bad Id\", \"name\": \"x\", \"type\": \"city\", \"px\": [1, 1], \"certainty\": \"mapa\", \"land\": true}]}");
        bad.put("certainty", "{\"version\": 1, \"places\": [{\"id\": \"x\", \"name\": \"x\", \"type\": \"city\", \"px\": [1, 1], \"certainty\": \"seguro\", \"land\": true}]}");
        bad.put("px", place("[1]", "true"));
        bad.put("px 3", place("[1, 2, 3]", "true"));
        bad.put("px -1", place("[-1, 2]", "true"));
        bad.put("px fraction", place("[1.5, 2]", "true"));
        bad.put("px text", place("[\"a\", 2]", "true"));
        bad.put("land", place("[1, 1]", "1"));
        bad.put("land yes", place("[1, 1]", "\"yes\""));
        bad.put("field", "{\"version\": 1, \"places\": [{\"id\": \"x\"}]}");
        bad.put("places", "{\"version\": 1}");
        bad.put("empty", "{}");
        bad.forEach((what, json) -> {
            IOException e = assertThrows(IOException.class,
                    () -> SatisfyPlaces.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), what);
            String key = what.split(" ")[0];
            assertTrue(e.getMessage() != null && (e.getMessage().contains(key) || what.equals("empty") || what.equals("field")),
                    what + ": the message names the field: " + e.getMessage());
        });
        // A valid place with the same shape is accepted.
        assertDoesNotThrow(() -> SatisfyPlaces.read(new ByteArrayInputStream(place("[1, 1]", "true").getBytes(StandardCharsets.UTF_8))));
    }

    // --- landing on a synthetic map: 11x11 cells of 10 px, the place in the middle cell (5, 5) ----------------

    private static SatisfyMapData grid(int land, int landCol, int landRow) {
        byte[] relief = new byte[121];
        byte[] region = new byte[121];
        if (land >= 0) {
            relief[landRow * 11 + landCol] = (byte) land;
            region[landRow * 11 + landCol] = (byte) (land >= SatisfyMapData.COAST_RELIEF ? 1 : 0);
        }
        return new SatisfyMapData(11, 11, 2, 10, 55, 55, relief, region, Map.of(0, "sea", 1, "minecraft:plains"));
    }

    private static SatisfyPlaces.Place at(int col, int row) {
        return new SatisfyPlaces.Place("p", "P", "city", col * 10 + 5, row * 10 + 5, "mapa", true);
    }

    @Test
    void landingStopsAtSearchCells() {
        SatisfyPlaces.Place place = at(5, 5);
        assertTrue(SatisfyPlaces.landing(grid(5, 5 + SatisfyPlaces.SEARCH_CELLS, 5), place).isPresent(), "land at SEARCH_CELLS is found");
        assertTrue(SatisfyPlaces.landing(grid(5, 5 + SatisfyPlaces.SEARCH_CELLS + 1, 5), place).isEmpty(), "land one cell farther is not");
        assertTrue(SatisfyPlaces.landing(grid(-1, 0, 0), place).isEmpty(), "all sea");
    }

    @Test
    void landingUsesACoastCellWhenThereIsNoFirmLand() {
        // An island of one cell with exactly COAST_RELIEF: the coast is all there is.
        SatisfyMapData island = grid(SatisfyMapData.COAST_RELIEF, 6, 5);
        int[] landing = SatisfyPlaces.landing(island, at(5, 5)).orElseThrow();
        assertEquals(6, island.colAtBlock(landing[0]));
        assertEquals(5, island.rowAtBlock(landing[1]));
        // Exactly FIRM_RELIEF counts as firm land.
        SatisfyMapData firm = grid(SatisfyPlaces.FIRM_RELIEF, 6, 5);
        assertTrue(SatisfyPlaces.landing(firm, at(5, 5)).isPresent());
        // Below the coast it is water.
        assertTrue(SatisfyPlaces.landing(grid(SatisfyMapData.COAST_RELIEF - 1, 6, 5), at(5, 5)).isEmpty());
    }

    @Test
    void landingNextToTheEdgeOfTheGridDoesNotThrow() {
        SatisfyMapData data = grid(5, 0, 0);
        SatisfyPlaces.Place corner = at(0, 0);
        int[] landing = SatisfyPlaces.landing(data, corner).orElseThrow();
        assertEquals(0, data.colAtBlock(landing[0]));
        assertEquals(0, data.rowAtBlock(landing[1]));
        assertTrue(SatisfyPlaces.landing(grid(-1, 0, 0), corner).isEmpty());
    }

    @Test
    void aPixelOffTheGridIsNotOnTheMap() {
        SatisfyMapData data = grid(5, 5, 5);
        assertFalse(SatisfyPlaces.onMap(data, new SatisfyPlaces.Place("p", "P", "city", -1, 5, "mapa", true)));
        assertFalse(SatisfyPlaces.onMap(data, new SatisfyPlaces.Place("p", "P", "city", 5, -1, "mapa", true)));
        assertTrue(SatisfyPlaces.onMap(data, new SatisfyPlaces.Place("p", "P", "city", 0, 0, "mapa", true)));
        assertFalse(SatisfyPlaces.onMap(data, new SatisfyPlaces.Place("p", "P", "city", 110, 5, "mapa", true)));
    }

    @Test
    void chunkOffsetIsTheChunkDistanceFromBairan() {
        assertEquals(new net.minecraft.world.level.ChunkPos(0, 0), SatisfyPlaces.chunkOffset(MAP, PLACES.get("bairan").orElseThrow()));
        assertEquals(new net.minecraft.world.level.ChunkPos(-13, -6), SatisfyPlaces.chunkOffset(MAP, PLACES.get("winston").orElseThrow()));
    }
}
