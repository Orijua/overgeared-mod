package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.QuestDeadlines;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.skill.SkillService;
import com.orijua.overgearedmod.feature.smithing.ItemForgedEvent;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilMenu;
import com.orijua.overgearedmod.feature.winston.ContestRules;
import com.orijua.overgearedmod.feature.winston.ContestState;
import com.orijua.overgearedmod.feature.winston.EupheminaNpc;
import com.orijua.overgearedmod.feature.winston.KhanContest;
import com.orijua.overgearedmod.feature.winston.KhanNpc;
import com.orijua.overgearedmod.feature.winston.MeroThug;
import com.orijua.overgearedmod.feature.winston.WinstonStory;
import com.orijua.overgearedmod.feature.world.Satisfy;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
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
        Satisfy.standIn(helper.getLevel()); // the map is of the Satisfy dimension, which the test server doesn't have
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
            Satisfy.restoreStandIn();
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

    /**
     * Pagma's successor who saved Khan's smithy, with the contest accepted at Khan: Euphemina comes (next to Khan, as a
     * test has no Winston) and the player stands by her, on the "stage".
     */
    private static EupheminaNpc contestAccepted(GameTestHelper helper, ServerPlayer player) {
        KhanNpc khan = metKhan(helper, player);
        TestSupport.completed(player, WinstonStory.KHANS_RAGE.getPath());
        helper.assertTrue(QuestService.accept(player, KhanContest.KHANS_CONTEST, khan) == QuestService.Result.SUCCESS, "accepted at Khan");
        List<EupheminaNpc> found = helper.getLevel().getEntitiesOfClass(EupheminaNpc.class, khan.getBoundingBox().inflate(8), EupheminaNpc::isAlive);
        helper.assertTrue(found.size() == 1, "Euphemina comes, got " + found.size());
        EupheminaNpc euphemina = found.getFirst();
        player.moveTo(euphemina.getX() + 1, euphemina.getY(), euphemina.getZ());
        return euphemina;
    }

    private static void forgeByHand(ServerPlayer player, ItemRating rating, double precision) {
        NeoForge.EVENT_BUS.post(new ItemForgedEvent(player, new ItemStack(ModItems.JAFFA_SWORD.get()), rating, precision));
    }

    /**
     * The whole contest: Khan gives the materials, an automatic forge doesn't count, a precise Epic beats her roll, and
     * claiming at Khan makes the player his disciple (the title, with its smithing XP bonus).
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void theContestFromStartToEnd(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EupheminaNpc euphemina = contestAccepted(helper, player);
        KhanNpc khan = helper.getLevel().getEntitiesOfClass(KhanNpc.class, euphemina.getBoundingBox().inflate(8)).getFirst();
        helper.assertTrue(KhanContest.canStart(player), "ready to start");
        helper.assertTrue(KhanContest.start(player, 250), "started");
        helper.assertFalse(KhanContest.start(player, 250), "not twice");
        helper.assertTrue(player.getInventory().countItem(ModItems.JAFFA_INGOT.get()) == 2, "Khan's jaffa ingots");

        NeoForge.EVENT_BUS.post(new ItemForgedEvent(player, new ItemStack(ModItems.JAFFA_SWORD.get()), ItemRating.UNIQUE));
        helper.assertTrue(KhanContest.state(player).running(), "an automatic forge doesn't count");
        forgeByHand(player, ItemRating.EPIC, 0.87);
        helper.assertFalse(KhanContest.state(player).running(), "the piece is judged");
        helper.assertTrue(GoalTracker.milestones(player, KhanContest.CONTEST_WON) == 1, "287 beats 250");

        helper.assertTrue(QuestService.claim(player, KhanContest.KHANS_CONTEST, khan) == QuestService.Result.SUCCESS, "claimed at Khan");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(KhanContest.KHANS_DISCIPLE), "Khan's disciple");
        int before = ProgressionService.get(player).smithingXp();
        ProgressionService.addSmithingXp(player, 100);
        helper.assertTrue(ProgressionService.get(player).smithingXp() - before == 110, "+10 % smithing XP");
        helper.succeed();
    }

    /** On the stage during the contest the anvil refuses the automatic forge; the manual one still starts. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theContestAnvilOnlyForgesByHand(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        contestAccepted(helper, player);
        SmithingAnvilMenu menu = new SmithingAnvilMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), player.blockPosition()));
        menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(ModItems.JAFFA_INGOT.get(), 2));
        menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(Items.STICK));
        helper.assertTrue(menu.canForge(), "the Jaffa sword is previewed");
        KhanContest.start(player, 150);
        helper.assertTrue(KhanContest.forcesManual(player), "on the stage, in the contest");
        helper.assertFalse(menu.clickMenuButton(player, SmithingAnvilMenu.AUTO_FORGE_BUTTON), "no automatic forge in the contest");
        helper.assertFalse(menu.autoForging(), "nothing started");
        helper.assertTrue(menu.clickMenuButton(player, SmithingAnvilMenu.MANUAL_FORGE_BUTTON), "by hand it is");
        helper.succeed();
    }

    /** A piece forged away from the stage is not the contest's. */
    @GameTest(template = TestSupport.EMPTY)
    public static void onlyAPieceForgedOnTheStageCounts(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EupheminaNpc euphemina = contestAccepted(helper, player);
        KhanContest.start(player, 150);
        player.moveTo(euphemina.getX() + 20, euphemina.getY(), euphemina.getZ());
        forgeByHand(player, ItemRating.EPIC, 1);
        helper.assertTrue(KhanContest.state(player).running(), "still running");
        helper.succeed();
    }

    /** With rematches (a shared world's default), losing allows another try only the next day. */
    @GameTest(template = TestSupport.EMPTY)
    public static void losingGivesARematchTheNextDay(GameTestHelper helper) {
        Balance.withOverride(Balance.current().withContestLoss(Balance.ContestLoss.REMATCH), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            contestAccepted(helper, player);
            KhanContest.start(player, 300);
            forgeByHand(player, ItemRating.RARE, 0.5);
            helper.assertTrue(QuestService.get(player).isActive(KhanContest.KHANS_CONTEST), "the quest goes on");
            helper.assertFalse(KhanContest.canStart(player), "not again today");
            ContestState lost = KhanContest.state(player);
            player.setData(ModAttachments.KHAN_CONTEST, lost.lost(lost.lostDay() - 1));
            helper.assertTrue(KhanContest.canStart(player), "tomorrow, a rematch");
        });
        helper.succeed();
    }

    /** Failing for good (a single-player world's default): the Mero keeps the smithy and Khan is lost. */
    @GameTest(template = TestSupport.EMPTY)
    public static void losingForGoodFailsTheQuest(GameTestHelper helper) {
        Balance.withOverride(Balance.current().withContestLoss(Balance.ContestLoss.FAIL), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            contestAccepted(helper, player);
            KhanContest.start(player, 300);
            forgeByHand(player, ItemRating.EPIC, 1);
            helper.assertTrue(QuestService.get(player).isFailed(KhanContest.KHANS_CONTEST), "a tie goes to Euphemina: failed");
            helper.assertTrue(WinstonStory.hasLostKhan(player) && !WinstonStory.heardByKhan(player), "Khan no longer teaches them");
        });
        helper.succeed();
    }

    /** No piece when the time is up is a loss. */
    @GameTest(template = TestSupport.EMPTY)
    public static void whenTheContestTimeRunsOutItIsLost(GameTestHelper helper) {
        Balance.withOverride(Balance.current().withContestLoss(Balance.ContestLoss.REMATCH), () -> {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            contestAccepted(helper, player);
            KhanContest.start(player, 150);
            KhanContest.tick(player);
            helper.assertTrue(KhanContest.state(player).running(), "time left");
            long late = helper.getLevel().getGameTime() - ContestRules.TIME_LIMIT_TICKS - 1;
            player.setData(ModAttachments.KHAN_CONTEST, KhanContest.state(player).start(late, 150));
            KhanContest.tick(player);
            helper.assertFalse(KhanContest.state(player).running(), "time's up");
            helper.assertTrue(KhanContest.state(player).lostDay() >= 0, "lost");
        });
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
