package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the real {@code satisfy.json} (as a classpath resource, no registries) for a consistent lore map. */
class SatisfyDimensionJsonTest {
    private static JsonObject source() throws IOException {
        try (InputStream in = SatisfyDimensionJsonTest.class.getResourceAsStream("/data/overgearedmod/dimension/satisfy.json")) {
            assertNotNull(in, "satisfy.json must be on the classpath");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.getAsJsonObject("generator").getAsJsonObject("biome_source");
        }
    }

    @Test
    void rowsAreRectangularAndUseOnlyLegendLetters() throws IOException {
        JsonObject source = source();
        JsonArray rows = source.getAsJsonArray("rows");
        JsonObject legend = source.getAsJsonObject("legend");
        int width = rows.get(0).getAsString().length();
        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i).getAsString();
            assertEquals(width, row.length(), "row " + i + " has a different length");
            for (char letter : row.toCharArray()) {
                assertTrue(letter == SatisfyMap.NONE || legend.has(String.valueOf(letter)), "letter '" + letter + "' in row " + i + " is not in the legend");
            }
        }
    }

    @Test
    void theOriginIsBairansPlains() throws IOException {
        JsonObject source = source();
        JsonArray rows = source.getAsJsonArray("rows");
        int col = source.get("origin_col").getAsInt();
        int row = source.get("origin_row").getAsInt();
        assertEquals('P', rows.get(row).getAsString().charAt(col));
        assertEquals("minecraft:plains", source.getAsJsonObject("legend").get("P").getAsString());
    }

    @Test
    void theMapPassesItsOwnValidation() throws IOException {
        JsonObject source = source();
        java.util.List<String> rows = new java.util.ArrayList<>();
        source.getAsJsonArray("rows").forEach(e -> rows.add(e.getAsString()));
        new SatisfyMap(rows, source.get("cell").getAsInt(), source.get("origin_col").getAsInt(), source.get("origin_row").getAsInt());
    }
}
