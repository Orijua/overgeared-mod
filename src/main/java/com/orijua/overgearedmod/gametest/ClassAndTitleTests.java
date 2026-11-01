package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatModifier;
import com.orijua.overgearedmod.feature.progression.StatSource;
import com.orijua.overgearedmod.feature.progression.StatType;
import com.orijua.overgearedmod.feature.quest.goal.PlayerCounters;
import com.orijua.overgearedmod.feature.skill.SkillService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

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
            TestSupport.killZombie(helper, player);
        }
        helper.assertTrue(player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(Overgearedmod.id("zombie_hunter")), "10 zombies earn the title");
        helper.assertTrue(ProgressionService.effectiveStats(player).strength() == 1, "zombie_hunter gives +1 STR");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void statModifiersNameEachSource(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ProgressionService.set(player, ProgressionService.get(player).with(StatType.STRENGTH, 2));
        ClassService.choose(player, Overgearedmod.id("warrior"));
        for (int i = 0; i < 10; i++) {
            TestSupport.killZombie(helper, player);
        }
        List<StatModifier> strength = ProgressionService.modifiers(player).stream()
                .filter(m -> m.stat() == StatType.STRENGTH && m.value() != 0).toList();
        helper.assertTrue(strength.size() == 3, "allocated, warrior and zombie_hunter: " + strength);
        helper.assertTrue(strength.get(0).source() == StatSource.ALLOCATED && strength.get(0).value() == 2, "2 allocated points first");
        helper.assertTrue(strength.get(1).source().name().getString().equals(ClassService.current(player)
                .flatMap(id -> Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, id))
                .orElseThrow().name().getString()), "the class bonus is named after the class");
        helper.assertTrue(strength.get(1).value() == 3 && strength.get(2).value() == 1, "warrior +3, zombie_hunter +1");
        helper.assertTrue(ProgressionService.effectiveStats(player).strength() == 6, "2 + 3 + 1 STR");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void forgingCountsForLowerTiers(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        TestSupport.forge(player, ItemRating.EPIC);
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
}
