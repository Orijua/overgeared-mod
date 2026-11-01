package com.orijua.overgearedmod.feature.skill;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Blacksmith's Rage: attack power +10% while it lasts (canon, cap. 71; the skill is created by Khan's quest, cap. 24).
 * Given by the skill of the same name ({@code self_buff}); how long it lasts is our own design.
 */
public class BlacksmithRageEffect extends MobEffect {
    public static final double ATTACK_BONUS = 0.10;

    public BlacksmithRageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD9531E);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, Overgearedmod.id("effect.blacksmith_rage"), ATTACK_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
