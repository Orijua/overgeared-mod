package com.orijua.overgearedmod.feature.quest.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/** Kill {@code count} mobs, optionally of one {@code entity} type, and optionally only kills made at night. */
public record KillGoal(Optional<EntityType<?>> entity, int count, boolean atNight) implements Goal {
    public static final MapCodec<KillGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(KillGoal::entity),
            Codec.intRange(1, 100_000).fieldOf("count").forGetter(KillGoal::count),
            Codec.BOOL.optionalFieldOf("at_night", false).forGetter(KillGoal::atNight)
    ).apply(instance, KillGoal::new));

    public KillGoal(Optional<EntityType<?>> entity, int count) {
        this(entity, count, false);
    }

    public static String counterKey(Optional<EntityType<?>> entity) {
        return counterKey(entity, false);
    }

    /** Night kills count twice: once in the normal counters and once in these {@code kill_night} ones. */
    public static String counterKey(Optional<EntityType<?>> entity, boolean atNight) {
        String base = atNight ? "kill_night" : "kill";
        return entity.map(type -> base + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(type)).orElse(base);
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
        return counters.get(counterKey(entity, atNight));
    }

    @Override
    public Component describe() {
        String suffix = atNight ? "_night" : "";
        return entity.map(type -> Component.translatable("goal.overgearedmod.kill_entity" + suffix, count, type.getDescription()))
                .orElse(Component.translatable("goal.overgearedmod.kill" + suffix, count));
    }
}
