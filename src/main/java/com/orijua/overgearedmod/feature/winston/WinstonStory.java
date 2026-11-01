package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.quest.goal.MilestoneGoal;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Optional;

/**
 * Khan's story in Winston (canon, caps. 24 and 36-42). Only Pagma's successor can help him: Grid gets Khan's quest
 * because he inherited Pagma's humanitarian ideals. Smith points the successor to Winston with a map; talking to Khan
 * then counts the {@link #MET_KHAN} milestone, which his quests listen to. Before that, Khan is just a drunk who sends
 * everyone away.
 */
public final class WinstonStory {
    /** Smith told Pagma's successor about Khan and gave them the map to Winston. */
    public static final ResourceLocation WINSTON_MAP = Overgearedmod.id("winston_map");
    /** Pagma's successor talked to Khan in his smithy. */
    public static final ResourceLocation MET_KHAN = Overgearedmod.id("met_khan");
    /** How many lines Khan has for those he sends away (message.overgearedmod.khan_drunk.N). */
    static final int DRUNK_LINES = 3;

    private WinstonStory() {
    }

    /**
     * Whether the player inherited Pagma's legacy, the condition of Khan's quests in the canon: they read the book or
     * completed "Pagma's Legacy" (which reading completes; a debug command can complete it alone). Not the class, so a
     * later class change doesn't undo it. Read from the synced counters and quests: both sides.
     */
    public static boolean isPagmasSuccessor(Player player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS).get(MilestoneGoal.counterKey(PagmaStory.READ)) > 0
                || QuestService.get(player).isCompleted(PagmaStory.LEGACY);
    }

    /** Talking to Smith: the first time as Pagma's successor, he tells of his old friend in Winston and gives the map. */
    public static void talkToSmith(ServerPlayer player) {
        if (!isPagmasSuccessor(player) || GoalTracker.milestones(player, WINSTON_MAP) > 0) {
            return;
        }
        Optional<ItemStack> map = winstonMap(player);
        if (map.isEmpty()) {
            return; // a world without Winston
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.smith_tells_of_khan"), false);
        GoalTracker.milestone(player, WINSTON_MAP);
        if (!player.getInventory().add(map.get())) {
            player.drop(map.get(), false);
        }
    }

    /** Talking to Khan: the successor is heard (the milestone, every time); anyone else is sent away. */
    public static void talkToKhan(ServerPlayer player) {
        if (isPagmasSuccessor(player)) {
            GoalTracker.milestone(player, MET_KHAN);
        } else {
            int line = player.getRandom().nextInt(DRUNK_LINES) + 1;
            player.displayClientMessage(Component.translatable("message.overgearedmod.khan_drunk." + line), true);
        }
    }

    /** An explorer map centered on the world's Winston, with the city marked; empty if the world has none. */
    static Optional<ItemStack> winstonMap(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return Optional.empty();
        }
        return WorldSites.siteFor(level.getSeed(), WorldSites.WINSTON).map(chunk -> {
            BlockPos winston = chunk.getMiddleBlockPosition(64);
            ItemStack map = MapItem.create(level, winston.getX(), winston.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, winston, "+", MapDecorationTypes.RED_X);
            map.set(DataComponents.ITEM_NAME, Component.translatable("item.overgearedmod.winston_map"));
            return map;
        });
    }
}
