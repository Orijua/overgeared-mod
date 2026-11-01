package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.rumor.MaterialFinds;
import com.orijua.overgearedmod.rumor.RumorDefinition;
import com.orijua.overgearedmod.rumor.Rumors;
import com.orijua.overgearedmod.smithing.GreatSuccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class RumorTests {
    private static final ResourceLocation SINGING_ANVIL = Overgearedmod.id("singing_anvil");
    private static final ResourceLocation THE_EARL = Overgearedmod.id("the_earl");
    private static final ResourceLocation WHITE_METAL = Overgearedmod.id("white_metal");

    private RumorTests() {
    }

    /** Talking to Smith writes today's rumor in the Journal once, and it is one of his. */
    @GameTest(template = TestSupport.EMPTY)
    public static void hearingARumorWritesItInTheJournal(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EntityType<?> smith = ModEntities.BLACKSMITH.get();
        ResourceLocation today = Rumors.today(player, smith).orElseThrow().id();
        Rumors.hear(player, smith);
        Rumors.hear(player, smith);
        helper.assertTrue(Rumors.heard(player).equals(List.of(today)), "heard once: " + Rumors.heard(player));
        helper.assertTrue(rumor(helper, today).isToldBy(smith), "a rumor Smith tells");
        helper.succeed();
    }

    /** A great success at the anvil is the secret of "the singing anvil": Smith stops telling it. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aSolvedRumorIsPutToRest(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EntityType<?> smith = ModEntities.BLACKSMITH.get();
        helper.assertTrue(isOpen(player, smith, SINGING_ANVIL), "open before");
        GoalTracker.milestone(player, GreatSuccess.MILESTONE);
        helper.assertFalse(isOpen(player, smith, SINGING_ANVIL), "put to rest after a great success");
        helper.assertTrue(Rumors.solved(player, rumor(helper, SINGING_ANVIL)), "and marked as found in the Journal");
        helper.succeed();
    }

    /** The chief only talks of the earl once Smith's chain is done. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aRumorWaitsForItsQuest(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EntityType<?> chief = ModEntities.VILLAGE_CHIEF.get();
        helper.assertFalse(isOpen(player, chief, THE_EARL), "not before");
        TestSupport.completed(player, "quality_test");
        helper.assertTrue(isOpen(player, chief, THE_EARL), "after Smith's chain");
        helper.succeed();
    }

    /** Mining the first mithril ore is the find that puts the white metal rumor to rest. */
    @GameTest(template = TestSupport.EMPTY)
    public static void miningMithrilIsAFind(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.MITHRIL_ORE.get().defaultBlockState());
        player.gameMode.destroyBlock(pos);
        helper.assertTrue(GoalTracker.milestones(player, MaterialFinds.MITHRIL) == 1, "found mithril");
        helper.assertFalse(isOpen(player, ModEntities.BLACKSMITH.get(), WHITE_METAL), "white metal put to rest");
        helper.succeed();
    }

    /** Every rumor is told by an NPC with a window, and its quests exist. */
    @GameTest(template = TestSupport.EMPTY)
    public static void everyRumorPointsAtRealContent(GameTestHelper helper) {
        Set<EntityType<?>> tellers = Set.of(ModEntities.BLACKSMITH.get(), ModEntities.VILLAGE_CHIEF.get(), ModEntities.EARL_ASHUR.get());
        List<Definitions.Entry<RumorDefinition>> all = Definitions.all(helper.getLevel(), ModDatapackRegistries.RUMOR);
        helper.assertTrue(all.size() >= 10, "the rumors load: " + all.size());
        for (Definitions.Entry<RumorDefinition> entry : all) {
            RumorDefinition rumor = entry.value();
            helper.assertTrue(!rumor.givers().isEmpty() && tellers.containsAll(rumor.givers()), entry.id() + " givers");
            for (ResourceLocation quest : concat(rumor.solvedBy().quests(), rumor.requiresQuest().stream().toList())) {
                helper.assertTrue(Definitions.get(helper.getLevel(), ModDatapackRegistries.QUEST, quest).isPresent(), entry.id() + " → " + quest);
            }
            helper.assertFalse(rumor.solvedBy().quests().isEmpty() && rumor.solvedBy().milestones().isEmpty(), entry.id() + " has a secret");
        }
        helper.succeed();
    }

    private static boolean isOpen(ServerPlayer player, EntityType<?> giver, ResourceLocation id) {
        return Rumors.open(player, giver).stream().anyMatch(entry -> entry.id().equals(id));
    }

    private static RumorDefinition rumor(GameTestHelper helper, ResourceLocation id) {
        return Definitions.get(helper.getLevel(), ModDatapackRegistries.RUMOR, id).orElseThrow();
    }

    private static List<ResourceLocation> concat(List<ResourceLocation> a, List<ResourceLocation> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
