package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.npc.BlacksmithNpc;
import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.progression.ProgressionService;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.skill.SkillService;
import com.orijua.overgearedmod.smithing.SmithingAnvilMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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

    /** Only for the anvil the player has open right now; the menu itself validates the result. */
    static void manualForge(ManualForgePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof SmithingAnvilMenu anvil
                && anvil.containerId == payload.containerId()) {
            anvil.finishManual(player, payload.precision());
        }
    }

    static void questsSeen(QuestsSeenPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            QuestService.markDiscoveriesSeen(player);
        }
    }

    /** From the Quests tab: quests with a giver are refused here (QuestService checks it). */
    static void questAction(QuestActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
                case ACCEPT -> QuestService.accept(player, payload.quest());
                case CLAIM -> QuestService.claim(player, payload.quest());
                case ABANDON -> QuestService.abandon(player, payload.quest());
            }
        }
    }

    /** From an NPC's quest window; QuestService checks the NPC gives that quest and stands within reach. */
    static void npcQuest(NpcQuestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Entity npc = player.level().getEntity(payload.npc());
            switch (payload.action()) {
                case ACCEPT -> QuestService.accept(player, payload.quest(), npc);
                case DELIVER -> QuestService.deliver(player, payload.quest(), npc);
                case CLAIM -> QuestService.claim(player, payload.quest(), npc);
            }
        }
    }

    static void npcTrade(NpcTradePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npc()) instanceof BlacksmithNpc smith
                && smith.distanceToSqr(player) <= QuestService.GIVER_REACH * QuestService.GIVER_REACH) {
            smith.startTrading(player);
        }
    }
}
