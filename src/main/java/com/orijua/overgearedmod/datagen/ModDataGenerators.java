package com.orijua.overgearedmod.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Data generation (`gradlew runData`): writes models, blockstates, loot tables, tags, recipes and global loot modifiers into
 * src/generated/resources. Edit the providers here instead of those JSON files; they are overwritten.
 * Translations, the Satisfy JSON (skills, quests...), worldgen and the damage type stay hand-written in src/main/resources.
 */
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, files));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, files));

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(ModEntityLoot::new, LootContextParamSets.ENTITY)), lookup));
        ModBlockTagsProvider blockTags = generator.addProvider(event.includeServer(), new ModBlockTagsProvider(output, lookup, files));
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new ModLootModifierProvider(output, lookup));
    }
}
