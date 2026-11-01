package com.orijua.overgearedmod.feature.equipment.option;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Chance to block an attack outright, as a shield would. Only attacks from a creature count (not falls or fire),
 * and nothing a shield can't stop. The rolled value is the chance (0.15 = 15%).
 */
public record BlockChanceOptionEffect() implements OptionEffect {
    public static final MapCodec<BlockChanceOptionEffect> CODEC = MapCodec.unit(new BlockChanceOptionEffect());

    @Override
    public OptionEffectType type() {
        return OptionEffectType.BLOCK_CHANCE;
    }

    @Override
    public Component formatValue(float value) {
        return Component.literal(String.format("%.0f%%", value * 100));
    }

    @Override
    public void onWearerHurt(ServerPlayer wearer, LivingIncomingDamageEvent event, float value) {
        if (event.getSource().getEntity() == null || event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) {
            return;
        }
        if (wearer.getRandom().nextFloat() < value) {
            event.setCanceled(true);
            wearer.level().playSound(null, wearer.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}
