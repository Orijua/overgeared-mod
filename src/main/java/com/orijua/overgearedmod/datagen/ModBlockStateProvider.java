package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.smelting.SmelterBlock;
import net.minecraft.core.registries.BuiltInRegistries;
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
        // Ores: a full cube with textures/block/<id>.png on all sides, and the same model for the item.
        cubeAllWithItem(ModBlocks.MITHRIL_ORE.get());
        cubeAllWithItem(ModBlocks.DEEPSLATE_MITHRIL_ORE.get());
        // Jaffa borrows vanilla copper's textures until it has its own.
        vanillaCubeWithItem(ModBlocks.JAFFA_ORE.get(), "copper_ore");
        vanillaCubeWithItem(ModBlocks.DEEPSLATE_JAFFA_ORE.get(), "deepslate_copper_ore");
        // Blue orichalcum borrows vanilla lapis' ores until it has its own.
        vanillaCubeWithItem(ModBlocks.BLUE_ORICHALCUM_ORE.get(), "lapis_ore");
        vanillaCubeWithItem(ModBlocks.DEEPSLATE_BLUE_ORICHALCUM_ORE.get(), "deepslate_lapis_ore");

        // The smithing anvil reuses vanilla's anvil model.
        ModelFile anvil = models().getExistingFile(mcLoc("block/anvil"));
        simpleBlockWithItem(ModBlocks.SMITHING_ANVIL.get(), anvil);

        // The smelter looks like vanilla's blast furnace, its front lit while the fuel burns, until it has its own art.
        ModelFile smelter = models().orientable("smelter", mcLoc("block/blast_furnace_side"), mcLoc("block/blast_furnace_front"),
                mcLoc("block/blast_furnace_top"));
        ModelFile smelterLit = models().orientable("smelter_on", mcLoc("block/blast_furnace_side"), mcLoc("block/blast_furnace_front_on"),
                mcLoc("block/blast_furnace_top"));
        horizontalBlock(ModBlocks.SMELTER.get(), state -> state.getValue(SmelterBlock.LIT) ? smelterLit : smelter);
        simpleBlockItem(ModBlocks.SMELTER.get(), smelter);
    }

    private void vanillaCubeWithItem(Block block, String vanillaTexture) {
        simpleBlockWithItem(block, models().cubeAll(BuiltInRegistries.BLOCK.getKey(block).getPath(), mcLoc("block/" + vanillaTexture)));
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
