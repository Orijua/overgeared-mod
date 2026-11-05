package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * "type": "overgearedmod:satisfy_terrain", "channel": "continents|erosion|ridges", "map": "overgearedmod:...": the
 * climate parameter the vanilla terrain splines read, taken from a {@link SatisfyTerrain} relief map instead of noise.
 * It only depends on x and z, so the JSON wraps it in {@code flat_cache}.
 */
public record SatisfyTerrainFunction(SatisfyTerrain.Channel channel, ResourceLocation map, SatisfyTerrain terrain)
        implements DensityFunction.SimpleFunction {
    public static final MapCodec<SatisfyTerrainFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(SatisfyTerrain.Channel::values).fieldOf("channel").forGetter(SatisfyTerrainFunction::channel),
            ResourceLocation.CODEC.fieldOf("map").forGetter(SatisfyTerrainFunction::map)
    ).apply(instance, SatisfyTerrainFunction::new));
    public static final KeyDispatchDataCodec<SatisfyTerrainFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

    public SatisfyTerrainFunction(SatisfyTerrain.Channel channel, ResourceLocation map) {
        this(channel, map, SatisfyTerrains.get(map));
    }

    @Override
    public double compute(FunctionContext context) {
        return channel.of(terrain.relief(context.blockX(), context.blockZ()));
    }

    @Override
    public double minValue() {
        return channel.min();
    }

    @Override
    public double maxValue() {
        return channel.max();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DATA_CODEC;
    }
}
