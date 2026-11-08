package com.orijua.overgearedmod.lang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Loads a language file of the mod from the resources, for tests. */
public final class LangFiles {
    private static final String LANG_DIR = "/assets/overgearedmod/lang/";

    private LangFiles() {
    }

    public static JsonObject load(String file) {
        InputStream stream = LangFiles.class.getResourceAsStream(LANG_DIR + file);
        assertNotNull(stream, "lang file not found: " + file);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("could not read " + file, e);
        }
    }
}
