package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.QuestService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class QuestTests {
    private static final ResourceLocation ZOMBIE_PLAGUE = Overgearedmod.id("zombie_plague");

    private QuestTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyKillsAfterAcceptingCount(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        for (int i = 0; i < 3; i++) {
            TestSupport.killZombie(helper, player); // before accepting: must not count
        }
        helper.assertTrue(QuestService.accept(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "quest should be acceptable");
        helper.assertTrue(QuestService.claim(player, ZOMBIE_PLAGUE) == QuestService.Result.NOT_DONE, "no kills since accepting yet");

        for (int i = 0; i < 5; i++) {
            TestSupport.killZombie(helper, player);
        }
        helper.assertTrue(QuestService.claim(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "5 kills complete the quest");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) == 3, "reward bread should be in the inventory");
        helper.assertTrue(ProgressionService.get(player).level() > 1, "100 XP should level the player up");
        helper.assertTrue(QuestService.accept(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "zombie_plague is repeatable");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void nonRepeatableQuestCannotBeRetaken(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation path = Overgearedmod.id("path_of_the_adventurer");
        QuestService.accept(player, path);
        ProgressionService.awardXp(player, 500); // level goal: absolute, not counted from acceptance
        helper.assertTrue(QuestService.claim(player, path) == QuestService.Result.SUCCESS, "reaching level 3 completes it");
        helper.assertTrue(QuestService.accept(player, path) == QuestService.Result.NOT_AVAILABLE, "non-repeatable quest stays done");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void chainedQuestUnlocksWhenThePreviousIsCompleted(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation commission = Overgearedmod.id("first_commission");
        ResourceLocation qualityTest = Overgearedmod.id("quality_test");
        ResourceLocation nightWatch = Overgearedmod.id("night_watch");
        helper.assertTrue(QuestService.accept(player, qualityTest) == QuestService.Result.NOT_AVAILABLE, "locked until first_commission is done");

        QuestService.accept(player, commission);
        TestSupport.forge(player, ItemRating.NORMAL);
        helper.assertTrue(QuestService.accept(player, qualityTest) == QuestService.Result.NOT_AVAILABLE, "active is not enough: it must be claimed");
        QuestService.claim(player, commission);
        helper.assertTrue(QuestService.accept(player, qualityTest) == QuestService.Result.SUCCESS, "unlocked once first_commission is claimed");
        helper.assertTrue(QuestService.accept(player, nightWatch) == QuestService.Result.NOT_AVAILABLE, "the next link stays locked");

        TestSupport.forge(player, ItemRating.RARE);
        TestSupport.forge(player, ItemRating.EPIC);
        helper.assertTrue(QuestService.claim(player, qualityTest) == QuestService.Result.SUCCESS, "two rare-or-better forges complete it");
        helper.assertTrue(QuestService.accept(player, nightWatch) == QuestService.Result.SUCCESS, "night_watch unlocks after quality_test");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void abandonedQuestCanBeTakenAgain(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation commission = Overgearedmod.id("first_commission");
        QuestService.accept(player, commission);
        helper.assertTrue(QuestService.abandon(player, commission) == QuestService.Result.SUCCESS, "active quest can be abandoned");
        helper.assertFalse(QuestService.get(player).isActive(commission), "abandoned quest is no longer active");
        helper.assertTrue(QuestService.accept(player, commission) == QuestService.Result.SUCCESS, "and can be accepted again");
        helper.succeed();
    }
}
