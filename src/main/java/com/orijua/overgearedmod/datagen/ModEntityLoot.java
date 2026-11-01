package com.orijua.overgearedmod.datagen;

import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.equipment.RateEquipmentFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.stream.Stream;

/** Loot tables of the mod's entities. */
final class ModEntityLoot extends EntityLootSubProvider {
    /** What the Bone Lord can drop: one piece, always Epic or better. */
    private static final List<Item> BONE_LORD_REWARDS = List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);

    ModEntityLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        LootPool.Builder reward = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        BONE_LORD_REWARDS.forEach(item -> reward.add(LootItem.lootTableItem(item).apply(RateEquipmentFunction.rate(40, ItemRating.EPIC))));
        add(ModEntities.BONE_LORD.get(), LootTable.lootTable()
                .withPool(reward)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.BONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 10))))));
    }

    /** Only our entities: the blacksmith has no loot table on purpose. */
    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(ModEntities.BONE_LORD.get());
    }
}
