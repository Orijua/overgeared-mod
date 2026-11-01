package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.insight.Observation;
import com.orijua.overgearedmod.insight.TalentSearch;
import com.orijua.overgearedmod.insight.Talents;
import com.orijua.overgearedmod.npc.VillageChiefNpc;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The swords of observation: quicker observing and more insight in hand, Talent Search, and the chief's loan. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class InsightSwordTests {
    private static final ResourceLocation TALENTS_QUEST = Overgearedmod.id("bairan_talents");

    private InsightSwordTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSwordOfObservationSharpensTheEye(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Observation.forget(player);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.COMMANDERS_SWORD.get()));
        helper.assertTrue(Observation.observeTicks(player) == 140 && Observation.effectiveInsight(player) == 10, "7 s and +10 with the Commander's");
        boolean gained = false;
        for (int i = 0; i < 140; i++) {
            gained |= Observation.observe(player, villager);
        }
        helper.assertTrue(gained && Observation.insight(player) == 1, "a point in seven seconds");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RULERS_SWORD.get()));
        helper.assertTrue(Observation.observeTicks(player) == 100 && Observation.effectiveInsight(player) == 26, "5 s and +25 with the Ruler's");
        villager.discard();
        helper.succeed();
    }

    /** Bairan's residents always hide a talent; each one counts once per sword. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchRevealsResidentsOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        Villager first = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        Villager second = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        first.addTag(Talents.BAIRAN_RESIDENT);
        second.addTag(Talents.BAIRAN_RESIDENT);
        ItemStack sword = new ItemStack(ModItems.RULERS_SWORD.get());
        int before = GoalTracker.milestones(player, TalentSearch.MILESTONE);

        helper.assertTrue(TalentSearch.search(player, sword) == 2, "two talents found");
        helper.assertTrue(first.hasEffect(MobEffects.GLOWING) && second.hasEffect(MobEffects.GLOWING), "they glow");
        helper.assertTrue(GoalTracker.milestones(player, TalentSearch.MILESTONE) == before + 2, "they count for the quest");
        helper.assertTrue(TalentSearch.search(player, sword) == 0 && sword.get(ModDataComponents.TALENTS_FOUND).size() == 2,
                "the same ones don't count twice");
        first.discard();
        second.discard();
        helper.succeed();
    }

    /** The chief lends the Ruler's Sword on accepting the quest and takes it back when it is dropped. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theChiefLendsTheRulersSword(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = helper.spawn(ModEntities.VILLAGE_CHIEF.get(), new BlockPos(1, 1, 1));
        player.moveTo(chief.position());
        ResourceLocation previous = Overgearedmod.id("watch_commander");
        player.setData(ModAttachments.PLAYER_QUESTS, PlayerQuests.EMPTY.accept(previous, 0).complete(previous));

        helper.assertTrue(QuestService.accept(player, TALENTS_QUEST, chief) == QuestService.Result.SUCCESS, "accepted at the chief");
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 1, "the sword is lent");
        boolean marked = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            marked |= stack.is(ModItems.RULERS_SWORD.get()) && TALENTS_QUEST.equals(stack.get(ModDataComponents.LOANED_FOR));
        }
        helper.assertTrue(marked, "marked as lent for the quest");
        QuestService.abandon(player, TALENTS_QUEST);
        helper.assertTrue(player.getInventory().countItem(ModItems.RULERS_SWORD.get()) == 0, "taken back when dropped");
        chief.discard();
        helper.succeed();
    }
}
