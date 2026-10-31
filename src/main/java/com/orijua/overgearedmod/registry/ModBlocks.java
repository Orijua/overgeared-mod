package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.smithing.SmithingAnvilBlock;
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

    // Jaffa ore (canon: the ore Smith teaches Grid to smelt in Bairan). Iron tier: around iron height, a bit rarer, plus
    // guaranteed veins in Bairan's mine (worldgen/placed_feature/ore_jaffa.json + neoforge/biome_modifier).
    public static final DeferredBlock<Block> JAFFA_ORE = BLOCKS.registerSimpleBlock("jaffa_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_JAFFA_ORE = BLOCKS.registerSimpleBlock("deepslate_jaffa_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // Mithril ore: generated in the Overworld (worldgen/placed_feature/ore_mithril.json + neoforge/biome_modifier).
    public static final DeferredBlock<Block> MITHRIL_ORE = BLOCKS.registerSimpleBlock("mithril_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DEEPSLATE_MITHRIL_ORE = BLOCKS.registerSimpleBlock("deepslate_mithril_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    private ModBlocks() {
    }
}
