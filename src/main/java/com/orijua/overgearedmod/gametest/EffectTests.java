package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModEffects;
import com.orijua.overgearedmod.feature.equipment.option.ApplyEffectOptionEffect;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class EffectTests {
    private EffectTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bleedingIgnoresArmor(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        float before = zombie.getHealth();
        ModEffects.BLEEDING.get().applyEffectTick(zombie, 1);
        TestSupport.assertClose(helper, 2.0, before - zombie.getHealth(), "Bleeding II deals 2 damage through diamond armor");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void bleedOptionAppliesTheEffect(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        // A 100% chance, so the roll always succeeds.
        new ApplyEffectOptionEffect(ModEffects.BLEEDING, 100, 0).onHit(player, zombie, 5.0f, 1.0f);
        MobEffectInstance effect = zombie.getEffect(ModEffects.BLEEDING);
        helper.assertTrue(effect != null && effect.getDuration() == 100, "the hit should make the zombie bleed for 100 ticks");
        helper.succeed();
    }
}
