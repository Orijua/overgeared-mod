package com.orijua.overgearedmod.core.network;

import com.orijua.overgearedmod.feature.capsule.CapsuleConnectingPayload;
import com.orijua.overgearedmod.feature.economy.ClientTerminal;
import com.orijua.overgearedmod.feature.economy.TerminalBuyEmeraldsPayload;
import com.orijua.overgearedmod.feature.economy.TerminalBuyLicensePayload;
import com.orijua.overgearedmod.feature.economy.TerminalBuyOfferPayload;
import com.orijua.overgearedmod.feature.economy.TerminalPayDebtPayload;
import com.orijua.overgearedmod.feature.economy.TerminalStatePayload;
import com.orijua.overgearedmod.feature.economy.TerminalWorkPayload;
import com.orijua.overgearedmod.feature.economy.TradeSellPayload;
import com.orijua.overgearedmod.feature.fatigue.ClientFatigue;
import com.orijua.overgearedmod.feature.fatigue.FatigueDebugPayload;
import com.orijua.overgearedmod.feature.capsule.ClientCapsule;
import com.orijua.overgearedmod.feature.notification.ClientNotifications;
import net.minecraft.Util;
import com.orijua.overgearedmod.feature.notification.SystemNotificationPayload;
import com.orijua.overgearedmod.feature.npc.ApprenticeActionPayload;
import com.orijua.overgearedmod.feature.npc.NpcTradePayload;
import com.orijua.overgearedmod.feature.pagma.ReadPagmaBookPayload;
import com.orijua.overgearedmod.feature.winston.StartContestPayload;
import com.orijua.overgearedmod.feature.progression.AllocateStatPayload;
import com.orijua.overgearedmod.feature.progression.ChooseClassPayload;
import com.orijua.overgearedmod.feature.quest.NpcQuestPayload;
import com.orijua.overgearedmod.feature.quest.QuestActionPayload;
import com.orijua.overgearedmod.feature.quest.QuestsSeenPayload;
import com.orijua.overgearedmod.feature.skill.AssignSkillPayload;
import com.orijua.overgearedmod.feature.skill.UseSkillPayload;
import com.orijua.overgearedmod.feature.smelting.ManualSmeltPayload;
import com.orijua.overgearedmod.feature.smithing.ManualForgePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Custom packets. Mostly client → server intents: server → client data syncs itself through the
 * attachments in ModAttachments. The exception is one-off events with no state, like system windows.
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(UseSkillPayload.TYPE, UseSkillPayload.STREAM_CODEC, ServerPayloadHandler::useSkill);
        registrar.playToServer(AssignSkillPayload.TYPE, AssignSkillPayload.STREAM_CODEC, ServerPayloadHandler::assignSkill);
        registrar.playToServer(AllocateStatPayload.TYPE, AllocateStatPayload.STREAM_CODEC, ServerPayloadHandler::allocateStat);
        registrar.playToServer(ChooseClassPayload.TYPE, ChooseClassPayload.STREAM_CODEC, ServerPayloadHandler::chooseClass);
        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, ServerPayloadHandler::questAction);
        registrar.playToServer(QuestsSeenPayload.TYPE, QuestsSeenPayload.STREAM_CODEC, ServerPayloadHandler::questsSeen);
        registrar.playToServer(NpcQuestPayload.TYPE, NpcQuestPayload.STREAM_CODEC, ServerPayloadHandler::npcQuest);
        registrar.playToServer(NpcTradePayload.TYPE, NpcTradePayload.STREAM_CODEC, ServerPayloadHandler::npcTrade);
        registrar.playToServer(ManualForgePayload.TYPE, ManualForgePayload.STREAM_CODEC, ServerPayloadHandler::manualForge);
        registrar.playToServer(ManualSmeltPayload.TYPE, ManualSmeltPayload.STREAM_CODEC, ServerPayloadHandler::manualSmelt);
        registrar.playToServer(ReadPagmaBookPayload.TYPE, ReadPagmaBookPayload.STREAM_CODEC, ServerPayloadHandler::readPagmaBook);
        registrar.playToServer(ApprenticeActionPayload.TYPE, ApprenticeActionPayload.STREAM_CODEC, ServerPayloadHandler::apprenticeAction);
        registrar.playToServer(TradeSellPayload.TYPE, TradeSellPayload.STREAM_CODEC, ServerPayloadHandler::tradeSell);
        registrar.playToServer(StartContestPayload.TYPE, StartContestPayload.STREAM_CODEC, ServerPayloadHandler::startContest);
        registrar.playToServer(TerminalWorkPayload.TYPE, TerminalWorkPayload.STREAM_CODEC, ServerPayloadHandler::terminalWork);
        registrar.playToServer(TerminalBuyEmeraldsPayload.TYPE, TerminalBuyEmeraldsPayload.STREAM_CODEC, ServerPayloadHandler::terminalBuyEmeralds);
        registrar.playToServer(TerminalPayDebtPayload.TYPE, TerminalPayDebtPayload.STREAM_CODEC, ServerPayloadHandler::terminalPayDebt);
        registrar.playToServer(TerminalBuyOfferPayload.TYPE, TerminalBuyOfferPayload.STREAM_CODEC, ServerPayloadHandler::terminalBuyOffer);
        registrar.playToServer(TerminalBuyLicensePayload.TYPE, TerminalBuyLicensePayload.STREAM_CODEC, ServerPayloadHandler::terminalBuyLicense);
        // Like the one below, the handler only touches a client-state class with no client imports.
        registrar.playToClient(CapsuleConnectingPayload.TYPE, CapsuleConnectingPayload.STREAM_CODEC,
                (payload, context) -> ClientCapsule.connecting(payload.ticks(), Util.getMillis()));
        registrar.playToClient(FatigueDebugPayload.TYPE, FatigueDebugPayload.STREAM_CODEC,
                (payload, context) -> ClientFatigue.debug(payload));
        // The handler only touches ClientTerminal (no client classes), so this is safe on a dedicated server.
        registrar.playToClient(TerminalStatePayload.TYPE, TerminalStatePayload.STREAM_CODEC,
                (payload, context) -> ClientTerminal.update(payload.containerId(), payload.state()));
        // The handler only touches ClientNotifications (no client classes), so this is safe on a dedicated server.
        registrar.playToClient(SystemNotificationPayload.TYPE, SystemNotificationPayload.STREAM_CODEC,
                (payload, context) -> ClientNotifications.QUEUE.push(payload.notification()));
    }
}
