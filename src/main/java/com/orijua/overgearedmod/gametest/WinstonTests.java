package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.winston.WinstonStory;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Khan's story in Winston (canon, caps. 24 and 36-42): only Pagma's successor gets through to him. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class WinstonTests {
    private WinstonTests() {
    }

    /** Khan sends away anyone who isn't Pagma's successor; the successor is heard, and the milestone counts it. */
    @GameTest(template = TestSupport.EMPTY)
    public static void khanOnlyListensToPagmasSuccessor(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        var khan = helper.spawn(ModEntities.KHAN.get(), 1, 1, 2);
        khan.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GoalTracker.milestones(player, WinstonStory.MET_KHAN) == 0, "a stranger is sent away");
        GoalTracker.milestone(player, PagmaStory.READ);
        helper.assertTrue(WinstonStory.isPagmasSuccessor(player), "reading the book makes the successor");
        khan.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GoalTracker.milestones(player, WinstonStory.MET_KHAN) == 1, "the successor is heard");
        helper.succeed();
    }

    /** Completing "Pagma's Legacy" without the book (as /satisfy debug quest complete does) also makes the successor. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theCompletedLegacyAlsoMakesTheSuccessor(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertFalse(WinstonStory.isPagmasSuccessor(player), "not yet");
        TestSupport.completed(player, PagmaStory.LEGACY.getPath());
        helper.assertTrue(WinstonStory.isPagmasSuccessor(player), "the legacy is enough");
        helper.succeed();
    }

    /** Smith gives Pagma's successor the map to Winston once, and nothing to anyone else. */
    @GameTest(template = TestSupport.EMPTY)
    public static void smithPointsTheSuccessorToWinston(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        long seed = helper.getLevel().getSeed();
        boolean hadWinston = WorldSites.siteFor(seed, WorldSites.WINSTON).isPresent();
        if (!hadWinston) {
            WorldSites.remember(seed, WorldSites.WINSTON, new ChunkPos(40, 0));
        }
        try {
            WinstonStory.talkToSmith(player);
            helper.assertTrue(player.getInventory().countItem(Items.FILLED_MAP) == 0, "no map before the legacy");
            GoalTracker.milestone(player, PagmaStory.READ);
            WinstonStory.talkToSmith(player);
            WinstonStory.talkToSmith(player);
            helper.assertTrue(player.getInventory().countItem(Items.FILLED_MAP) == 1, "one map to Winston");
        } finally {
            if (!hadWinston) {
                WorldSites.forget(seed, WorldSites.WINSTON);
            }
        }
        helper.succeed();
    }
}
