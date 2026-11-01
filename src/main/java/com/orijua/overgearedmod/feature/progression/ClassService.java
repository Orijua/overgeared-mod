package com.orijua.overgearedmod.feature.progression;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.skill.SkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * How a player gets a class: choosing a basic one once from the Profile tab, or being granted
 * one (usually advanced) by a quest. Either way the class skills are learned and bonuses applied.
 */
public final class ClassService {
    private ClassService() {
    }

    public static Optional<ResourceLocation> current(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_IDENTITY).playerClass();
    }

    /** @return false if the player already has a class, or the class doesn't exist or isn't a selectable basic class. */
    public static boolean choose(ServerPlayer player, ResourceLocation classId) {
        Optional<PlayerClassDefinition> definition = Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, classId);
        if (current(player).isPresent() || definition.isEmpty() || !definition.get().selectable()) {
            return false;
        }
        apply(player, classId, definition.get(), false);
        return true;
    }

    /** Sets the class regardless of how it is obtained, replacing the current one. Skills already learned are kept. */
    public static boolean grant(ServerPlayer player, ResourceLocation classId) {
        Optional<PlayerClassDefinition> definition = Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, classId);
        if (definition.isEmpty()) {
            return false;
        }
        apply(player, classId, definition.get(), true);
        return true;
    }

    /** @param granted true when a quest gives the class, false when the player picks it at the start */
    private static void apply(ServerPlayer player, ResourceLocation classId, PlayerClassDefinition definition, boolean granted) {
        String messageKey = granted ? "message.overgearedmod.class_granted" : "message.overgearedmod.class_chosen";
        PlayerIdentity identity = player.getData(ModAttachments.PLAYER_IDENTITY);
        player.setData(ModAttachments.PLAYER_IDENTITY, identity.withClass(classId));
        definition.skills().forEach(skill -> SkillService.learn(player, skill));
        ProgressionService.refreshEffects(player);

        player.displayClientMessage(Component.translatable(messageKey, definition.name()), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        NeoForge.EVENT_BUS.post(new ClassChangedEvent(player, classId, granted));
    }
}
