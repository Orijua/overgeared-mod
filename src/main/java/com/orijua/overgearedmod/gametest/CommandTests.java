package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.debug.SiteLanding;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.LogoutService;
import com.orijua.overgearedmod.feature.capsule.WorldSeparation;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.NpcGrade;
import com.orijua.overgearedmod.feature.pagma.AshurBetrayal;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.world.Satisfy;
import com.orijua.overgearedmod.feature.world.SatisfyMapData;
import com.orijua.overgearedmod.feature.world.SatisfyPlaces;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class CommandTests {
    private CommandTests() {
    }

    /** Runs a command as {@code player} with op permission, as typed in chat (without the slash). */
    private static void run(GameTestHelper helper, ServerPlayer player, String command) {
        helper.getLevel().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
    }

    /** Like {@link #run}, but returns the command's result and throws if it fails. */
    private static int execute(GameTestHelper helper, ServerPlayer player, String command) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, player.createCommandSourceStack().withPermission(4));
        } catch (CommandSyntaxException e) {
            throw new AssertionError(command + ": " + e.getMessage(), e);
        }
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void simulatedKillsCountAndRevealHiddenQuests(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation call = Overgearedmod.id("call_of_the_night");
        run(helper, player, "satisfy debug simulate kill minecraft:skeleton 30");
        helper.assertTrue(GoalTracker.counters(player).get("kill/minecraft:skeleton") == 30,
                "30 skeleton kills counted: " + GoalTracker.counters(player));
        helper.assertTrue(QuestService.get(player).isDiscovered(call), "30 simulated kills reveal the hidden quest");

        run(helper, player, "satisfy debug reset quests");
        helper.assertFalse(QuestService.get(player).isDiscovered(call), "quest reset forgets discoveries");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void discoveredQuestIsNewUntilTheTabIsOpened(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation call = Overgearedmod.id("call_of_the_night");
        run(helper, player, "satisfy debug simulate kill minecraft:skeleton 30");
        helper.assertTrue(QuestService.get(player).isNew(call), "a fresh discovery is new");
        QuestService.markDiscoveriesSeen(player); // what opening the Quests tab asks the server to do
        helper.assertFalse(QuestService.get(player).isNew(call), "seen once the tab was opened");
        helper.assertTrue(QuestService.get(player).isDiscovered(call), "and still discovered");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void simulatedForgesCountForTheirTier(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        run(helper, player, "satisfy debug simulate forge epic 2");
        helper.assertTrue(GoalTracker.counters(player).get("forge/epic") == 2, "2 epic forges: " + GoalTracker.counters(player));
        helper.assertTrue(GoalTracker.counters(player).get("forge/rare") == 2, "which also count as rare");
        helper.succeed();
    }

    /** Completing the end of Smith's chain completes the whole chain, so Ashur listens (what the command is for). */
    @GameTest(template = TestSupport.EMPTY)
    public static void questCompleteMarksTheWholeChainDone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        int emeralds = player.getInventory().countItem(ModItems.SATISFY_EMERALD.get());
        run(helper, player, "satisfy debug quest complete overgearedmod:quality_test");
        PlayerQuests quests = QuestService.get(player);
        helper.assertTrue(quests.isCompleted(PagmaStory.SMITH_CHAIN_END), "the quest itself");
        helper.assertTrue(quests.isCompleted(Overgearedmod.id("first_commission")), "and the one before it");
        helper.assertTrue(quests.isCompleted(Overgearedmod.id("smith_firewood")), "back to the start of the chain");
        helper.assertTrue(player.getInventory().countItem(ModItems.SATISFY_EMERALD.get()) == emeralds, "no rewards");

        run(helper, player, "satisfy debug quest complete overgearedmod:quality_test");
        helper.assertTrue(QuestService.get(player).timesCompleted(PagmaStory.SMITH_CHAIN_END) == 1, "done ones are not done twice");

        AshurBetrayal.talk(player);
        helper.assertTrue(QuestService.get(player).isDiscovered(PagmaStory.SECRET_REQUEST), "Ashur now tells of his request");
        helper.succeed();
    }

    /** An apprentice of a chosen grade and level, to see his window without the 90/9/1 roll (playtest). */
    @GameTest(template = TestSupport.EMPTY)
    public static void debugApprenticeHiresTheChosenGrade(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(execute(helper, player, "satisfy debug set smithing 80") > 0, "smithing raised");
        helper.assertTrue(execute(helper, player, "satisfy debug spawn apprentice named 30") == 1, "hired");
        List<UUID> hired = player.getData(ModAttachments.APPRENTICES);
        helper.assertTrue(hired.size() == 1 && helper.getLevel().getEntity(hired.getFirst()) instanceof ApprenticeSmith,
                "one apprentice, in the world");
        ApprenticeSmith apprentice = (ApprenticeSmith) helper.getLevel().getEntity(hired.getFirst());
        helper.assertTrue(apprentice.grade() == NpcGrade.NAMED && apprentice.isOwnedBy(player), "named, and his");
        helper.assertTrue(apprentice.smithingLevel() == 30, "smithing 30: " + apprentice.smithingLevel());
        apprentice.discard();
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void gotoLandsOnTheSurfaceOrRefuses(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ServerLevel level = helper.getLevel();
        long seed = level.getSeed();
        Optional<ChunkPos> bairan = WorldSites.siteFor(seed, WorldSites.BAIRAN);
        Optional<ChunkPos> cave = WorldSites.siteFor(seed, WorldSites.NORTH_END_CAVE);
        Optional<BlockPos> landingBefore = WorldSites.landingFor(seed);
        try {
            WorldSites.forget(seed, WorldSites.NORTH_END_CAVE);
            Satisfy.standIn(level); // the test server has no Satisfy dimension
            WorldSites.remember(seed, WorldSites.BAIRAN, new ChunkPos(level.getSharedSpawnPos()));
            WorldSites.rememberLanding(seed, level.getSharedSpawnPos());
            helper.assertTrue(SiteLanding.locate(level, WorldSites.NORTH_END_CAVE).reason() == SiteLanding.Reason.NO_SITE, "no cave: NO_SITE, not sea");
            helper.assertTrue(SiteLanding.locate(level, "red_sea").reason() == SiteLanding.Reason.SEA, "a sea place: SEA");
            helper.assertTrue(SiteLanding.locate(level, "nowhere").reason() == SiteLanding.Reason.UNKNOWN, "an unknown name: UNKNOWN");
            helper.assertTrue(SiteLanding.find(level, WorldSites.NORTH_END_CAVE).isEmpty(), "no cave in this world: nowhere to go");
            helper.assertTrue(SiteLanding.find(level, "nowhere").isEmpty(), "an unknown place");

            BlockPos landing = SiteLanding.find(level, WorldSites.BAIRAN).orElseThrow();
            BlockPos spawn = level.getSharedSpawnPos();
            helper.assertTrue(landing.getX() == spawn.getX() && landing.getZ() == spawn.getZ(), "Bairan: its plaza, where the landing was saved");
            helper.assertTrue(level.getBlockState(landing).getCollisionShape(level, landing).isEmpty()
                    && level.getBlockState(landing.above()).getCollisionShape(level, landing.above()).isEmpty(), "room to stand");
            helper.assertFalse(level.getBlockState(landing.below()).isAir(), "on the ground");

            // A fake player ignores the teleport packet, so only the command's success is checked here.
            helper.assertTrue(execute(helper, player, "satisfy debug goto bairan") == 1, "the command goes there");

            // Bairan with no site and no landing in this world: "no such place", never "in the sea".
            WorldSites.forget(seed, WorldSites.BAIRAN);
            WorldSites.forgetLanding(seed);
            helper.assertTrue("message.overgearedmod.debug_goto_none".equals(refusal(helper, player, "satisfy debug goto bairan")),
                    "Bairan without a site is refused as 'none'");
            helper.assertTrue("message.overgearedmod.debug_goto_none".equals(refusal(helper, player, "satisfy debug goto north_end_cave")),
                    "the cave without a site is refused as 'none'");
        } finally {
            bairan.ifPresentOrElse(chunk -> WorldSites.remember(seed, WorldSites.BAIRAN, chunk), () -> WorldSites.forget(seed, WorldSites.BAIRAN));
            cave.ifPresent(chunk -> WorldSites.remember(seed, WorldSites.NORTH_END_CAVE, chunk));
            Satisfy.restoreStandIn();
            landingBefore.ifPresentOrElse(pos -> WorldSites.rememberLanding(seed, pos), () -> WorldSites.forgetLanding(seed));
        }
        helper.succeed();
    }

    /** Runs a command with the given permission level and returns its result (a refusal throws). */
    private static int executeWith(GameTestHelper helper, ServerPlayer player, String command, int permission) throws CommandSyntaxException {
        return helper.getLevel().getServer().getCommands().getDispatcher()
                .execute(command, player.createCommandSourceStack().withPermission(permission));
    }

    private static boolean refusedWith(GameTestHelper helper, ServerPlayer player, String command, int permission) {
        try {
            executeWith(helper, player, command, permission);
            return false;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    /**
     * Whether a source with {@code permission} may use every node along {@code path} (literal names). Fails the test if
     * the command does not exist, so a missing command never counts as "denied".
     */
    private static boolean canUse(GameTestHelper helper, ServerPlayer player, int permission, String... path) {
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(permission);
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        boolean allowed = true;
        for (String name : path) {
            node = node.getChild(name);
            helper.assertTrue(node != null, "the command node exists: " + String.join(" ", path));
            allowed &= node.canUse(source);
        }
        return allowed;
    }

    /** /satisfy logout is for everyone; the debug and validate parts of /satisfy stay op-only. */
    @GameTest(template = TestSupport.EMPTY)
    public static void logoutIsOpenToEveryoneButDebugAndValidateAreNot(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            try {
                helper.assertTrue(executeWith(helper, player, "satisfy logout", 0) == 1, "in Satisfy: it starts");
                helper.assertTrue(LogoutService.isCountingDown(player), "and the countdown is running");
            } catch (CommandSyntaxException e) {
                throw new AssertionError("satisfy logout with permission 0: " + e.getMessage(), e);
            } finally {
                LogoutService.forgetAll();
            }
        });
        CapsuleTests.outsideSatisfy(helper, () -> {
            try {
                helper.assertTrue(executeWith(helper, player, "satisfy logout", 0) == 0, "outside Satisfy: it returns 0");
                helper.assertFalse(LogoutService.isCountingDown(player), "and nothing counts");
            } catch (CommandSyntaxException e) {
                throw new AssertionError("satisfy logout with permission 0: " + e.getMessage(), e);
            }
        });
        helper.assertTrue(canUse(helper, player, 0, "satisfy", "logout"), "logout is open to everyone");
        helper.assertFalse(canUse(helper, player, 0, "satisfy", "debug", "reset", "quests"), "debug needs op (the node is denied)");
        helper.assertFalse(canUse(helper, player, 0, "satisfy", "validate"), "validate needs op (the node is denied)");
        helper.assertTrue(canUse(helper, player, 4, "satisfy", "debug", "reset", "quests"), "debug node is open to ops");
        helper.assertTrue(canUse(helper, player, 4, "satisfy", "validate"), "validate node is open to ops");
        helper.assertTrue(refusedWith(helper, player, "satisfy debug reset quests", 0), "debug needs op");
        helper.assertTrue(refusedWith(helper, player, "satisfy validate", 0), "validate needs op");
        helper.assertFalse(refusedWith(helper, player, "satisfy debug reset quests", 4), "debug works for ops");
        helper.assertFalse(refusedWith(helper, player, "satisfy validate", 4), "and so does validate");
        helper.succeed();
    }

    /** In danger, or with a countdown already running, /satisfy logout returns 0 and starts nothing new. */
    @GameTest(template = TestSupport.EMPTY)
    public static void logoutIsRefusedInDangerAndWhileAlreadyRunning(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            try {
                LogoutService.markCombat(player, helper.getLevel().getServer().getTickCount());
                helper.assertTrue(executeWith(helper, player, "satisfy logout", 0) == 0, "in danger: returns 0");
                helper.assertFalse(LogoutService.isCountingDown(player), "and nothing counts (the message is client-side: N4)");

                LogoutService.forgetAll();
                helper.assertTrue(executeWith(helper, player, "satisfy logout", 0) == 1, "out of danger: starts");
                helper.assertTrue(executeWith(helper, player, "satisfy logout", 0) == 0, "already running: returns 0");
                helper.assertTrue(LogoutService.isCountingDown(player), "and the first countdown goes on");
            } catch (CommandSyntaxException e) {
                throw new AssertionError("satisfy logout with permission 0: " + e.getMessage(), e);
            } finally {
                LogoutService.forgetAll();
            }
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void debugDisconnectIsInstant(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CapsuleTests.inSatisfy(helper, helper.absolutePos(new BlockPos(1, 1, 1)), () -> {
            helper.assertTrue(execute(helper, player, "satisfy debug capsule disconnect") == 1, "no countdown: it disconnects at once");
            helper.assertTrue(CapsuleService.state(player).lastSave().isPresent(), "and the spot is saved");
        });
        helper.succeed();
    }

    /** The debug goto is a permitted crossing: its flag is not left on, and it still goes (with the test level as Satisfy). */
    @GameTest(template = TestSupport.EMPTY)
    public static void gotoStillWorksAndLeavesNoCrossingFlag(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        try {
            Satisfy.standIn(helper.getLevel());
            helper.assertTrue(execute(helper, player, "satisfy debug goto reidan") == 1, "goto reidan");
            helper.assertFalse(WorldSeparation.crossingAllowed(), "the flag is off afterwards");
        } finally {
            Satisfy.restoreStandIn();
        }
        helper.succeed();
    }

    /** The key of the message a command fails with, or {@code null} if it does not fail. */
    private static String refusal(GameTestHelper helper, ServerPlayer player, String command) {
        try {
            helper.getLevel().getServer().getCommands().getDispatcher().execute(command, player.createCommandSourceStack().withPermission(4));
            return null;
        } catch (CommandSyntaxException e) {
            return e.getRawMessage() instanceof Component component && component.getContents() instanceof TranslatableContents contents
                    ? contents.getKey() : e.getMessage();
        }
    }

    /** The map's canon places (C4): goto lands on dry ground near them, refuses the sea and unknown names, and completes them. */
    @GameTest(template = TestSupport.EMPTY)
    public static void gotoReachesTheMapPlacesOrRefusesTheSea(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ServerLevel level = helper.getLevel();
        Satisfy.standIn(level); // the test server has no Satisfy dimension: its flat overworld plays it
        try {
            helper.assertTrue(SiteLanding.sites().containsAll(List.of(WorldSites.BAIRAN, WorldSites.NORTH_END_CAVE, WorldSites.ASHUR_MANOR,
                    WorldSites.WINSTON, "reidan", "titan", "behen_archipelago")), "structures and map places: " + SiteLanding.sites());
            helper.assertTrue(SiteLanding.sites().size() == SiteLanding.sites().stream().distinct().count(), "each name once");

            SatisfyMapData map = SatisfyMapData.west();
            SatisfyPlaces.Place reidan = SatisfyPlaces.west().get("reidan").orElseThrow();
            BlockPos landing = SiteLanding.findPlace(level, "reidan").orElseThrow(() -> new AssertionError("Reidan has a landing"));
            int[] expected = SatisfyPlaces.landing(map, reidan).orElseThrow();
            helper.assertTrue(Math.abs(landing.getX() - expected[0]) <= SiteLanding.DRY_SEARCH && Math.abs(landing.getZ() - expected[1]) <= SiteLanding.DRY_SEARCH,
                    "Reidan: near its cell on the map, " + landing);
            helper.assertTrue(map.isLandCell(map.colAtBlock(landing.getX()), map.rowAtBlock(landing.getZ())), "on a land cell of the map");
            helper.assertTrue(level.getBlockState(landing).getCollisionShape(level, landing).isEmpty()
                    && level.getBlockState(landing.above()).getCollisionShape(level, landing.above()).isEmpty(), "room to stand");
            helper.assertFalse(level.getBlockState(landing.below()).isAir(), "on the ground");
            helper.assertTrue(level.getFluidState(landing.below()).isEmpty(), "not on water");

            // Every land place of the map that is not also a structure of the world (those have their own rules).
            long started = System.nanoTime();
            int visited = 0;
            for (SatisfyPlaces.Place place : SatisfyPlaces.west().all()) {
                if (!place.land() || SiteLanding.STRUCTURE_SITES.contains(place.id())) {
                    continue;
                }
                helper.assertTrue(SiteLanding.locate(level, place.id()).reason() != SiteLanding.Reason.UNKNOWN, place.id() + " is known");
                SiteLanding.Landing there = SiteLanding.locate(level, place.id());
                helper.assertTrue(there.reason() == SiteLanding.Reason.OK || there.reason() == SiteLanding.Reason.SEA, place.id() + ": " + there);
                if (there.reason() == SiteLanding.Reason.OK) {
                    BlockPos spot = there.pos().get();
                    helper.assertTrue(level.getFluidState(spot.below()).isEmpty(), place.id() + " lands on dry ground: " + spot);
                    visited++;
                }
            }
            Overgearedmod.LOGGER.info("goto: {} map places landed in {} ms", visited, (System.nanoTime() - started) / 1_000_000);
            helper.assertTrue(visited >= 10, "most land places have a landing: " + visited);

            helper.assertTrue(execute(helper, player, "satisfy debug goto reidan") == 1, "goto reidan");
            helper.assertTrue(execute(helper, player, "satisfy debug goto behen_archipelago") == 1, "goto an island of Behen");
            helper.assertTrue("message.overgearedmod.debug_goto_sea".equals(refusal(helper, player, "satisfy debug goto red_sea")),
                    "the Red Sea is refused as sea");
            helper.assertTrue("message.overgearedmod.debug_goto_unknown".equals(refusal(helper, player, "satisfy debug goto atlantis")),
                    "an unknown name is refused");

            List<String> suggested = level.getServer().getCommands().getDispatcher()
                    .getCompletionSuggestions(level.getServer().getCommands().getDispatcher()
                            .parse("satisfy debug goto ti", player.createCommandSourceStack().withPermission(4)))
                    .join().getList().stream().map(Suggestion::getText).toList();
            helper.assertTrue(suggested.contains("titan"), "titan is suggested: " + suggested);
        } finally {
            Satisfy.restoreStandIn();
        }
        helper.succeed();
    }
}
