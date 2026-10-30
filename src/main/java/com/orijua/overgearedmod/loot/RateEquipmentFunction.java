package com.orijua.overgearedmod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.registry.ModLoot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Loot function {@code overgearedmod:rate_equipment}: gives a dropped weapon or armor piece a rating and
 * options, as if it had been forged. For dungeon chests and bosses, in a loot table entry:
 * <pre>{ "function": "overgearedmod:rate_equipment", "luck": 40, "min_rating": "rare" }</pre>
 * {@code luck} works like a smith's skill (0 = base odds); {@code min_rating} is the lowest tier it can give.
 */
public final class RateEquipmentFunction extends LootItemConditionalFunction {
    public static final MapCodec<RateEquipmentFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            Codec.intRange(0, 1000).optionalFieldOf("luck", 0).forGetter(f -> f.luck),
            ItemRating.CODEC.optionalFieldOf("min_rating", ItemRating.NORMAL).forGetter(f -> f.minimum)
    )).apply(instance, RateEquipmentFunction::new));

    private final int luck;
    private final ItemRating minimum;

    private RateEquipmentFunction(List<LootItemCondition> conditions, int luck, ItemRating minimum) {
        super(conditions);
        this.luck = luck;
        this.minimum = minimum;
    }

    @Override
    public LootItemFunctionType<RateEquipmentFunction> getType() {
        return ModLoot.RATE_EQUIPMENT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        EquipmentService.rateLoot(context.getLevel(), stack, luck, minimum, context.getRandom());
        return stack;
    }
}
