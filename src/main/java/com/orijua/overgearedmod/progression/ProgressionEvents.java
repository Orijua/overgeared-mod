package com.orijua.overgearedmod.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.orijua.overgearedmod.Config;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ProgressionEvents {
    private ProgressionEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ProgressionEvents::onLivingDeath);
        gameBus.addListener(ProgressionEvents::onLogin);
        gameBus.addListener(ProgressionEvents::onRespawn);
        gameBus.addListener(ProgressionEvents::onRegisterCommands);
    }

    /** Killing a non-player mob grants Satisfy XP (separate from vanilla XP). */
    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        ProgressionService.awardXp(killer, ProgressionRules.killXp(victim.getMaxHealth(), Config.XP_MULTIPLIER.get()));
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatEffects.apply(player, ProgressionService.get(player));
        }
    }

    /** Respawn creates a new player entity: stats are copied (copyOnDeath), modifiers must be re-applied. */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatEffects.apply(player, ProgressionService.get(player));
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
