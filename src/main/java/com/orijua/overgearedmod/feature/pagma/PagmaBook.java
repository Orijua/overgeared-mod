package com.orijua.overgearedmod.feature.pagma;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.skill.PlayerSkills;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;

/**
 * Reading Pagma's rare book (canon, caps. 2-3): the reader becomes Pagma's successor, but goes back to level 1 with
 * every stat point and every skill of the old class gone, and {@link PagmaStory#BONUS_POINTS} points to spend. Smithing
 * experience stays. Pagma's legacy is completed on the spot, which grants the class, Fracaso's blueprint and, through its
 * milestone, the title "One Who Became a Legend". The legacy is checked first: a second book is refused untouched
 * (nothing is spent or reset) once the legacy is done, or if it can't be started.
 */
public final class PagmaBook {
    private PagmaBook() {
    }

    /** Reads the book the player carries (and spends it); false if they carry none or the legacy can't be taken. */
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
        if (QuestService.get(player).isCompleted(PagmaStory.LEGACY)) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.pagma_book_already_read"), false);
            return false;
        }

        PlayerQuests quests = QuestService.get(player);
        if (!quests.isDiscovered(PagmaStory.LEGACY)) {
            player.setData(ModAttachments.PLAYER_QUESTS, quests.discover(PagmaStory.LEGACY));
        }
        if (!QuestService.get(player).isActive(PagmaStory.LEGACY)) {
            QuestService.accept(player, PagmaStory.LEGACY);
        }
        if (!QuestService.get(player).isActive(PagmaStory.LEGACY)) {
            Overgearedmod.LOGGER.warn("{} can't start Pagma's legacy: the book is left unread", player.getGameProfile().getName());
            return false;
        }

        inventory.removeItem(slot, 1);
        PlayerStats before = ProgressionService.get(player);
        ProgressionService.set(player, PlayerStats.DEFAULT.withProgress(1, 0, PagmaStory.BONUS_POINTS).withSmithingXp(before.smithingXp()));
        player.setData(ModAttachments.PLAYER_SKILLS, PlayerSkills.EMPTY);

        GoalTracker.milestone(player, PagmaStory.READ);
        QuestService.claim(player, PagmaStory.LEGACY);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));

        player.displayClientMessage(Component.translatable("message.overgearedmod.read_pagma_book"), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }
}
