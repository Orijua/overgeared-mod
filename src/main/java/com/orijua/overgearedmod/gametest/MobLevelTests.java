package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.mob.MobLevel;
import com.orijua.overgearedmod.mob.MobLevelRules;
import com.orijua.overgearedmod.mob.MobLevelService;
import com.orijua.overgearedmod.progression.ProgressionRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class MobLevelTests {
    private MobLevelTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void mobLevelStaysNearThePlayer(GameTestHelper helper) {
        RandomSource random = RandomSource.create(42);
        for (int i = 0; i < 200; i++) {
            int level = MobLevelRules.roll(Balance.DEFAULT, 10, random).level();
            helper.assertTrue(level >= 8 && level <= 12, "level 10 player, spread 2: got " + level);
            helper.assertTrue(MobLevelRules.roll(Balance.DEFAULT, 0, random).level() >= 1, "never below level 1");
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void levelScalesHealthAndDamage(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        double baseDamage = zombie.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        MobLevelService.assign(zombie, new MobLevel(11, false), Balance.DEFAULT);
        // Level 11 = 10 levels above 1: +80% health, +50% damage.
        TestSupport.assertClose(helper, 36.0, zombie.getMaxHealth(), "max health");
        TestSupport.assertClose(helper, 36.0, zombie.getHealth(), "spawns at full health");
        TestSupport.assertClose(helper, baseDamage * 1.5,
                zombie.getAttributeValue(Attributes.ATTACK_DAMAGE), "attack damage");

        // Assigning again replaces the bonus instead of stacking it.
        MobLevelService.assign(zombie, new MobLevel(1, true), Balance.DEFAULT);
        TestSupport.assertClose(helper, 30.0, zombie.getMaxHealth(), "a level 1 elite has +50% health");
        helper.assertTrue(zombie.hasGlowingTag(), "elites glow");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void elitesGiveMoreXp(GameTestHelper helper) {
        int normal = ProgressionRules.killXp(Balance.DEFAULT, new ProgressionRules.Victim(20, 3, 2, false, false));
        int elite = ProgressionRules.killXp(Balance.DEFAULT, new ProgressionRules.Victim(20, 3, 2, false, true));
        helper.assertTrue(elite == normal * 3, "elite XP should be x3: " + normal + " vs " + elite);
        helper.succeed();
    }
}
