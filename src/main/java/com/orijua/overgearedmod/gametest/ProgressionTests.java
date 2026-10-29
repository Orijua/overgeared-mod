package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.StatEffects;
import com.orijua.overgearedmod.progression.StatType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ProgressionTests {
    private ProgressionTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void levelUpGrantsStatPoints(GameTestHelper helper) {
        // Level 1 → 2 costs 75 XP, 2 → 3 costs 100.
        ProgressionRules.XpResult result = ProgressionRules.addXp(Balance.DEFAULT, PlayerStats.DEFAULT, 180);
        helper.assertTrue(result.stats().level() == 3, "expected level 3, got " + result.stats().level());
        helper.assertTrue(result.stats().xp() == 5, "expected 5 leftover xp, got " + result.stats().xp());
        helper.assertTrue(result.stats().freePoints() == 10, "expected 10 points, got " + result.stats().freePoints());

        PlayerStats spent = ProgressionRules.allocate(result.stats(), StatType.DEXTERITY);
        helper.assertTrue(spent.dexterity() == 1 && spent.freePoints() == 9, "allocate should move one point to DEX");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void staminaRaisesMaxHealth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StatEffects.apply(player, PlayerStats.DEFAULT.with(StatType.STAMINA, 4));
        TestSupport.assertClose(helper, 22.0, player.getMaxHealth(), "max health with 4 STA");
        StatEffects.apply(player, PlayerStats.DEFAULT);
        TestSupport.assertClose(helper, 20.0, player.getMaxHealth(), "max health after reset");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void respecRefundsAllocatedPoints(GameTestHelper helper) {
        PlayerStats stats = PlayerStats.DEFAULT.with(StatType.STRENGTH, 4).with(StatType.DEXTERITY, 3).withProgress(5, 0, 2);
        PlayerStats refunded = ProgressionRules.refundAll(stats);
        helper.assertTrue(refunded.freePoints() == 9, "2 free + 7 allocated = 9, got " + refunded.freePoints());
        helper.assertTrue(refunded.strength() == 0 && refunded.dexterity() == 0, "allocated stats go back to 0");
        helper.assertTrue(refunded.level() == 5, "level is untouched");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void killXpRewardsStrengthAndPunishesSpawners(GameTestHelper helper) {
        int zombie = ProgressionRules.killXp(Balance.DEFAULT, new ProgressionRules.Victim(20, 3, 2, false));
        int armoredBrute = ProgressionRules.killXp(Balance.DEFAULT, new ProgressionRules.Victim(20, 8, 10, false));
        int farmedZombie = ProgressionRules.killXp(Balance.DEFAULT, new ProgressionRules.Victim(20, 3, 2, true));
        helper.assertTrue(zombie == 28, "default zombie should give 28, got " + zombie);
        helper.assertTrue(armoredBrute > zombie, "a harder-hitting, armored mob gives more XP");
        helper.assertTrue(farmedZombie == 7, "spawner zombie gives a quarter (7), got " + farmedZombie);
        helper.succeed();
    }
}
