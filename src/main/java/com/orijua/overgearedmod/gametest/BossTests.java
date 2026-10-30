package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.boss.BoneLord;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.mob.MobLevelService;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModEffects;
import com.orijua.overgearedmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class BossTests {
    private BossTests() {
    }

    /** Spawned the way a spawn egg does it, so the spawn events (and mob levels) run. */
    private static BoneLord spawnBoss(GameTestHelper helper) {
        BoneLord boss = ModEntities.BONE_LORD.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), MobSpawnType.SPAWN_EGG);
        helper.assertTrue(boss != null, "the boss should spawn");
        return boss;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bossKeepsItsFixedStrength(GameTestHelper helper) {
        BoneLord boss = spawnBoss(helper);
        helper.assertFalse(MobLevelService.get(boss).isLeveled(), "a boss never gets a random mob level");
        // Control: a regular monster spawned the same way does get one, so the check above isn't passing by accident.
        Skeleton regular = EntityType.SKELETON.spawn(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), MobSpawnType.SPAWN_EGG);
        helper.assertTrue(regular != null && MobLevelService.get(regular).isLeveled(), "a regular skeleton spawned this way is leveled");
        regular.discard();
        helper.assertTrue(boss.getMaxHealth() == 200.0f, "200 max health, got " + boss.getMaxHealth());
        helper.assertTrue(boss.getMainHandItem().is(Items.DIAMOND_SWORD), "fights with a sword, not a bow");
        helper.assertFalse(boss.isPhaseTwo(), "starts in phase one");
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void phaseTwoStartsOnceBelowHalfHealthAndSummonsTwoMinions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BoneLord boss = spawnBoss(helper);
        boss.hurt(level.damageSources().generic(), 5.0f);
        helper.assertFalse(boss.isPhaseTwo(), "a scratch at full health changes nothing");

        boss.invulnerableTime = 0;
        boss.setHealth(boss.getMaxHealth() / 2 + 1);
        boss.hurt(level.damageSources().generic(), 5.0f);
        helper.assertTrue(boss.isPhaseTwo(), "below half health it enrages");
        helper.assertTrue(minionsAround(boss) == BoneLord.MINIONS, "summons " + BoneLord.MINIONS + ", found " + minionsAround(boss));

        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().generic(), 5.0f);
        helper.assertTrue(minionsAround(boss) == BoneLord.MINIONS, "only once: still " + BoneLord.MINIONS + ", found " + minionsAround(boss));

        level.getEntitiesOfClass(Skeleton.class, boss.getBoundingBox().inflate(6)).forEach(Skeleton::discard);
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void onlyPhaseTwoHitsCauseBleeding(GameTestHelper helper) {
        BoneLord boss = spawnBoss(helper);
        Pig calm = EntityType.PIG.create(helper.getLevel());
        boss.doHurtTarget(calm);
        helper.assertFalse(calm.hasEffect(ModEffects.BLEEDING), "phase one hits don't bleed");

        boss.setHealth(boss.getMaxHealth() / 2 + 1);
        boss.hurt(helper.getLevel().damageSources().generic(), 5.0f);
        Pig enraged = EntityType.PIG.create(helper.getLevel());
        boss.doHurtTarget(enraged);
        helper.assertTrue(enraged.hasEffect(ModEffects.BLEEDING), "phase two hits bleed");

        helper.getLevel().getEntitiesOfClass(Skeleton.class, boss.getBoundingBox().inflate(6)).forEach(Skeleton::discard);
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bossAlwaysDropsAnEpicOrBetterPiece(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BoneLord boss = spawnBoss(helper);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(boss.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, boss)
                .withParameter(LootContextParams.ORIGIN, boss.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        for (int i = 0; i < 20; i++) {
            int rewards = 0;
            for (ItemStack stack : table.getRandomItems(params, i)) {
                if (EquipmentService.canEnhance(stack)) {
                    rewards++;
                    ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
                    helper.assertTrue(rating != null && rating.ordinal() >= ItemRating.EPIC.ordinal(), "expected Epic or better, got " + rating);
                }
            }
            helper.assertTrue(rewards == 1, "exactly one rated piece per kill, got " + rewards);
        }
        boss.discard();
        helper.succeed();
    }

    private static int minionsAround(BoneLord boss) {
        return boss.level().getEntitiesOfClass(Skeleton.class, boss.getBoundingBox().inflate(6)).size();
    }
}
