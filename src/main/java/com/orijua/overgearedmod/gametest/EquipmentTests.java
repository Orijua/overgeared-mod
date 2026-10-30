package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipRequirements;
import com.orijua.overgearedmod.equipment.EquipmentRules;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.registry.ModDataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private EquipmentTests() {
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
        helper.assertTrue(EquipmentRules.enhancementChance(Balance.DEFAULT, 9, 0) < EquipmentRules.enhancementChance(Balance.DEFAULT, 0, 0), "higher levels must be harder");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void nameShowsEnhancementInRatingColor(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ITEM_RATING, ItemRating.EPIC);
        sword.set(ModDataComponents.ENHANCEMENT, 3);
        EquipmentService.refreshName(sword);
        helper.assertTrue(sword.getHoverName().getString().endsWith(" +3"), "name should end in +3: " + sword.getHoverName().getString());
        helper.assertTrue(ItemRating.EPIC.color().getColor().equals(sword.getHoverName().getStyle().getColor().getValue()),
                "name should use the epic color");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void unmetRequirementsDisableBonuses(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ITEM_RATING, ItemRating.UNIQUE);
        sword.set(ModDataComponents.ENHANCEMENT, 2);
        sword.set(ModDataComponents.REQUIREMENTS, new EquipRequirements(5, Map.of()));
        TestSupport.assertClose(helper, 0.0, TestSupport.attackBonusOver(helper, sword), "bonus with unmet requirements");
        helper.succeed();
    }
}
