package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.npc.VillageChiefNpc;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.LoanGuard;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.world.Satisfy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class QuestTests {
    private static final ResourceLocation ZOMBIE_PLAGUE = Overgearedmod.id("zombie_plague");

    private QuestTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyKillsAfterAcceptingCount(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = TestSupport.chief(helper);
        for (int i = 0; i < 3; i++) {
            TestSupport.killZombie(helper, player); // before accepting: must not count
        }
        helper.assertTrue(QuestService.accept(player, ZOMBIE_PLAGUE, chief) == QuestService.Result.SUCCESS, "quest should be acceptable");
        helper.assertTrue(QuestService.claim(player, ZOMBIE_PLAGUE, chief) == QuestService.Result.NOT_DONE, "no kills since accepting yet");

        for (int i = 0; i < 5; i++) {
            TestSupport.killZombie(helper, player);
        }
        helper.assertTrue(QuestService.claim(player, ZOMBIE_PLAGUE, chief) == QuestService.Result.SUCCESS, "5 kills complete the quest");
        helper.assertTrue(player.getInventory().countItem(Items.BREAD) == 3, "reward bread should be in the inventory");
        helper.assertTrue(ProgressionService.get(player).level() > 1, "100 XP should level the player up");
        helper.assertTrue(QuestService.accept(player, ZOMBIE_PLAGUE, chief) == QuestService.Result.SUCCESS, "zombie_plague is repeatable");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void nonRepeatableQuestCannotBeRetaken(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = TestSupport.chief(helper);
        ResourceLocation path = Overgearedmod.id("path_of_the_adventurer");
        QuestService.accept(player, path, chief);
        ProgressionService.awardXp(player, 500); // level goal: absolute, not counted from acceptance
        helper.assertTrue(QuestService.claim(player, path, chief) == QuestService.Result.SUCCESS, "reaching level 3 completes it");
        helper.assertTrue(QuestService.accept(player, path, chief) == QuestService.Result.NOT_AVAILABLE, "non-repeatable quest stays done");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void chainedQuestUnlocksWhenThePreviousIsCompleted(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlacksmithNpc smith = TestSupport.smith(helper);
        ResourceLocation commission = Overgearedmod.id("first_commission");
        ResourceLocation qualityTest = Overgearedmod.id("quality_test");
        helper.assertTrue(QuestService.accept(player, commission, smith) == QuestService.Result.NOT_AVAILABLE,
                "locked until Smith's Jaffa sword quest is done");
        TestSupport.completed(player, "smith_jaffa_sword");
        helper.assertTrue(QuestService.accept(player, qualityTest, smith) == QuestService.Result.NOT_AVAILABLE,
                "locked until first_commission is done");

        QuestService.accept(player, commission, smith);
        TestSupport.forge(player, ItemRating.NORMAL);
        helper.assertTrue(QuestService.accept(player, qualityTest, smith) == QuestService.Result.NOT_AVAILABLE,
                "active is not enough: it must be claimed");
        QuestService.claim(player, commission, smith);
        helper.assertTrue(QuestService.accept(player, qualityTest, smith) == QuestService.Result.SUCCESS,
                "unlocked once first_commission is claimed");

        TestSupport.forge(player, ItemRating.RARE);
        TestSupport.forge(player, ItemRating.EPIC);
        helper.assertTrue(QuestService.claim(player, qualityTest, smith) == QuestService.Result.SUCCESS, "two rare-or-better forges complete it");
        helper.succeed();
    }

    /** The chief's chain: the night watch opens once the zombie plague has been dealt with. */
    @GameTest(template = TestSupport.EMPTY)
    public static void nightWatchFollowsTheZombiePlague(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = TestSupport.chief(helper);
        ResourceLocation nightWatch = Overgearedmod.id("night_watch");
        helper.assertTrue(QuestService.accept(player, nightWatch, chief) == QuestService.Result.NOT_AVAILABLE, "locked at first");
        QuestService.accept(player, ZOMBIE_PLAGUE, chief);
        for (int i = 0; i < 5; i++) {
            TestSupport.killZombie(helper, player);
        }
        QuestService.claim(player, ZOMBIE_PLAGUE, chief);
        helper.assertTrue(QuestService.accept(player, nightWatch, chief) == QuestService.Result.SUCCESS, "open after the plague");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void hiddenQuestAppearsAfterItsSecretGoalAndGrantsTheHiddenClass(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation call = Overgearedmod.id("call_of_the_night");
        helper.assertTrue(QuestService.accept(player, call) == QuestService.Result.NOT_AVAILABLE, "hidden before its secret goal");

        for (int i = 0; i < 29; i++) {
            TestSupport.kill(helper, player, EntityType.SKELETON);
        }
        helper.assertFalse(QuestService.get(player).isDiscovered(call), "29 skeletons are not enough");
        TestSupport.kill(helper, player, EntityType.SKELETON);
        helper.assertTrue(QuestService.get(player).isDiscovered(call), "the 30th skeleton reveals it");

        // A hidden class quest has no giver: it is accepted and claimed from the Quests tab.
        helper.assertTrue(QuestService.accept(player, call) == QuestService.Result.SUCCESS, "discovered quests can be accepted");
        for (int i = 0; i < 15; i++) {
            TestSupport.kill(helper, player, EntityType.SKELETON);
        }
        helper.assertTrue(QuestService.claim(player, call) == QuestService.Result.SUCCESS, "15 more skeletons complete it");
        helper.assertTrue(ClassService.current(player).filter(Overgearedmod.id("night_hunter")::equals).isPresent(),
                "claiming grants the hidden class");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void abandonedQuestCanBeTakenAgain(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = TestSupport.chief(helper);
        QuestService.accept(player, ZOMBIE_PLAGUE, chief);
        helper.assertTrue(QuestService.abandon(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "active quest can be abandoned anywhere");
        helper.assertFalse(QuestService.get(player).isActive(ZOMBIE_PLAGUE), "abandoned quest is no longer active");
        helper.assertTrue(QuestService.accept(player, ZOMBIE_PLAGUE, chief) == QuestService.Result.SUCCESS, "and can be accepted again");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void followedQuestsOnTheHud(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = TestSupport.chief(helper);
        helper.assertTrue(QuestService.toggleTracked(player, ZOMBIE_PLAGUE) == QuestService.Result.NOT_ACTIVE,
                "only accepted quests can be followed");
        QuestService.accept(player, ZOMBIE_PLAGUE, chief);
        helper.assertTrue(QuestService.get(player).isTracked(ZOMBIE_PLAGUE), "an accepted quest is followed at once");
        helper.assertTrue(QuestService.toggleTracked(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "unfollow");
        helper.assertFalse(QuestService.get(player).isTracked(ZOMBIE_PLAGUE), "no longer on the HUD");

        // Three other quests fill the HUD, so the plague can't be followed again until one is dropped.
        TestSupport.completed(player, "smith_jaffa_sword");
        QuestService.accept(player, Overgearedmod.id("path_of_the_adventurer"), chief);
        QuestService.accept(player, Overgearedmod.id("first_commission"), TestSupport.smith(helper));
        player.setData(ModAttachments.PLAYER_QUESTS, QuestService.get(player).accept(Overgearedmod.id("night_watch"), 0));
        helper.assertTrue(QuestService.toggleTracked(player, ZOMBIE_PLAGUE) == QuestService.Result.TRACKING_FULL, "the HUD is full");
        QuestService.abandon(player, Overgearedmod.id("night_watch"));
        helper.assertTrue(QuestService.toggleTracked(player, ZOMBIE_PLAGUE) == QuestService.Result.SUCCESS, "room again");

        for (int i = 0; i < 5; i++) {
            TestSupport.killZombie(helper, player);
        }
        QuestService.claim(player, ZOMBIE_PLAGUE, chief);
        PlayerQuests quests = QuestService.get(player);
        helper.assertFalse(quests.isTracked(ZOMBIE_PLAGUE), "a claimed quest leaves the HUD");
        helper.assertTrue(quests.timesCompleted(ZOMBIE_PLAGUE) == 1, "and its completion is counted");
        helper.succeed();
    }

    private static final ResourceLocation TALENTS = Overgearedmod.id("bairan_talents");

    private static ItemStack loan(ResourceLocation quest) {
        ItemStack sword = new ItemStack(ModItems.RULERS_SWORD.get());
        sword.set(ModDataComponents.LOANED_FOR, quest);
        return sword;
    }

    private static VillageChiefNpc talentsChief(GameTestHelper helper, ServerPlayer player) {
        VillageChiefNpc chief = TestSupport.chief(helper);
        player.moveTo(chief.position());
        TestSupport.completed(player, "watch_commander");
        return chief;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanInTheEnderChestIsReclaimedOnAbandon(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = talentsChief(helper, player);
        helper.assertTrue(QuestService.accept(player, TALENTS, chief) == QuestService.Result.SUCCESS, "accepted");
        player.getEnderChestInventory().addItem(loan(TALENTS));
        player.containerMenu.setCarried(loan(TALENTS));
        helper.assertTrue(QuestService.abandon(player, TALENTS) == QuestService.Result.SUCCESS, "abandoned");
        helper.assertTrue(player.getEnderChestInventory().countItem(ModItems.RULERS_SWORD.get()) == 0, "none left in the Ender chest");
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "none left on the cursor");
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 0, "none left in the inventory");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanInsideACarriedShulkerBoxIsReclaimed(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = talentsChief(helper, player);
        QuestService.accept(player, TALENTS, chief);
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(loan(TALENTS), new ItemStack(Items.BREAD))));
        player.getInventory().setItem(0, box);
        QuestService.abandon(player, TALENTS);
        helper.assertTrue(box.get(DataComponents.CONTAINER).nonEmptyStream().noneMatch(s -> s.is(ModItems.RULERS_SWORD.get())), "the box holds no loan");
        helper.assertTrue(box.get(DataComponents.CONTAINER).nonEmptyStream().anyMatch(s -> s.is(Items.BREAD)), "the rest of the box stays");
        helper.succeed();
    }

    /** The old sword (left on the ground, in a chest...) picked up again after re-accepting is a duplicate: it goes. */
    @GameTest(template = TestSupport.EMPTY)
    public static void reacceptingDoesNotDuplicateTheLoan(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = talentsChief(helper, player);
        QuestService.accept(player, TALENTS, chief);
        ItemStack old = loan(TALENTS); // the first sword, out of the inventory
        player.getInventory().clearContent();
        QuestService.abandon(player, TALENTS);
        helper.assertTrue(QuestService.accept(player, TALENTS, chief) == QuestService.Result.SUCCESS, "accepted again");
        player.getInventory().add(old);
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 2, "two swords before the purge");
        helper.assertTrue(LoanGuard.purge(player) == 1, "one purged");
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 1, "only one is left");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanWithoutItsQuestIsPurged(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(loan(TALENTS));
        player.getEnderChestInventory().addItem(loan(TALENTS));
        helper.assertTrue(LoanGuard.purge(player) == 2, "both stacks purged");
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 0, "none in the inventory");
        helper.assertTrue(player.getEnderChestInventory().countItem(ModItems.RULERS_SWORD.get()) == 0, "none in the Ender chest");
        player.getInventory().add(new ItemStack(ModItems.RULERS_SWORD.get())); // not a loan: it is theirs
        helper.assertTrue(LoanGuard.purge(player) == 0, "what is not lent stays");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanIsKeptWhileItsQuestIsActive(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = talentsChief(helper, player);
        QuestService.accept(player, TALENTS, chief);
        helper.assertTrue(LoanGuard.purge(player) == 0, "nothing to purge");
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 1, "the lent sword stays");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aLoanOutsideSatisfyIsNotTouched(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(loan(TALENTS));
        Satisfy.standIn(null);
        try {
            helper.assertTrue(LoanGuard.purge(player) == 0, "the purge does not run in the real world");
        } finally {
            Satisfy.restoreStandIn();
        }
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 1, "untouched");
        helper.succeed();
    }
}
