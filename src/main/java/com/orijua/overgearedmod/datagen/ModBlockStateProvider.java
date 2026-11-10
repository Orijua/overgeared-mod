package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.feature.pagma.GoldenTableBlock;
import com.orijua.overgearedmod.feature.smelting.SmelterBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
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

        // The capsules: a box with a front of another material per tier, until they have their own art: iron with lapis,
        // gold with lapis, diamond with netherite.
        ModelFile capsule = models().orientable("capsule", mcLoc("block/iron_block"), mcLoc("block/lapis_block"), mcLoc("block/iron_block"));
        horizontalBlock(ModBlocks.CAPSULE.get(), capsule);
        simpleBlockItem(ModBlocks.CAPSULE.get(), capsule);
        ModelFile advancedCapsule = models().orientable("advanced_capsule", mcLoc("block/gold_block"), mcLoc("block/lapis_block"), mcLoc("block/gold_block"));
        horizontalBlock(ModBlocks.ADVANCED_CAPSULE.get(), advancedCapsule);
        simpleBlockItem(ModBlocks.ADVANCED_CAPSULE.get(), advancedCapsule);
        ModelFile diamondCapsule = models().orientable("diamond_capsule", mcLoc("block/diamond_block"), mcLoc("block/netherite_block"), mcLoc("block/diamond_block"));
        horizontalBlock(ModBlocks.DIAMOND_CAPSULE.get(), diamondCapsule);
        simpleBlockItem(ModBlocks.DIAMOND_CAPSULE.get(), diamondCapsule);

        // The account terminal: an iron box with a glass front, until it has its own art.
        ModelFile terminal = models().orientable("account_terminal", mcLoc("block/iron_block"), mcLoc("block/light_blue_stained_glass"), mcLoc("block/iron_block"));
        horizontalBlock(ModBlocks.ACCOUNT_TERMINAL.get(), terminal);
        simpleBlockItem(ModBlocks.ACCOUNT_TERMINAL.get(), terminal);

        // The golden table: a gold-block table, with Pagma's book lying on it until taken.
        ModelFile table = goldenTable("golden_table", false);
        ModelFile tableWithBook = goldenTable("golden_table_book", true);
        getVariantBuilder(ModBlocks.GOLDEN_TABLE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(GoldenTableBlock.BOOK) ? tableWithBook : table).build());
        simpleBlockItem(ModBlocks.GOLDEN_TABLE.get(), tableWithBook);
    }

    private ModelFile goldenTable(String name, boolean book) {
        BlockModelBuilder model = models().getBuilder(name).renderType("cutout")
                .texture("gold", mcLoc("block/gold_block")).texture("particle", mcLoc("block/gold_block"));
        model.element().from(0, 12, 0).to(16, 14, 16).allFaces((side, face) -> face.texture("#gold")).end();
        for (int[] leg : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            model.element().from(leg[0], 0, leg[1]).to(leg[0] + 2, 12, leg[1] + 2).allFaces((side, face) -> face.texture("#gold")).end();
        }
        if (book) {
            model.texture("book", modLoc("item/pagma_rare_book"));
            model.element().from(2, 14, 2).to(14, 14.25f, 14).face(Direction.UP).texture("#book").end().end();
        }
        return model;
    }

    private void vanillaCubeWithItem(Block block, String vanillaTexture) {
        simpleBlockWithItem(block, models().cubeAll(BuiltInRegistries.BLOCK.getKey(block).getPath(), mcLoc("block/" + vanillaTexture)));
    }

    private void cubeAllWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
