package com.orijua.overgearedmod.pagma;

import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.skill.PlayerSkills;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;

/**
 * Reading Pagma's rare book (canon, caps. 2-3): the reader becomes Pagma's successor, but goes back to level 1 with
 * every stat point and every skill of the old class gone, and {@link PagmaStory#BONUS_POINTS} points to spend. Smithing
 * experience stays. Pagma's legacy is completed on the spot, which grants the class, Fracaso's blueprint and, through its
 * milestone, the title "One Who Became a Legend".
 */
public final class PagmaBook {
    private PagmaBook() {
    }

    /** Reads the book the player carries (and spends it); false if they carry none. */
    public static boolean read(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inventory.getContainerSize() && slot < 0; i++) {
            if (inventory.getItem(i).is(ModItems.PAGMA_RARE_BOOK.get())) {
                slot = i;
            }
        }
        if (slot < 0) {
            return false;
        }
        inventory.removeItem(slot, 1);

        PlayerStats before = ProgressionService.get(player);
        ProgressionService.set(player, PlayerStats.DEFAULT.withProgress(1, 0, PagmaStory.BONUS_POINTS).withSmithingXp(before.smithingXp()));
        player.setData(ModAttachments.PLAYER_SKILLS, PlayerSkills.EMPTY);

        PlayerQuests quests = QuestService.get(player);
        if (!quests.isDiscovered(PagmaStory.LEGACY)) {
            player.setData(ModAttachments.PLAYER_QUESTS, quests.discover(PagmaStory.LEGACY));
        }
        if (!QuestService.get(player).isActive(PagmaStory.LEGACY)) {
            QuestService.accept(player, PagmaStory.LEGACY);
        }
        GoalTracker.milestone(player, PagmaStory.READ);
        QuestService.claim(player, PagmaStory.LEGACY);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));

        player.displayClientMessage(Component.translatable("message.overgearedmod.read_pagma_book"), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }
}
