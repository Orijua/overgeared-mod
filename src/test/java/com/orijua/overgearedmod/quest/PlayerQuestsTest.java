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
        assertEquals(List.of(), loaded.seen());
        assertTrue(loaded.isActive(OTHER) && loaded.isCompleted(SECRET));
    }

    @Test
    void discoveriesAreNewUntilSeen() {
        PlayerQuests quests = PlayerQuests.EMPTY.discover(SECRET);
        assertTrue(quests.isNew(SECRET) && quests.hasNew());
        PlayerQuests seen = quests.seeAllDiscovered();
        assertFalse(seen.isNew(SECRET) || seen.hasNew(), "seen once the tab is opened");
        assertTrue(seen.isDiscovered(SECRET), "seeing it keeps it discovered");
        assertSame(seen, seen.seeAllDiscovered(), "nothing new, nothing changes");
        assertTrue(seen.discover(OTHER).isNew(OTHER), "a later discovery is new again");
        assertFalse(PlayerQuests.EMPTY.isNew(SECRET), "undiscovered quests are never new");
    }

    @Test
    void discoveringOneQuestDoesNotRevealAnother() {
        assertFalse(PlayerQuests.EMPTY.discover(OTHER).isDiscovered(SECRET));
    }

    @Test
    void deliveriesAddUpOnlyOnActiveQuests() {
        PlayerQuests quests = PlayerQuests.EMPTY.accept(OTHER, 0).deliver(OTHER, 40).deliver(OTHER, 24);
        assertEquals(64, quests.active().get(OTHER), "partial deliveries add up");
        assertSame(quests, quests.deliver(SECRET, 5), "a quest that isn't active takes nothing");
        assertSame(quests, quests.deliver(OTHER, 0), "nothing delivered, nothing changes");
    }
}
