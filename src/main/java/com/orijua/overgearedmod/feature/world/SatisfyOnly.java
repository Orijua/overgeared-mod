package com.orijua.overgearedmod.feature.world;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * What only works in Satisfy. The real world is any dimension that is not Satisfy ({@link Satisfy#is}); there the mod's
 * objects are inert (they can't be placed or used). The exceptions are the capsules (how you get to Satisfy) and the
 * account terminal and the phone (how you reach the account and the work from the real world).
 * The ores (jaffa, mithril, blue orichalcum) don't generate in the real world either ({@link SatisfyOnlyPlacement}).
 */
public final class SatisfyOnly {
    /** The action-bar message shown when something that only works in Satisfy is tried in the real world. */
    public static final String WARNING_KEY = "message.overgearedmod.satisfy_only";

    /** The items that work in the real world: the capsules (all tiers) and their licenses, the account terminal and the phone. */
    private static final Set<String> REAL_WORLD_ITEMS = Set.of("capsule", "advanced_capsule", "diamond_capsule",
            "advanced_capsule_license", "diamond_capsule_license", "account_terminal", "phone");
    /** The blocks that can be placed in the real world: the capsules (all tiers) and the account terminal. */
    private static final Set<String> REAL_WORLD_BLOCKS = Set.of("capsule", "advanced_capsule", "diamond_capsule", "account_terminal");

    private SatisfyOnly() {
    }

    /** Whether the entity is where the restriction applies: anywhere that is not Satisfy. */
    public static boolean active(Entity entity) {
        return !Satisfy.is(entity.level());
    }

    /** An item of the mod that is inert in the real world: everything but the capsules and their licenses, the terminal and the phone. */
    public static boolean isSatisfyItem(ResourceLocation item) {
        return Overgearedmod.MODID.equals(item.getNamespace()) && !REAL_WORLD_ITEMS.contains(item.getPath());
    }

    /** A block of the mod that can't be placed in the real world: all of them but the capsules and the terminal. */
    public static boolean blocksPlacement(ResourceLocation block) {
        return Overgearedmod.MODID.equals(block.getNamespace()) && !REAL_WORLD_BLOCKS.contains(block.getPath());
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
