package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ClassAndTitleTests {
    private ClassAndTitleTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void choosingClassGrantsSkillsAndBonuses(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(ClassService.choose(player, Overgearedmod.id("warrior")), "warrior should be defined");
        helper.assertFalse(ClassService.choose(player, Overgearedmod.id("mage")), "a class can only be chosen once");
        helper.assertTrue(SkillService.get(player).knows(Overgearedmod.id("pagma_link")), "warrior starts with pagma_link");
        helper.assertTrue(ProgressionService.effectiveStats(player).strength() == 3, "warrior gives +3 STR");
        helper.assertTrue(ProgressionService.get(player).strength() == 0, "bonuses are not allocated points");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void killsUnlockTitle(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        for (int i = 0; i < 10; i++) {
            GoalTracker.onKill(player, EntityType.ZOMBIE);
        }
        helper.assertTrue(player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(Overgearedmod.id("zombie_hunter")), "10 zombies earn the title");
        helper.assertTrue(ProgressionService.effectiveStats(player).strength() == 1, "zombie_hunter gives +1 STR");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void forgingCountsForLowerTiers(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        GoalTracker.onForged(player, ItemRating.EPIC);
        PlayerCounters counters = player.getData(ModAttachments.PLAYER_COUNTERS);
        helper.assertTrue(counters.get("forge") == 1 && counters.get("forge/rare") == 1 && counters.get("forge/epic") == 1,
                "an epic forge counts for normal, rare and epic: " + counters);
        helper.assertTrue(counters.get("forge/unique") == 0, "but not for unique");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(Overgearedmod.id("epic_forger")), "epic forge earns the title");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void advancedClassCannotBeChosen(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertFalse(ClassService.choose(player, Overgearedmod.id("legendary_smith_successor")), "advanced classes are quest-only");
        helper.assertTrue(ClassService.current(player).isEmpty(), "no class should be set");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void classQuestRequiresClassAndGrantsAdvancedOne(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation legacy = Overgearedmod.id("pagma_legacy");
        helper.assertTrue(QuestService.accept(player, legacy) == QuestService.Result.NOT_AVAILABLE, "needs the Blacksmith class first");

        ClassService.choose(player, Overgearedmod.id("blacksmith"));
        helper.assertTrue(QuestService.accept(player, legacy) == QuestService.Result.SUCCESS, "a blacksmith can accept it");
        GoalTracker.onForged(player, ItemRating.UNIQUE);
        helper.assertTrue(QuestService.claim(player, legacy) == QuestService.Result.SUCCESS, "a unique forge completes it");

        helper.assertTrue(ClassService.current(player).filter(Overgearedmod.id("legendary_smith_successor")::equals).isPresent(),
                "claiming grants the advanced class");
        helper.assertTrue(SkillService.get(player).knows(Overgearedmod.id("iron_guard")), "skills learned before are kept");
        // +8 DEX from the new class (the Blacksmith's +3 is gone) and +2 from the Epic Forger title the unique forge also earned.
        int dexterity = ProgressionService.effectiveStats(player).dexterity();
        helper.assertTrue(dexterity == 10, "expected 8 (class) + 2 (title) DEX, got " + dexterity);
        helper.succeed();
    }
}
