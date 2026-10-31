package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.structure.AdventurerCamp;
import com.orijua.overgearedmod.structure.TemplateBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** The structure pipeline: generated pieces, the hand-written worldgen JSON around them, and the camp they make. */
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
        for (Map.Entry<String, TemplateBuilder> piece : AdventurerCamp.pieces().entrySet()) {
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
        ServerLevel level = helper.getLevel();
        Structure camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AdventurerCamp.STRUCTURE);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureStart start = camp.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), 12345L, new ChunkPos(400, 400), 0, level,
                biome -> true);
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
}
