package com.orijua.overgearedmod.feature.character;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Everything of a character that is swapped between the two worlds, as a tag. It uses the vanilla serialization
 * (the same calls as {@code player.dat}), never a codec of our own: enchantments are registry holders in 1.21.1 and a
 * plain codec fails on them. The provider is always the player's registry access.
 *
 * <p>Not covered: slots that other mods add (e.g. Curios). They stay with the player in both worlds.
 */
public final class CharacterSnapshot {
    private static final String VERSION = "data_version";
    private static final String INVENTORY = "inventory";
    private static final String ENDER_CHEST = "ender_chest";

    private CharacterSnapshot() {
    }

    /** Reads the player; changes nothing. */
    public static CompoundTag capture(ServerPlayer player) {
        HolderLookup.Provider provider = player.registryAccess();
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.put(INVENTORY, player.getInventory().save(new ListTag()));
        tag.put(ENDER_CHEST, player.getEnderChestInventory().createTag(provider));
        return tag;
    }

    /**
     * Whether {@link #apply} can read this tag. It is checked BEFORE anything is touched, so a damaged tag means no
     * swap at all and nothing is lost.
     */
    public static boolean isReadable(CompoundTag tag) {
        return tag.contains(INVENTORY, Tag.TAG_LIST) && tag.contains(ENDER_CHEST, Tag.TAG_LIST);
    }

    /** Replaces what the player carries (inventory, armor, offhand, Ender chest) by the snapshot's. */
    public static void apply(ServerPlayer player, CompoundTag tag) {
        HolderLookup.Provider provider = player.registryAccess();
        player.getInventory().load(tag.getList(INVENTORY, Tag.TAG_COMPOUND));
        player.getEnderChestInventory().fromTag(tag.getList(ENDER_CHEST, Tag.TAG_COMPOUND), provider);
    }

    /** A snapshot of a character with nothing: what the other world is before the player has ever been there. */
    public static CompoundTag empty() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.put(INVENTORY, new ListTag());
        tag.put(ENDER_CHEST, new ListTag());
        return tag;
    }
}
