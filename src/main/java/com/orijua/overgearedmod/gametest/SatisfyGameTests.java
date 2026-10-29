package com.orijua.overgearedmod.gametest;

import com.mojang.authlib.GameProfile;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionRules;
import com.orijua.overgearedmod.progression.StatEffects;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.equipment.RatingRoller;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.skill.SkillService;
import com.orijua.overgearedmod.smithing.ForgingRecipeInput;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * In-game tests (like integration tests): run with `gradlew runGameTestServer`, or `/test runall` in a dev world.
 * They boot a real server, so they also prove registries and the datapack (recipes) load.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SatisfyGameTests {
    private static final String EMPTY = "empty"; // data/overgearedmod/structure/empty.nbt

    private SatisfyGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void levelUpGrantsStatPoints(GameTestHelper helper) {
        // Level 1 → 2 costs 75 XP, 2 → 3 costs 100.
        ProgressionRules.XpResult result = ProgressionRules.addXp(PlayerStats.DEFAULT, 180, 5);
        helper.assertTrue(result.stats().level() == 3, "expected level 3, got " + result.stats().level());
        helper.assertTrue(result.stats().xp() == 5, "expected 5 leftover xp, got " + result.stats().xp());
        helper.assertTrue(result.stats().freePoints() == 10, "expected 10 points, got " + result.stats().freePoints());

        PlayerStats spent = ProgressionRules.allocate(result.stats(), StatType.DEXTERITY);
        helper.assertTrue(spent.dexterity() == 1 && spent.freePoints() == 9, "allocate should move one point to DEX");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
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

    @GameTest(template = EMPTY)
    public static void staminaRaisesMaxHealth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StatEffects.apply(player, PlayerStats.DEFAULT.with(StatType.STAMINA, 4));
        helper.assertTrue(player.getMaxHealth() == 22.0f, "4 STA should give 22 max health, got " + player.getMaxHealth());
        StatEffects.apply(player, PlayerStats.DEFAULT);
        helper.assertTrue(player.getMaxHealth() == 20.0f, "resetting stats should remove the modifier");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void forgingRecipeLoadsFromDatapack(GameTestHelper helper) {
        ForgingRecipeInput input = new ForgingRecipeInput(new ItemStack(ModItems.TEST_INGOT.get(), 2), new ItemStack(Items.STICK));
        ItemStack result = helper.getLevel().getRecipeManager()
                .getRecipeFor(ModRecipes.FORGING.get(), input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ModItems.JAFFA_SWORD.get()), "2 test ingots + stick should forge the Jaffa sword, got " + result);

        ForgingRecipeInput tooFew = new ForgingRecipeInput(new ItemStack(ModItems.TEST_INGOT.get(), 1), new ItemStack(Items.STICK));
        helper.assertFalse(helper.getLevel().getRecipeManager()
                .getRecipeFor(ModRecipes.FORGING.get(), tooFew, helper.getLevel()).isPresent(), "1 ingot must not be enough");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ratingAddsAttackDamage(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Equipment changes are applied on the entity tick; force it so the test needn't wait.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.tick();
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        ItemStack rated = new ItemStack(Items.IRON_SWORD);
        rated.set(ModDataComponents.ITEM_RATING, ItemRating.UNIQUE);
        player.setItemInHand(InteractionHand.MAIN_HAND, rated);
        player.tick();
        double boosted = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(boosted - base - ItemRating.UNIQUE.attackDamageBonus()) < 1e-6,
                "unique sword should add " + ItemRating.UNIQUE.attackDamageBonus() + ", got " + (boosted - base));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pagmaLinkHitsMobInFront(GameTestHelper helper) {
        // FakePlayer: a ServerPlayer with a no-op connection, so attachment syncing is simply ignored.
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "satisfy-test"));
        Vec3 origin = helper.absoluteVec(new Vec3(0.5, 1, 0.5));
        // yaw 0 = looking towards +Z
        player.moveTo(origin.x, origin.y, origin.z, 0.0f, 0.0f);
        player.setYHeadRot(0.0f);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(0.5, 1, 2.5));
        zombie.setNoAi(true);

        SkillService.Result result = SkillService.tryUseHeldSkill(player);
        helper.assertTrue(result == SkillService.Result.SUCCESS, "skill should fire, got " + result);
        helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "zombie in front should be hit");

        SkillService.Result again = SkillService.tryUseHeldSkill(player);
        helper.assertTrue(again == SkillService.Result.ON_COOLDOWN, "second use should be on cooldown, got " + again);
        helper.succeed();
    }
}
