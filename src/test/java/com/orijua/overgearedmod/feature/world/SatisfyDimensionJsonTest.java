package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Checks the real {@code satisfy.json} (as a classpath resource, no registries) against the map it names. */
class SatisfyDimensionJsonTest {
    private static JsonObject json(String path) throws IOException {
        try (InputStream in = SatisfyDimensionJsonTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " must be on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject generator() throws IOException {
        return json("/data/overgearedmod/dimension/satisfy.json").getAsJsonObject("generator");
    }

    private static JsonObject source() throws IOException {
        return generator().getAsJsonObject("biome_source");
    }

    @Test
    void theTerrainFollowsTheSameMap() throws IOException {
        assertEquals("overgearedmod:satisfy", generator().get("settings").getAsString(), "the map-driven noise settings");
        for (String channel : new String[]{"continents", "erosion", "ridges"}) {
            JsonObject function = json("/data/overgearedmod/worldgen/density_function/satisfy/" + channel + ".json");
            assertEquals("minecraft:flat_cache", function.get("type").getAsString());
            JsonObject terrain = function.getAsJsonObject("argument");
            assertEquals("overgearedmod:satisfy_terrain", terrain.get("type").getAsString());
            assertEquals(channel, terrain.get("channel").getAsString());
            assertEquals(source().get("map").getAsString(), terrain.get("map").getAsString(), channel + " reads the biomes' map");
            assertEquals("minecraft:overworld/" + channel, terrain.get("noise").getAsString(), channel + " mixes in vanilla's noise");
        }
    }

    @Test
    void theOldLetterGridIsGone() throws IOException {
        JsonObject source = source();
        for (String field : new String[]{"rows", "cell", "origin_col", "origin_row"}) {
            assertFalse(source.has(field), "no '" + field + "' any more: the map is satisfy_map/west");
        }
        assertEquals("minecraft:multi_noise", source.getAsJsonObject("outside").get("type").getAsString(), "the sea is vanilla's");
    }

    @Test
    void theMapIsTheWestContinent() throws IOException {
        assertEquals("overgearedmod:west", source().get("map").getAsString());
        SatisfyMapData.load("/data/overgearedmod/satisfy_map/west/");
    }

    @Test
    void theLegendIsTheMapsLegendWithoutTheSea() throws IOException {
        Map<Integer, String> legend = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : source().getAsJsonObject("legend").entrySet()) {
            legend.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
        }
        Map<Integer, String> expected = new TreeMap<>(SatisfyMapData.west().legend());
        expected.remove(SatisfyMapData.SEA);
        assertEquals(expected, legend, "every land region has the biome meta.json gives it; region 0 goes to 'outside'");
        assertEquals("minecraft:plains", legend.get(SatisfyMapData.PLAINS));
        assertEquals("minecraft:badlands", legend.get(9));
        assertEquals("minecraft:stony_peaks", legend.get(10));
        assertEquals("minecraft:frozen_peaks", legend.get(11));
    }
}
