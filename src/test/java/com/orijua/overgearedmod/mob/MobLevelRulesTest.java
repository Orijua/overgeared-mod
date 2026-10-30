package com.orijua.overgearedmod.mob;

import com.orijua.overgearedmod.Balance;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobLevelRulesTest {
    private static final Balance B = Balance.DEFAULT;

    @Test
    void mobLevelStaysNearThePlayer() {
        RandomSource random = RandomSource.create(42);
        for (int i = 0; i < 500; i++) {
            int level = MobLevelRules.roll(B, 10, random).level();
            assertTrue(level >= 8 && level <= 12, "level 10 player, spread 2: got " + level);
        }
    }

    @Test
    void neverBelowLevelOne() {
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 500; i++) {
            assertTrue(MobLevelRules.roll(B, 0, random).level() >= 1);
        }
    }

    @Test
    void aboutFivePercentAreElites() {
        RandomSource random = RandomSource.create(1);
        int elites = 0;
        for (int i = 0; i < 10_000; i++) {
            if (MobLevelRules.roll(B, 5, random).elite()) {
                elites++;
            }
        }
        assertTrue(elites > 400 && elites < 600, "expected ~500 elites in 10000, got " + elites);
    }

    @Test
    void bonusesGrowPerLevelAndElitesAddMore() {
        assertEquals(0.0, MobLevelRules.healthBonus(B, new MobLevel(1, false)), 1e-9);
        assertEquals(0.8, MobLevelRules.healthBonus(B, new MobLevel(11, false)), 1e-9);
        assertEquals(0.5, MobLevelRules.damageBonus(B, new MobLevel(11, false)), 1e-9);
        assertEquals(1.3, MobLevelRules.healthBonus(B, new MobLevel(11, true)), 1e-9);
    }
}
