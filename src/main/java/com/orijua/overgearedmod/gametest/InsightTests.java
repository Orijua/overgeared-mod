package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.insight.InsightRules;
import com.orijua.overgearedmod.feature.insight.Observation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Insight: observing someone (sneaking while looking) for ten seconds gives a point, once per target and day. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class InsightTests {
    private InsightTests() {
    }

    /** Observes {@code target} for {@code ticks} ticks; returns how many points that gave. */
    private static int observeFor(ServerPlayer player, Villager target, int ticks) {
        return observeFor(player, target, ticks, player.level().getGameTime());
    }

    /** As {@link #observeFor(ServerPlayer, Villager, int)} at a chosen game time (does not touch the world's clock). */
    private static int observeFor(ServerPlayer player, Villager target, int ticks, long gameTime) {
        int gained = 0;
        for (int i = 0; i < ticks; i++) {
            if (Observation.observe(player, target, gameTime)) {
                gained++;
            }
        }
        return gained;
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void observingTeachesOncePerTargetAndDay(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Observation.forget(player);
        Villager first = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        Villager second = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 1));

        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS - 1) == 0, "not before ten seconds");
        Observation.observe(player, null);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS - 1) == 0, "looking away starts it again");
        helper.assertTrue(observeFor(player, first, 1) == 1 && Observation.insight(player) == 1, "a point after ten steady seconds");
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS * 2) == 0, "the same villager teaches once a day");
        helper.assertTrue(observeFor(player, second, InsightRules.OBSERVE_TICKS) == 1 && Observation.insight(player) == 2, "another one does");

        // The next day the same villager teaches again; the one learned from today stays blocked until then.
        long t = 5 * InsightRules.DAY_TICKS + 100;
        Observation.forget(player);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS, t) == 1, "wins at day t");
        Observation.observe(player, null);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS, t) == 0, "not twice the same day");
        Observation.observe(player, null);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS, t + InsightRules.DAY_TICKS) == 1, "wins again a day later");
        Observation.observe(player, null);
        helper.assertTrue(observeFor(player, second, InsightRules.OBSERVE_TICKS, t + InsightRules.DAY_TICKS) == 1,
                "a target learned yesterday doesn't block another today");
        Observation.observe(player, null);

        Observation.forget(player);
        player.setData(ModAttachments.INSIGHT, InsightRules.MEDIUM - 1);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS) == 1, "crossing a threshold works too");
        helper.assertTrue(InsightRules.detail(Observation.insight(player)) == InsightRules.Detail.MEDIUM, "now medium detail");
        first.discard();
        second.discard();
        helper.succeed();
    }

    /** Losing the bar (looking away, logging out) doesn't give the day back: only the saved state counts. */
    @GameTest(template = TestSupport.EMPTY)
    public static void lookingAwayDoesNotResetTheDay(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Observation.forget(player);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        helper.assertTrue(observeFor(player, villager, InsightRules.OBSERVE_TICKS) == 1, "learned once");
        Observation.observe(player, null); // what logging out does to the bar
        helper.assertTrue(player.getData(ModAttachments.INSIGHT_LEARNED).has(villager.getUUID(),
                InsightRules.day(player.level().getGameTime())), "still remembered");
        helper.assertTrue(observeFor(player, villager, InsightRules.OBSERVE_TICKS) == 0, "still blocked");
        villager.discard();
        helper.succeed();
    }

    private static Villager still(GameTestHelper helper, BlockPos at) {
        Villager villager = helper.spawn(EntityType.VILLAGER, at);
        villager.setNoAi(true);
        return villager;
    }

    /** Sneaks the fake player and aims it level towards +Z (setShiftKeyDown alone may not stick on a FakePlayer). */
    private static void sneakAhead(ServerPlayer player) {
        player.setShiftKeyDown(true);
        player.setPose(Pose.CROUCHING);
        player.setXRot(0.0f);
        player.setYRot(0.0f);
        player.setYHeadRot(0.0f);
    }

    /** Debug text for a failed targeting assertion: sneaking state, eye and villager positions. */
    private static String why(ServerPlayer player, Villager villager) {
        return " [shift=" + player.isShiftKeyDown() + " spectator=" + player.isSpectator() + " eye=" + player.getEyePosition()
                + " view=" + player.getViewVector(1.0f) + " villager=" + villager.position() + " alive=" + villager.isAlive()
                + " los=" + player.hasLineOfSight(villager) + " lookedAt=" + Observation.lookedAt(player) + "]";
    }

    /**
     * The fake player stands at (0.5, 1, 0.5) looking towards +Z. Everything that must be seen stays inside the 3x3x3 template:
     * outside it the game test ring of barriers blocks the line of sight (that is what failed in CI).
     */
    @GameTest(template = TestSupport.EMPTY)
    public static void targetNeedsSneakingReachAndSight(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Villager near = still(helper, new BlockPos(0, 1, 2));
        helper.assertTrue(Observation.target(player) == null, "not sneaking: nobody");
        sneakAhead(player);
        helper.assertTrue(Observation.target(player) == near, "sneaking at a villager 2 blocks away" + why(player, near));

        near.kill();
        helper.assertTrue(Observation.target(player) == null, "a dead one isn't observed");
        near.discard();

        Villager far = still(helper, new BlockPos(0, 1, 20));
        helper.assertTrue(Observation.target(player) == null, "beyond the reach");
        far.discard();

        Villager hidden = still(helper, new BlockPos(0, 1, 2));
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.STONE);
        helper.assertTrue(Observation.target(player) == null, "a wall in between");
        hidden.discard();
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.AIR);

        ServerPlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "satisfy-other"));
        other.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(0.5, 1, 2.5)));
        helper.assertTrue(Observation.target(player) == null, "players aren't observed");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void spectatorsDoNotObserve(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Villager villager = still(helper, new BlockPos(0, 1, 2));
        sneakAhead(player);
        helper.assertTrue(Observation.target(player) == villager, "a survival player would observe" + why(player, villager));
        player.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(Observation.target(player) == null, "a spectator doesn't");
        player.setGameMode(GameType.SURVIVAL);
        villager.discard();
        helper.succeed();
    }
}
