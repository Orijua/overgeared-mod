package com.orijua.overgearedmod.quest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestsTest {
    private static final ResourceLocation SECRET = ResourceLocation.fromNamespaceAndPath("test", "secret");
    private static final ResourceLocation OTHER = ResourceLocation.fromNamespaceAndPath("test", "other");

    @Test
    void discoverAddsOnceAndKeepsTheRest() {
        PlayerQuests quests = PlayerQuests.EMPTY.accept(OTHER, 3).discover(SECRET);
        assertTrue(quests.isDiscovered(SECRET));
        assertTrue(quests.isActive(OTHER), "active quests are kept");
        assertSame(quests, quests.discover(SECRET), "discovering twice changes nothing");
        assertTrue(quests.complete(OTHER).isDiscovered(SECRET), "completing another quest keeps the discovery");
    }

    @Test
    void savesFromBeforeHiddenQuestsStillLoad() {
        PlayerQuests loaded = PlayerQuests.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"active\": {\"test:other\": 2}, \"completed\": [\"test:secret\"]}")).getOrThrow();
        assertEquals(List.of(), loaded.discovered());
        assertTrue(loaded.isActive(OTHER) && loaded.isCompleted(SECRET));
    }

    @Test
    void discoveringOneQuestDoesNotRevealAnother() {
        assertFalse(PlayerQuests.EMPTY.discover(OTHER).isDiscovered(SECRET));
    }
}
