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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyPlacesTest {
    private static final SatisfyMapData MAP = SatisfyMapData.west();
    private static final SatisfyPlaces PLACES = SatisfyPlaces.west();

    @Test
    void theRegistryHasTheCanonPlaces() {
        assertTrue(PLACES.all().size() >= 20, "places: " + PLACES.ids());
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

    @Test
    void badFilesAreRefused() {
        Map<String, String> bad = Map.of(
                "version", "{\"version\": 2, \"places\": []}",
                "id", "{\"version\": 1, \"places\": [{\"id\": \"Bad Id\", \"name\": \"x\", \"type\": \"city\", \"px\": [1, 1], \"certainty\": \"mapa\", \"land\": true}]}",
                "certainty", "{\"version\": 1, \"places\": [{\"id\": \"x\", \"name\": \"x\", \"type\": \"city\", \"px\": [1, 1], \"certainty\": \"seguro\", \"land\": true}]}",
                "px", "{\"version\": 1, \"places\": [{\"id\": \"x\", \"name\": \"x\", \"type\": \"city\", \"px\": [1], \"certainty\": \"mapa\", \"land\": true}]}",
                "field", "{\"version\": 1, \"places\": [{\"id\": \"x\"}]}");
        bad.forEach((what, json) -> assertThrows(IOException.class,
                () -> SatisfyPlaces.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), what));
    }
}
