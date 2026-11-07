package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The two characters, step 1: the data model. The vault is saved with the player and survives death, a snapshot reads
 * back exactly what it captured (enchantments, names, damage, shulker contents, Ender chest), a damaged tag is
 * recognised before anything is touched, and "reset all" leaves the vault alone. Nothing swaps yet.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class CharacterTests {
    private CharacterTests() {
    }

    /** A sword with everything a naive codec would lose: an enchantment, a name and damage. */
    private static ItemStack fancySword(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Espada de Grid"));
        sword.setDamageValue(7);
        return sword;
    }

    private static ItemStack loadedShulker() {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3))));
        return shulker;
    }

    /** The first test: the vault is a "copy on death" attachment, so the old player's vault reaches the new one. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theVaultSurvivesRespawn(GameTestHelper helper) {
        ServerPlayer old = TestSupport.fakePlayer(helper);
        old.getInventory().setItem(0, fancySword(helper));
        CharacterVault vault = CharacterVault.of(WorldSide.SATISFY, CharacterSnapshot.capture(old));
        old.setData(ModAttachments.CHARACTER_VAULT.get(), vault);

        ServerPlayer fresh = TestSupport.fakePlayer(helper);
        helper.assertTrue(fresh.getData(ModAttachments.CHARACTER_VAULT.get()).equals(CharacterVault.EMPTY), "a new player has no vault");
        fresh.restoreFrom(old, false);
        helper.assertTrue(fresh.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "the vault is copied to the player who respawns");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theVaultIsSavedWithThePlayer(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().setItem(3, fancySword(helper));
        CharacterVault vault = CharacterVault.of(WorldSide.REAL, CharacterSnapshot.capture(player));
        player.setData(ModAttachments.CHARACTER_VAULT.get(), vault);

        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer other = TestSupport.fakePlayer(helper);
        other.load(saved);
        helper.assertTrue(other.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "the vault is read back from the saved player");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSnapshotReadsBackExactlyWhatItCaptured(GameTestHelper helper) {
        ServerPlayer source = TestSupport.fakePlayer(helper);
        ItemStack sword = fancySword(helper);
        ItemStack shulker = loadedShulker();
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ItemStack offhand = new ItemStack(Items.SHIELD);
        source.getInventory().setItem(0, sword.copy());
        source.getInventory().setItem(8, shulker.copy());
        source.getInventory().armor.set(3, helmet.copy());
        source.getInventory().offhand.set(0, offhand.copy());
        source.getEnderChestInventory().setItem(5, new ItemStack(Items.EMERALD, 12));
        CompoundTag snapshot = CharacterSnapshot.capture(source);

        ServerPlayer target = TestSupport.fakePlayer(helper);
        target.getInventory().setItem(1, new ItemStack(Items.DIRT, 64));
        target.getEnderChestInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        CharacterSnapshot.apply(target, snapshot);

        helper.assertTrue(ItemStack.matches(target.getInventory().getItem(0), sword), "the enchanted, named, damaged sword comes back identical");
        helper.assertTrue(ItemStack.matches(target.getInventory().getItem(8), shulker), "the shulker keeps its contents");
        helper.assertTrue(ItemStack.matches(target.getInventory().armor.get(3), helmet), "the armor comes back");
        helper.assertTrue(ItemStack.matches(target.getInventory().offhand.get(0), offhand), "the offhand comes back");
        helper.assertTrue(target.getEnderChestInventory().getItem(5).is(Items.EMERALD) && target.getEnderChestInventory().getItem(5).getCount() == 12,
                "the Ender chest comes back");
        helper.assertTrue(target.getInventory().getItem(1).isEmpty(), "what the target carried before is replaced, not merged");
        helper.assertTrue(target.getEnderChestInventory().getItem(0).isEmpty(), "the target's old Ender chest is replaced too");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aSnapshotKeepsNoSharedState(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        CompoundTag snapshot = CharacterSnapshot.capture(player);
        CharacterVault vault = CharacterVault.of(WorldSide.REAL, snapshot);
        snapshot.putInt("tampered", 1);
        helper.assertFalse(vault.stored().orElseThrow().contains("tampered"), "the vault holds its own copy of the tag");
        vault.stored().orElseThrow().putInt("tampered", 1);
        helper.assertFalse(vault.stored().orElseThrow().contains("tampered"), "what the vault hands out is a copy too");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aDamagedTagIsRecognisedBeforeAnythingIsTouched(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertFalse(CharacterSnapshot.isReadable(new CompoundTag()), "an empty tag is not a snapshot");
        helper.assertTrue(CharacterSnapshot.isReadable(CharacterSnapshot.capture(player)), "a captured snapshot is readable");
        helper.assertTrue(CharacterSnapshot.isReadable(CharacterSnapshot.empty()), "the empty character is readable");
        CharacterSnapshot.apply(player, CharacterSnapshot.empty());
        helper.assertTrue(player.getInventory().isEmpty() && player.getEnderChestInventory().isEmpty(), "applying the empty character leaves nothing");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void worldSideSeparatesSatisfyFromEverythingElse(GameTestHelper helper) {
        com.orijua.overgearedmod.feature.world.Satisfy.standIn(helper.getLevel());
        try {
            helper.assertTrue(WorldSide.of(helper.getLevel()) == WorldSide.SATISFY, "the level that plays Satisfy is SATISFY");
        } finally {
            com.orijua.overgearedmod.feature.world.Satisfy.restoreStandIn();
        }
        com.orijua.overgearedmod.feature.world.Satisfy.standIn(null);
        try {
            helper.assertTrue(WorldSide.of(helper.getLevel()) == WorldSide.REAL, "any other level is REAL");
        } finally {
            com.orijua.overgearedmod.feature.world.Satisfy.restoreStandIn();
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void resetAllLeavesTheVaultAlone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.getInventory().setItem(0, fancySword(helper));
        CharacterVault vault = CharacterVault.of(WorldSide.SATISFY, CharacterSnapshot.capture(player));
        player.setData(ModAttachments.CHARACTER_VAULT.get(), vault);
        player.setData(ModAttachments.CAPSULE.get(), CapsuleState.EMPTY.disconnected(player.blockPosition()));

        ModAttachments.resetAll(player);

        helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "reset all does not touch the other world's belongings");
        helper.assertTrue(player.getData(ModAttachments.CAPSULE.get()).equals(CapsuleState.EMPTY), "but it does reset the rest");
        helper.succeed();
    }
}
