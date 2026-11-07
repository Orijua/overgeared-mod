package com.orijua.overgearedmod.gametest;

import com.mojang.authlib.GameProfile;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.mob.MobKilledEvent;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.npc.VillageChiefNpc;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.smithing.ItemForgedEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/**
 * Shared helpers for the game tests. Tests run with `gradlew runGameTestServer`
 * (or `/test runall` in a dev world); each class groups the tests of one system.
 */
final class TestSupport {
    /** data/overgearedmod/structure/empty.nbt: a 3x3x3 air box. */
    static final String EMPTY = "empty";
    /**
     * How far, in chunks, the searches for Bairan and Winston look on the vanilla overworld, whose terrain doesn't follow
     * the canon map (seas and rivers anywhere): the radius they had before the Satisfy terrain followed the map.
     */
    static final int VANILLA_TERRAIN_SEARCH_RADIUS = 32;

    private TestSupport() {
    }

    /** A ServerPlayer with a no-op connection (syncing is ignored), standing in the test area and looking towards +Z. */
    static ServerPlayer fakePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "satisfy-test"));
        Vec3 origin = helper.absoluteVec(new Vec3(0.5, 1, 0.5));
        player.moveTo(origin.x, origin.y, origin.z, 0.0f, 0.0f);
        player.setYHeadRot(0.0f);
        return player;
    }

    /** Posts a zombie kill, as a real kill would after awarding XP (the zombie is never added to the world). */
    static void killZombie(GameTestHelper helper, ServerPlayer player) {
        kill(helper, player, EntityType.ZOMBIE);
    }

    /** Posts a kill of a mob of {@code type}, which is never added to the world. */
    static void kill(GameTestHelper helper, ServerPlayer player, EntityType<? extends LivingEntity> type) {
        NeoForge.EVENT_BUS.post(new MobKilledEvent(player, type.create(helper.getLevel())));
    }

    /** Posts a forge of the given rating, as taking it out of the smithing anvil would. */
    static void forge(ServerPlayer player, ItemRating rating) {
        forge(player, Items.IRON_SWORD, rating);
    }

    static void forge(ServerPlayer player, Item item, ItemRating rating) {
        NeoForge.EVENT_BUS.post(new ItemForgedEvent(player, new ItemStack(item), rating));
    }

    /** Smith (the blacksmith NPC), next to the fake player: the NPC to deal with for the quests he gives. */
    static BlacksmithNpc smith(GameTestHelper helper) {
        return helper.spawn(ModEntities.BLACKSMITH.get(), 1, 1, 2);
    }

    /** The village chief, next to the fake player. */
    static VillageChiefNpc chief(GameTestHelper helper) {
        return helper.spawn(ModEntities.VILLAGE_CHIEF.get(), 1, 1, 2);
    }

    /** Marks quests as already completed, to start a test further along a chain. */
    static void completed(ServerPlayer player, String... quests) {
        PlayerQuests state = QuestService.get(player);
        for (String quest : quests) {
            state = state.complete(Overgearedmod.id(quest));
        }
        player.setData(ModAttachments.PLAYER_QUESTS, state);
    }

    /** Attack damage {@code weapon} gives on top of a plain iron sword. */
    static double attackBonusOver(GameTestHelper helper, ItemStack weapon) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Equipment modifiers are applied on the entity tick; force it so tests needn't wait.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.tick();
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.tick();
        return player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base;
    }

    static void assertClose(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-4, what + ": expected " + expected + ", got " + actual);
    }
}
