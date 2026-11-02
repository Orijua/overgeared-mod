package com.orijua.overgearedmod.feature.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The biomes of the Satisfy dimension: the lore regions of {@link SatisfyMap} in the middle, and the {@code outside}
 * source (vanilla multi-noise) for everything the map doesn't cover. ({"type": "overgearedmod:satisfy_map", ...} in
 * the dimension's JSON.)
 */
public final class SatisfyBiomeSource extends BiomeSource {
    public static final MapCodec<SatisfyBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("outside").forGetter(source -> source.outside),
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("legend").forGetter(source -> source.legend),
            Codec.STRING.listOf().fieldOf("rows").forGetter(source -> source.map.rows()),
            Codec.intRange(16, 4096).fieldOf("cell").forGetter(source -> source.map.cell()),
            Codec.INT.fieldOf("origin_col").forGetter(source -> source.map.originCol()),
            Codec.INT.fieldOf("origin_row").forGetter(source -> source.map.originRow())
    ).apply(instance, SatisfyBiomeSource::new));

    private final BiomeSource outside;
    private final Map<String, Holder<Biome>> legend;
    private final SatisfyMap map;

    public SatisfyBiomeSource(BiomeSource outside, Map<String, Holder<Biome>> legend, List<String> rows, int cell, int originCol, int originRow) {
        this.outside = outside;
        this.legend = Map.copyOf(legend);
        this.map = new SatisfyMap(rows, cell, originCol, originRow);
        java.util.Set<String> missing = new java.util.TreeSet<>();
        for (String row : this.map.rows()) {
            for (char letter : row.toCharArray()) {
                if (letter != SatisfyMap.NONE && !this.legend.containsKey(String.valueOf(letter))) {
                    missing.add(String.valueOf(letter));
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("the map uses letters missing from the legend: " + missing);
        }
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(legend.values().stream(), outside.possibleBiomes().stream()).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        Holder<Biome> lore = legend.get(String.valueOf(map.at((quartX << 2) + 2, (quartZ << 2) + 2)));
        return lore != null ? lore : outside.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    /** The lore biome at a block, or none outside the map (tests and the site search use it). */
    public SatisfyMap map() {
        return map;
    }
}
