package com.orijua.overgearedmod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Custom packets. Only client → server intents live here: server → client data
 * syncs itself through the attachments in ModAttachments.
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
    }
}
