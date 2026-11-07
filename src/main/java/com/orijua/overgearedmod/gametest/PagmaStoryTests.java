package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.pagma.AshurBetrayal;
import com.orijua.overgearedmod.feature.pagma.AshurKnight;
import com.orijua.overgearedmod.feature.pagma.AshurRetinue;
import com.orijua.overgearedmod.feature.pagma.EarlAshur;
import com.orijua.overgearedmod.feature.pagma.EyeOfPallalianItem;
import com.orijua.overgearedmod.feature.pagma.LegendTitle;
import com.orijua.overgearedmod.feature.pagma.PagmaBook;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.skill.SkillService;
import com.orijua.overgearedmod.feature.world.AshurManor;
import com.orijua.overgearedmod.feature.world.Satisfy;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Pagma's story (canon, caps. 1-3): Earl Ashur's request, appraising the book, his anger and reading the book. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class PagmaStoryTests {
    private PagmaStoryTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void ashurOnlyConfidesInSomeoneSmithTrusts(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        AshurBetrayal.talk(player);
        helper.assertFalse(QuestService.get(player).isDiscovered(PagmaStory.SECRET_REQUEST), "no time for a nobody");
        TestSupport.completed(player, "quality_test");
        AshurBetrayal.talk(player);
        helper.assertTrue(QuestService.get(player).isDiscovered(PagmaStory.SECRET_REQUEST), "after Smith's chain he tells of it");
        helper.succeed();
    }

    /** Bringing him the book unappraised completes the request: Ashur keeps it and pays. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theBookUnopenedFulfilsTheRequest(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        EarlAshur ashur = helper.spawn(ModEntities.EARL_ASHUR.get(), 1, 1, 2);
        TestSupport.completed(player, "quality_test");
        AshurBetrayal.talk(player);
        helper.assertTrue(QuestService.accept(player, PagmaStory.SECRET_REQUEST, ashur) == QuestService.Result.SUCCESS, "accepted at Ashur");
        player.getInventory().add(new ItemStack(ModItems.UNAPPRAISED_BOOK.get()));
        helper.assertTrue(QuestService.deliver(player, PagmaStory.SECRET_REQUEST, ashur) == QuestService.Result.SUCCESS, "delivered");
        helper.assertTrue(QuestService.claim(player, PagmaStory.SECRET_REQUEST, ashur) == QuestService.Result.SUCCESS, "claimed");
        helper.assertTrue(player.getInventory().countItem(ModItems.UNAPPRAISED_BOOK.get()) == 0, "Ashur has the book");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 48, "and paid for it");
        helper.assertFalse(PagmaStory.isAshursEnemy(player), "no anger");
        helper.succeed();
    }

    /** Appraising the book turns it into Pagma's rare book, and Ashur comes with his knights: the SS quest starts. */
    @GameTest(template = TestSupport.EMPTY)
    public static void appraisingTheBookBringsAshursAnger(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        TestSupport.completed(player, "quality_test");
        AshurBetrayal.talk(player);
        QuestService.accept(player, PagmaStory.SECRET_REQUEST, helper.spawn(ModEntities.EARL_ASHUR.get(), 1, 1, 2));
        helper.assertFalse(EyeOfPallalianItem.appraise(player), "nothing to appraise yet");
        player.getInventory().add(new ItemStack(ModItems.UNAPPRAISED_BOOK.get()));
        helper.assertTrue(EyeOfPallalianItem.appraise(player), "appraised");

        helper.assertTrue(player.getInventory().countItem(ModItems.PAGMA_RARE_BOOK.get()) == 1, "it is Pagma's rare book");
        helper.assertTrue(player.getInventory().countItem(ModItems.UNAPPRAISED_BOOK.get()) == 0, "no old book left");
        helper.assertTrue(PagmaStory.isAshursEnemy(player), "Ashur is after the player");
        helper.assertFalse(QuestService.get(player).isActive(PagmaStory.SECRET_REQUEST), "the secret request is over");
        helper.assertTrue(QuestService.get(player).isActive(PagmaStory.ANGER), "Earl Ashur's Anger has started");
        helper.assertTrue(QuestService.get(player).isDiscovered(PagmaStory.LEGACY), "and Pagma's legacy is revealed");

        List<AshurRetinue> summoned = retinueAround(helper, player);
        long earls = summoned.stream().filter(EarlAshur.class::isInstance).count();
        long knights = summoned.stream().filter(AshurKnight.class::isInstance).count();
        helper.assertTrue(earls == 1 && knights == AshurBetrayal.KNIGHTS, "Ashur and his knights came: " + earls + " + " + knights);
        EarlAshur earl = (EarlAshur) summoned.stream().filter(EarlAshur.class::isInstance).findFirst().orElseThrow();
        helper.assertFalse(earl.isInvulnerableTo(player.damageSources().playerAttack(player)), "the thief can fight him");
        helper.assertTrue(earl.isInvulnerableTo(player.damageSources().generic()), "nothing else hurts him");
        summoned.forEach(AshurRetinue::discard);
        helper.succeed();
    }

    /** Canon (cap. 2): whoever dies at Ashur's hands with the book loses it to him. */
    @GameTest(template = TestSupport.EMPTY)
    public static void dyingWhileAshurHuntsYouLosesTheBook(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.UNAPPRAISED_BOOK.get()));
        EyeOfPallalianItem.appraise(player);
        retinueAround(helper, player).forEach(AshurRetinue::discard);
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertTrue(player.getInventory().countItem(ModItems.PAGMA_RARE_BOOK.get()) == 0, "Ashur took it");
        helper.succeed();
    }

    /** Killing Ashur ends his anger: the SS quest can be claimed and the title "Noble Killer" is earned. */
    @GameTest(template = TestSupport.EMPTY)
    public static void killingAshurEndsHisAnger(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.UNAPPRAISED_BOOK.get()));
        EyeOfPallalianItem.appraise(player);
        retinueAround(helper, player).forEach(AshurRetinue::discard);
        TestSupport.kill(helper, player, ModEntities.EARL_ASHUR.get());
        helper.assertFalse(PagmaStory.isAshursEnemy(player), "he is dead");
        helper.assertTrue(player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(PagmaStory.NOBLE_KILLER), "Noble Killer");
        helper.assertTrue(QuestService.claim(player, PagmaStory.ANGER) == QuestService.Result.SUCCESS, "the SS quest is done");
        helper.succeed();
    }

    /**
     * Reading the book (canon, caps. 2-3): a warrior becomes Pagma's successor, back to level 1 with no stat points
     * spent, no skills of the old class and 20 points to spend; smithing stays, and the blueprint and title come with it.
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void readingTheBookMakesPagmasSuccessor(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ClassService.choose(player, Overgearedmod.id("warrior"));
        SkillService.learn(player, Overgearedmod.id("rending_slash"));
        ProgressionService.set(player, new PlayerStats(12, 40, 3, 9, 4, 2, 1, 5, 700));
        helper.assertFalse(PagmaBook.read(player), "no book, nothing to read");
        player.getInventory().add(new ItemStack(ModItems.PAGMA_RARE_BOOK.get()));
        helper.assertTrue(PagmaBook.read(player), "read");

        PlayerStats stats = ProgressionService.get(player);
        helper.assertTrue(stats.level() == 1 && stats.xp() == 0, "back to level 1: " + stats);
        helper.assertTrue(stats.freePoints() == PagmaStory.BONUS_POINTS, "20 points to spend: " + stats.freePoints());
        helper.assertTrue(stats.strength() == 0 && stats.dexterity() == 0, "spent points are gone: " + stats);
        helper.assertTrue(stats.smithingXp() == 700, "smithing stays");
        helper.assertTrue(ClassService.current(player).filter(PagmaStory.SUCCESSOR::equals).isPresent(), "Pagma's successor");
        helper.assertFalse(SkillService.get(player).knows(Overgearedmod.id("rending_slash")), "old skills are forgotten");
        helper.assertTrue(SkillService.get(player).knows(Overgearedmod.id("iron_guard")), "the successor's skills are learned");
        helper.assertTrue(player.getInventory().countItem(ModItems.FRACASO_BLUEPRINT.get()) == 1, "Fracaso's blueprint");
        helper.assertTrue(LegendTitle.has(player), "One Who Became a Legend");
        helper.assertTrue(QuestService.get(player).isCompleted(PagmaStory.LEGACY), "Pagma's legacy is done");
        helper.assertTrue(player.getInventory().countItem(ModItems.PAGMA_RARE_BOOK.get()) == 0, "the book is spent");
        helper.succeed();
    }

    /** "One Who Became a Legend": a killing blow leaves the player at 1 health, once every five minutes. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theLegendTitleHoldsOffDeath(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.PAGMA_RARE_BOOK.get()));
        PagmaBook.read(player);
        LivingDeathEvent first = NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertTrue(first.isCanceled() && player.getHealth() == 1.0f, "saved at 1 health");
        LivingDeathEvent second = NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertFalse(second.isCanceled(), "not twice in a row");
        LegendTitle.readyNow(player);
        player.setHealth(player.getMaxHealth());
        LivingDeathEvent third = NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertTrue(third.isCanceled() && player.getHealth() == 1.0f, "saved again once the cooldown is over");
        helper.succeed();
    }

    /** Only Ashur takes the book, and only from someone he hunts: dying with an unappraised-by-the-eye book keeps it. */
    @GameTest(template = TestSupport.EMPTY)
    public static void dyingWithoutAshurKeepsTheBook(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.PAGMA_RARE_BOOK.get()));
        helper.assertFalse(PagmaStory.isAshursEnemy(player), "Ashur is not after this player");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertTrue(player.getInventory().countItem(ModItems.PAGMA_RARE_BOOK.get()) == 1, "nobody took the book");
        helper.succeed();
    }

    /** A second book changes nothing once the legacy is done: it isn't spent, and stats and skills stay. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aSecondBookIsRefusedOnceTheLegacyIsDone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().add(new ItemStack(ModItems.PAGMA_RARE_BOOK.get()));
        helper.assertTrue(PagmaBook.read(player), "the first book is read");
        PlayerStats grown = ProgressionService.get(player).withProgress(12, 40, 3);
        ProgressionService.set(player, grown);
        SkillService.learn(player, Overgearedmod.id("iron_guard"));
        var skills = SkillService.get(player);

        player.getInventory().add(new ItemStack(ModItems.PAGMA_RARE_BOOK.get()));
        helper.assertFalse(PagmaBook.read(player), "the second book is refused");
        helper.assertTrue(player.getInventory().countItem(ModItems.PAGMA_RARE_BOOK.get()) == 1, "and kept");
        helper.assertTrue(ProgressionService.get(player).equals(grown), "stats are untouched");
        helper.assertTrue(SkillService.get(player).equals(skills), "skills are untouched");
        helper.succeed();
    }

    /** Ashur's map comes in the first talk where the world has a cave to show; the secret is told the first time anyway. */
    @GameTest(template = TestSupport.EMPTY)
    public static void ashurGivesTheMapOnceThereIsACave(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        TestSupport.completed(player, "quality_test");
        long seed = helper.getLevel().getSeed();
        AshurBetrayal.talk(player);
        helper.assertTrue(QuestService.get(player).isDiscovered(PagmaStory.SECRET_REQUEST), "the secret is told");
        helper.assertTrue(player.getInventory().countItem(Items.FILLED_MAP) == 0, "no map without the cave dimension");

        Satisfy.standIn(helper.getLevel());
        boolean hadCave = WorldSites.siteFor(seed, WorldSites.NORTH_END_CAVE).isPresent();
        if (!hadCave) {
            WorldSites.remember(seed, WorldSites.NORTH_END_CAVE, new ChunkPos(40, 0));
        }
        try {
            AshurBetrayal.talk(player);
            helper.assertTrue(player.getInventory().countItem(Items.FILLED_MAP) == 1, "the map comes once there is a cave");
            AshurBetrayal.talk(player);
            helper.assertTrue(player.getInventory().countItem(Items.FILLED_MAP) == 1, "and only once");
        } finally {
            Satisfy.restoreStandIn();
            if (!hadCave) {
                WorldSites.forget(seed, WorldSites.NORTH_END_CAVE);
            }
        }
        helper.succeed();
    }

    /** The manor: Ashur by his seat and two knights at the door, all wearing what they should. */
    @GameTest(template = AshurManor.MANOR)
    public static void theManorHasAshurAndHisGuard(GameTestHelper helper) {
        List<EarlAshur> earls = helper.getEntities(ModEntities.EARL_ASHUR.get());
        List<AshurKnight> knights = helper.getEntities(ModEntities.ASHUR_KNIGHT.get());
        helper.assertTrue(earls.size() == 1 && knights.size() == 2, "Ashur and two knights, got " + earls.size() + " + " + knights.size());
        helper.assertTrue(earls.getFirst().getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET), "his crown");
        helper.assertTrue(knights.stream().allMatch(knight -> knight.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD)), "armed");
        helper.assertFalse(earls.getFirst().isSummoned(), "he lives here");
        helper.succeed();
    }

    /** Those the betrayal summoned around the player (not the one a test spawned to talk to). */
    private static List<AshurRetinue> retinueAround(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(AshurRetinue.class, new AABB(player.blockPosition()).inflate(12), AshurRetinue::isSummoned);
    }
}
