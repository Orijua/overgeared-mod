package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A contract for one apprentice blacksmith: used on a block, the apprentice comes to stand there and works for whoever
 * used it ({@link ApprenticeSmith}). Smith gives the first when the player reaches effective smithing 50 and sells the
 * rest; each one only works if the player's smithing allows one more ({@link ApprenticeRules#allowedApprentices}).
 */
public class ApprenticeContractItem extends Item {
    /** Smith has introduced this player to their first apprentice. */
    public static final ResourceLocation INTRODUCED = Overgearedmod.id("apprentice_introduced");

    public ApprenticeContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (hire(player, context.getClickedPos().relative(context.getClickedFace())).isEmpty()) {
            return InteractionResult.FAIL;
        }
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }

    /** Hires an apprentice standing at {@code at}, if the player may have one more; empty (with a message) if not. */
    public static Optional<ApprenticeSmith> hire(ServerPlayer player, BlockPos at) {
        List<UUID> hired = player.getData(ModAttachments.APPRENTICES);
        int level = ProgressionService.effectiveSmithingLevel(player);
        if (hired.size() >= ApprenticeRules.allowedApprentices(level)) {
            int needed = ApprenticeRules.levelForApprentice(hired.size());
            player.displayClientMessage(needed < 0
                    ? Component.translatable("message.overgearedmod.apprentice_max", ApprenticeRules.MAX_APPRENTICES)
                    : Component.translatable("message.overgearedmod.apprentice_needs_level", needed), true);
            return Optional.empty();
        }
        ServerLevel serverLevel = player.serverLevel();
        ApprenticeSmith apprentice = ModEntities.APPRENTICE_SMITH.get().create(serverLevel);
        if (apprentice == null) {
            return Optional.empty();
        }
        apprentice.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
        apprentice.hireBy(player);
        serverLevel.addFreshEntity(apprentice);
        List<UUID> updated = new ArrayList<>(hired);
        updated.add(apprentice.getUUID());
        player.setData(ModAttachments.APPRENTICES, List.copyOf(updated));
        player.displayClientMessage(Component.translatable("message.overgearedmod.apprentice_hired", apprentice.getDisplayName()), false);
        return Optional.of(apprentice);
    }

    /**
     * Smith introduces the first apprentice: the first time a player with effective smithing
     * {@link ApprenticeRules#FIRST_APPRENTICE_LEVEL} talks to him, he gives them a contract for free.
     */
    public static void introduce(ServerPlayer player) {
        if (ProgressionService.effectiveSmithingLevel(player) < ApprenticeRules.FIRST_APPRENTICE_LEVEL
                || GoalTracker.milestones(player, INTRODUCED) > 0) {
            return;
        }
        GoalTracker.milestone(player, INTRODUCED);
        for (ItemStack gift : List.of(new ItemStack(ModItems.APPRENTICE_CONTRACT.get()), new ItemStack(ModItems.WORKSHOP_LEDGER.get()))) {
            if (!player.getInventory().add(gift)) {
                player.drop(gift, false);
            }
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.smith_apprentice"), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
