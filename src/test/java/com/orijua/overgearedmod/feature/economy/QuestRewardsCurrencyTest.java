package com.orijua.overgearedmod.feature.economy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No quest pays or lends the vanilla emerald: the Satisfy world has its own currency. */
class QuestRewardsCurrencyTest {
    private static final Path QUESTS = Path.of("src/main/resources/data/overgearedmod/overgearedmod/quest");

    @Test
    void noQuestRewardOrLoanIsTheVanillaEmerald() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(QUESTS)) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty(), "no quest files found in " + QUESTS.toAbsolutePath());
        for (Path file : files) {
            JsonObject quest;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                quest = JsonParser.parseReader(reader).getAsJsonObject();
            }
            for (String key : new String[]{"item_rewards", "loaned_items"}) {
                if (quest.has(key)) {
                    for (JsonElement item : quest.getAsJsonArray(key)) {
                        String id = item.getAsJsonObject().get("id").getAsString();
                        assertTrue(!"minecraft:emerald".equals(id), file.getFileName() + " pays " + key + " in the vanilla emerald");
                    }
                }
            }
        }
    }
}
