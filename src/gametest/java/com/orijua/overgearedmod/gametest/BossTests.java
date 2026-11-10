package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModEffects;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.feature.equipment.EquipmentService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.mob.BoneLord;
import com.orijua.overgearedmod.feature.mob.MobLevelService;
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
        helper.assertTrue(boss.summonedMinions().size() == BoneLord.MINIONS, "summons " + BoneLord.MINIONS + ", found " + boss.summonedMinions().size());

        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().generic(), 5.0f);
        helper.assertTrue(boss.summonedMinions().size() == BoneLord.MINIONS, "only once: still " + BoneLord.MINIONS + ", found " + boss.summonedMinions().size());

        boss.summonedMinions().forEach(Skeleton::discard);
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

        boss.summonedMinions().forEach(Skeleton::discard);
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 320)
    public static void bossHealsWhenLeftAlone(GameTestHelper helper) {
        BoneLord boss = spawnBoss(helper);
        boss.setHealth(100.0f);
        helper.runAtTickTime(BoneLord.OUT_OF_COMBAT_TICKS - 20, () ->
                helper.assertTrue(boss.getHealth() == 100.0f, "no healing before the out-of-combat delay, got " + boss.getHealth()));
        helper.runAtTickTime(BoneLord.OUT_OF_COMBAT_TICKS + 60, () -> {
            helper.assertTrue(boss.getHealth() > 100.0f, "heals once left alone, got " + boss.getHealth());
            boss.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 320)
    public static void enragedBossCalmsDownOnceFullyHealed(GameTestHelper helper) {
        BoneLord boss = spawnBoss(helper);
        boss.setHealth(boss.getMaxHealth() / 2 + 1);
        boss.hurt(helper.getLevel().damageSources().generic(), 5.0f);
        helper.assertTrue(boss.isPhaseTwo(), "enraged below half health");
        boss.setHealth(boss.getMaxHealth() - 1);
        helper.runAtTickTime(BoneLord.OUT_OF_COMBAT_TICKS + 60, () -> {
            helper.assertFalse(boss.isPhaseTwo(), "back to phase one once fully healed");
            helper.assertFalse(boss.hasGlowingTag(), "and no longer glowing");
            boss.summonedMinions().forEach(Skeleton::discard);
            boss.discard();
            helper.succeed();
        });
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
                if (EquipmentService.isEquipment(stack)) {
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

    /** The second phase starts when health reaches half (a hit that leaves it above half changes nothing). */
    @GameTest(template = TestSupport.EMPTY)
    public static void boneLordChangesPhaseAtExactlyHalfHealth(GameTestHelper helper) {
        BoneLord boss = spawnBoss(helper);
        ServerLevel level = helper.getLevel();
        boss.setHealth(boss.getMaxHealth() / 2 + 20);
        boss.hurt(level.damageSources().generic(), 0.5f);
        helper.assertFalse(boss.isPhaseTwo(), "above half health there is no phase two: " + boss.getHealth() + "/" + boss.getMaxHealth());

        boss.invulnerableTime = 0;
        boss.setHealth(boss.getMaxHealth() / 2);
        boss.hurt(level.damageSources().generic(), 0.0f);
        helper.assertTrue(boss.isPhaseTwo(), "at exactly half health it enrages: " + boss.getHealth() + "/" + boss.getMaxHealth());
        boss.summonedMinions().forEach(Skeleton::discard);
        boss.discard();
        helper.succeed();
    }
}
