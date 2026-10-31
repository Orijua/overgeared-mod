package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.npc.BlacksmithNpc;
import com.orijua.overgearedmod.npc.VillageChiefNpc;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class NpcTests {
    private NpcTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void blacksmithSellsEnhancementStones(GameTestHelper helper) {
        BlacksmithNpc blacksmith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        helper.assertTrue(blacksmith.getOffers().stream().anyMatch(offer -> offer.getResult().is(ModItems.ENHANCEMENT_STONE.get())),
                "the shop should sell enhancement stones");
        helper.assertTrue(blacksmith.requiresCustomPersistence(), "the blacksmith never despawns");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void blacksmithIgnoresMobDamage(GameTestHelper helper) {
        BlacksmithNpc blacksmith = helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 1);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        float health = blacksmith.getHealth();
        blacksmith.hurt(blacksmith.damageSources().mobAttack(zombie), 10.0f);
        helper.assertTrue(blacksmith.getHealth() == health, "mobs can't hurt the blacksmith");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void villageChiefStaysAndIgnoresMobDamage(GameTestHelper helper) {
        VillageChiefNpc chief = helper.spawn(ModEntities.VILLAGE_CHIEF.get(), 1, 1, 1);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        float health = chief.getHealth();
        chief.hurt(chief.damageSources().mobAttack(zombie), 10.0f);
        helper.assertTrue(chief.getHealth() == health, "mobs can't hurt the chief");
        helper.assertTrue(chief.requiresCustomPersistence() && !chief.removeWhenFarAway(1000), "the chief never despawns");
        helper.succeed();
    }
}
