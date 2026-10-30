package com.orijua.overgearedmod.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LangKeysTest {
    private static final String LANG_DIR = "/assets/overgearedmod/lang/";

    @Test
    void englishAndSpanishHaveTheSameKeys() {
        JsonObject en = load("en_us.json");
        JsonObject es = load("es_es.json");

        TreeSet<String> missingInEs = new TreeSet<>(en.keySet());
        missingInEs.removeAll(es.keySet());
        TreeSet<String> missingInEn = new TreeSet<>(es.keySet());
        missingInEn.removeAll(en.keySet());

        if (!missingInEs.isEmpty() || !missingInEn.isEmpty()) {
            fail("Missing in es_es.json: " + missingInEs + "\nMissing in en_us.json: " + missingInEn);
        }
    }

    @Test
    void noValueIsEmpty() {
        for (String file : List.of("en_us.json", "es_es.json")) {
            TreeSet<String> empty = new TreeSet<>();
            for (Map.Entry<String, JsonElement> entry : load(file).entrySet()) {
                if (entry.getValue().getAsString().isBlank()) {
                    empty.add(entry.getKey());
                }
            }
            assertTrue(empty.isEmpty(), "Empty values in " + file + ": " + empty);
        }
    }

    private static JsonObject load(String file) {
        InputStream stream = LangKeysTest.class.getResourceAsStream(LANG_DIR + file);
        assertNotNull(stream, "lang file not found: " + file);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("could not read " + file, e);
        }
    }
}
