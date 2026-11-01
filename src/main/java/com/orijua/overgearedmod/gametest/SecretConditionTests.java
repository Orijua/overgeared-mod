package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.insight.InsightRules;
import com.orijua.overgearedmod.feature.insight.Observation;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.quest.goal.KillGoal;
import com.orijua.overgearedmod.feature.rumor.RumorDefinition;
import com.orijua.overgearedmod.feature.rumor.Rumors;
import com.orijua.overgearedmod.feature.smelting.SmelterBlockEntity;
import com.orijua.overgearedmod.feature.smithing.GreatSuccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/** The secret titles and hidden quests that hang on night kills, the bellows and observing. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SecretConditionTests {
    private static final long MIDNIGHT = 18_000;

    private SecretConditionTests() {
    }

    /** Kills {@code count} mobs at midnight, all in this tick so the clock is back before any other test sees it. */
    private static void killAtNight(GameTestHelper helper, ServerPlayer player, EntityType<? extends LivingEntity> type, int count) {
        ServerLevel level = helper.getLevel();
        long time = level.getDayTime();
        level.setDayTime(MIDNIGHT);
        level.updateSkyBrightness();
        try {
            helper.assertTrue(level.isNight(), "midnight is night");
            for (int i = 0; i < count; i++) {
                TestSupport.kill(helper, player, type);
            }
        } finally {
            level.setDayTime(time);
            level.updateSkyBrightness();
        }
    }

    private static RumorDefinition rumor(GameTestHelper helper, String id) {
        return Definitions.get(helper.getLevel(), ModDatapackRegistries.RUMOR, Overgearedmod.id(id)).orElseThrow();
    }

    private static int nightKills(ServerPlayer player, Optional<EntityType<?>> type) {
        return GoalTracker.counters(player).get(KillGoal.counterKey(type, true));
    }

    private static boolean hasTitle(ServerPlayer player, String title) {
        return player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(Overgearedmod.id(title));
    }

    /** Night kills count twice: in the usual counters and in the night ones. Day kills only in the usual ones. */
    @GameTest(template = TestSupport.EMPTY)
    public static void nightKillsCountApart(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ServerLevel level = helper.getLevel();
        long time = level.getDayTime();
        level.setDayTime(6_000);
        level.updateSkyBrightness();
        TestSupport.killZombie(helper, player);
        level.setDayTime(time);
        level.updateSkyBrightness();
        helper.assertTrue(nightKills(player, Optional.empty()) == 0, "a noon kill is not a night kill");

        killAtNight(helper, player, EntityType.ZOMBIE, 1);
        helper.assertTrue(nightKills(player, Optional.empty()) == 1, "a midnight kill is");
        helper.assertTrue(nightKills(player, Optional.of(EntityType.ZOMBIE)) == 1, "and counts for its type too");
        helper.assertTrue(GoalTracker.counters(player).get(KillGoal.counterKey(Optional.empty())) == 2, "both are kills");
        helper.succeed();
    }

    /** 20 night kills reveal "Moonlit Hunt", which needs 10 more zombies at night; 50 earn Night Stalker. */
    @GameTest(template = TestSupport.EMPTY)
    public static void huntingAtNightRevealsMoonlitHunt(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation hunt = Overgearedmod.id("moonlit_hunt");
        killAtNight(helper, player, EntityType.SKELETON, 19);
        helper.assertFalse(QuestService.get(player).isDiscovered(hunt), "19 are not enough");
        killAtNight(helper, player, EntityType.SKELETON, 1);
        helper.assertTrue(QuestService.get(player).isDiscovered(hunt), "the 20th reveals it");

        helper.assertTrue(QuestService.accept(player, hunt) == QuestService.Result.SUCCESS, "it is accepted from the Quests tab");
        for (int i = 0; i < 10; i++) {
            TestSupport.killZombie(helper, player);
        }
        helper.assertFalse(QuestService.claim(player, hunt) == QuestService.Result.SUCCESS, "zombies by day don't count");
        killAtNight(helper, player, EntityType.ZOMBIE, 10);
        helper.assertTrue(QuestService.claim(player, hunt) == QuestService.Result.SUCCESS, "10 at night complete it");
        helper.assertTrue(Rumors.solved(player, rumor(helper, "moon_hunters")), "the chief's rumor is put to rest");

        helper.assertFalse(hasTitle(player, "night_stalker"), "30 night kills: no title yet");
        killAtNight(helper, player, EntityType.ZOMBIE, 20);
        helper.assertTrue(hasTitle(player, "night_stalker"), "50 night kills earn Night Stalker");
        helper.succeed();
    }

    /** Every Insight point gained by observing is a milestone: 10 reveal "The Smith's Gaze", 20 earn Keen Eye. */
    @GameTest(template = TestSupport.EMPTY)
    public static void observingRevealsTheSmithsGaze(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation gaze = Overgearedmod.id("smiths_gaze");
        Villager target = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        for (int point = 0; point < 10; point++) {
            Observation.forget(player); // a new day each time
            for (int i = 0; i < InsightRules.OBSERVE_TICKS; i++) {
                Observation.observe(player, target);
            }
        }
        target.discard();
        helper.assertTrue(GoalTracker.milestones(player, Observation.OBSERVED) == 10, "10 points, 10 milestones");
        helper.assertTrue(QuestService.get(player).isDiscovered(gaze), "and the quest appears");
        helper.assertFalse(hasTitle(player, "keen_eye"), "Keen Eye needs 20");
        helper.succeed();
    }

    /** Extra ingots from the bellows reveal "The Breathing Bellows" and silence Smith's rumor about them. */
    @GameTest(template = TestSupport.EMPTY)
    public static void bellowsExtrasRevealThePerfectBellows(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GoalTracker.milestone(player, SmelterBlockEntity.BELLOWS_EXTRA);
        helper.assertTrue(QuestService.get(player).isDiscovered(Overgearedmod.id("perfect_bellows")), "the first extra reveals it");
        helper.assertTrue(Rumors.solved(player, rumor(helper, "breathing_bellows")), "Smith's rumor is put to rest");
        for (int i = 0; i < 9; i++) {
            GoalTracker.milestone(player, SmelterBlockEntity.BELLOWS_EXTRA);
        }
        helper.assertTrue(hasTitle(player, "bellows_master"), "10 extras earn Master of the Bellows");
        helper.succeed();
    }

    /** A great success reveals "Steady Hand"; three earn Blessed by the Anvil. */
    @GameTest(template = TestSupport.EMPTY)
    public static void greatSuccessesRevealSteadyHand(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GoalTracker.milestone(player, GreatSuccess.MILESTONE);
        helper.assertTrue(QuestService.get(player).isDiscovered(Overgearedmod.id("steady_hand")), "one great success reveals it");
        GoalTracker.milestone(player, GreatSuccess.MILESTONE);
        helper.assertFalse(hasTitle(player, "anvil_blessed"), "two are not enough");
        GoalTracker.milestone(player, GreatSuccess.MILESTONE);
        helper.assertTrue(hasTitle(player, "anvil_blessed"), "three earn the title");
        helper.succeed();
    }
}
