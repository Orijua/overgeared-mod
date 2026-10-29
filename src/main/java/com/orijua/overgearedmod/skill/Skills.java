package com.orijua.overgearedmod.skill;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Known skills. A plain list is enough until skills need to be data-driven or per-player. */
public final class Skills {
    public static final Skill PAGMA_LINK = new PagmaLinkSkill();

    public static final List<Skill> ALL = List.of(PAGMA_LINK);

    private Skills() {
    }

    public static Optional<Skill> forHeldItem(ItemStack mainHand) {
        return ALL.stream().filter(skill -> skill.canUseWith(mainHand)).findFirst();
    }
}
