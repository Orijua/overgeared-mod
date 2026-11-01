package com.orijua.overgearedmod.feature.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Who the player is in Satisfy: chosen class (once) and earned titles. */
public record PlayerIdentity(Optional<ResourceLocation> playerClass, List<ResourceLocation> titles) {
    public static final PlayerIdentity DEFAULT = new PlayerIdentity(Optional.empty(), List.of());

    public static final Codec<PlayerIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("class").forGetter(PlayerIdentity::playerClass),
            ResourceLocation.CODEC.listOf().optionalFieldOf("titles", List.of()).forGetter(PlayerIdentity::titles)
    ).apply(instance, PlayerIdentity::new));

    public static final StreamCodec<ByteBuf, PlayerIdentity> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public PlayerIdentity withClass(ResourceLocation id) {
        return new PlayerIdentity(Optional.of(id), titles);
    }

    public PlayerIdentity withTitle(ResourceLocation id) {
        List<ResourceLocation> copy = new ArrayList<>(titles);
        copy.add(id);
        return new PlayerIdentity(playerClass, List.copyOf(copy));
    }

    public boolean hasTitle(ResourceLocation id) {
        return titles.contains(id);
    }
}
