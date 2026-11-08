package com.orijua.overgearedmod.feature.character;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;

/**
 * Everything of a character that is swapped between the two worlds, as a tag. It uses the vanilla serialization
 * (the same calls as {@code player.dat}), never a codec of our own: enchantments are registry holders in 1.21.1 and a
 * plain codec fails on them. The provider is always the player's registry access.
 *
 * <p>Not covered: slots that other mods add (e.g. Curios). They stay with the player in both worlds.
 */
public final class CharacterSnapshot {
    /**
     * The game's data version when the snapshot was taken. Nothing reads it yet, on purpose: it is there so that a
     * future change of format (or a Minecraft update that needs a data fixer) can tell an old snapshot from a new one.
     */
    private static final String VERSION = "data_version";
    private static final String INVENTORY = "inventory";
    private static final String ENDER_CHEST = "ender_chest";
    private static final String XP_LEVEL = "xp_level";
    private static final String XP_PROGRESS = "xp_progress";
    private static final String XP_TOTAL = "xp_total";
    private static final String HEALTH = "health";
    private static final String ABSORPTION = "absorption";
    private static final String EFFECTS = "effects";
    private static final String FOOD = "food";

    private CharacterSnapshot() {
    }

    /** Reads the player; changes nothing. */
    public static CompoundTag capture(ServerPlayer player) {
        HolderLookup.Provider provider = player.registryAccess();
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.put(INVENTORY, player.getInventory().save(new ListTag()));
        tag.put(ENDER_CHEST, player.getEnderChestInventory().createTag(provider));
        tag.putInt(XP_LEVEL, player.experienceLevel);
        tag.putFloat(XP_PROGRESS, player.experienceProgress);
        tag.putInt(XP_TOTAL, player.totalExperience);
        tag.putFloat(HEALTH, player.getHealth());
        tag.putFloat(ABSORPTION, player.getAbsorptionAmount());
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.save());
        }
        tag.put(EFFECTS, effects);
        CompoundTag food = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(food);
        tag.put(FOOD, food);
        return tag;
    }

    /**
     * Whether {@link #apply} can read this tag. It is checked BEFORE anything is touched, so a damaged tag means no
     * swap at all and nothing is lost.
     */
    public static boolean isReadable(CompoundTag tag) {
        return tag.contains(INVENTORY, Tag.TAG_LIST) && tag.contains(ENDER_CHEST, Tag.TAG_LIST);
    }

    /**
     * Replaces what the player carries (inventory, armor, offhand, Ender chest) and their vanilla XP, hunger and effects
     * by the snapshot's. A part the snapshot lacks (the character of a world never visited) is a fresh start: no XP,
     * full hunger, no effects. Health is NOT set here: it depends on the maximum health, which changes with the world,
     * so {@link #applyHealth} runs after the bonuses are recomputed.
     *
     * <p>Effects are frozen while stored: their remaining time does not run in the other world.
     */
    public static void apply(ServerPlayer player, CompoundTag tag) {
        HolderLookup.Provider provider = player.registryAccess();
        player.getInventory().load(tag.getList(INVENTORY, Tag.TAG_COMPOUND));
        player.getEnderChestInventory().fromTag(tag.getList(ENDER_CHEST, Tag.TAG_COMPOUND), provider);

        player.experienceLevel = tag.getInt(XP_LEVEL);
        player.experienceProgress = tag.getFloat(XP_PROGRESS);
        player.totalExperience = tag.getInt(XP_TOTAL);
        // Makes the server send the bar again at its next tick (the value it last sent may be the same).
        player.giveExperienceLevels(0);

        player.removeAllEffects();
        for (Tag saved : tag.getList(EFFECTS, Tag.TAG_COMPOUND)) {
            MobEffectInstance effect = MobEffectInstance.load((CompoundTag) saved);
            if (effect != null) {
                player.addEffect(effect);
            }
        }
        player.setAbsorptionAmount(tag.getFloat(ABSORPTION));
        player.setRemainingFireTicks(0);

        FoodData fresh = new FoodData();
        if (tag.contains(FOOD, Tag.TAG_COMPOUND)) {
            fresh.readAdditionalSaveData(tag.getCompound(FOOD));
        }
        player.getFoodData().setFoodLevel(fresh.getFoodLevel());
        player.getFoodData().setSaturation(fresh.getSaturationLevel());
        player.getFoodData().setExhaustion(fresh.getExhaustionLevel());
    }

    /** The saved health, capped at the maximum of the world the player is in now; a fresh character starts at full. */
    public static void applyHealth(ServerPlayer player, CompoundTag tag) {
        float max = player.getMaxHealth();
        player.setHealth(tag.contains(HEALTH, Tag.TAG_ANY_NUMERIC) ? Math.max(1.0f, Math.min(tag.getFloat(HEALTH), max)) : max);
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
