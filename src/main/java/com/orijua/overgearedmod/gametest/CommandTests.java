package com.orijua.overgearedmod.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.command.SiteLanding;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.pagma.AshurBetrayal;
import com.orijua.overgearedmod.pagma.PagmaStory;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.structure.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

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
        run(helper, player, "satisfy simulate kill minecraft:skeleton 30");
        helper.assertTrue(GoalTracker.counters(player).get("kill/minecraft:skeleton") == 30,
                "30 skeleton kills counted: " + GoalTracker.counters(player));
        helper.assertTrue(QuestService.get(player).isDiscovered(call), "30 simulated kills reveal the hidden quest");

        run(helper, player, "satisfy quest reset");
        helper.assertFalse(QuestService.get(player).isDiscovered(call), "quest reset forgets discoveries");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void discoveredQuestIsNewUntilTheTabIsOpened(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation call = Overgearedmod.id("call_of_the_night");
        run(helper, player, "satisfy simulate kill minecraft:skeleton 30");
        helper.assertTrue(QuestService.get(player).isNew(call), "a fresh discovery is new");
        QuestService.markDiscoveriesSeen(player); // what opening the Quests tab asks the server to do
        helper.assertFalse(QuestService.get(player).isNew(call), "seen once the tab was opened");
        helper.assertTrue(QuestService.get(player).isDiscovered(call), "and still discovered");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void simulatedForgesCountForTheirTier(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        run(helper, player, "satisfy simulate forge epic 2");
        helper.assertTrue(GoalTracker.counters(player).get("forge/epic") == 2, "2 epic forges: " + GoalTracker.counters(player));
        helper.assertTrue(GoalTracker.counters(player).get("forge/rare") == 2, "which also count as rare");
        helper.succeed();
    }

    /** Completing the end of Smith's chain completes the whole chain, so Ashur listens (what the command is for). */
    @GameTest(template = TestSupport.EMPTY)
    public static void questCompleteMarksTheWholeChainDone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        int emeralds = player.getInventory().countItem(Items.EMERALD);
        run(helper, player, "satisfy debug quest complete overgearedmod:quality_test");
        PlayerQuests quests = QuestService.get(player);
        helper.assertTrue(quests.isCompleted(PagmaStory.SMITH_CHAIN_END), "the quest itself");
        helper.assertTrue(quests.isCompleted(Overgearedmod.id("first_commission")), "and the one before it");
        helper.assertTrue(quests.isCompleted(Overgearedmod.id("smith_firewood")), "back to the start of the chain");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == emeralds, "no rewards");

        run(helper, player, "satisfy debug quest complete overgearedmod:quality_test");
        helper.assertTrue(QuestService.get(player).timesCompleted(PagmaStory.SMITH_CHAIN_END) == 1, "done ones are not done twice");

        AshurBetrayal.talk(player);
        helper.assertTrue(QuestService.get(player).isDiscovered(PagmaStory.SECRET_REQUEST), "Ashur now tells of his request");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void gotoLandsOnTheSurfaceOrRefuses(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ServerLevel level = helper.getLevel();
        long seed = level.getSeed();
        Optional<ChunkPos> bairan = WorldSites.siteFor(seed, WorldSites.BAIRAN);
        Optional<ChunkPos> cave = WorldSites.siteFor(seed, WorldSites.NORTH_END_CAVE);
        WorldSites.forget(seed, WorldSites.NORTH_END_CAVE);
        WorldSites.remember(seed, WorldSites.BAIRAN, new ChunkPos(level.getSharedSpawnPos()));
        try {
            helper.assertTrue(SiteLanding.find(level, WorldSites.NORTH_END_CAVE).isEmpty(), "no cave in this world: nowhere to go");
            helper.assertTrue(SiteLanding.find(level, "nowhere").isEmpty(), "an unknown place");

            BlockPos landing = SiteLanding.find(level, WorldSites.BAIRAN).orElseThrow();
            BlockPos spawn = level.getSharedSpawnPos();
            helper.assertTrue(landing.getX() == spawn.getX() && landing.getZ() == spawn.getZ(), "Bairan: its plaza, the world spawn");
            helper.assertTrue(level.getBlockState(landing).getCollisionShape(level, landing).isEmpty()
                    && level.getBlockState(landing.above()).getCollisionShape(level, landing.above()).isEmpty(), "room to stand");
            helper.assertFalse(level.getBlockState(landing.below()).isAir(), "on the ground");

            // A fake player ignores the teleport packet, so only the command's success is checked here.
            helper.assertTrue(execute(helper, player, "satisfy debug goto bairan") == 1, "the command goes there");
        } finally {
            bairan.ifPresentOrElse(chunk -> WorldSites.remember(seed, WorldSites.BAIRAN, chunk), () -> WorldSites.forget(seed, WorldSites.BAIRAN));
            cave.ifPresent(chunk -> WorldSites.remember(seed, WorldSites.NORTH_END_CAVE, chunk));
        }
        helper.succeed();
    }
}
