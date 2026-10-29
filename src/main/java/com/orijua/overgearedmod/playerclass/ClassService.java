package com.orijua.overgearedmod.playerclass;

import com.orijua.overgearedmod.progression.PlayerIdentity;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.registry.Definitions;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;

/** Choosing a class: once per character (reset with /satisfy reset). Grants the class skills and bonuses. */
public final class ClassService {
    private ClassService() {
    }

    /** @return false if the player already has a class or the class doesn't exist. */
    public static boolean choose(ServerPlayer player, ResourceLocation classId) {
        PlayerIdentity identity = player.getData(ModAttachments.PLAYER_IDENTITY);
        Optional<PlayerClassDefinition> definition = Definitions.get(player.level(), ModDatapackRegistries.PLAYER_CLASS, classId);
        if (identity.playerClass().isPresent() || definition.isEmpty()) {
            return false;
        }
        player.setData(ModAttachments.PLAYER_IDENTITY, identity.withClass(classId));
        definition.get().skills().forEach(skill -> SkillService.learn(player, skill));
        ProgressionService.refreshEffects(player);

        player.displayClientMessage(Component.translatable("message.overgearedmod.class_chosen", definition.get().name()), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }
}
