package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.mob.MobKilledEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;

public final class ProgressionEvents {
    private ProgressionEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ProgressionEvents::onMobSpawn);
        gameBus.addListener(ProgressionEvents::onLivingDeath);
        gameBus.addListener(ProgressionEvents::onLogin);
        gameBus.addListener(ProgressionEvents::onRespawn);
    }

    /** Remembers which mobs came from a spawner, so farms give less XP. */
    private static void onMobSpawn(FinalizeSpawnEvent event) {
        if (MobSpawnType.isSpawner(event.getSpawnType())) {
            event.getEntity().setData(ModAttachments.FROM_SPAWNER, true);
        }
    }

    /** Killing a non-player mob grants Satisfy XP (separate from vanilla XP). */
    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof ServerPlayer dead) {
            penalizeDeath(dead);
        }
        ServerPlayer killer = creditedKiller(victim, event.getSource());
        if (victim instanceof Player || killer == null) {
            return;
        }
        ProgressionService.awardXp(killer, ProgressionRules.killXp(Balance.current(), new ProgressionRules.Victim(
                victim.getMaxHealth(),
                victim.getAttribute(Attributes.ATTACK_DAMAGE) != null ? victim.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0,
                victim.getArmorValue(),
                victim.getData(ModAttachments.FROM_SPAWNER),
                victim.getData(ModAttachments.MOB_LEVEL).elite())));
        NeoForge.EVENT_BUS.post(new MobKilledEvent(killer, victim));
    }

    /**
     * The player a death counts for: the one who dealt the killing blow, or else the one who hurt the victim last if it
     * was recently (vanilla's kill credit, about 5 s), so a mob that burns, falls or drowns after a hit is still yours.
     */
    public static @Nullable ServerPlayer creditedKiller(LivingEntity victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return victim.getKillCredit() instanceof ServerPlayer player ? player : null;
    }

    /** Runs before the respawn copy, so the reduced stats are what the new player entity inherits. */
    private static void penalizeDeath(ServerPlayer player) {
        PlayerStats before = ProgressionService.get(player);
        PlayerStats after = ProgressionRules.applyDeathPenalty(Balance.current(), before);
        if (after.xp() < before.xp()) {
            player.setData(ModAttachments.PLAYER_STATS, after);
            player.sendSystemMessage(Component.translatable("message.overgearedmod.death_xp_lost", before.xp() - after.xp()));
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionService.refreshEffects(player);
        }
    }

    /** Respawn creates a new player entity: stats are copied (copyOnDeath), modifiers must be re-applied. */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionService.refreshEffects(player);
            player.setHealth(player.getMaxHealth());
        }
    }

}
