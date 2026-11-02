package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import com.orijua.overgearedmod.feature.mob.MobLevelService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
        TestSupport.assertClose(helper, baseDamage * 1.5, zombie.getAttributeValue(Attributes.ATTACK_DAMAGE),
                "a level 1 elite has +50% damage, not stacked on the level 11 bonus");
        TestSupport.assertClose(helper, 30.0, zombie.getHealth(), "refilled to the new max");

        // A plain level 1 has no bonus at all: the modifier is removed.
        MobLevelService.assign(zombie, new MobLevel(1, false), Balance.DEFAULT);
        TestSupport.assertClose(helper, 20.0, zombie.getMaxHealth(), "back to the base health");
        TestSupport.assertClose(helper, 20.0, zombie.getHealth(), "and full");
        TestSupport.assertClose(helper, baseDamage, zombie.getAttributeValue(Attributes.ATTACK_DAMAGE), "back to the base damage");
        helper.succeed();
    }
}
