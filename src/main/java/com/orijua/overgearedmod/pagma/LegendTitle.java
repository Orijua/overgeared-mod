package com.orijua.overgearedmod.pagma;

import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * What the title "One Who Became a Legend" does (canon, cap. 3: resistance to abnormal conditions, and death is
 * prevented when health reaches its minimum). Here: a harmful effect has a {@link #RESIST_CHANCE} chance not to take
 * hold, and a killing blow leaves the player at 1 health instead, once every {@link #COOLDOWN_TICKS}.
 */
public final class LegendTitle {
    public static final double RESIST_CHANCE = 0.3;
    /** Five minutes between two saves from death. */
    public static final int COOLDOWN_TICKS = 20 * 60 * 5;
    private static final String READY_AT = "overgearedmod.legend_ready_at";

    private LegendTitle() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGH, LegendTitle::onDeath);
        gameBus.addListener(LegendTitle::onEffect);
    }

    public static boolean has(Player player) {
        return player.getData(ModAttachments.PLAYER_IDENTITY).hasTitle(PagmaStory.BECAME_LEGEND);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !has(player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(READY_AT)) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(1.0f);
        player.getPersistentData().putLong(READY_AT, now + COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable("message.overgearedmod.legend_saved"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && has(player)
                && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && player.getRandom().nextDouble() < RESIST_CHANCE) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
