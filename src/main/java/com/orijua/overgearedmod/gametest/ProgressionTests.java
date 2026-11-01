package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatEffects;
import com.orijua.overgearedmod.feature.progression.StatType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Stats applied to a real player. The XP and point formulas are unit tests (ProgressionRulesTest). */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ProgressionTests {
    private ProgressionTests() {
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

    /** A mob the player hit that then dies of something else (fire, a fall) still counts as his kill (playtest). */
    @GameTest(template = TestSupport.EMPTY)
    public static void aKillByFireAfterYourHitIsYours(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        int before = ProgressionService.get(player).xp();
        zombie.hurt(player.damageSources().playerAttack(player), 1.0f);
        zombie.hurt(helper.getLevel().damageSources().onFire(), 1000.0f);
        helper.assertTrue(zombie.isDeadOrDying(), "it burned");
        helper.assertTrue(ProgressionService.get(player).xp() > before, "and the XP is his");

        Zombie untouched = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        int after = ProgressionService.get(player).xp();
        untouched.hurt(helper.getLevel().damageSources().onFire(), 1000.0f);
        helper.assertTrue(ProgressionService.get(player).xp() == after, "one he never hit is nobody's");
        helper.succeed();
    }
}
