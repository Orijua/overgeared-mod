package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.StatEffects;
import com.orijua.overgearedmod.progression.StatType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
