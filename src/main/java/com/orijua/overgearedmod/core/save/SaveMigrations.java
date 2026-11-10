package com.orijua.overgearedmod.core.save;

import com.mojang.logging.LogUtils;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Consumer;

/**
 * The version of the mod's saved player data, and the steps that bring an older save up to date.
 *
 * <p>Every player carries the version their data was last saved with ({@link ModAttachments#SAVE_VERSION}). When they
 * join, the steps they are missing run in order, before any other system reads the data, and the player is stamped
 * with {@link #CURRENT}.
 *
 * <p>Two ways to change saved data:
 * <ul>
 *   <li><b>Compatible</b> (a new field with a default, a new attachment): read it with {@code optionalFieldOf} and
 *       nothing else is needed. No version bump.</li>
 *   <li><b>Incompatible</b> (a field renamed or removed, a value that changes meaning or scale, an id renamed): raise
 *       {@link #CURRENT}, add a {@link Step} to {@link #STEPS} and add the fixture {@code saves/v<N>.snbt} of the new
 *       shape to the unit tests. The codec must still <em>read</em> the old shape (the step runs after loading), so
 *       keep the old field as optional until the step has moved it.</li>
 * </ul>
 * The fixtures of older versions are never edited or deleted: {@code SavedDataCompatibilityTest} loads them all with
 * today's codecs, so a change that would lose players' data fails the build.
 *
 * <p>A step also runs on players that never had the old data (their version is the baseline too), so it must be
 * harmless on default values.
 */
public final class SaveMigrations {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The version of every save from before versions existed, and the default of the attachment. */
    public static final int BASELINE = 1;

    /** The version this build of the mod writes. */
    public static final int CURRENT = 1;

    /** Brings a player from version {@code to - 1} to {@code to}; {@code what} is a short description for the log. */
    public record Step(int to, String what, Consumer<ServerPlayer> migrate) {
    }

    /** One step per version after the baseline, in order. */
    private static final List<Step> STEPS = List.of();

    static {
        validate(STEPS, CURRENT);
    }

    private SaveMigrations() {
    }

    public static void register(IEventBus gameBus) {
        // Highest priority: the other systems' login handlers must see the data already migrated.
        gameBus.addListener(EventPriority.HIGHEST, SaveMigrations::onLoggedIn);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            upgrade(player);
        }
    }

    /** Runs the steps the player is missing and stamps them with the current version; returns how many ran. */
    public static int upgrade(ServerPlayer player) {
        return upgrade(player, STEPS, CURRENT);
    }

    /** As {@link #upgrade(ServerPlayer)}, with the steps and version given (for the tests). */
    public static int upgrade(ServerPlayer player, List<Step> steps, int current) {
        int saved = player.getData(ModAttachments.SAVE_VERSION.get());
        if (saved > current) {
            // Saved by a newer version of the mod: nothing here knows how to read it back, so it is left as it is.
            LOGGER.warn("The data of {} was saved by a newer version of the mod (save version {}, this one writes {})",
                    player.getGameProfile().getName(), saved, current);
            return 0;
        }
        List<Step> pending = pending(saved, steps);
        for (Step step : pending) {
            LOGGER.info("Updating the data of {} to save version {}: {}", player.getGameProfile().getName(), step.to(), step.what());
            step.migrate().accept(player);
            // Stamped after each step, so a step that throws is retried next time and the earlier ones are not repeated.
            player.setData(ModAttachments.SAVE_VERSION.get(), step.to());
        }
        if (saved < current && pending.isEmpty()) {
            player.setData(ModAttachments.SAVE_VERSION.get(), current);
        }
        return pending.size();
    }

    /** The steps a save of version {@code saved} still needs, in order. */
    public static List<Step> pending(int saved, List<Step> steps) {
        return steps.stream().filter(step -> step.to() > saved).toList();
    }

    /** Fails if the steps are not exactly one per version from the baseline to {@code current}, in order. */
    public static void validate(List<Step> steps, int current) {
        if (current < BASELINE) {
            throw new IllegalStateException("The current save version " + current + " is below the baseline " + BASELINE);
        }
        if (steps.size() != current - BASELINE) {
            throw new IllegalStateException("Save version " + current + " needs " + (current - BASELINE) + " steps, there are " + steps.size());
        }
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).to() != BASELINE + 1 + i) {
                throw new IllegalStateException("Step " + i + " should lead to save version " + (BASELINE + 1 + i) + ", not " + steps.get(i).to());
            }
        }
    }
}
