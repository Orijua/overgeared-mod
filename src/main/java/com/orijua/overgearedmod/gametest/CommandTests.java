package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.quest.QuestService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class CommandTests {
    private CommandTests() {
    }

    /** Runs a command as {@code player} with op permission, as typed in chat (without the slash). */
    private static void run(GameTestHelper helper, ServerPlayer player, String command) {
        helper.getLevel().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
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
}
