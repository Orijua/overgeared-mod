package com.orijua.overgearedmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Balance knobs, per world in saves/<world>/serverconfig/overgearedmod-server.toml.
 * A server config is synced to clients, so screens show the same numbers the server uses.
 * Code reads them through {@link Balance#current()}, never directly.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Balance D = Balance.DEFAULT;

    static {
        BUILDER.push("progression");
    }

    private static final ModConfigSpec.IntValue STAT_POINTS_PER_LEVEL = BUILDER
            .comment("Free stat points granted on each level up.")
            .defineInRange("statPointsPerLevel", D.statPointsPerLevel(), 0, 100);
    private static final ModConfigSpec.DoubleValue KILL_XP_MULTIPLIER = BUILDER
            .comment("Kill XP = (max health + attack damage * killXpPerAttackDamage + armor * killXpPerArmor) * killXpMultiplier.",
                    "With the defaults a zombie (20 HP, 3 attack, 2 armor) gives 28.")
            .defineInRange("killXpMultiplier", D.killXpMultiplier(), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue KILL_XP_PER_ATTACK_DAMAGE = BUILDER
            .defineInRange("killXpPerAttackDamage", D.killXpPerAttackDamage(), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue KILL_XP_PER_ARMOR = BUILDER
            .defineInRange("killXpPerArmor", D.killXpPerArmor(), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue SPAWNER_XP_MULTIPLIER = BUILDER
            .comment("Kill XP multiplier for mobs that came from a spawner or trial spawner (anti-farming).")
            .defineInRange("spawnerXpMultiplier", D.spawnerXpMultiplier(), 0.0, 1.0);
    private static final ModConfigSpec.IntValue XP_BASE = BUILDER
            .comment("XP to the next level = xpBase + xpPerLevel * current level.")
            .defineInRange("xpBase", D.xpBase(), 1, 100_000);
    private static final ModConfigSpec.IntValue XP_PER_LEVEL = BUILDER
            .defineInRange("xpPerLevel", D.xpPerLevel(), 0, 100_000);

    static {
        BUILDER.pop().push("mana");
    }

    private static final ModConfigSpec.DoubleValue BASE_MANA = BUILDER
            .comment("Max mana = baseMana + manaPerIntelligence * INT.")
            .defineInRange("baseMana", (double) D.baseMana(), 0.0, 10_000.0);
    private static final ModConfigSpec.DoubleValue MANA_PER_INTELLIGENCE = BUILDER
            .defineInRange("manaPerIntelligence", (double) D.manaPerIntelligence(), 0.0, 1_000.0);
    private static final ModConfigSpec.DoubleValue BASE_MANA_REGEN = BUILDER
            .comment("Mana per second = baseManaRegen + manaRegenPerIntelligence * INT.")
            .defineInRange("baseManaRegen", (double) D.baseManaRegen(), 0.0, 1_000.0);
    private static final ModConfigSpec.DoubleValue MANA_REGEN_PER_INTELLIGENCE = BUILDER
            .defineInRange("manaRegenPerIntelligence", (double) D.manaRegenPerIntelligence(), 0.0, 100.0);

    static {
        BUILDER.pop().push("enhancement");
    }

    private static final ModConfigSpec.DoubleValue ENHANCE_BASE_CHANCE = BUILDER
            .comment("Success chance = baseChance - chanceLossPerLevel * current level + chancePerDexterity * DEX,",
                    "clamped between minChance and baseChance.")
            .defineInRange("baseChance", D.enhanceBaseChance(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue ENHANCE_LOSS_PER_LEVEL = BUILDER
            .defineInRange("chanceLossPerLevel", D.enhanceChanceLossPerLevel(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue ENHANCE_PER_DEXTERITY = BUILDER
            .defineInRange("chancePerDexterity", D.enhanceChancePerDexterity(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue ENHANCE_MIN_CHANCE = BUILDER
            .defineInRange("minChance", D.enhanceMinChance(), 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    static Balance balance() {
        return new Balance(
                STAT_POINTS_PER_LEVEL.get(), KILL_XP_MULTIPLIER.get(), KILL_XP_PER_ATTACK_DAMAGE.get(), KILL_XP_PER_ARMOR.get(),
                SPAWNER_XP_MULTIPLIER.get(), XP_BASE.get(), XP_PER_LEVEL.get(),
                BASE_MANA.get().floatValue(), MANA_PER_INTELLIGENCE.get().floatValue(),
                BASE_MANA_REGEN.get().floatValue(), MANA_REGEN_PER_INTELLIGENCE.get().floatValue(),
                ENHANCE_BASE_CHANCE.get(), ENHANCE_LOSS_PER_LEVEL.get(), ENHANCE_PER_DEXTERITY.get(), ENHANCE_MIN_CHANCE.get());
    }
}
