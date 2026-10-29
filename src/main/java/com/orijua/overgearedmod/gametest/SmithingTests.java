package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import com.orijua.overgearedmod.smithing.ForgingRecipeInput;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SmithingTests {
    private SmithingTests() {
    }

    private static Optional<RecipeHolder<ForgingRecipe>> find(GameTestHelper helper, ForgingRecipeInput input) {
        return helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(), input, helper.getLevel());
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void forgingRecipeLoadsFromDatapack(GameTestHelper helper) {
        ForgingRecipeInput input = new ForgingRecipeInput(new ItemStack(ModItems.TEST_INGOT.get(), 2), new ItemStack(Items.STICK));
        Optional<RecipeHolder<ForgingRecipe>> recipe = find(helper, input);
        helper.assertTrue(recipe.isPresent(), "2 test ingots + stick should match a forging recipe");
        helper.assertTrue(recipe.get().value().result().is(ModItems.JAFFA_SWORD.get()), "result should be the Jaffa sword");
        helper.assertTrue(recipe.get().value().requirements().isPresent(), "the Jaffa sword recipe declares requirements");

        ForgingRecipeInput tooFew = new ForgingRecipeInput(new ItemStack(ModItems.TEST_INGOT.get(), 1), new ItemStack(Items.STICK));
        helper.assertFalse(find(helper, tooFew).isPresent(), "1 ingot must not be enough");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void armorNeverRollsWeaponOptions(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ProgressionService.set(smith, ProgressionService.get(smith).with(StatType.DEXTERITY, 200)); // high ratings, more options
        Set<ResourceLocation> weaponOnly = Set.of(Overgearedmod.id("sharpness"), Overgearedmod.id("swiftness"), Overgearedmod.id("life_steal"));
        for (int i = 0; i < 50; i++) {
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            EquipmentService.stampForged(smith, chestplate, Optional.empty());
            for (RolledOption option : chestplate.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
                helper.assertFalse(weaponOnly.contains(option.option()), "armor rolled a weapon option: " + option.option());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void forgedItemGetsRatingAndRequirements(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ForgingRecipe recipe = find(helper, new ForgingRecipeInput(new ItemStack(ModItems.TEST_INGOT.get(), 2), new ItemStack(Items.STICK)))
                .orElseThrow().value();
        ItemStack sword = recipe.result().copy();
        EquipmentService.stampForged(smith, sword, recipe.requirements());

        helper.assertTrue(sword.has(ModDataComponents.ITEM_RATING.get()), "forged item must have a rating");
        helper.assertTrue(sword.has(ModDataComponents.REQUIREMENTS.get()), "forged item must carry the recipe's requirements");
        helper.succeed();
    }
}
