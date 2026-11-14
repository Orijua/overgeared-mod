package com.orijua.overgearedmod.feature.quest;

import com.orijua.overgearedmod.core.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.world.Satisfy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What is lent for a quest only lives while that quest is active. Reclaiming on abandon, complete or fail only reaches
 * what the player carries at that moment; this guard closes the rest: a loan that ends up in an inventory whose quest
 * is not active (picked up from a chest or the ground, taken from another player, brought back by a failed quest in the
 * real world) disappears. It only runs in Satisfy, like the quests.
 */
public final class LoanGuard {
    private LoanGuard() {
    }

    /** Removes the loans of quests that are not active for the player. Returns how many stacks were removed. */
    public static int purge(ServerPlayer player) {
        if (!Satisfy.is(player.level())) {
            return 0;
        }
        PlayerQuests quests = QuestService.get(player);
        ResourceLocation[] first = new ResourceLocation[1];
        Map<ResourceLocation, List<ItemStack>> allowed = new HashMap<>();
        int removed = QuestService.sweep(player, stack -> {
            ResourceLocation loan = stack.get(ModDataComponents.LOANED_FOR);
            if (loan == null) {
                return false;
            }
            if (quests.isActive(loan) && takeFromAllowance(player, loan, stack, allowed)) {
                return false;
            }
            if (first[0] == null) {
                first[0] = loan;
            }
            return true;
        });
        if (removed > 0) {
            Component name = Definitions.get(player.level(), ModDatapackRegistries.QUEST, first[0])
                    .map(QuestDefinition::name).orElse(Component.literal(first[0].toString()));
            player.displayClientMessage(Component.translatable("message.overgearedmod.quest_loan_returned", name), false);
        }
        return removed;
    }

    /**
     * An active quest justifies only what it lends: one stack per lent item. A second sword (the old one picked up
     * again after abandoning and re-accepting) is a duplicate.
     */
    private static boolean takeFromAllowance(ServerPlayer player, ResourceLocation loan, ItemStack stack,
            Map<ResourceLocation, List<ItemStack>> allowed) {
        List<ItemStack> left = allowed.computeIfAbsent(loan, id -> new ArrayList<>(
                Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).map(QuestDefinition::loanedItems).orElse(List.of())));
        for (int i = 0; i < left.size(); i++) {
            if (ItemStack.isSameItem(left.get(i), stack)) {
                left.remove(i);
                return true;
            }
        }
        return false;
    }
}
