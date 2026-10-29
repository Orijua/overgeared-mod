package com.orijua.overgearedmod.rating;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Item rating tiers.
 * Canon: Satisfy grades equipment by rating (normal, rare, epic, unique, legendary, myth...).
 * Project design: the colors and the attack bonus values are our own numbers, not canon.
 */
public enum ItemRating implements StringRepresentable {
    NORMAL("normal", ChatFormatting.WHITE, 0.0),
    RARE("rare", ChatFormatting.BLUE, 0.5),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 1.0),
    UNIQUE("unique", ChatFormatting.GOLD, 2.0),
    LEGENDARY("legendary", ChatFormatting.YELLOW, 3.5),
    MYTH("myth", ChatFormatting.RED, 5.0);

    // Codec = how it is saved to disk (NBT/JSON); StreamCodec = how it travels over the network.
    public static final Codec<ItemRating> CODEC = StringRepresentable.fromEnum(ItemRating::values);
    public static final StreamCodec<ByteBuf, ItemRating> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(ItemRating::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP),
            ItemRating::ordinal);

    private final String name;
    private final ChatFormatting color;
    private final double attackDamageBonus;

    ItemRating(String name, ChatFormatting color, double attackDamageBonus) {
        this.name = name;
        this.color = color;
        this.attackDamageBonus = attackDamageBonus;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Flat attack damage added on top of the weapon's base damage. */
    public double attackDamageBonus() {
        return attackDamageBonus;
    }

    public Component displayName() {
        return Component.translatable("rating.overgearedmod." + name).withStyle(color);
    }
}
