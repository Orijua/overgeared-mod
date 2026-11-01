package com.orijua.overgearedmod.feature.quest.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.Optional;

/** Forge {@code count} items at the smithing anvil, optionally one {@code item} and of at least {@code min_rating}. */
public record ForgeGoal(Optional<Item> item, Optional<ItemRating> minRating, int count) implements Goal {
    public static final MapCodec<ForgeGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(ForgeGoal::item),
            ItemRating.CODEC.optionalFieldOf("min_rating").forGetter(ForgeGoal::minRating),
            Codec.intRange(1, 100_000).fieldOf("count").forGetter(ForgeGoal::count)
    ).apply(instance, ForgeGoal::new));

    /** "forge", "forge/epic", "forge/overgearedmod:jaffa_sword" or "forge/overgearedmod:jaffa_sword/epic". */
    static String counterKey(Optional<Item> item, Optional<ItemRating> minRating) {
        String key = item.map(it -> "forge/" + BuiltInRegistries.ITEM.getKey(it)).orElse("forge");
        return minRating.map(rating -> key + "/" + rating.getSerializedName()).orElse(key);
    }

    @Override
    public GoalType type() {
        return GoalType.FORGE;
    }

    @Override
    public int target() {
        return count;
    }

    @Override
    public int progress(PlayerCounters counters, PlayerStats stats) {
        return counters.get(counterKey(item, minRating));
    }

    @Override
    public Component describe() {
        if (item.isPresent()) {
            Component name = item.get().getDescription();
            return minRating.map(rating -> Component.translatable("goal.overgearedmod.forge_item_rating", count, name, rating.displayName()))
                    .orElse(Component.translatable("goal.overgearedmod.forge_item", count, name));
        }
        return minRating.map(rating -> Component.translatable("goal.overgearedmod.forge_rating", count, rating.displayName()))
                .orElse(Component.translatable("goal.overgearedmod.forge", count));
    }
}
