package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.insight.InsightRules;
import com.orijua.overgearedmod.feature.insight.InsightSwordItem;
import com.orijua.overgearedmod.feature.insight.Observation;
import com.orijua.overgearedmod.feature.insight.TalentSearch;
import com.orijua.overgearedmod.feature.insight.Talents;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.NpcGrade;
import com.orijua.overgearedmod.feature.npc.VillageChiefNpc;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

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
        helper.assertTrue(Observation.observeTicks(player) == 100 && Observation.effectiveInsight(player) == 25, "5 s and +25 with the Commander's");
        helper.assertTrue(((InsightSwordItem) ModItems.COMMANDERS_SWORD.get()).observeSpeedPercent() == 50
                && ((InsightSwordItem) ModItems.RULERS_SWORD.get()).observeSpeedPercent() == 30, "what the tooltips say: 50 % and 30 % faster");
        helper.assertTrue(((InsightSwordItem) ModItems.COMMANDERS_SWORD.get()).observeTicks() < ((InsightSwordItem) ModItems.RULERS_SWORD.get()).observeTicks(),
                "the Commander observes faster than the Ruler (canon)");
        for (int i = 0; i < 99; i++) {
            helper.assertTrue(!Observation.observe(player, villager), "no point before five seconds (tick " + (i + 1) + ")");
        }
        helper.assertTrue(Observation.observe(player, villager) && Observation.insight(player) == 1, "the point comes on tick 100");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RULERS_SWORD.get()));
        // 7 s for the sword, a little less with the point just learned (more Insight, faster).
        helper.assertTrue(Observation.observeTicks(player) == 137 && Observation.effectiveInsight(player) == 11, "137 ticks and +10 with the Ruler's");
        villager.discard();
        helper.succeed();
    }

    /** Bairan's residents always hide a talent; each one counts once per sword. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchRevealsResidentsOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        Villager first = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        Villager second = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        first.addTag(Talents.BAIRAN_RESIDENT);
        second.addTag(Talents.BAIRAN_RESIDENT);
        ItemStack sword = new ItemStack(ModItems.RULERS_SWORD.get());
        int before = GoalTracker.milestones(player, TalentSearch.MILESTONE);

        helper.assertTrue(TalentSearch.search(player, sword) == 2, "two talents found");
        helper.assertTrue(first.hasEffect(MobEffects.GLOWING) && second.hasEffect(MobEffects.GLOWING), "they glow");
        helper.assertTrue(GoalTracker.milestones(player, TalentSearch.MILESTONE) == before + 2, "they count for the quest");
        helper.assertTrue(TalentSearch.search(player, sword) == 0 && player.getData(ModAttachments.TALENTS_FOUND).contains(first.getUUID())
                && player.getData(ModAttachments.TALENTS_FOUND).contains(second.getUUID()), "the same ones don't count twice");
        // A new sword doesn't make them count again: they are remembered by the player.
        helper.assertTrue(TalentSearch.search(player, new ItemStack(ModItems.RULERS_SWORD.get())) == 0, "not with another sword either");
        helper.assertTrue(GoalTracker.milestones(player, TalentSearch.MILESTONE) == before + 2, "the milestone didn't move");
        first.discard();
        second.discard();
        helper.succeed();
    }

    /** Each new talent found raises Insight by {@link TalentSearch#INSIGHT_PER_TALENT}; searching again gives nothing. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchRaisesInsight(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.INSIGHT, 0);
        player.setData(ModAttachments.TALENTS_FOUND, java.util.List.of());
        Villager first = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        Villager second = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        first.addTag(Talents.BAIRAN_RESIDENT);
        second.addTag(Talents.BAIRAN_RESIDENT);
        ItemStack sword = new ItemStack(ModItems.RULERS_SWORD.get());
        helper.assertTrue(TalentSearch.search(player, sword) == 2, "two new talents");
        helper.assertTrue(Observation.insight(player) == 2 * TalentSearch.INSIGHT_PER_TALENT, "Insight rises by 2 per talent");
        helper.assertTrue(TalentSearch.search(player, sword) == 0 && Observation.insight(player) == 2 * TalentSearch.INSIGHT_PER_TALENT,
                "searching again changes nothing");
        first.discard();
        second.discard();
        helper.succeed();
    }

    private static ApprenticeSmith apprentice(GameTestHelper helper, BlockPos at, NpcGrade grade) {
        ApprenticeSmith apprentice = helper.spawn(ModEntities.APPRENTICE_SMITH.get(), at);
        apprentice.setGrade(grade);
        return apprentice;
    }

    private static void rulersSwordWithInsight(ServerPlayer player, int insight) {
        player.setData(ModAttachments.INSIGHT, insight);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RULERS_SWORD.get()));
    }

    /** Named apprentices glow; semi-named ones only with high (effective) Insight; the rest never; none counts for the quest. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchRevealsApprenticesByGrade(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ApprenticeSmith named = apprentice(helper, new BlockPos(0, 1, 2), NpcGrade.NAMED);
        ApprenticeSmith semi = apprentice(helper, new BlockPos(1, 1, 2), NpcGrade.SEMI_NAMED);
        ApprenticeSmith normal = apprentice(helper, new BlockPos(2, 1, 2), NpcGrade.NORMAL);
        int before = GoalTracker.milestones(player, TalentSearch.MILESTONE);

        rulersSwordWithInsight(player, 29); // 29 + 10 = 39: medium
        TalentSearch.search(player, player.getMainHandItem());
        helper.assertTrue(named.hasEffect(MobEffects.GLOWING), "the named glows at 39");
        helper.assertTrue(!semi.hasEffect(MobEffects.GLOWING) && !normal.hasEffect(MobEffects.GLOWING), "the others don't at 39");

        rulersSwordWithInsight(player, 30); // 40: high
        TalentSearch.search(player, player.getMainHandItem());
        helper.assertTrue(semi.hasEffect(MobEffects.GLOWING), "the semi-named glows at 40");
        helper.assertTrue(!normal.hasEffect(MobEffects.GLOWING), "a normal one never does");
        helper.assertTrue(GoalTracker.milestones(player, TalentSearch.MILESTONE) == before, "apprentices don't count for the quest");
        named.discard();
        semi.discard();
        normal.discard();
        helper.succeed();
    }

    /** A villager without a talent doesn't glow and isn't remembered. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchIgnoresVillagersWithoutTalent(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Villager plain = EntityType.VILLAGER.create(helper.getLevel());
        plain.setUUID(new UUID(1, 0)); // floorMod(1, 4) != 0: no talent
        helper.assertTrue(Talents.of(plain).isEmpty(), "the fixed UUID has no talent");
        plain.setPos(helper.absoluteVec(new Vec3(1.5, 1, 2.5)));
        helper.getLevel().addFreshEntity(plain);
        TalentSearch.search(player, new ItemStack(ModItems.RULERS_SWORD.get()));
        helper.assertTrue(!plain.hasEffect(MobEffects.GLOWING), "doesn't glow");
        helper.assertTrue(!player.getData(ModAttachments.TALENTS_FOUND).contains(plain.getUUID()), "not remembered");
        plain.discard();
        helper.succeed();
    }

    /** The range is a sphere: 22 blocks along an axis is in, the corner of the cube (about 24.04 away) is out. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentSearchRangeIsASphere(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Villager in = helper.spawn(EntityType.VILLAGER, new BlockPos(22, 1, 0));
        Villager corner = helper.spawn(EntityType.VILLAGER, new BlockPos(17, 1, 17));
        in.addTag(Talents.BAIRAN_RESIDENT);
        corner.addTag(Talents.BAIRAN_RESIDENT);
        TalentSearch.search(player, new ItemStack(ModItems.RULERS_SWORD.get()));
        helper.assertTrue(in.hasEffect(MobEffects.GLOWING), "22 blocks away is found");
        helper.assertTrue(!corner.hasEffect(MobEffects.GLOWING), "the corner of the cube is not");
        in.discard();
        corner.discard();
        helper.succeed();
    }

    /** Using the Ruler's Sword in the main hand searches and starts the cooldown; the off hand and the Commander's don't. */
    @GameTest(template = TestSupport.EMPTY)
    public static void usingTheRulersSword(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Villager resident = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        resident.addTag(Talents.BAIRAN_RESIDENT);
        ItemStack rulers = new ItemStack(ModItems.RULERS_SWORD.get());
        player.setItemInHand(InteractionHand.OFF_HAND, rulers);
        var off = rulers.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(off.getResult() == net.minecraft.world.InteractionResult.PASS && !player.getCooldowns().isOnCooldown(rulers.getItem())
                && !resident.hasEffect(MobEffects.GLOWING), "off hand: pass, no search, no cooldown");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        player.setItemInHand(InteractionHand.MAIN_HAND, rulers);
        var main = rulers.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(main.getResult().consumesAction() && resident.hasEffect(MobEffects.GLOWING), "main hand: it searches");
        helper.assertTrue(player.getCooldowns().isOnCooldown(rulers.getItem()), "and the cooldown starts");

        ItemStack commanders = new ItemStack(ModItems.COMMANDERS_SWORD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, commanders);
        var pass = commanders.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pass.getResult() == net.minecraft.world.InteractionResult.PASS && !player.getCooldowns().isOnCooldown(commanders.getItem()),
                "the Commander's has no Talent Search");
        resident.discard();
        helper.succeed();
    }

    /** The Talents tag makes a villager hide a talent, both when added and when loaded from the NBT Bairan writes. */
    @GameTest(template = TestSupport.EMPTY)
    public static void bairanTagMakesAVillagerTalented(GameTestHelper helper) {
        Villager tagged = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        tagged.addTag(Talents.BAIRAN_RESIDENT);
        helper.assertTrue(Talents.of(tagged).isPresent(), "tagged: has a talent");

        // The NBT of Bairan's resident: a "Tags" list with the tag, loaded into a fresh villager.
        Villager source = EntityType.VILLAGER.create(helper.getLevel());
        source.setUUID(new UUID(1, 0)); // without the tag it would have none
        net.minecraft.nbt.CompoundTag nbt = source.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf(Talents.BAIRAN_RESIDENT));
        nbt.put("Tags", tags);
        Villager loaded = EntityType.VILLAGER.create(helper.getLevel());
        loaded.load(nbt);
        helper.assertTrue(loaded.getTags().contains(Talents.BAIRAN_RESIDENT) && Talents.of(loaded).isPresent(), "loaded from the NBT: has a talent");
        tagged.discard();
        helper.succeed();
    }

    /** Dropping the quest and taking it again doesn't make the same villagers count again. */
    @GameTest(template = TestSupport.EMPTY)
    public static void talentsAreNotRecountedAfterRetakingTheQuest(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        VillageChiefNpc chief = helper.spawn(ModEntities.VILLAGE_CHIEF.get(), new BlockPos(1, 1, 1));
        player.moveTo(chief.position());
        Villager resident = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        resident.addTag(Talents.BAIRAN_RESIDENT);
        ResourceLocation previous = Overgearedmod.id("watch_commander");
        player.setData(ModAttachments.PLAYER_QUESTS, PlayerQuests.EMPTY.accept(previous, 0).complete(previous));

        helper.assertTrue(QuestService.accept(player, TALENTS_QUEST, chief) == QuestService.Result.SUCCESS, "accepted");
        int before = GoalTracker.milestones(player, TalentSearch.MILESTONE);
        helper.assertTrue(TalentSearch.search(player, lentSword(player)) >= 1, "the resident is found");
        int after = GoalTracker.milestones(player, TalentSearch.MILESTONE);
        QuestService.abandon(player, TALENTS_QUEST);
        helper.assertTrue(QuestService.accept(player, TALENTS_QUEST, chief) == QuestService.Result.SUCCESS, "accepted again");
        helper.assertTrue(TalentSearch.search(player, lentSword(player)) == 0, "already known to the player");
        helper.assertTrue(GoalTracker.milestones(player, TalentSearch.MILESTONE) == after && after > before, "the lifetime milestone didn't move");
        QuestService.abandon(player, TALENTS_QUEST);
        resident.discard();
        chief.discard();
        helper.succeed();
    }

    private static ItemStack lentSword(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.RULERS_SWORD.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
