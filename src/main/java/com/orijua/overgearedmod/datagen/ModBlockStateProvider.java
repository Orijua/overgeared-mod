package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Blockstates, block models and their item models. */
final class ModBlockStateProvider extends BlockStateProvider {
    ModBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, Overgearedmod.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        // Same model for every block and item: a full cube with textures/block/<id>.png on all sides.
        cubeAllWithItem(ModBlocks.MITHRIL_ORE.get());
        cubeAllWithItem(ModBlocks.DEEPSLATE_MITHRIL_ORE.get());

        // The smithing anvil reuses vanilla's anvil model.
        ModelFile anvil = models().getExistingFile(mcLoc("block/anvil"));
        simpleBlockWithItem(ModBlocks.SMITHING_ANVIL.get(), anvil);
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
