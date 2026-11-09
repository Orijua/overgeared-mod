package com.orijua.overgearedmod.feature.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * The two characters of a player. {@code live} says which world's things the player is carrying right now (empty
 * until the first settle: the player is "adopted" where they stand); {@code stored} is the snapshot of the OTHER
 * world's character. Saved with the player and kept on death, never synced.
 *
 * <p>The tag is mutable, so every way in and out copies it: nobody shares the vault's tag.
 *
 * <p>Nothing but {@link Characters#settle} moves items between the characters. The only documented exception is the
 * purchases of {@code TerminalShop}, which only add entries (emeralds and pieces of the offer) to the stored inventory.
 */
public record CharacterVault(Optional<WorldSide> live, Optional<CompoundTag> stored) {
    public static final CharacterVault EMPTY = new CharacterVault(Optional.empty(), Optional.empty());

    public static final Codec<CharacterVault> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WorldSide.CODEC.optionalFieldOf("live").forGetter(CharacterVault::live),
            CompoundTag.CODEC.optionalFieldOf("stored").forGetter(CharacterVault::stored)
    ).apply(instance, CharacterVault::new));

    public CharacterVault {
        stored = stored.map(CompoundTag::copy);
    }

    @Override
    public Optional<CompoundTag> stored() {
        return stored.map(CompoundTag::copy);
    }

    /** The player is carrying {@code side}'s things and {@code snapshot} holds the other world's. */
    public static CharacterVault of(WorldSide side, CompoundTag snapshot) {
        return new CharacterVault(Optional.of(side), Optional.of(snapshot));
    }

    /** First time: no swap, the things the player carries are taken as {@code side}'s. */
    public CharacterVault adopted(WorldSide side) {
        return new CharacterVault(Optional.of(side), stored);
    }
}
