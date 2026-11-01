package com.orijua.overgearedmod.feature.mob;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Gives a mob its level: stores it (synced, so clients can show it above the head) and adds permanent
 * attribute modifiers, which vanilla saves with the mob. Kill XP already grows with the mob's health
 * and damage, so a higher level is automatically worth more XP.
 */
public final class MobLevelService {
    private static final ResourceLocation MODIFIER_ID = Overgearedmod.id("mob_level");

    private MobLevelService() {
    }

    public static MobLevel get(Mob mob) {
        return mob.getData(ModAttachments.MOB_LEVEL);
    }

    public static void assign(Mob mob, MobLevel level, Balance balance) {
        mob.setData(ModAttachments.MOB_LEVEL, level);
        setBonus(mob, Attributes.MAX_HEALTH, MobLevelRules.healthBonus(balance, level));
        setBonus(mob, Attributes.ATTACK_DAMAGE, MobLevelRules.damageBonus(balance, level));
        mob.setHealth(mob.getMaxHealth());
        if (level.elite()) {
            mob.setGlowingTag(true);
        }
    }

    private static void setBonus(Mob mob, Holder<Attribute> attribute, double fraction) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(MODIFIER_ID);
        if (fraction > 0) {
            instance.addPermanentModifier(new AttributeModifier(MODIFIER_ID, fraction, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
