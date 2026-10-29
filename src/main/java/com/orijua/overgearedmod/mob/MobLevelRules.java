package com.orijua.overgearedmod.mob;

import com.orijua.overgearedmod.Balance;
import net.minecraft.util.RandomSource;

/**
 * Pure rules for mob levels: mobs scale with the nearest player (± {@code mobLevelSpread}),
 * and each level adds a percentage of base health and damage. Elites add a flat bonus on top.
 */
public final class MobLevelRules {
    private MobLevelRules() {
    }

    /** @param playerLevel level of the nearest player, or 0 if there is none (the mob is then level 1). */
    public static MobLevel roll(Balance balance, int playerLevel, RandomSource random) {
        int spread = balance.mobLevelSpread();
        int level = Math.max(1, playerLevel + random.nextIntBetweenInclusive(-spread, spread));
        return new MobLevel(level, random.nextDouble() < balance.eliteChance());
    }

    /** Extra max health as a fraction of the base (0.5 = +50%). */
    public static double healthBonus(Balance balance, MobLevel mob) {
        return (mob.level() - 1) * balance.mobHealthPerLevel() + (mob.elite() ? balance.eliteStatBonus() : 0.0);
    }

    /** Extra attack damage as a fraction of the base. */
    public static double damageBonus(Balance balance, MobLevel mob) {
        return (mob.level() - 1) * balance.mobDamagePerLevel() + (mob.elite() ? balance.eliteStatBonus() : 0.0);
    }
}
