package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.feature.equipment.EquipRequirements;
import com.orijua.overgearedmod.feature.equipment.EquipmentRules;
import com.orijua.overgearedmod.feature.equipment.EquipmentService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.option.RolledOption;
import com.orijua.overgearedmod.feature.equipment.EquipmentEvents;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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

    @GameTest(template = TestSupport.EMPTY)
    public static void requirementsIgnoreStatsFromOtherEquipment(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
        armor.set(ModDataComponents.ITEM_OPTIONS, List.of(new RolledOption(Overgearedmod.id("fracaso_agility"), 10f)));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.REQUIREMENTS, new EquipRequirements(1, Map.of(StatType.AGILITY, 10)));
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        ProgressionService.refreshEffects(player);

        helper.assertTrue(ProgressionService.effectiveStats(player).agility() >= 10, "the armor does give AGI");
        helper.assertFalse(EquipmentEvents.meetsRequirements(player, sword), "AGI from other equipment doesn't count");

        ProgressionService.set(player, ProgressionService.get(player).with(StatType.AGILITY, 10));
        helper.assertTrue(EquipmentEvents.meetsRequirements(player, sword), "assigned AGI does count");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void enhancingAddsOneOnSuccessAndKeepsTheLevelOnFailure(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(EquipmentService.enhance(smith, sword, 0.0) == EquipmentService.EnhanceResult.SUCCESS, "a low roll succeeds");
        helper.assertTrue(sword.getOrDefault(ModDataComponents.ENHANCEMENT, 0) == 1, "the sword is +1");
        helper.assertTrue(sword.getHoverName().getString().contains("+1"), "and its name says so: " + sword.getHoverName().getString());
        helper.assertTrue(EquipmentService.enhance(smith, sword, 0.999) == EquipmentService.EnhanceResult.FAILURE, "a high roll fails");
        helper.assertTrue(sword.getOrDefault(ModDataComponents.ENHANCEMENT, 0) == 1, "and the level stays at +1");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void enhancingStopsAtTen(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ENHANCEMENT, EquipmentRules.MAX_ENHANCEMENT);
        helper.assertTrue(EquipmentService.enhance(smith, sword, 0.0) == EquipmentService.EnhanceResult.MAXED, "no more than the maximum");
        helper.assertTrue(sword.getOrDefault(ModDataComponents.ENHANCEMENT, 0) == EquipmentRules.MAX_ENHANCEMENT, "the level doesn't change");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyWeaponsCanBeEnhanced(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        helper.assertTrue(EquipmentService.canEnhance(new ItemStack(Items.IRON_SWORD)), "a sword can");
        helper.assertFalse(EquipmentService.canEnhance(new ItemStack(Items.IRON_CHESTPLATE)), "armor can't: it gets no bonus from it");
        helper.assertFalse(EquipmentService.canEnhance(new ItemStack(Items.FISHING_ROD)), "a fishing rod can't");
        helper.assertFalse(EquipmentService.canEnhance(new ItemStack(Items.SHEARS)), "shears can't");
        helper.assertFalse(EquipmentService.canEnhance(new ItemStack(Items.IRON_SWORD, 2)), "a stack of two can't");
        helper.assertFalse(EquipmentService.canEnhance(ItemStack.EMPTY), "nothing can't");
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        helper.assertTrue(EquipmentService.enhance(smith, chestplate, 0.0) == EquipmentService.EnhanceResult.NOT_EQUIPMENT, "enhancing armor is refused");
        helper.assertFalse(chestplate.has(ModDataComponents.ENHANCEMENT), "and changes nothing");
        helper.succeed();
    }
}
