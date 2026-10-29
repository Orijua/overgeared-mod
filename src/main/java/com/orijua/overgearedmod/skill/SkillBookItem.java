package com.orijua.overgearedmod.skill;

import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Right-click to learn the skill stored in its {@code skill_book} component. Consumed on success. */
public class SkillBookItem extends Item {
    public SkillBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forSkill(Item book, ResourceLocation skill) {
        ItemStack stack = new ItemStack(book);
        stack.set(ModDataComponents.SKILL_BOOK, skill);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation skill = stack.get(ModDataComponents.SKILL_BOOK);
        if (skill == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (SkillService.learn(serverPlayer, skill)) {
                SkillService.definition(serverPlayer, skill).ifPresent(def ->
                        serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.learned", def.name()), true));
                stack.consume(1, player);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.overgearedmod.already_known"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation skill = stack.get(ModDataComponents.SKILL_BOOK);
        if (skill == null || context.registries() == null) {
            return;
        }
        Definitions.get(context.registries(), ModDatapackRegistries.SKILL, skill).ifPresent(def -> {
            tooltip.add(def.name().copy().withStyle(ChatFormatting.GOLD));
            tooltip.add(def.description().copy().withStyle(ChatFormatting.GRAY));
        });
    }
}
