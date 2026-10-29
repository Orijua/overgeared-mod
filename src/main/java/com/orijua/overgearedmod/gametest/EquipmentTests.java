package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.RatingRoller;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModDataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private EquipmentTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void higherDexterityForgesBetterRatings(GameTestHelper helper) {
        double novice = averageTier(0, 0);
        double master = averageTier(50, 10);
        helper.assertTrue(master > novice, "master avg " + master + " should beat novice avg " + novice);
        helper.succeed();
    }

    private static double averageTier(int dexterity, int smithingLevel) {
        int samples = 10_000;
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += RatingRoller.roll(dexterity, smithingLevel, (i + 0.5) / samples).ordinal();
        }
        return sum / samples;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void ratingAddsAttackDamage(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ITEM_RATING, ItemRating.UNIQUE);
        TestSupport.assertClose(helper, ItemRating.UNIQUE.attackDamageBonus(), TestSupport.attackBonusOver(helper, sword), "unique bonus");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void attributeOptionAddsAttackDamage(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ITEM_OPTIONS, List.of(new RolledOption(Overgearedmod.id("sharpness"), 2.0f)));
        TestSupport.assertClose(helper, 2.0, TestSupport.attackBonusOver(helper, sword), "sharpness option");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void enhancementAddsAttackDamage(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ENHANCEMENT, 3);
        TestSupport.assertClose(helper, 3 * EquipmentRules.ATTACK_PER_ENHANCEMENT, TestSupport.attackBonusOver(helper, sword), "+3 enhancement");
        helper.assertTrue(EquipmentRules.enhancementChance(9, 0) < EquipmentRules.enhancementChance(0, 0), "higher levels must be harder");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void requirementsCheckLevelAndStats(GameTestHelper helper) {
        EquipRequirements requirements = new EquipRequirements(2, Map.of(StatType.STRENGTH, 2));
        PlayerStats weak = PlayerStats.DEFAULT.withProgress(2, 0, 0);
        helper.assertFalse(requirements.metBy(weak), "level 2 with 0 STR must not meet it");
        helper.assertTrue(requirements.metBy(weak.with(StatType.STRENGTH, 2)), "level 2 with 2 STR must meet it");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void optionPicksAreDistinct(GameTestHelper helper) {
        List<String> picked = EquipmentRules.pickWeighted(List.of("a", "b", "c"), s -> 10, 3, () -> 0.0);
        helper.assertTrue(new HashSet<>(picked).size() == 3, "expected 3 distinct picks, got " + picked);
        helper.assertTrue(EquipmentRules.optionCount(ItemRating.NORMAL) == 0, "normal items roll no options");
        helper.succeed();
    }
}
