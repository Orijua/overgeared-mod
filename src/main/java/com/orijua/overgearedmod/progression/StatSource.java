package com.orijua.overgearedmod.progression;

import net.minecraft.network.chat.Component;

/** Where a {@link StatModifier} comes from, as shown in the stat tooltip ("Warrior", "Zombie Hunter"...). */
public record StatSource(Component name) {
    public static final StatSource ALLOCATED = new StatSource(Component.translatable("screen.overgearedmod.stat_source.allocated"));
}
