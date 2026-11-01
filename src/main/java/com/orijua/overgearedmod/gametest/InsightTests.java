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
import net.minecraft.world.entity.npc.Villager;
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
        int gained = 0;
        for (int i = 0; i < ticks; i++) {
            if (Observation.observe(player, target)) {
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

        Observation.forget(player); // as if a day went by
        player.setData(ModAttachments.INSIGHT, InsightRules.MEDIUM - 1);
        helper.assertTrue(observeFor(player, first, InsightRules.OBSERVE_TICKS) == 1, "crossing a threshold works too");
        helper.assertTrue(InsightRules.detail(Observation.insight(player)) == InsightRules.Detail.MEDIUM, "now medium detail");
        first.discard();
        second.discard();
        helper.succeed();
    }
}
