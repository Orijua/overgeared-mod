package com.orijua.overgearedmod.feature.equipment;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.equipment.option.OptionEffect;
import com.orijua.overgearedmod.feature.equipment.option.RolledOption;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Connects the equipment components (rating, enhancement, options, requirements) to the game and the tooltip. */
public final class EquipmentEvents {
    private EquipmentEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EquipmentEvents::onEquipmentChange);
        gameBus.addListener(EquipmentEvents::onTooltip);
        gameBus.addListener(EquipmentEvents::onDamageDealt);
        gameBus.addListener(EquipmentEvents::onAttack);
        gameBus.addListener(EquipmentEvents::onIncomingDamage);
        gameBus.addListener(EquipmentEvents::onPlayerTick);
    }

    public static boolean meetsRequirements(Player player, ItemStack stack) {
        EquipRequirements requirements = stack.get(ModDataComponents.REQUIREMENTS);
        return requirements == null || requirements.metBy(ProgressionService.statsWithoutEquipment(player));
    }

    /** Same rule with stats already computed, to reuse one calculation across several items. */
    public static boolean meetsRequirements(PlayerStats stats, ItemStack stack) {
        EquipRequirements requirements = stack.get(ModDataComponents.REQUIREMENTS);
        return requirements == null || requirements.metBy(stats);
    }

    /** Equipment can give stats too (stat options), so both the stat effects and the bonuses are re-applied. */
    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            ProgressionService.refreshEffects(player);
        }
    }

    /** Every option of the equipment the player wears where it counts and whose requirements they meet. */
    public static void forEachActiveOption(Player player, BiConsumer<OptionEffect, Float> action) {
        PlayerStats stats = ProgressionService.statsWithoutEquipment(player);
        for (ItemStack stack : EquipmentBonuses.wornEquipment(player)) {
            if (!meetsRequirements(stats, stack)) {
                continue;
            }
            for (RolledOption rolled : stack.getOrDefault(ModDataComponents.ITEM_OPTIONS, List.<RolledOption>of())) {
                Definitions.get(player.level(), ModDatapackRegistries.ITEM_OPTION, rolled.option())
                        .ifPresent(def -> action.accept(def.effect(), rolled.value()));
            }
        }
    }

    /** Options that react to damage: blocking it as the wearer, or making the wearer's attacks hit harder. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer wearer) {
            forEachActiveOption(wearer, (effect, value) -> {
                if (!event.isCanceled()) {
                    effect.onWearerHurt(wearer, event, value);
                }
            });
        }
        if (!event.isCanceled() && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            forEachActiveOption(attacker, (effect, value) ->
                    event.setAmount(effect.outgoingDamage(attacker, event.getEntity(), event.getAmount(), value)));
        }
    }

    /** Once a second, options that act on their own while worn (Fracaso's fear). */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            forEachActiveOption(player, (effect, value) -> effect.tickWorn(player, value));
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
            PlayerStats stats = ProgressionService.statsWithoutEquipment(event.getEntity());
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
