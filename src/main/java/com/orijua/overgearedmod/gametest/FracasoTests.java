package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipmentEvents;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.skill.SkillService;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import com.orijua.overgearedmod.smithing.ForgingRecipeInput;
import com.orijua.overgearedmod.smithing.ForgingRules;
import com.orijua.overgearedmod.smithing.SmithingAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Fracaso, Grid's first legendary work: its blueprint, who can forge it, its rating range and its canon effects. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class FracasoTests {
    private static final ResourceLocation SUCCESSOR = Overgearedmod.id("legendary_smith_successor");
    private static final ResourceLocation BISECTION = Overgearedmod.id("bisection");
    private static final Set<ResourceLocation> CANON_EFFECTS = Set.of(Overgearedmod.id("fracaso_agility"),
            Overgearedmod.id("fracaso_block"), Overgearedmod.id("fracaso_darkness"), Overgearedmod.id("fracaso_fear"));

    private FracasoTests() {
    }

    private static ForgingRecipe fracasoRecipe(GameTestHelper helper) {
        ForgingRecipeInput input = new ForgingRecipeInput(new ItemStack(ModItems.BLUE_ORICHALCUM_INGOT.get(), 15),
                new ItemStack(ModItems.FRACASO_BLUEPRINT.get()));
        return helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.FORGING.get(), input, helper.getLevel())
                .orElseThrow(() -> new AssertionError("15 blue orichalcum + the blueprint should be a recipe")).value();
    }

    private static SmithingAnvilMenu anvilWithFracasoInputs(GameTestHelper helper, ServerPlayer smith) {
        SmithingAnvilMenu menu = new SmithingAnvilMenu(1, smith.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(SmithingAnvilMenu.TOP_SLOT).set(new ItemStack(ModItems.BLUE_ORICHALCUM_INGOT.get(), 15));
        menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).set(new ItemStack(ModItems.FRACASO_BLUEPRINT.get()));
        return menu;
    }

    /** A Fracaso forged by a successor with the recipe's own rules, rerolled until it comes out {@code rating}. */
    private static ItemStack forgedFracaso(GameTestHelper helper, ServerPlayer smith, ItemRating rating) {
        ForgingRecipe recipe = fracasoRecipe(helper);
        for (int i = 0; i < 1000; i++) {
            ItemStack sword = recipe.result().copy();
            EquipmentService.stampForged(smith, sword, recipe.requirements(), recipe.extras(), 0, 0);
            if (sword.get(ModDataComponents.ITEM_RATING) == rating) {
                return sword;
            }
        }
        throw new AssertionError("no " + rating + " Fracaso in 1000 forges");
    }

    /** Gives the player what Fracaso asks for: level 300 and 5000 STR. */
    private static void meetRequirements(ServerPlayer player) {
        ProgressionService.set(player, ProgressionService.get(player).withProgress(300, 0, 0).with(StatType.STRENGTH, 5000));
    }

    private static void hold(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ProgressionService.refreshEffects(player);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyTheSuccessorCanUseTheBlueprint(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ClassService.choose(smith, Overgearedmod.id("blacksmith"));
        helper.assertFalse(anvilWithFracasoInputs(helper, smith).canForge(), "a plain blacksmith can't forge Fracaso");

        ClassService.grant(smith, SUCCESSOR);
        helper.assertTrue(anvilWithFracasoInputs(helper, smith).canForge(), "the Legendary Smith Successor can");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = ForgingRules.MAX_AUTO_TICKS + 40)
    public static void forgingFracasoUsesUpTheBlueprint(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ClassService.grant(smith, SUCCESSOR);
        SmithingAnvilMenu menu = anvilWithFracasoInputs(helper, smith);
        helper.assertTrue(menu.clickMenuButton(smith, SmithingAnvilMenu.AUTO_FORGE_BUTTON), "auto forging starts");
        helper.runAtTickTime(ForgingRules.MAX_AUTO_TICKS + 5, () -> {
            menu.broadcastChanges();
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.TOP_SLOT).getItem().isEmpty(), "the 15 ingots are spent");
            helper.assertTrue(menu.getSlot(SmithingAnvilMenu.BOTTOM_SLOT).getItem().isEmpty(), "the blueprint is used up");
            helper.assertTrue(smith.getInventory().items.stream().anyMatch(stack -> stack.is(ModItems.FRACASO_SWORD.get())),
                    "Fracaso goes to the inventory");
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fracasoIsUniqueOrLegendaryWithTheCanonEffects(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ClassService.grant(smith, SUCCESSOR);
        ForgingRecipe recipe = fracasoRecipe(helper);
        for (int i = 0; i < 100; i++) {
            ItemStack sword = recipe.result().copy();
            EquipmentService.stampForged(smith, sword, recipe.requirements(), recipe.extras(), 0, 1);
            ItemRating rating = sword.get(ModDataComponents.ITEM_RATING);
            helper.assertTrue(rating == ItemRating.UNIQUE || rating == ItemRating.LEGENDARY, "got " + rating);
            List<ResourceLocation> options = sword.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())
                    .stream().map(RolledOption::option).toList();
            helper.assertTrue(options.containsAll(CANON_EFFECTS), "always the canon effects, got " + options);
            helper.assertTrue(options.contains(Overgearedmod.id("fracaso_bisection")) == (rating == ItemRating.LEGENDARY),
                    "Bisection only on a Legendary, got " + options + " on " + rating);
            helper.assertTrue(options.size() == CANON_EFFECTS.size() + (rating == ItemRating.LEGENDARY ? 1 : 0),
                    "fixed options only, no random extra: " + options);
        }
        helper.assertTrue(fracasoRecipe(helper).requirements().filter(r -> r.level() == 300
                && r.stats().get(StatType.STRENGTH) == 5000).isPresent(), "canon requirements: level 300, 5000 STR");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fracasoOptionsNeverRollOnOtherItems(GameTestHelper helper) {
        ServerPlayer smith = TestSupport.fakePlayer(helper);
        ProgressionService.set(smith, ProgressionService.get(smith).with(StatType.DEXTERITY, 200)); // high ratings, more options
        for (int i = 0; i < 50; i++) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            EquipmentService.stampForged(smith, sword, Optional.empty());
            for (RolledOption option : sword.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
                helper.assertFalse(option.option().getPath().startsWith("fracaso_"), "rolled a fixed-only option: " + option.option());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fracasoGivesAgilityOnlyToWhoMeetsItsRequirements(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ClassService.grant(player, SUCCESSOR);
        ItemStack fracaso = forgedFracaso(helper, player, ItemRating.UNIQUE);
        int baseAgility = ProgressionService.effectiveStats(player).agility();

        hold(player, fracaso);
        helper.assertTrue(ProgressionService.effectiveStats(player).agility() == baseAgility, "nobody meets Fracaso's requirements");
        meetRequirements(player);
        int agility = ProgressionService.effectiveStats(player).agility();
        helper.assertTrue(agility == baseAgility + 30, "expected +30 AGI, got " + (agility - baseAgility));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bisectionIsTheLegendaryFracasosWeaponSkill(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ClassService.grant(player, SUCCESSOR);
        ItemStack unique = forgedFracaso(helper, player, ItemRating.UNIQUE);
        ItemStack legendary = forgedFracaso(helper, player, ItemRating.LEGENDARY);
        helper.assertTrue(SkillService.weaponSkill(unique).isEmpty(), "a Unique Fracaso has no skill");
        helper.assertTrue(SkillService.weaponSkill(legendary).filter(BISECTION::equals).isPresent(), "a Legendary one has Bisection");

        player.setData(ModAttachments.MANA, 1000f);
        hold(player, legendary);
        helper.assertTrue(SkillService.use(player, SkillService.WEAPON_SLOT) == SkillService.Result.REQUIREMENTS_UNMET,
                "it can't be cast without meeting the requirements");
        meetRequirements(player);
        helper.assertTrue(SkillService.use(player, SkillService.WEAPON_SLOT) == SkillService.Result.SUCCESS, "then it casts");
        helper.assertFalse(SkillService.get(player).knows(BISECTION), "without being learned");
        helper.assertTrue(SkillService.use(player, SkillService.WEAPON_SLOT) == SkillService.Result.ON_COOLDOWN, "and has a cooldown");
        hold(player, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(SkillService.use(player, SkillService.WEAPON_SLOT) == SkillService.Result.EMPTY_SLOT, "only while held");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void fracasoFrightensMuchWeakerMonsters(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ClassService.grant(player, SUCCESSOR);
        hold(player, forgedFracaso(helper, player, ItemRating.UNIQUE));
        meetRequirements(player);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 1, 2);
        EquipmentEvents.forEachActiveOption(player, (effect, value) -> effect.tickWorn(player, value));
        helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && zombie.hasEffect(MobEffects.WEAKNESS),
                "a level-1 zombie fears a level-300 wielder");
        helper.succeed();
    }
}
