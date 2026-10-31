package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.npc.BlacksmithNpc;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.structure.AdventurerCamp;
import com.orijua.overgearedmod.structure.Bairan;
import com.orijua.overgearedmod.structure.StartingVillage;
import com.orijua.overgearedmod.structure.StartingVillagePlacement;
import com.orijua.overgearedmod.structure.StructurePieces;
import com.orijua.overgearedmod.structure.TemplateBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The structure pipeline: generated pieces, the hand-written worldgen JSON around them, and the camp and Bairan they make. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class StructureTests {
    private StructureTests() {
    }

    /** A test's template is loaded one block above its structure block, which is what the helper's positions count from. */
    private static BlockPos inPiece(int x, int y, int z) {
        return new BlockPos(x, y + 1, z);
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void campIsRegisteredWithItsPoolsSetAndBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE).containsKey(AdventurerCamp.STRUCTURE), "structure");
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE_SET).containsKey(AdventurerCamp.STRUCTURE), "structure set");
        for (String pool : List.of("adventurer_camp/center", "adventurer_camp/sides")) {
            helper.assertTrue(registries.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(Overgearedmod.id(pool)), "pool " + pool);
        }
        TagKey<Biome> biomes = TagKey.create(Registries.BIOME, Overgearedmod.id("has_structure/adventurer_camp"));
        helper.assertTrue(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).is(biomes), "spawns in plains");
        helper.assertFalse(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT).is(biomes), "not in deserts");
        helper.succeed();
    }

    /** Every piece in code has its .nbt (written by gradlew runData), with the same size: rerun datagen if this fails. */
    @GameTest(template = TestSupport.EMPTY)
    public static void everyPieceHasItsNbtFile(GameTestHelper helper) {
        for (Map.Entry<String, TemplateBuilder> piece : StructurePieces.all().entrySet()) {
            StructureTemplate loaded = helper.getLevel().getStructureManager().get(Overgearedmod.id(piece.getKey())).orElse(null);
            helper.assertTrue(loaded != null, "missing " + piece.getKey() + ".nbt");
            var size = piece.getValue().build(0).getList("size", Tag.TAG_INT);
            helper.assertTrue(loaded.getSize().equals(new Vec3i(size.getInt(0), size.getInt(1), size.getInt(2))),
                    piece.getKey() + " size " + loaded.getSize());
        }
        helper.succeed();
    }

    /** Runs the real jigsaw assembly (no blocks placed): the center plus at least one side piece. */
    @GameTest(template = TestSupport.EMPTY)
    public static void campAssemblesFromItsPieces(GameTestHelper helper) {
        StructureStart start = assemble(helper, AdventurerCamp.STRUCTURE);
        helper.assertTrue(start.isValid(), "the camp should assemble");
        List<StructurePiece> pieces = start.getPieces();
        helper.assertTrue(pieces.size() >= 2 && pieces.size() <= 5, "center + 1 to 4 sides, got " + pieces.size());
        helper.assertTrue(pieces.getFirst().toString().contains(AdventurerCamp.CENTER), "starts at the center: " + pieces.getFirst());
        helper.succeed();
    }

    /** The tent piece itself as the test area: its blocks and its loot chest land in the world. */
    @GameTest(template = AdventurerCamp.TENT)
    public static void tentPieceLoadsWithItsChest(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.WHITE_WOOL, inPiece(2, 3, 0));
        helper.assertBlockPresent(Blocks.JIGSAW, inPiece(2, 0, 4));
        ChestBlockEntity chest = helper.getBlockEntity(inPiece(2, 1, 1));
        ResourceKey<?> loot = chest.getLootTable();
        helper.assertTrue(loot != null && loot.location().getPath().equals("chests/village/village_weaponsmith"), "loot " + loot);
        helper.succeed();
    }

    @GameTest(template = AdventurerCamp.FORGE)
    public static void forgePieceHasTheSmithingAnvil(GameTestHelper helper) {
        helper.assertBlockPresent(ModBlocks.SMITHING_ANVIL.get(), inPiece(2, 1, 1));
        helper.assertBlockPresent(Blocks.FURNACE, inPiece(1, 1, 0));
        helper.succeed();
    }

    /** Bairan generates only at the site its world saved, and /locate can follow it there. */
    @GameTest(template = TestSupport.EMPTY)
    public static void bairanGeneratesOnlyAtItsWorldsSite(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.STRUCTURE).containsKey(Bairan.STRUCTURE), "structure");
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(Bairan.STRUCTURE);
        helper.assertTrue(set != null && set.placement() instanceof StartingVillagePlacement, "placed as the starting village");
        StartingVillagePlacement placement = (StartingVillagePlacement) set.placement();

        long seed = -7_123_456_789L; // no real world in the test server has it
        helper.assertFalse(placement.applyAdditionalChunkRestrictions(0, 0, seed), "a world without a site has no Bairan");
        ChunkPos site = new ChunkPos(12, -40);
        StartingVillage.remember(seed, site);
        try {
            helper.assertTrue(placement.applyAdditionalChunkRestrictions(0, 0, seed), "a world with a site has it");
            for (ChunkPos region : List.of(new ChunkPos(0, 0), new ChunkPos(500, -300), new ChunkPos(-64, 64))) {
                helper.assertTrue(site.equals(placement.getPotentialStructureChunk(seed, region.x, region.z)),
                        "every region points /locate at the site, from " + region);
            }
        } finally {
            StartingVillage.forget(seed);
        }
        TagKey<Biome> biomes = TagKey.create(Registries.BIOME, Overgearedmod.id("has_structure/bairan"));
        helper.assertTrue(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).is(biomes), "plains");
        helper.assertFalse(registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST).is(biomes), "not among trees");
        helper.succeed();
    }

    /**
     * On real overworld terrain (not the flat test world), every one of 40 seeds gets a Bairan site near its usual spawn:
     * the whole village on dry land, in its biomes. With the old ring placement only 26 of these 40 seeds had one.
     */
    @GameTest(template = TestSupport.EMPTY, timeoutTicks = 4000)
    public static void bairanFindsASiteInEveryRealWorld(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Holder<NoiseGeneratorSettings> settings = registries.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        BiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        int farthest = 0;
        for (long seed = 1; seed <= 40; seed++) {
            RandomState random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            ChunkPos usualSpawn = new ChunkPos(random.sampler().findSpawnPosition());
            Optional<StartingVillage.Site> site = StartingVillage.findSite(registries, generator, random,
                    helper.getLevel().getStructureManager(), seed, height, usualSpawn);
            helper.assertTrue(site.isPresent(), "no Bairan site for seed " + seed);
            farthest = Math.max(farthest, usualSpawn.getChessboardDistance(site.get().chunk()));
        }
        Overgearedmod.LOGGER.info("Bairan: a site in all 40 seeds, at most {} chunks from the usual spawn", farthest);
        helper.succeed();
    }

    /** The square, and on its sides Smith's smithy, the chief's house, the mine and at least one house. */
    @GameTest(template = TestSupport.EMPTY)
    public static void bairanAssemblesAroundItsSquare(GameTestHelper helper) {
        StructureStart start = assemble(helper, Bairan.STRUCTURE);
        helper.assertTrue(start.isValid(), "Bairan should assemble");
        List<String> pieces = start.getPieces().stream().map(Object::toString).toList();
        helper.assertTrue(pieces.getFirst().contains(Bairan.PLAZA), "starts at the square: " + pieces.getFirst());
        for (String building : List.of(Bairan.SMITHY, Bairan.CHIEF_HOUSE, Bairan.MINE)) {
            helper.assertTrue(pieces.stream().filter(piece -> piece.contains(building + "]")).count() == 1, "one " + building + " in " + pieces);
        }
        helper.assertTrue(pieces.stream().anyMatch(piece -> piece.contains(Bairan.HOUSE + "]") || piece.contains(Bairan.GARDEN_HOUSE + "]")),
                "houses in " + pieces);
        helper.succeed();
    }

    @GameTest(template = Bairan.SMITHY)
    public static void smithyPieceHasSmithAtTheAnvil(GameTestHelper helper) {
        helper.assertBlockPresent(ModBlocks.SMITHING_ANVIL.get(), inPiece(3, 1, 3));
        List<BlacksmithNpc> smiths = helper.getEntities(ModEntities.BLACKSMITH.get());
        helper.assertTrue(smiths.size() == 1, "one blacksmith, got " + smiths.size());
        helper.assertTrue(smiths.getFirst().getCustomName() != null
                && Bairan.SMITH_NAME.equals(smiths.getFirst().getCustomName().getString()), "named Smith");
        helper.succeed();
    }

    @GameTest(template = Bairan.CHIEF_HOUSE)
    public static void chiefHousePieceHasTheChief(GameTestHelper helper) {
        helper.assertTrue(helper.getEntities(ModEntities.VILLAGE_CHIEF.get()).size() == 1, "the village chief is home");
        helper.assertBlockPresent(Blocks.LECTERN, inPiece(7, 1, 1));
        helper.succeed();
    }

    /** Runs a structure's real jigsaw assembly at a fixed seed and spot, without placing any block. */
    private static StructureStart assemble(GameTestHelper helper, ResourceLocation id) {
        ServerLevel level = helper.getLevel();
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), 12345L, new ChunkPos(400, 400), 0, level,
                biome -> true);
    }
}
