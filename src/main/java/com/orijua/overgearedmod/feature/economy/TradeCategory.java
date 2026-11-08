package com.orijua.overgearedmod.feature.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;

import java.util.function.Predicate;

/** What kind of equipment a piece is, for the trade saturation: selling many of one kind lowers its price. */
public enum TradeCategory implements StringRepresentable {
    SWORD("sword"),
    AXE("axe"),
    PICKAXE("pickaxe"),
    SHOVEL("shovel"),
    BOW("bow"),
    SHIELD("shield"),
    HELMET("helmet"),
    CHESTPLATE("chestplate"),
    LEGGINGS("leggings"),
    BOOTS("boots"),
    OTHER("other");

    // CLAMP: a bogus id can never index outside the enum.
    public static final StreamCodec<ByteBuf, TradeCategory> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(TradeCategory::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP), TradeCategory::ordinal);

    private final String name;

    TradeCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The first category whose tag the item has, in this order; {@link #OTHER} if none. Pure: tests pass a predicate. */
    public static TradeCategory classify(Predicate<TagKey<Item>> hasTag) {
        if (hasTag.test(ItemTags.SWORDS)) {
            return SWORD;
        }
        if (hasTag.test(ItemTags.AXES)) {
            return AXE;
        }
        if (hasTag.test(ItemTags.PICKAXES)) {
            return PICKAXE;
        }
        if (hasTag.test(ItemTags.SHOVELS)) {
            return SHOVEL;
        }
        if (hasTag.test(Tags.Items.TOOLS_BOW)) {
            return BOW;
        }
        if (hasTag.test(Tags.Items.TOOLS_SHIELD)) {
            return SHIELD;
        }
        if (hasTag.test(ItemTags.HEAD_ARMOR)) {
            return HELMET;
        }
        if (hasTag.test(ItemTags.CHEST_ARMOR)) {
            return CHESTPLATE;
        }
        if (hasTag.test(ItemTags.LEG_ARMOR)) {
            return LEGGINGS;
        }
        if (hasTag.test(ItemTags.FOOT_ARMOR)) {
            return BOOTS;
        }
        return OTHER;
    }
}
