package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Optional;

/**
 * {@code "type": "overgearedmod:satisfy_terrain", "channel": "continents|erosion|ridges", "map": "overgearedmod:west",
 * "noise": "minecraft:overworld/continents"}: the climate parameter the vanilla terrain splines read, taken from a relief
 * map ({@link SatisfyTerrains}: the canon map or the test island) instead of noise. The optional {@code noise} (vanilla's
 * own function for that parameter) is added in a small share ({@link SatisfyTerrain.Channel#noiseWeight}), so coasts and
 * ranges are not smooth. Without noise it only depends on x and z; the JSON wraps it in {@code flat_cache} either way.
 */
public record SatisfyTerrainFunction(SatisfyTerrain.Channel channel, ResourceLocation map, SatisfyRelief relief,
                                     Optional<DensityFunction> noise) implements DensityFunction {
    public static final MapCodec<SatisfyTerrainFunction> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(SatisfyTerrain.Channel::values).fieldOf("channel").forGetter(Raw::channel),
            ResourceLocation.CODEC.fieldOf("map").forGetter(Raw::map),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("noise").forGetter(Raw::noise)
    ).apply(instance, Raw::new)).flatXmap(SatisfyTerrainFunction::create,
            function -> DataResult.success(new Raw(function.channel, function.map, function.noise)));
    public static final KeyDispatchDataCodec<SatisfyTerrainFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

    /** The fields as written in the JSON. */
    private record Raw(SatisfyTerrain.Channel channel, ResourceLocation map, Optional<DensityFunction> noise) {
    }

    public SatisfyTerrainFunction(SatisfyTerrain.Channel channel, ResourceLocation map, Optional<DensityFunction> noise) {
        this(channel, map, SatisfyTerrains.get(map), noise);
    }

    private static DataResult<SatisfyTerrainFunction> create(Raw raw) {
        try {
            return DataResult.success(new SatisfyTerrainFunction(raw.channel(), raw.map(), raw.noise()));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "bad satisfy_terrain: " + e.getMessage());
        }
    }

    @Override
    public double compute(FunctionContext context) {
        double value = relief.reliefAt(context.blockX(), context.blockZ());
        return noise.isPresent() ? channel.of(value, noise.get().compute(context)) : channel.of(value);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new SatisfyTerrainFunction(channel, map, relief, noise.map(function -> function.mapAll(visitor))));
    }

    @Override
    public double minValue() {
        return noise.isPresent() ? channel.noisyMin() : channel.min();
    }

    @Override
    public double maxValue() {
        return noise.isPresent() ? channel.noisyMax() : channel.max();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DATA_CODEC;
    }
}
