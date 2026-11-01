package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.quest.QuestDeadlines;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.skill.SkillService;
import com.orijua.overgearedmod.feature.winston.KhanNpc;
import com.orijua.overgearedmod.feature.winston.MeroThug;
import com.orijua.overgearedmod.feature.winston.WinstonStory;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Khan's story in Winston (canon, caps. 24 and 36-42): only Pagma's successor gets through to him. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class WinstonTests {
    private static final ResourceLocation BLACKSMITH_RAGE = Overgearedmod.id("blacksmith_rage");

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

    /** Pagma's successor who has met Khan, with Khan next to them: "The Blacksmith's Rage" is discovered. */
    private static KhanNpc metKhan(GameTestHelper helper, ServerPlayer player) {
        KhanNpc khan = helper.spawn(ModEntities.KHAN.get(), 1, 1, 2);
        GoalTracker.milestone(player, PagmaStory.READ);
        khan.interact(player, InteractionHand.MAIN_HAND);
        return khan;
    }

    /** The thugs around Khan: some stand outside the small test area. */
    private static List<MeroThug> thugs(GameTestHelper helper, KhanNpc khan) {
        return helper.getLevel().getEntitiesOfClass(MeroThug.class, khan.getBoundingBox().inflate(16), MeroThug::isAlive);
    }

    /**
     * The whole quest (canon, cap. 24): accepting it creates the skill and brings 5 thugs who only the player can hurt;
     * it can't be abandoned; the last thug drops the contract, and tearing it up lets Khan's quest be claimed.
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void theBlacksmithsRageFromStartToEnd(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        KhanNpc khan = metKhan(helper, player);
        helper.assertTrue(QuestService.get(player).isDiscovered(WinstonStory.KHANS_RAGE), "meeting Khan reveals his quest");
        helper.assertTrue(QuestService.accept(player, WinstonStory.KHANS_RAGE, khan) == QuestService.Result.SUCCESS, "accepted at Khan");
        helper.assertTrue(SkillService.get(player).knows(BLACKSMITH_RAGE), "accepting creates the Blacksmith's Rage");
        helper.assertTrue(QuestService.get(player).deadline(WinstonStory.KHANS_RAGE).isPresent(), "the clock runs");
        helper.assertTrue(QuestService.abandon(player, WinstonStory.KHANS_RAGE) == QuestService.Result.CANNOT_ABANDON, "no way out");

        List<MeroThug> thugs = thugs(helper, khan);
        helper.assertTrue(thugs.size() == WinstonStory.THUGS, "5 thugs in the smithy, got " + thugs.size());
        ServerPlayer stranger = TestSupport.fakePlayer(helper);
        MeroThug first = thugs.getFirst();
        first.hurt(stranger.damageSources().playerAttack(stranger), 10.0f);
        helper.assertTrue(first.getHealth() == first.getMaxHealth(), "nobody else can hurt them");
        for (MeroThug thug : thugs) {
            thug.hurt(player.damageSources().playerAttack(player), 1000.0f);
        }
        List<ItemEntity> contracts = helper.getLevel().getEntitiesOfClass(ItemEntity.class, khan.getBoundingBox().inflate(16),
                item -> item.getItem().is(ModItems.TRANSFER_CONTRACT.get()));
        helper.assertTrue(contracts.size() == 1, "the last one drops the contract, got " + contracts.size());
        contracts.getFirst().discard();

        helper.assertTrue(QuestService.claim(player, WinstonStory.KHANS_RAGE, khan) == QuestService.Result.NOT_DONE, "not yet");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TRANSFER_CONTRACT.get()));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "torn up");
        helper.assertTrue(QuestService.claim(player, WinstonStory.KHANS_RAGE, khan) == QuestService.Result.SUCCESS, "Khan's smithy is saved");
        helper.succeed();
    }

    /** With a day left a warning comes once; when the time runs out the quest fails and Khan is lost for that player. */
    @GameTest(template = TestSupport.EMPTY)
    public static void whenTheTimeRunsOutKhanIsLost(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        KhanNpc khan = metKhan(helper, player);
        QuestService.accept(player, WinstonStory.KHANS_RAGE, khan);
        long now = QuestDeadlines.now(player);
        player.setData(ModAttachments.PLAYER_QUESTS, QuestService.get(player).withDeadline(WinstonStory.KHANS_RAGE, now + 100));
        QuestDeadlines.check(player);
        helper.assertTrue(QuestService.get(player).warned().contains(WinstonStory.KHANS_RAGE), "warned on the last day");
        helper.assertTrue(QuestService.get(player).isActive(WinstonStory.KHANS_RAGE), "still running");

        player.setData(ModAttachments.PLAYER_QUESTS, QuestService.get(player).withDeadline(WinstonStory.KHANS_RAGE, now - 1));
        QuestDeadlines.check(player);
        helper.assertTrue(QuestService.get(player).isFailed(WinstonStory.KHANS_RAGE), "failed");
        helper.assertTrue(WinstonStory.hasLostKhan(player) && !WinstonStory.heardByKhan(player), "Khan no longer speaks to them");
        helper.assertFalse(QuestService.canAccept(player, WinstonStory.KHANS_RAGE,
                Definitions.get(helper.getLevel(), ModDatapackRegistries.QUEST, WinstonStory.KHANS_RAGE).orElseThrow()), "never again");
        helper.assertTrue(khan.isAlive(), "a test server isn't a single-player world: Khan lives on for the others");
        thugs(helper, khan).forEach(MeroThug::discard);
        helper.succeed();
    }

    /** Khan dead for the whole world: a grave sign where he stood. */
    @GameTest(template = TestSupport.EMPTY)
    public static void khanIsBuriedWhereHeStood(GameTestHelper helper) {
        KhanNpc khan = helper.spawn(ModEntities.KHAN.get(), 1, 1, 2);
        BlockPos at = khan.blockPosition();
        WinstonStory.bury(khan);
        helper.assertTrue(khan.isRemoved(), "Khan is gone");
        helper.assertTrue(helper.getLevel().getBlockState(at).is(Blocks.OAK_SIGN), "a grave sign");
        helper.succeed();
    }
}
