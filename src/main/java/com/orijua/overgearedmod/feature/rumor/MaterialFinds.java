package com.orijua.overgearedmod.feature.rumor;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;
import java.util.function.Supplier;

/** The first time a player mines a rare ore it counts as a milestone, so the rumors about it are put to rest. */
public final class MaterialFinds {
    public static final ResourceLocation MITHRIL = Overgearedmod.id("found_mithril");
    public static final ResourceLocation BLUE_ORICHALCUM = Overgearedmod.id("found_blue_orichalcum");

    private static final Map<Supplier<? extends Block>, ResourceLocation> ORES = Map.of(
            ModBlocks.MITHRIL_ORE, MITHRIL,
            ModBlocks.DEEPSLATE_MITHRIL_ORE, MITHRIL,
            ModBlocks.BLUE_ORICHALCUM_ORE, BLUE_ORICHALCUM,
            ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE, BLUE_ORICHALCUM);

    private MaterialFinds() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MaterialFinds::onBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ORES.forEach((ore, milestone) -> {
            if (event.getState().is(ore.get()) && GoalTracker.milestones(player, milestone) == 0) {
                GoalTracker.milestone(player, milestone);
            }
        });
    }
}
