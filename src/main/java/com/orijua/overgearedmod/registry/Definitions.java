package com.orijua.overgearedmod.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Read access to the JSON-defined content, the same way on server and client. */
public final class Definitions {
    public record Entry<T>(ResourceLocation id, T value) {
    }

    private Definitions() {
    }

    public static <T> Optional<T> get(Level level, ResourceKey<Registry<T>> registry, ResourceLocation id) {
        return level.registryAccess().registry(registry).flatMap(r -> r.getOptional(id));
    }

    public static <T> Optional<T> get(HolderLookup.Provider registries, ResourceKey<Registry<T>> registry, ResourceLocation id) {
        return registries.lookup(registry)
                .flatMap(lookup -> lookup.get(ResourceKey.create(registry, id)))
                .map(holder -> holder.value());
    }

    /** All entries sorted by id, so lists in the UI have a stable order. */
    public static <T> List<Entry<T>> all(Level level, ResourceKey<Registry<T>> registry) {
        return level.registryAccess().registry(registry)
                .map(r -> r.entrySet().stream()
                        .map(e -> new Entry<>(e.getKey().location(), e.getValue()))
                        .sorted(Comparator.comparing(Entry::id))
                        .toList())
                .orElse(List.of());
    }
}
