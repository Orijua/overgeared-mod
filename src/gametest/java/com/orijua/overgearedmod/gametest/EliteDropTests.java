package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.LootEvents;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import com.orijua.overgearedmod.feature.mob.MobLevelService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The elite drop as it happens in play: a player kills an elite and LootEvents adds the piece to its drops.
 * The chance is forced to 100% through {@link Balance#withOverride} so the result never depends on luck.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class EliteDropTests {
    private EliteDropTests() {
    }

    /** Reads the final drops of one mob, after every other listener (LOWEST priority runs last). */
    public static final class DropRecorder {
        private final Zombie victim;
        final List<ItemEntity> drops = new ArrayList<>();

        DropRecorder(Zombie victim) {
            this.victim = victim;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onDrops(LivingDropsEvent event) {
            if (event.getEntity() == victim) {
                drops.addAll(event.getDrops());
            }
        }
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void eliteKilledByAPlayerDropsARatedPiece(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        List<ItemStack> pieces = killZombie(helper, true, 1.0, zombie -> player.damageSources().playerAttack(player));
        helper.assertTrue(pieces.size() == 1, "exactly one elite piece, got " + pieces);
        ItemRating rating = pieces.get(0).get(ModDataComponents.ITEM_RATING);
        helper.assertTrue(rating != null && rating.ordinal() >= ItemRating.RARE.ordinal(), "Rare or better, got " + rating);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void eliteKilledWithoutAPlayerDropsNoPiece(GameTestHelper helper) {
        List<ItemStack> pieces = killZombie(helper, true, 1.0, zombie -> zombie.damageSources().generic());
        helper.assertTrue(pieces.isEmpty(), "only player kills earn the piece, got " + pieces);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void eliteDropRespectsAZeroChance(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        List<ItemStack> pieces = killZombie(helper, true, 0.0, zombie -> player.damageSources().playerAttack(player));
        helper.assertTrue(pieces.isEmpty(), "a 0% chance never drops the piece, got " + pieces);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void regularMobKilledByAPlayerDropsNoPiece(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        List<ItemStack> pieces = killZombie(helper, false, 1.0, zombie -> player.damageSources().playerAttack(player));
        helper.assertTrue(pieces.isEmpty(), "only elites drop the piece, got " + pieces);
        helper.succeed();
    }

    /**
     * Spawns a zombie (elite or not), kills it with the given damage source at a 100% drop chance, and returns
     * the elite pieces among its drops. The dropped items are removed afterwards.
     */
    private static List<ItemStack> killZombie(GameTestHelper helper, boolean elite, double chance, Function<Zombie, DamageSource> source) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        MobLevelService.assign(zombie, new MobLevel(5, elite), Balance.current());
        DropRecorder recorder = new DropRecorder(zombie);
        NeoForge.EVENT_BUS.register(recorder);
        try {
            Balance.withOverride(Balance.current().withEliteEquipmentChance(chance),
                    () -> zombie.hurt(source.apply(zombie), 10_000.0f));
        } finally {
            NeoForge.EVENT_BUS.unregister(recorder);
        }
        helper.assertTrue(zombie.isDeadOrDying(), "the zombie should die");
        List<ItemStack> pieces = recorder.drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(LootEvents.ELITE_DROPS)).toList();
        recorder.drops.forEach(ItemEntity::discard);
        return pieces;
    }
}
