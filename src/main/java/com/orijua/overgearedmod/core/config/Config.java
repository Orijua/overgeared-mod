package com.orijua.overgearedmod.core.config;

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
        BUILDER.pop().push("death");
    }

    private static final ModConfigSpec.DoubleValue DEATH_XP_LOSS = BUILDER
            .comment("Fraction of the current level's XP lost on death (0.1 = 10%). Never drops a level. 0 by default: the canon has no XP penalty for dying.")
            .defineInRange("xpLoss", D.deathXpLoss(), 0.0, 1.0);

    static {
        BUILDER.pop().push("mobs");
    }

    private static final ModConfigSpec.IntValue MOB_LEVEL_SPREAD = BUILDER
            .comment("Hostile mobs spawn at the nearest player's level, plus or minus up to this many levels.")
            .defineInRange("levelSpread", D.mobLevelSpread(), 0, 100);
    private static final ModConfigSpec.DoubleValue MOB_HEALTH_PER_LEVEL = BUILDER
            .comment("Extra max health per level above 1, as a fraction of the base (0.08 = +8%).")
            .defineInRange("healthPerLevel", D.mobHealthPerLevel(), 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue MOB_DAMAGE_PER_LEVEL = BUILDER
            .comment("Extra attack damage per level above 1, as a fraction of the base.")
            .defineInRange("damagePerLevel", D.mobDamagePerLevel(), 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue ELITE_CHANCE = BUILDER
            .comment("Chance for a hostile mob to be an elite: glowing, +eliteStatBonus health and damage, eliteXpMultiplier XP.")
            .defineInRange("eliteChance", D.eliteChance(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue ELITE_STAT_BONUS = BUILDER
            .defineInRange("eliteStatBonus", D.eliteStatBonus(), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue ELITE_XP_MULTIPLIER = BUILDER
            .defineInRange("eliteXpMultiplier", D.eliteXpMultiplier(), 1.0, 100.0);
    private static final ModConfigSpec.DoubleValue ELITE_EQUIPMENT_CHANCE = BUILDER
            .comment("Chance for an elite killed by a player to drop a rated (Rare or better) piece from the #overgearedmod:elite_drops tag.")
            .defineInRange("eliteEquipmentChance", D.eliteEquipmentChance(), 0.0, 1.0);

    static {
        BUILDER.pop().push("story");
    }

    private static final ModConfigSpec.EnumValue<Balance.ContestLoss> CONTEST_LOSS = BUILDER
            .comment("Losing the production contest against Euphemina in Winston: FAIL closes the quest for good, REMATCH allows",
                    "another try the next game day, AUTO fails it in a single-player world and gives a rematch in a shared one.")
            .defineEnum("contestLoss", D.contestLoss());

    static {
        BUILDER.pop().push("fatigue");
    }

    private static final FatigueBalance F = FatigueBalance.DEFAULT;
    private static final ModConfigSpec.BooleanValue FATIGUE_ENABLED = BUILDER
            .comment("Fatigue: a bar that drains only in Satisfy and refills in the real world. When it runs out the capsule disconnects you.",
                    "These numbers are the mod's own design, not the canon's.")
            .define("enabled", F.enabled());
    private static final ModConfigSpec.DoubleValue FATIGUE_SESSION_MINUTES = BUILDER
            .comment("Real minutes of normal play from full energy to empty.")
            .defineInRange("sessionMinutes", F.sessionMinutes(), 1.0, 1440.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_REST_MULTIPLIER = BUILDER
            .comment("Share of the normal drain paid while standing still.")
            .defineInRange("restMultiplier", F.restMultiplier(), 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_COMBAT_MULTIPLIER = BUILDER
            .comment("Multiplier of the normal drain while fighting (damage given or taken in the last 10 seconds).")
            .defineInRange("combatMultiplier", F.combatMultiplier(), 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_FORGE_COST_SECONDS = BUILDER
            .comment("Seconds of normal play that each forged item costs.")
            .defineInRange("forgeCostSeconds", F.forgeCostSeconds(), 0.0, 3600.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_RECOVERY_MINUTES = BUILDER
            .comment("Real minutes to go from empty to full while resting in the real world.")
            .defineInRange("recoveryMinutes", F.recoveryMinutes(), 0.1, 1440.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_MIN_ENERGY_TO_CONNECT = BUILDER
            .comment("The capsule refuses to connect below this energy (0 to 1).")
            .defineInRange("minEnergyToConnect", F.minEnergyToConnect(), 0.0, 1.0);
    private static final ModConfigSpec.IntValue FATIGUE_GRACE_SECONDS = BUILDER
            .comment("Seconds between running out of energy and the forced disconnection.")
            .defineInRange("graceSeconds", F.graceSeconds(), 0, 600);
    private static final ModConfigSpec.DoubleValue FATIGUE_WARN_1 = BUILDER
            .comment("Energies (0 to 1) at which a warning shows once when crossed downwards.")
            .defineInRange("warnFirst", F.warnAt().get(0), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_WARN_2 = BUILDER
            .defineInRange("warnSecond", F.warnAt().get(1), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue FATIGUE_WARN_3 = BUILDER
            .defineInRange("warnThird", F.warnAt().get(2), 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    static FatigueBalance fatigueBalance() {
        return new FatigueBalance(
                FATIGUE_ENABLED.get(), FATIGUE_SESSION_MINUTES.get(), FATIGUE_REST_MULTIPLIER.get(), FATIGUE_COMBAT_MULTIPLIER.get(),
                FATIGUE_FORGE_COST_SECONDS.get(), FATIGUE_RECOVERY_MINUTES.get(), FATIGUE_MIN_ENERGY_TO_CONNECT.get(),
                FATIGUE_GRACE_SECONDS.get(),
                java.util.stream.Stream.of(FATIGUE_WARN_1.get(), FATIGUE_WARN_2.get(), FATIGUE_WARN_3.get())
                        .sorted(java.util.Comparator.reverseOrder()).toList());
    }

    static Balance balance() {
        return new Balance(
                STAT_POINTS_PER_LEVEL.get(), KILL_XP_MULTIPLIER.get(), KILL_XP_PER_ATTACK_DAMAGE.get(), KILL_XP_PER_ARMOR.get(),
                SPAWNER_XP_MULTIPLIER.get(), XP_BASE.get(), XP_PER_LEVEL.get(),
                BASE_MANA.get().floatValue(), MANA_PER_INTELLIGENCE.get().floatValue(),
                BASE_MANA_REGEN.get().floatValue(), MANA_REGEN_PER_INTELLIGENCE.get().floatValue(),
                ENHANCE_BASE_CHANCE.get(), ENHANCE_LOSS_PER_LEVEL.get(), ENHANCE_PER_DEXTERITY.get(), ENHANCE_MIN_CHANCE.get(),
                DEATH_XP_LOSS.get(),
                MOB_LEVEL_SPREAD.get(), MOB_HEALTH_PER_LEVEL.get(), MOB_DAMAGE_PER_LEVEL.get(),
                ELITE_CHANCE.get(), ELITE_STAT_BONUS.get(), ELITE_XP_MULTIPLIER.get(), ELITE_EQUIPMENT_CHANCE.get(),
                CONTEST_LOSS.get());
    }
}
