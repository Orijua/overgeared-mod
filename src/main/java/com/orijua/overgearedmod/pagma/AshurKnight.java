package com.orijua.overgearedmod.pagma;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * One of Earl Ashur's escort knights (canon, caps. 1-2: knights of about level 180; one kills Grid with a single blow).
 * In full iron with a sword; it guards the manor's door and comes with Ashur for the book.
 */
public class AshurKnight extends AshurRetinue {
    public static final int CANON_LEVEL = 180;

    public AshurKnight(EntityType<? extends AshurKnight> type, Level level) {
        super(type, level);
        xpReward = 100;
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected int canonLevel() {
        return CANON_LEVEL;
    }

    @Override
    protected void registerAttackGoal() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
    }
}
