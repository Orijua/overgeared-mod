package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.goal.PlayerCounters;
import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
