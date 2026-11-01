package com.orijua.overgearedmod.feature.quest.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/** Kill {@code count} mobs, optionally of one {@code entity} type. */
public record KillGoal(Optional<EntityType<?>> entity, int count) implements Goal {
    public static final MapCodec<KillGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(KillGoal::entity),
            Codec.intRange(1, 100_000).fieldOf("count").forGetter(KillGoal::count)
    ).apply(instance, KillGoal::new));

    public static String counterKey(Optional<EntityType<?>> entity) {
        return entity.map(type -> "kill/" + BuiltInRegistries.ENTITY_TYPE.getKey(type)).orElse("kill");
    }

    @Override
    public GoalType type() {
        return GoalType.KILL;
    }

    @Override
    public int target() {
        return count;
    }

    @Override
    public int progress(PlayerCounters counters, PlayerStats stats) {
        return counters.get(counterKey(entity));
    }

    @Override
    public Component describe() {
        return entity.map(type -> Component.translatable("goal.overgearedmod.kill_entity", count, type.getDescription()))
                .orElse(Component.translatable("goal.overgearedmod.kill", count));
    }
}
