package com.orijua.overgearedmod.feature.pagma;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.feature.quest.goal.KillGoal;
import com.orijua.overgearedmod.feature.quest.goal.MilestoneGoal;
import com.orijua.overgearedmod.feature.quest.goal.PlayerCounters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * The ids of Pagma's story (canon, caps. 1-3): Earl Ashur's secret request, the book in the Northern End Cave, its
 * appraisal, Ashur's anger and reading the book. The milestones are counted by {@code GoalTracker.milestone}; quests
 * and titles in the datapack listen to them.
 */
public final class PagmaStory {
    /** Ashur, once the player is somebody (Smith's chain done), tells them of his secret request. */
    public static final ResourceLocation ASHUR_SECRET = Overgearedmod.id("ashur_secret");
    /** Ashur gave the cave map: separate from the secret, because the map only exists once the world has a cave to point to. */
    public static final ResourceLocation ASHUR_MAP_GIVEN = Overgearedmod.id("ashur_map_given");
    /** The book was appraised: Pagma's legacy is revealed, and Ashur comes for it. */
    public static final ResourceLocation APPRAISED = Overgearedmod.id("appraised_pagma_book");
    public static final ResourceLocation BETRAYAL = Overgearedmod.id("ashur_betrayal");
    public static final ResourceLocation READ = Overgearedmod.id("read_pagma_book");

    public static final ResourceLocation SECRET_REQUEST = Overgearedmod.id("ashur_secret_request");
    public static final ResourceLocation ANGER = Overgearedmod.id("ashur_anger");
    public static final ResourceLocation LEGACY = Overgearedmod.id("pagma_legacy");
    /** The last quest of Smith's chain: after it, the player is worth Ashur's attention. */
    public static final ResourceLocation SMITH_CHAIN_END = Overgearedmod.id("quality_test");
    public static final ResourceLocation SUCCESSOR = Overgearedmod.id("legendary_smith_successor");
    public static final ResourceLocation BECAME_LEGEND = Overgearedmod.id("became_legend");
    public static final ResourceLocation NOBLE_KILLER = Overgearedmod.id("noble_killer");

    /** Canon (cap. 3): Pagma's successor starts from level 1 with 20 stat points more than anyone else. */
    public static final int BONUS_POINTS = 20;

    private PagmaStory() {
    }

    /**
     * Whether Ashur and his knights are after this player: they appraised the book and Ashur is still alive for them.
     * Read from the synced counters, so it works on both sides.
     */
    public static boolean isAshursEnemy(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        PlayerCounters counters = player.getData(ModAttachments.PLAYER_COUNTERS);
        return counters.get(MilestoneGoal.counterKey(BETRAYAL)) > 0 && !killedAshur(player);
    }

    public static boolean killedAshur(Player player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS).get(KillGoal.counterKey(Optional.of(ModEntities.EARL_ASHUR.get()))) > 0;
    }
}
