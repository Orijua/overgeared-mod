package com.orijua.overgearedmod.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.progression.PlayerStats;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** Forge {@code count} items at the smithing anvil, optionally of at least {@code min_rating}. */
public record ForgeGoal(Optional<ItemRating> minRating, int count) implements Goal {
    public static final MapCodec<ForgeGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemRating.CODEC.optionalFieldOf("min_rating").forGetter(ForgeGoal::minRating),
            Codec.intRange(1, 100_000).fieldOf("count").forGetter(ForgeGoal::count)
    ).apply(instance, ForgeGoal::new));

    static String counterKey(Optional<ItemRating> minRating) {
        return minRating.map(rating -> "forge/" + rating.getSerializedName()).orElse("forge");
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
        return counters.get(counterKey(minRating));
    }

    @Override
    public Component describe() {
        return minRating.map(rating -> Component.translatable("goal.overgearedmod.forge_rating", count, rating.displayName()))
                .orElse(Component.translatable("goal.overgearedmod.forge", count));
    }
}
