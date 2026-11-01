package com.orijua.overgearedmod.feature.quest.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hand {@code count} of {@code items} to the quest's giver: {"type": "deliver", "items": "#minecraft:logs", "count": 64}.
 * {@code items} is an item id, a list of them or a #tag. Only for quests with a giver, at the NPC; deliveries can be
 * partial, and the amount handed over so far is what the quest stores. Nothing counts it, so it can't be a title's goal
 * or a discover_when (the content validator checks both).
 */
public record DeliverGoal(HolderSet<Item> items, int count) implements Goal {
    public static final MapCodec<DeliverGoal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(DeliverGoal::items),
            Codec.intRange(1, 100_000).fieldOf("count").forGetter(DeliverGoal::count)
    ).apply(instance, DeliverGoal::new));

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.is(items);
    }

    @Override
    public GoalType type() {
        return GoalType.DELIVER;
    }

    @Override
    public int target() {
        return count;
    }

    /** Nothing is counted: the quest itself keeps what was delivered. */
    @Override
    public int progress(PlayerCounters counters, PlayerStats stats) {
        return 0;
    }

    @Override
    public boolean cumulative() {
        return false;
    }

    @Override
    public int questProgress(int stored, PlayerCounters counters, PlayerStats stats) {
        return stored;
    }

    @Override
    public Component describe() {
        return Component.translatable("goal.overgearedmod.deliver", count, itemsName());
    }

    /** The tag's name ("tag.item.minecraft.logs" in the lang files) or the first item's. */
    public Component itemsName() {
        return items.unwrapKey()
                .map(DeliverGoal::tagName)
                .orElseGet(() -> items.stream().findFirst().map(Holder::value).map(Item::getDescription).orElse(Component.empty()));
    }

    private static Component tagName(TagKey<Item> tag) {
        String key = "tag.item." + tag.location().getNamespace() + "." + tag.location().getPath().replace('/', '.');
        return Component.translatableWithFallback(key, "#" + tag.location());
    }
}
