package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.notification.ClientNotifications;
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
        // The handler only touches ClientNotifications (no client classes), so this is safe on a dedicated server.
        registrar.playToClient(SystemNotificationPayload.TYPE, SystemNotificationPayload.STREAM_CODEC,
                (payload, context) -> ClientNotifications.QUEUE.push(payload.notification()));
    }
}
