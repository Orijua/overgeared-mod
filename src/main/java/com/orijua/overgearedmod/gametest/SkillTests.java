package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SkillTests {
    private static final ResourceLocation PAGMA_LINK = Overgearedmod.id("pagma_link");
    private static final ResourceLocation IRON_GUARD = Overgearedmod.id("iron_guard");

    private SkillTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void learnedSkillFillsFirstFreeSlot(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(SkillService.learn(player, PAGMA_LINK), "pagma_link should be defined in the datapack");
        helper.assertTrue(SkillService.learn(player, IRON_GUARD), "iron_guard should be defined");
        helper.assertFalse(SkillService.learn(player, PAGMA_LINK), "learning twice must fail");
        helper.assertTrue(SkillService.get(player).slot(1).filter(IRON_GUARD::equals).isPresent(), "second skill should land in slot 2");

        SkillService.Result noSword = SkillService.use(player, 0);
        helper.assertTrue(noSword == SkillService.Result.WRONG_ITEM, "pagma_link needs a sword, got " + noSword);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void pagmaLinkHitsMobInFront(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        SkillService.learn(player, PAGMA_LINK);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(0.5, 1, 2.5));
        zombie.setNoAi(true);

        SkillService.Result result = SkillService.use(player, 0);
        helper.assertTrue(result == SkillService.Result.SUCCESS, "skill should fire, got " + result);
        helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "zombie in front should be hit");

        SkillService.Result again = SkillService.use(player, 0);
        helper.assertTrue(again == SkillService.Result.ON_COOLDOWN, "second use should be on cooldown, got " + again);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void skillIconIsReadFromJson(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(SkillService.definition(player, PAGMA_LINK).flatMap(def -> def.icon()).filter(Items.IRON_SWORD::equals).isPresent(),
                "pagma_link should use the iron sword as icon");
        helper.succeed();
    }
}
