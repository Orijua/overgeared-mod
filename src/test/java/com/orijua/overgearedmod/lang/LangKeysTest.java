package com.orijua.overgearedmod.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LangKeysTest {

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

    @Test
    void placeholdersMatchBetweenLanguages() {
        JsonObject en = load("en_us.json");
        JsonObject es = load("es_es.json");
        TreeSet<String> mismatched = new TreeSet<>();
        for (String key : en.keySet()) {
            if (!es.has(key)) {
                continue; // reported by englishAndSpanishHaveTheSameKeys
            }
            List<String> inEn = placeholders(en.get(key).getAsString());
            List<String> inEs = placeholders(es.get(key).getAsString());
            if (!inEn.equals(inEs)) {
                mismatched.add(key + " en=" + inEn + " es=" + inEs);
            }
        }
        assertTrue(mismatched.isEmpty(), "Placeholders differ between languages: " + mismatched);
    }

    /** The arguments a format string uses, as sorted "position:type" entries; "%%" is a literal and is ignored. */
    private static List<String> placeholders(String text) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        List<String> result = new ArrayList<>();
        int next = 1;
        while (matcher.find()) {
            if (matcher.group().equals("%%")) {
                continue;
            }
            String position = matcher.group(1);
            result.add((position != null ? position : String.valueOf(next++)) + ":" + matcher.group(2));
        }
        result.sort(null);
        return result;
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%%|%(?:(\\d+)\\$)?([sd])");

    private static JsonObject load(String file) {
        return LangFiles.load(file);
    }

    private static final Pattern LITERAL_KEY = Pattern.compile("translatable\\(\\s*\"([^\"]+)\"\\s*[,)]");

    /**
     * Every key written literally in {@code Component.translatable("...")} exists in the language file. Keys built by
     * concatenation or chosen with a ternary (e.g. {@code getDescriptionId() + ".desc"}) are not covered. The gametest
     * package is skipped: it isn't shown to players.
     */
    @Test
    void everyLiteralKeyInCodeExists() throws Exception {
        JsonObject en = load("en_us.json");
        Path root = Path.of("src", "main", "java");
        TreeSet<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().replace(File.separatorChar, '/').contains("/gametest/")).toList()) {
                Matcher matcher = LITERAL_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    if (!en.has(matcher.group(1))) {
                        missing.add(matcher.group(1) + " (" + file.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Keys used in code but missing in en_us.json: " + missing);
    }
}
