package com.orijua.overgearedmod.feature.capsule;

import com.orijua.overgearedmod.core.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The license to upgrade a capsule to a tier. It is bought at the terminal and used up, with the capsule of the tier
 * below and some materials, in a crafting table; it does nothing on its own.
 */
public class CapsuleLicenseItem extends Item {
    private final CapsuleTier tier;

    public CapsuleLicenseItem(CapsuleTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CapsuleTier tier() {
        return tier;
    }

    /** One license of {@code tier}; the basic capsule has none. */
    public static ItemStack stackFor(CapsuleTier tier) {
        return switch (tier) {
            case ADVANCED -> new ItemStack(ModItems.ADVANCED_CAPSULE_LICENSE.get());
            case DIAMOND -> new ItemStack(ModItems.DIAMOND_CAPSULE_LICENSE.get());
            case BASIC -> throw new IllegalArgumentException("The basic capsule has no license");
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.overgearedmod.capsule_license." + tier.getSerializedName()).withStyle(ChatFormatting.GRAY));
    }
}
