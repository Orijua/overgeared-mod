package com.orijua.overgearedmod.core.network;

import com.orijua.overgearedmod.feature.npc.ApprenticeActionPayload;
import com.orijua.overgearedmod.feature.npc.ApprenticeAssignments;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.npc.NpcTradePayload;
import com.orijua.overgearedmod.feature.pagma.PagmaBook;
import com.orijua.overgearedmod.feature.pagma.ReadPagmaBookPayload;
import com.orijua.overgearedmod.feature.progression.AllocateStatPayload;
import com.orijua.overgearedmod.feature.progression.ChooseClassPayload;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.quest.NpcQuestPayload;
import com.orijua.overgearedmod.feature.quest.QuestActionPayload;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.QuestsSeenPayload;
import com.orijua.overgearedmod.feature.skill.AssignSkillPayload;
import com.orijua.overgearedmod.feature.skill.SkillService;
import com.orijua.overgearedmod.feature.skill.UseSkillPayload;
import com.orijua.overgearedmod.feature.smelting.ManualSmeltPayload;
import com.orijua.overgearedmod.feature.smelting.SmelterMenu;
import com.orijua.overgearedmod.feature.smithing.ManualForgePayload;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilMenu;
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

    /** Only for the smelter the player has open right now; the menu checks the timing, the smelter the rest. */
    static void manualSmelt(ManualSmeltPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof SmelterMenu smelter
                && smelter.containerId == payload.containerId()) {
            smelter.finishManual(player, payload.precision());
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
                case TRACK -> QuestService.toggleTracked(player, payload.quest());
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

    static void readPagmaBook(ReadPagmaBookPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PagmaBook.read(player);
        }
    }

    /** From an apprentice's window: only his master, standing near him. */
    static void apprenticeAction(ApprenticeActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level().getEntity(payload.apprentice()) instanceof ApprenticeSmith apprentice)
                || !apprentice.isOwnedBy(player) || apprentice.distanceToSqr(player) > ApprenticeSmith.WINDOW_REACH * ApprenticeSmith.WINDOW_REACH) {
            return;
        }
        switch (payload.action()) {
            case PAY_DAY -> apprentice.payDay(player);
            case CANCEL_ORDER -> apprentice.cancelOrder(player);
            case ASSIGN_MATERIALS -> ApprenticeAssignments.start(player, apprentice, ApprenticeSmith.Slot.MATERIALS);
            case ASSIGN_OUTPUT -> ApprenticeAssignments.start(player, apprentice, ApprenticeSmith.Slot.OUTPUT);
            case CLEAR_CONTAINERS -> apprentice.clearAssignments(player);
            case DISMISS -> apprentice.dismiss(player);
            case GIVE_HAMMER -> apprentice.giveHammer(player);
            case TAKE_HAMMER -> apprentice.takeHammer(player);
        }
    }
}
