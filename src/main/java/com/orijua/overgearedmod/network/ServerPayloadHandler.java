package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.skill.SkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Every client → server packet lands here and is forwarded to a service.
 * Payloads only carry data; services re-check every rule, since the client can't be trusted.
 */
final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    static void useSkill(UseSkillPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SkillService.use(player, payload.slot());
        }
    }

    static void assignSkill(AssignSkillPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SkillService.assign(player, payload.slot(), payload.skill());
        }
    }

    static void allocateStat(AllocateStatPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ProgressionService.allocatePoints(player, payload.stat(), payload.amount());
        }
    }

    static void chooseClass(ChooseClassPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ClassService.choose(player, payload.playerClass());
        }
    }

    static void questAction(QuestActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
                case ACCEPT -> QuestService.accept(player, payload.quest());
                case CLAIM -> QuestService.claim(player, payload.quest());
                case ABANDON -> QuestService.abandon(player, payload.quest());
            }
        }
    }
}
