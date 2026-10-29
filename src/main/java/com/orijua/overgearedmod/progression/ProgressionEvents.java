package com.orijua.overgearedmod.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ProgressionEvents {
    private ProgressionEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ProgressionEvents::onMobSpawn);
        gameBus.addListener(ProgressionEvents::onLivingDeath);
        gameBus.addListener(ProgressionEvents::onLogin);
        gameBus.addListener(ProgressionEvents::onRespawn);
        gameBus.addListener(ProgressionEvents::onRegisterCommands);
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
        if (victim instanceof Player || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        ProgressionService.awardXp(killer, ProgressionRules.killXp(Balance.current(), new ProgressionRules.Victim(
                victim.getMaxHealth(),
                victim.getAttribute(Attributes.ATTACK_DAMAGE) != null ? victim.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0,
                victim.getArmorValue(),
                victim.getData(ModAttachments.FROM_SPAWNER))));
        GoalTracker.onKill(killer, victim.getType());
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

    /** Debug commands for testing without grinding: /satisfy xp <amount>, /satisfy reset. */
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("satisfy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ProgressionService.awardXp(player, IntegerArgumentType.getInteger(ctx, "amount"));
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ModAttachments.resetAll(player);
                            ProgressionService.set(player, PlayerStats.DEFAULT);
                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.reset"), false);
                            return 1;
                        })));
    }
}
