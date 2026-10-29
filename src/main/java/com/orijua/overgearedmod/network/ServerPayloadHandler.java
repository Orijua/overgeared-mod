package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.progression.ProgressionService;
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
            SkillService.tryUseHeldSkill(player);
        }
    }

    static void allocateStat(AllocateStatPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ProgressionService.allocatePoint(player, payload.stat());
        }
    }
}
