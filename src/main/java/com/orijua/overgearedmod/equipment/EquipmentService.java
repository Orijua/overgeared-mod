package com.orijua.overgearedmod.equipment;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.equipment.option.FixedOption;
import com.orijua.overgearedmod.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.equipment.option.RolledOption;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.smithing.ForgingExtras;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Server-side changes to equipment: what forging and enhancing write onto an item. */
public final class EquipmentService {
    public enum EnhanceResult { SUCCESS, FAILURE, NOT_EQUIPMENT, MAXED }

    private EquipmentService() {
    }

    /** Rolls rating and options for a freshly forged item and stamps its requirements (automatic forging). */
    public static void stampForged(ServerPlayer smith, ItemStack stack, Optional<EquipRequirements> requirements) {
        stampForged(smith, stack, requirements, 0, 0);
    }

    /**
     * As above, for a manual forge: {@code skillBonus} shifts the rating roll (negative when botched) and
     * {@code extraOptionChance} may add one option beyond what the rating allows.
     */
    public static void stampForged(ServerPlayer smith, ItemStack stack, Optional<EquipRequirements> requirements,
                                   double skillBonus, double extraOptionChance) {
        stampForged(smith, stack, requirements, ForgingExtras.NONE, skillBonus, extraOptionChance);
    }

    /**
     * As above, for a recipe with {@link ForgingExtras}: the rating stays within its range and, when it has
     * fixed options, the item gets exactly those (the ones its rating reaches) instead of random ones.
     */
    public static void stampForged(ServerPlayer smith, ItemStack stack, Optional<EquipRequirements> requirements,
                                   ForgingExtras extras, double skillBonus, double extraOptionChance) {
        PlayerStats stats = ProgressionService.effectiveStats(smith);
        stampForged(smith.level(), stack, requirements, extras, stats.dexterity(), ProgressionService.effectiveSmithingLevel(smith),
                skillBonus, extraOptionChance, smith.getRandom());
    }

    /** As above, for a smith who isn't a player (an apprentice): their dexterity and smithing level are given. */
    public static void stampForged(Level level, ItemStack stack, Optional<EquipRequirements> requirements, ForgingExtras extras,
                                   int dexterity, int smithingLevel, double skillBonus, double extraOptionChance, RandomSource random) {
        ItemRating rating = RatingRoller.roll(dexterity, smithingLevel, skillBonus, extras.minRating(), extras.maxRating(), random.nextDouble());
        requirements.ifPresent(r -> stack.set(ModDataComponents.REQUIREMENTS, r));
        if (!extras.fixedOptions().isEmpty()) {
            stack.set(ModDataComponents.ITEM_RATING, rating);
            stack.set(ModDataComponents.ITEM_OPTIONS, FixedOption.forRating(extras.fixedOptions(), rating));
            refreshName(stack);
            return;
        }
        int extraOptions = random.nextDouble() < extraOptionChance ? 1 : 0;
        applyRating(level, stack, rating, extraOptions, random);
    }

    /**
     * Gives equipment found as loot (chests, elites, bosses) a rating and options, as if it had been forged.
     * Leaves anything that isn't equipment, or already has a rating, untouched.
     */
    public static void rateLoot(Level level, ItemStack stack, int luck, ItemRating minimum, RandomSource random) {
        if (!canEnhance(stack) || stack.has(ModDataComponents.ITEM_RATING)) {
            return;
        }
        applyRating(level, stack, RatingRoller.rollLoot(luck, minimum, random.nextDouble()), 0, random);
    }

    /**
     * Stamps {@code rating}, rolls the options it allows (plus {@code extraOptions}) from the item_option pool,
     * and renames the item.
     */
    private static void applyRating(Level level, ItemStack stack, ItemRating rating, int extraOptions, RandomSource random) {
        stack.set(ModDataComponents.ITEM_RATING, rating);
        List<Definitions.Entry<ItemOptionDefinition>> pool = Definitions.all(level, ModDatapackRegistries.ITEM_OPTION).stream()
                .filter(e -> e.value().weight() > 0 && e.value().canRollOn(stack))
                .toList();
        int count = EquipmentRules.optionCount(rating) + extraOptions;
        List<RolledOption> options = EquipmentRules.pickWeighted(pool, e -> e.value().weight(), count, random::nextDouble)
                .stream()
                .map(e -> new RolledOption(e.id(), EquipmentRules.rollValue(e.value().min(), e.value().max(), rating, random.nextDouble())))
                .toList();
        if (!options.isEmpty()) {
            stack.set(ModDataComponents.ITEM_OPTIONS, options);
        }
        refreshName(stack);
    }

    /**
     * Writes the display name as "<name> +N" in the rating color. It goes into vanilla's item_name
     * component, so it shows everywhere (tooltip, hotbar, inventory); a name set on a vanilla anvil still wins.
     */
    public static void refreshName(ItemStack stack) {
        ItemRating rating = stack.get(ModDataComponents.ITEM_RATING);
        int enhancement = stack.getOrDefault(ModDataComponents.ENHANCEMENT, 0);
        if (rating == null && enhancement == 0) {
            return;
        }
        MutableComponent name = Component.translatable(stack.getItem().getDescriptionId());
        if (enhancement > 0) {
            name.append(" +" + enhancement);
        }
        stack.set(DataComponents.ITEM_NAME, rating != null ? name.withStyle(rating.color()) : name);
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
        double chance = EquipmentRules.enhancementChance(Balance.current(), level, ProgressionService.effectiveStats(smith).dexterity());
        boolean success = smith.getRandom().nextDouble() < chance;
        if (success) {
            equipment.set(ModDataComponents.ENHANCEMENT, level + 1);
            refreshName(equipment);
        }
        smith.displayClientMessage(Component.translatable(success ? "message.overgearedmod.enhance_success" : "message.overgearedmod.enhance_failure",
                equipment.getHoverName(), success ? level + 1 : level), true);
        smith.level().playSound(null, smith.blockPosition(), success ? SoundEvents.SMITHING_TABLE_USE : SoundEvents.ANVIL_DESTROY,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        return success ? EnhanceResult.SUCCESS : EnhanceResult.FAILURE;
    }
}
