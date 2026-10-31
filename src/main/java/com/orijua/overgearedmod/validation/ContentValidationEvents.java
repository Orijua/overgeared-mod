package com.orijua.overgearedmod.validation;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;

/** Runs the content validator when the server starts (warnings in the log) and on /satisfy validate. */
public final class ContentValidationEvents {
    private ContentValidationEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ContentValidationEvents::onServerStarted);
        gameBus.addListener(ContentValidationEvents::onRegisterCommands);
    }

    /** Only warns: a broken reference shouldn't stop the server, but it should be easy to spot. */
    private static void onServerStarted(ServerStartedEvent event) {
        List<String> problems = ContentValidator.validate(ContentScanner.index(event.getServer().registryAccess(), event.getServer().getRecipeManager()));
        if (!problems.isEmpty()) {
            Overgearedmod.LOGGER.warn("Satisfy content has {} problem(s):", problems.size());
            problems.forEach(problem -> Overgearedmod.LOGGER.warn("  - {}", problem));
        }
    }

    /** /satisfy validate: checks the content as currently loaded, e.g. right after /reload. Joins the /satisfy root. */
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("satisfy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("validate")
                        .executes(ctx -> {
                            List<String> problems = ContentValidator.validate(ContentScanner.index(ctx.getSource().registryAccess(), ctx.getSource().getServer().getRecipeManager()));
                            if (problems.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.content_valid")
                                        .withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }
                            ctx.getSource().sendFailure(Component.translatable("message.overgearedmod.content_problems", problems.size()));
                            problems.forEach(problem -> ctx.getSource().sendFailure(Component.literal("- " + problem)));
                            return 0;
                        })));
    }
}
