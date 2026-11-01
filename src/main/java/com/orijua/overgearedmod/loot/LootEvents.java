package com.orijua.overgearedmod.loot;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.progression.ProgressionEvents;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.EquipmentService;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.mob.MobLevelService;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;
import java.util.Optional;

/** Elites killed by a player sometimes drop a rated piece of equipment. */
public final class LootEvents {
    /** The pieces an elite can drop (filled in datagen: iron and chainmail gear). */
    public static final TagKey<Item> ELITE_DROPS = ItemTags.create(Overgearedmod.id("elite_drops"));
    /** Elite drops roll like a smith with this much skill, and are never below Rare. */
    private static final int ELITE_LUCK = 20;
    private static final ItemRating ELITE_MINIMUM = ItemRating.RARE;

    private LootEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LootEvents::onLivingDrops);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || ProgressionEvents.creditedKiller(mob, event.getSource()) == null
                || !MobLevelService.get(mob).elite()) {
            return;
        }
        RandomSource random = mob.getRandom();
        if (random.nextDouble() >= Balance.current().eliteEquipmentChance()) {
            return;
        }
        eliteDrop((ServerLevel) mob.level(), random).ifPresent(stack ->
                event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), stack)));
    }

    /** A random piece from {@link #ELITE_DROPS}, rated Rare or better; empty if the tag has no items. */
    public static Optional<ItemStack> eliteDrop(ServerLevel level, RandomSource random) {
        List<Holder<Item>> pieces = BuiltInRegistries.ITEM.getTag(ELITE_DROPS).map(tag -> tag.stream().toList()).orElse(List.of());
        if (pieces.isEmpty()) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(pieces.get(random.nextInt(pieces.size())));
        EquipmentService.rateLoot(level, stack, ELITE_LUCK, ELITE_MINIMUM, random);
        return Optional.of(stack);
    }
}
