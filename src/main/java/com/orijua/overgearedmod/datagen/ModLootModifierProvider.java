package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.loot.RateEquipmentModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.concurrent.CompletableFuture;

/** Global loot modifiers: changes to the loot of tables that already exist (vanilla's and other mods'). */
final class ModLootModifierProvider extends GlobalLootModifierProvider {
    ModLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, Overgearedmod.MODID);
    }

    @Override
    protected void start() {
        // Weapons and armor in any chest come rated, at base odds (the same as a novice smith).
        add("rate_chest_equipment", new RateEquipmentModifier(new LootItemCondition[0], "chests/", 0, ItemRating.NORMAL));
    }
}
