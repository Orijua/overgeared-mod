package com.orijua.overgearedmod.equipment;

import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.progression.StatType;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Connects the equipment components (rating, enhancement, options, requirements) to the game and the tooltip. */
public final class EquipmentEvents {
    private EquipmentEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EquipmentEvents::onEquipmentChange);
        gameBus.addListener(EquipmentEvents::onTooltip);
        gameBus.addListener(EquipmentEvents::onDamageDealt);
        gameBus.addListener(EquipmentEvents::onAttack);
    }

    public static boolean meetsRequirements(Player player, ItemStack stack) {
        EquipRequirements requirements = stack.get(ModDataComponents.REQUIREMENTS);
        return requirements == null || requirements.metBy(ProgressionService.effectiveStats(player));
    }

    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            EquipmentBonuses.apply(player);
        }
    }

    /** Life steal and any other on-hit option, read from the attacker's main hand. */
    private static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || event.getNewDamage() <= 0) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!meetsRequirements(attacker, weapon)) {
            return;
        }
        for (RolledOption rolled : weapon.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
            Definitions.get(attacker.level(), ModDatapackRegistries.ITEM_OPTION, rolled.option())
                    .ifPresent(def -> def.effect().onHit(attacker, event.getEntity(), event.getNewDamage(), rolled.value()));
        }
    }

    /** Fighting with an item whose requirements you don't meet is not allowed. */
    private static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!meetsRequirements(player, player.getMainHandItem())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.overgearedmod.requirements_unmet"), true);
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        HolderLookup.Provider registries = event.getContext().registries();
        List<Component> lines = new ArrayList<>();

        ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
        if (rating != null) {
            lines.add(rating.attackDamageBonus() > 0
                    ? Component.translatable("tooltip.overgearedmod.rating_bonus", rating.displayName(), rating.attackDamageBonus())
                    : Component.translatable("tooltip.overgearedmod.rating", rating.displayName()));
        }
        int enhancement = stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0);
        if (enhancement > 0) {
            lines.add(Component.translatable("tooltip.overgearedmod.enhancement", enhancement,
                    enhancement * EquipmentRules.ATTACK_PER_ENHANCEMENT).withStyle(ChatFormatting.AQUA));
        }
        if (registries != null) {
            for (RolledOption rolled : stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
                Definitions.get(registries, ModDatapackRegistries.ITEM_OPTION, rolled.option()).ifPresent(def ->
                        lines.add(Component.literal("  ").append(def.name()).append(" ").append(def.effect().formatValue(rolled.value()))
                                .withStyle(ChatFormatting.GREEN)));
            }
        }
        EquipRequirements requirements = stack.get(ModDataComponents.REQUIREMENTS);
        if (requirements != null && event.getEntity() != null) {
            PlayerStats stats = ProgressionService.effectiveStats(event.getEntity());
            lines.addAll(requirementLines(requirements, stats));
            if (!requirements.metBy(stats)) {
                lines.add(Component.translatable("tooltip.overgearedmod.bonuses_inactive").withStyle(ChatFormatting.RED));
            }
        }

        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    private static List<Component> requirementLines(EquipRequirements requirements, PlayerStats stats) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.overgearedmod.requires").withStyle(ChatFormatting.GRAY));
        lines.add(requirementLine(Component.translatable("tooltip.overgearedmod.requires_level", requirements.level()),
                stats.level() >= requirements.level()));
        for (Map.Entry<StatType, Integer> entry : requirements.stats().entrySet()) {
            lines.add(requirementLine(Component.literal(entry.getValue() + " ").append(entry.getKey().displayName()),
                    stats.get(entry.getKey()) >= entry.getValue()));
        }
        return lines;
    }

    private static Component requirementLine(Component text, boolean met) {
        return Component.literal("  ").append(text).withStyle(met ? ChatFormatting.GRAY : ChatFormatting.RED);
    }
}
