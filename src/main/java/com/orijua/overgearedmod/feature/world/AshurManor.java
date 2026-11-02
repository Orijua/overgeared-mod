package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Earl Ashur's manor near Bairan, where he gives his secret request (own design: in the canon Ashur is a noble of
 * Patrian, which doesn't exist yet). One per world, 130 to 260 blocks from Bairan, chosen with it. A stone hall with
 * the earl by his seat at the back and two knights at the door.
 */
public final class AshurManor {
    public static final ResourceLocation STRUCTURE = Overgearedmod.id("ashur_manor");
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, STRUCTURE);
    public static final String MANOR = "ashur_manor/manor";

    /** How far from Bairan, in chunks (the nearest that fits wins); farther in worlds that existed before the manor. */
    public static final int MIN_DISTANCE = 8;
    public static final int MAX_DISTANCE = 16;
    private static final int MAX_SLOPE = 3;
    private static final String AIR = "minecraft:air";
    private static final String WALL = "minecraft:stone_bricks";

    private AshurManor() {
    }

    public static Map<String, TemplateBuilder> pieces() {
        Map<String, TemplateBuilder> pieces = new LinkedHashMap<>();
        pieces.put(MANOR, manor());
        return pieces;
    }

    /** 15x9x13: the hall (z 0-10) with its door to the south, and the path in front where the knights stand. */
    static TemplateBuilder manor() {
        TemplateBuilder piece = new TemplateBuilder(15, 9, 13)
                .fill(0, 0, 0, 14, 0, 10, "minecraft:polished_andesite")
                .fill(0, 0, 11, 14, 0, 12, "minecraft:gravel")
                .fill(6, 0, 11, 8, 0, 12, "minecraft:stone_bricks")
                .fill(0, 1, 0, 14, 8, 12, AIR)
                .walls(0, 1, 0, 14, 4, 10, WALL)
                .fill(0, 5, 0, 14, 5, 10, "minecraft:dark_oak_planks")
                .fill(1, 6, 1, 13, 6, 9, "minecraft:dark_oak_slab[type=bottom]")
                .walls(0, 6, 0, 14, 6, 10, "minecraft:stone_brick_wall")
                // Purple banners of the earl's house, as wool strips above the windows.
                .fill(1, 4, 10, 13, 4, 10, "minecraft:purple_wool")
                .fill(1, 4, 0, 13, 4, 0, "minecraft:purple_wool")
                // The hall: a carpet to the seat, bookshelves, a table and his lectern.
                .fill(7, 1, 3, 7, 1, 9, "minecraft:red_carpet")
                .set(7, 1, 2, "minecraft:dark_oak_stairs[facing=north,half=bottom,shape=straight]")
                .fill(1, 1, 1, 13, 2, 1, "minecraft:bookshelf")
                .set(3, 1, 4, "minecraft:dark_oak_fence")
                .set(3, 2, 4, "minecraft:dark_oak_pressure_plate")
                .set(11, 1, 4, "minecraft:lectern[facing=west]")
                .set(4, 3, 5, "minecraft:lantern[hanging=true]")
                .set(10, 3, 5, "minecraft:lantern[hanging=true]")
                .set(7, 3, 7, "minecraft:lantern[hanging=true]");
        for (int[] corner : new int[][]{{0, 0}, {14, 0}, {0, 10}, {14, 10}}) {
            piece.fill(corner[0], 1, corner[1], corner[0], 5, corner[1], "minecraft:dark_oak_log");
        }
        for (int x : new int[]{3, 11}) {
            piece.set(x, 2, 10, "minecraft:glass_pane").set(x, 3, 10, "minecraft:glass_pane");
        }
        for (int z : new int[]{4, 7}) {
            piece.set(0, 2, z, "minecraft:glass_pane").set(14, 2, z, "minecraft:glass_pane");
        }
        piece.set(7, 1, 10, "minecraft:dark_oak_door[facing=south,half=lower,hinge=left,open=false]")
                .set(7, 2, 10, "minecraft:dark_oak_door[facing=south,half=upper,hinge=left,open=false]")
                .set(5, 1, 11, "minecraft:lantern")
                .set(9, 1, 11, "minecraft:lantern");
        piece.entity(7, 1, 4, Overgearedmod.MODID + ":earl_ashur", new CompoundTag());
        piece.entity(6, 1, 12, Overgearedmod.MODID + ":ashur_knight", new CompoundTag());
        piece.entity(8, 1, 12, Overgearedmod.MODID + ":ashur_knight", new CompoundTag());
        return piece;
    }

    /**
     * The site for this world's manor: the nearest chunk {@code minDistance}-{@code maxDistance} chunks from Bairan
     * where it stands on dry, fairly flat land. Empty if the generator doesn't have it or nothing fits.
     */
    public static Optional<ChunkPos> findSite(RegistryAccess registries, ChunkGenerator generator, RandomState random,
                                              StructureTemplateManager templates, long seed, LevelHeightAccessor height, ChunkPos bairan,
                                              int minDistance, int maxDistance) {
        Structure manor = registries.registryOrThrow(Registries.STRUCTURE).get(KEY);
        if (manor == null) {
            return Optional.empty();
        }
        for (ChunkPos chunk : ChunkSpiral.around(bairan, maxDistance)) {
            if (bairan.getChessboardDistance(chunk) < minDistance) {
                continue;
            }
            StructureStart start = manor.generate(registries, generator, generator.getBiomeSource(), random, templates, seed, chunk, 0,
                    height, biome -> true);
            if (start.isValid() && flatAndDry(start.getPieces().getFirst().getBoundingBox(), generator, random, height)) {
                return Optional.of(chunk);
            }
        }
        return Optional.empty();
    }

    private static boolean flatAndDry(BoundingBox box, ChunkGenerator generator, RandomState random, LevelHeightAccessor height) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (BlockPos corner : List.of(new BlockPos(box.minX(), 0, box.minZ()), new BlockPos(box.maxX(), 0, box.minZ()),
                new BlockPos(box.minX(), 0, box.maxZ()), new BlockPos(box.maxX(), 0, box.maxZ()), box.getCenter())) {
            int surface = generator.getBaseHeight(corner.getX(), corner.getZ(), Heightmap.Types.WORLD_SURFACE_WG, height, random);
            if (surface != generator.getBaseHeight(corner.getX(), corner.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, height, random)) {
                return false;
            }
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
        }
        return highest - lowest <= MAX_SLOPE;
    }
}
