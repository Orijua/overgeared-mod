package com.orijua.overgearedmod.feature.quest;

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

    @Test
    void completionsAreCounted() {
        PlayerQuests quests = PlayerQuests.EMPTY.accept(OTHER, 0).complete(OTHER).accept(OTHER, 0).complete(OTHER);
        assertEquals(2, quests.timesCompleted(OTHER));
        assertEquals(List.of(OTHER), quests.completed(), "listed as completed once");
        assertEquals(0, quests.timesCompleted(SECRET));
    }

    @Test
    void savesFromBeforeCountingStillCountACompletedQuestOnce() {
        PlayerQuests loaded = PlayerQuests.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"completed\": [\"test:other\"]}")).getOrThrow();
        assertEquals(1, loaded.timesCompleted(OTHER));
        assertEquals(List.of(), loaded.tracked());
    }

    @Test
    void acceptedQuestsAreFollowedWhileThereIsRoom() {
        PlayerQuests quests = PlayerQuests.EMPTY;
        for (int i = 0; i < PlayerQuests.MAX_TRACKED + 1; i++) {
            quests = quests.accept(ResourceLocation.fromNamespaceAndPath("test", "q" + i), 0);
        }
        assertEquals(PlayerQuests.MAX_TRACKED, quests.tracked().size());
        assertFalse(quests.isTracked(ResourceLocation.fromNamespaceAndPath("test", "q" + PlayerQuests.MAX_TRACKED)), "no room for the last");
    }

    @Test
    void followingTogglesOnlyActiveQuestsAndStopsWhenTheyEnd() {
        PlayerQuests quests = PlayerQuests.EMPTY.accept(OTHER, 0);
        assertTrue(quests.isTracked(OTHER));
        assertFalse(quests.toggleTracked(OTHER).isTracked(OTHER), "unfollowed");
        assertTrue(quests.toggleTracked(OTHER).toggleTracked(OTHER).isTracked(OTHER), "followed again");
        assertSame(quests, quests.toggleTracked(SECRET), "a quest that isn't active can't be followed");
        assertFalse(quests.complete(OTHER).isTracked(OTHER), "a completed quest stops being followed");
        assertFalse(quests.abandon(OTHER).isTracked(OTHER), "so does an abandoned one");
    }

    @Test
    void aFullHudFollowsNothingMore() {
        PlayerQuests quests = PlayerQuests.EMPTY;
        for (int i = 0; i < PlayerQuests.MAX_TRACKED; i++) {
            quests = quests.accept(ResourceLocation.fromNamespaceAndPath("test", "q" + i), 0);
        }
        quests = quests.accept(OTHER, 0);
        assertSame(quests, quests.toggleTracked(OTHER));
    }

    @Test
    void aDeadlineWarnsOnceOnItsLastDayAndIsOverdueAfter() {
        PlayerQuests quests = PlayerQuests.EMPTY.accept(OTHER, 0).withDeadline(OTHER, 1000);
        assertEquals(1000L, quests.deadline(OTHER).getAsLong());
        assertEquals(List.of(), quests.dueSoon(0, 500), "more than the window left");
        assertEquals(List.of(OTHER), quests.dueSoon(600, 500), "inside the window");
        assertEquals(List.of(), quests.warn(OTHER).dueSoon(600, 500), "warned once");
        assertEquals(List.of(), quests.overdue(999));
        assertEquals(List.of(OTHER), quests.overdue(1000));
        assertSame(PlayerQuests.EMPTY, PlayerQuests.EMPTY.withDeadline(OTHER, 5), "only active quests get a deadline");
    }

    @Test
    void aFailedQuestIsClosedForGood() {
        PlayerQuests failed = PlayerQuests.EMPTY.accept(OTHER, 0).withDeadline(OTHER, 1000).warn(OTHER).fail(OTHER);
        assertTrue(failed.isFailed(OTHER));
        assertFalse(failed.isActive(OTHER) || failed.isTracked(OTHER) || failed.isCompleted(OTHER));
        assertTrue(failed.deadline(OTHER).isEmpty() && failed.warned().isEmpty(), "the clock is gone");
        assertTrue(failed.overdue(Long.MAX_VALUE).isEmpty(), "it can't fail twice");
    }

    @Test
    void completingOrAbandoningStopsTheClock() {
        PlayerQuests timed = PlayerQuests.EMPTY.accept(OTHER, 0).withDeadline(OTHER, 1000);
        assertTrue(timed.complete(OTHER).deadline(OTHER).isEmpty());
        assertTrue(timed.abandon(OTHER).deadline(OTHER).isEmpty());
        assertFalse(timed.complete(OTHER).isFailed(OTHER));
    }

    @Test
    void savesFromBeforeTimeLimitsStillLoad() {
        PlayerQuests loaded = PlayerQuests.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"active\": {\"test:other\": 2}, \"tracked\": [\"test:other\"]}")).getOrThrow();
        assertTrue(loaded.deadlines().isEmpty() && loaded.failed().isEmpty() && loaded.warned().isEmpty());
        assertTrue(loaded.isActive(OTHER));
    }
}
