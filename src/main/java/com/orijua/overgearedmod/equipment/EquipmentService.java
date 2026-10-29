package com.orijua.overgearedmod.equipment;

import com.orijua.overgearedmod.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Server-side changes to equipment: what forging and enhancing write onto an item. */
public final class EquipmentService {
    public enum EnhanceResult { SUCCESS, FAILURE, NOT_EQUIPMENT, MAXED }

    private EquipmentService() {
    }

    /** Rolls rating and options for a freshly forged item and stamps its requirements. */
    public static void stampForged(ServerPlayer smith, ItemStack stack, Optional<EquipRequirements> requirements) {
        PlayerStats stats = ProgressionService.effectiveStats(smith);
        RandomSource random = smith.getRandom();
        ItemRating rating = RatingRoller.roll(stats.dexterity(), ProgressionService.effectiveSmithingLevel(smith), random.nextDouble());
        stack.set(ModDataComponents.ITEM_RATING, rating);

        List<Definitions.Entry<ItemOptionDefinition>> pool = Definitions.all(smith.level(), ModDatapackRegistries.ITEM_OPTION);
        List<RolledOption> options = EquipmentRules.pickWeighted(pool, e -> e.value().weight(), EquipmentRules.optionCount(rating), random::nextDouble)
                .stream()
                .map(e -> new RolledOption(e.id(), EquipmentRules.rollValue(e.value().min(), e.value().max(), rating, random.nextDouble())))
                .toList();
        if (!options.isEmpty()) {
            stack.set(ModDataComponents.ITEM_OPTIONS, options);
        }
        requirements.ifPresent(r -> stack.set(ModDataComponents.REQUIREMENTS, r));
    }

    public static boolean canEnhance(ItemStack stack) {
        return stack.isDamageableItem() && stack.getCount() == 1;
    }

    public static EnhanceResult enhance(ServerPlayer smith, ItemStack equipment) {
        if (!canEnhance(equipment)) {
            return EnhanceResult.NOT_EQUIPMENT;
        }
        int level = equipment.getOrDefault(ModDataComponents.ENHANCEMENT, 0);
        if (level >= EquipmentRules.MAX_ENHANCEMENT) {
            return EnhanceResult.MAXED;
        }
        double chance = EquipmentRules.enhancementChance(level, ProgressionService.effectiveStats(smith).dexterity());
        boolean success = smith.getRandom().nextDouble() < chance;
        if (success) {
            equipment.set(ModDataComponents.ENHANCEMENT, level + 1);
        }
        smith.displayClientMessage(Component.translatable(success ? "message.overgearedmod.enhance_success" : "message.overgearedmod.enhance_failure",
                equipment.getHoverName(), success ? level + 1 : level), true);
        smith.level().playSound(null, smith.blockPosition(), success ? SoundEvents.SMITHING_TABLE_USE : SoundEvents.ANVIL_DESTROY,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        return success ? EnhanceResult.SUCCESS : EnhanceResult.FAILURE;
    }
}
