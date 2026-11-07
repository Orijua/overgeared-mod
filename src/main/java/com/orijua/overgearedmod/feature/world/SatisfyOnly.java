package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * What only works in Satisfy. The real world is any dimension that is not Satisfy ({@link Satisfy#is}); there the mod's
 * objects are inert (they can't be placed or used). The only exception is the capsule, which is how you get to Satisfy.
 * The ores (jaffa, mithril, blue orichalcum) don't generate in the real world either ({@link SatisfyOnlyPlacement}).
 */
public final class SatisfyOnly {
    /** The action-bar message shown when something that only works in Satisfy is tried in the real world. */
    public static final String WARNING_KEY = "message.overgearedmod.satisfy_only";

    private static final String CAPSULE = "capsule";

    private SatisfyOnly() {
    }

    /** Whether the entity is where the restriction applies: anywhere that is not Satisfy. */
    public static boolean active(Entity entity) {
        return !Satisfy.is(entity.level());
    }

    /** An item of the mod that is inert in the real world: everything but the capsule. */
    public static boolean isSatisfyItem(ResourceLocation item) {
        return Overgearedmod.MODID.equals(item.getNamespace()) && !CAPSULE.equals(item.getPath());
    }

    /** A block of the mod that can't be placed in the real world: all of them but the capsule. */
    public static boolean blocksPlacement(ResourceLocation block) {
        return Overgearedmod.MODID.equals(block.getNamespace()) && !CAPSULE.equals(block.getPath());
    }

    /** An entity type of the mod: its right-click interaction is refused in the real world. */
    public static boolean isSatisfyEntity(ResourceLocation type) {
        return Overgearedmod.MODID.equals(type.getNamespace());
    }

    /** Tells the player (action bar) that this only works in Satisfy. */
    public static void warn(Player player) {
        player.displayClientMessage(Component.translatable(WARNING_KEY), true);
    }
}
