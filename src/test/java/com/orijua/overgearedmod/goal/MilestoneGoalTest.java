package com.orijua.overgearedmod.goal;

import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestoneGoalTest {
    private static final ResourceLocation READ = ResourceLocation.fromNamespaceAndPath("overgearedmod", "read_pagma_book");

    @Test
    void countsOnlyItsOwnMilestone() {
        MilestoneGoal goal = new MilestoneGoal(READ, 1);
        PlayerCounters counters = PlayerCounters.EMPTY.increment(MilestoneGoal.counterKey(
                ResourceLocation.fromNamespaceAndPath("overgearedmod", "ashur_secret")));
        assertEquals(0, goal.progress(counters, PlayerStats.DEFAULT), "another milestone doesn't count");
        assertEquals(1, goal.progress(counters.increment(MilestoneGoal.counterKey(READ)), PlayerStats.DEFAULT));
    }

    @Test
    void aQuestOnlyCountsWhatHappensAfterItIsAccepted() {
        MilestoneGoal goal = new MilestoneGoal(READ, 1);
        PlayerCounters before = PlayerCounters.EMPTY.increment(MilestoneGoal.counterKey(READ));
        assertTrue(goal.cumulative());
        int stored = goal.startingValue(before, PlayerStats.DEFAULT);
        assertEquals(0, goal.questProgress(stored, before, PlayerStats.DEFAULT));
        assertEquals(1, goal.questProgress(stored, before.increment(MilestoneGoal.counterKey(READ)), PlayerStats.DEFAULT));
    }

    @Test
    void itsTextComesFromTheMilestonesName() {
        assertEquals("goal.overgearedmod.milestone.read_pagma_book",
                ((TranslatableContents) new MilestoneGoal(READ, 1).describe().getContents()).getKey());
    }
}
