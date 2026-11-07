package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.MapCodec;
import com.orijua.overgearedmod.core.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * "type": "overgearedmod:satisfy_only": lets a placed feature generate only in the Satisfy dimension (the mod's ores:
 * the biome modifiers add them to every overworld biome, and Satisfy uses vanilla biomes, so the dimension is the filter).
 * Reads {@link Satisfy#is} on the ServerLevel behind the WorldGenLevel, so the game-test stand-in is honored too.
 */
public final class SatisfyOnlyPlacement extends PlacementFilter {
    public static final SatisfyOnlyPlacement INSTANCE = new SatisfyOnlyPlacement();
    public static final MapCodec<SatisfyOnlyPlacement> CODEC = MapCodec.unit(INSTANCE);

    private SatisfyOnlyPlacement() {
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return Satisfy.is(context.getLevel().getLevel());
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.SATISFY_ONLY.get();
    }
}
