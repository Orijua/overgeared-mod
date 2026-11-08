package com.orijua.overgearedmod.core.config;

import com.orijua.overgearedmod.feature.equipment.ItemRating;
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
    private static final ModConfigSpec.DoubleValue FATIGUE_SLEEP_RECOVERY = BUILDER
            .comment("Energy (0 to 1) that sleeping through the night in the real world gives back.")
            .defineInRange("sleepRecovery", F.sleepRecovery(), 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("trade");
    }

    private static final TradeBalance T = TradeBalance.DEFAULT;
    private static final ModConfigSpec.IntValue TRADE_BASE_NORMAL = BUILDER
            .comment("Trade: selling equipment for Satisfy emeralds or exporting it for Won. These numbers are the mod's own design, not the canon's.",
                    "Reference price of a piece of each rating, before material and enhancement (this one: normal).")
            .defineInRange("basePriceNormal", T.basePrice().get(ItemRating.NORMAL), 1, 1_000_000);
    private static final ModConfigSpec.IntValue TRADE_BASE_RARE = BUILDER
            .comment("Reference price of a rare piece.")
            .defineInRange("basePriceRare", T.basePrice().get(ItemRating.RARE), 1, 1_000_000);
    private static final ModConfigSpec.IntValue TRADE_BASE_EPIC = BUILDER
            .comment("Reference price of a epic piece.")
            .defineInRange("basePriceEpic", T.basePrice().get(ItemRating.EPIC), 1, 1_000_000);
    private static final ModConfigSpec.IntValue TRADE_BASE_UNIQUE = BUILDER
            .comment("Reference price of a unique piece.")
            .defineInRange("basePriceUnique", T.basePrice().get(ItemRating.UNIQUE), 1, 1_000_000);
    private static final ModConfigSpec.IntValue TRADE_BASE_LEGENDARY = BUILDER
            .comment("Reference price of a legendary piece.")
            .defineInRange("basePriceLegendary", T.basePrice().get(ItemRating.LEGENDARY), 1, 1_000_000);
    private static final ModConfigSpec.IntValue TRADE_BASE_MYTH = BUILDER
            .comment("Reference price of a myth piece.")
            .defineInRange("basePriceMyth", T.basePrice().get(ItemRating.MYTH), 1, 1_000_000);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_WOOD = BUILDER
            .comment("Price multiplier of wood equipment.")
            .defineInRange("materialWood", T.materialMultiplier().get("wood"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_STONE = BUILDER
            .comment("Price multiplier of stone equipment.")
            .defineInRange("materialStone", T.materialMultiplier().get("stone"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_IRON = BUILDER
            .comment("Price multiplier of iron equipment.")
            .defineInRange("materialIron", T.materialMultiplier().get("iron"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_GOLD = BUILDER
            .comment("Price multiplier of gold equipment.")
            .defineInRange("materialGold", T.materialMultiplier().get("gold"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_DIAMOND = BUILDER
            .comment("Price multiplier of diamond equipment.")
            .defineInRange("materialDiamond", T.materialMultiplier().get("diamond"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_NETHERITE = BUILDER
            .comment("Price multiplier of netherite equipment.")
            .defineInRange("materialNetherite", T.materialMultiplier().get("netherite"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_LEATHER = BUILDER
            .comment("Price multiplier of leather equipment.")
            .defineInRange("materialLeather", T.materialMultiplier().get("leather"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_MATERIAL_CHAINMAIL = BUILDER
            .comment("Price multiplier of chainmail equipment.")
            .defineInRange("materialChainmail", T.materialMultiplier().get("chainmail"), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_OTHER_MATERIAL = BUILDER
            .comment("Price multiplier of any other material.")
            .defineInRange("otherMaterial", T.otherMaterial(), 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue TRADE_ENHANCEMENT_BONUS = BUILDER
            .comment("Share of the reference price added per enhancement level (weapons only).")
            .defineInRange("enhancementBonus", T.enhancementBonus(), 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue TRADE_SELL_SHARE = BUILDER
            .comment("Share of the reference price paid in Satisfy emeralds when selling (0 to 1: selling never pays more than the reference).")
            .defineInRange("sellShare", T.sellShare(), 0.0, 1.0);
    private static final ModConfigSpec.IntValue TRADE_WON_PER_REFERENCE = BUILDER
            .comment("Won that one point of reference price is worth when exporting.")
            .defineInRange("wonPerReference", T.wonPerReference(), 0, 1_000_000);
    private static final ModConfigSpec.DoubleValue TRADE_EXPORT_COMMISSION = BUILDER
            .comment("Share of the export that is kept as commission (0.05 = the player gets 95 %).")
            .defineInRange("exportCommission", T.exportCommission(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue TRADE_SATURATION_STEP = BUILDER
            .comment("Price factor for each point of pressure (each piece sold or exported of the same category and rating).")
            .defineInRange("saturationStep", T.saturationStep(), 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue TRADE_SATURATION_FLOOR = BUILDER
            .comment("The saturation factor never goes below this.")
            .defineInRange("saturationFloor", T.saturationFloor(), 0.0, 1.0);
    private static final ModConfigSpec.IntValue TRADE_RECOVERY_PER_DAY = BUILDER
            .comment("Points of pressure that go away each whole game day.")
            .defineInRange("recoveryPerDay", T.recoveryPerDay(), 0, 10_000);
    private static final ModConfigSpec.IntValue TRADE_PRESSURE_CAP = BUILDER
            .comment("Pressure never grows beyond this.")
            .defineInRange("pressureCap", T.pressureCap(), 1, 10_000);
    private static final ModConfigSpec.LongValue TRADE_MAX_SELL_EMERALDS = BUILDER
            .comment("A sale that would pay more Satisfy emeralds than this is refused without touching the piece.")
            .defineInRange("maxSellEmeralds", T.maxSellEmeralds(), 64L, 100_000L);

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
                        .sorted(java.util.Comparator.reverseOrder()).toList(),
                FATIGUE_SLEEP_RECOVERY.get());
    }

    static TradeBalance tradeBalance() {
        java.util.Map<ItemRating, Integer> prices = new java.util.EnumMap<>(ItemRating.class);
        prices.put(ItemRating.NORMAL, TRADE_BASE_NORMAL.get());
        prices.put(ItemRating.RARE, TRADE_BASE_RARE.get());
        prices.put(ItemRating.EPIC, TRADE_BASE_EPIC.get());
        prices.put(ItemRating.UNIQUE, TRADE_BASE_UNIQUE.get());
        prices.put(ItemRating.LEGENDARY, TRADE_BASE_LEGENDARY.get());
        prices.put(ItemRating.MYTH, TRADE_BASE_MYTH.get());
        java.util.Map<String, Double> materials = new java.util.HashMap<>();
        materials.put("wood", TRADE_MATERIAL_WOOD.get());
        materials.put("stone", TRADE_MATERIAL_STONE.get());
        materials.put("iron", TRADE_MATERIAL_IRON.get());
        materials.put("gold", TRADE_MATERIAL_GOLD.get());
        materials.put("diamond", TRADE_MATERIAL_DIAMOND.get());
        materials.put("netherite", TRADE_MATERIAL_NETHERITE.get());
        materials.put("leather", TRADE_MATERIAL_LEATHER.get());
        materials.put("chainmail", TRADE_MATERIAL_CHAINMAIL.get());
        return new TradeBalance(prices, materials, TRADE_OTHER_MATERIAL.get(), TRADE_ENHANCEMENT_BONUS.get(), TRADE_SELL_SHARE.get(),
                TRADE_WON_PER_REFERENCE.get(), TRADE_EXPORT_COMMISSION.get(), TRADE_SATURATION_STEP.get(), TRADE_SATURATION_FLOOR.get(),
                TRADE_RECOVERY_PER_DAY.get(), TRADE_PRESSURE_CAP.get(), TRADE_MAX_SELL_EMERALDS.get());
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
