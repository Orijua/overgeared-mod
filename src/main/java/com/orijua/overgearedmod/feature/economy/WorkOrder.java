package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;

/**
 * One order of the terminal's daily work: hand over {@code count} items that are the item {@code id} or, if {@code tag},
 * belong to the item tag {@code id}. Text form (the config): {@code #minecraft:coals=32} for a tag, {@code minecraft:wheat=64} for an item.
 */
public record WorkOrder(ResourceLocation id, boolean tag, int count) {
    public static final Codec<WorkOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(WorkOrder::id),
            Codec.BOOL.fieldOf("tag").forGetter(WorkOrder::tag),
            Codec.INT.fieldOf("count").forGetter(WorkOrder::count)
    ).apply(instance, WorkOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkOrder> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, WorkOrder::id,
            ByteBufCodecs.BOOL, WorkOrder::tag,
            ByteBufCodecs.VAR_INT, WorkOrder::count,
            WorkOrder::new);

    /** A test for stacks that builds the tag key once; use it when checking many stacks. */
    public Predicate<ItemStack> matcher() {
        if (tag) {
            TagKey<Item> key = TagKey.create(Registries.ITEM, id);
            return stack -> !stack.isEmpty() && stack.is(key);
        }
        return stack -> !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    /** Whether the stack counts for this order (an empty stack never does). */
    public boolean matches(ItemStack stack) {
        return matcher().test(stack);
    }

    /** An item to draw for the order: the first of the tag, or the item itself; a barrier if the tag or item is unknown. */
    public ItemStack icon() {
        Item item;
        if (tag) {
            item = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
                    .flatMap(set -> set.stream().findFirst()).map(holder -> holder.value()).orElse(Items.BARRIER);
        } else {
            item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BARRIER);
        }
        return new ItemStack(item);
    }

    /** The text form used by the config. */
    public String asString() {
        return (tag ? "#" : "") + id + "=" + count;
    }
}
