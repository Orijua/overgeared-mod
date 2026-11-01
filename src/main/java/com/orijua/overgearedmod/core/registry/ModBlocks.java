package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.pagma.GoldenTableBlock;
import com.orijua.overgearedmod.feature.smelting.SmelterBlock;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Overgearedmod.MODID);

    public static final DeferredBlock<SmithingAnvilBlock> SMITHING_ANVIL = BLOCKS.registerBlock("smithing_anvil",
            SmithingAnvilBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL)
                    .noOcclusion());

    // The smelter: a furnace with a temperature for advanced metals (block entity: SmelterBlockEntity).
    public static final DeferredBlock<SmelterBlock> SMELTER = BLOCKS.registerBlock("smelter",
            SmelterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(SmelterBlock.LIT) ? 13 : 0));

    // Jaffa ore (canon: the ore Smith teaches Grid to smelt in Bairan). Iron tier: around iron height, a bit rarer, plus
    // guaranteed veins in Bairan's mine (worldgen/placed_feature/ore_jaffa.json + neoforge/biome_modifier).
    public static final DeferredBlock<Block> JAFFA_ORE = BLOCKS.registerSimpleBlock("jaffa_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_JAFFA_ORE = BLOCKS.registerSimpleBlock("deepslate_jaffa_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // Blue orichalcum ore (canon: born of moonlight and the Guardian of the Forest, the hardest mineral): rare and deep
    // under forests (worldgen/placed_feature/ore_blue_orichalcum.json + neoforge/biome_modifier). Needs a diamond pickaxe.
    public static final DeferredBlock<Block> BLUE_ORICHALCUM_ORE = BLOCKS.registerSimpleBlock("blue_orichalcum_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5.0f, 6.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_BLUE_ORICHALCUM_ORE = BLOCKS.registerSimpleBlock("deepslate_blue_orichalcum_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(6.5f, 6.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // Mithril ore: generated in the Overworld (worldgen/placed_feature/ore_mithril.json + neoforge/biome_modifier).
    public static final DeferredBlock<Block> MITHRIL_ORE = BLOCKS.registerSimpleBlock("mithril_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_MITHRIL_ORE = BLOCKS.registerSimpleBlock("deepslate_mithril_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // The golden table at the back of the Northern End Cave, with Pagma's book on it (canon, cap. 1). Unbreakable and
    // drops nothing; it glows while the book is still there.
    public static final DeferredBlock<GoldenTableBlock> GOLDEN_TABLE = BLOCKS.registerBlock("golden_table",
            GoldenTableBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0f, 3_600_000.0f)
                    .noLootTable()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(GoldenTableBlock.BOOK) ? 10 : 4));

    private ModBlocks() {
    }
}
