package com.orijua.overgearedmod.feature.fatigue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FatigueDebugPayloadTest {
    @Test
    void thePayloadSurvivesTheNetwork() {
        FatigueDebugPayload sent = new FatigueDebugPayload(true, 0.42f, 2, 0.0008f, 315, 12, 4, true);
        ByteBuf buffer = Unpooled.buffer();
        FatigueDebugPayload.STREAM_CODEC.encode(buffer, sent);
        assertEquals(sent, FatigueDebugPayload.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes(), "nothing left over");
    }

    @Test
    void negativeValuesMeanNoneAndSurviveToo() {
        ByteBuf buffer = Unpooled.buffer();
        FatigueDebugPayload.STREAM_CODEC.encode(buffer, FatigueDebugPayload.HIDDEN);
        FatigueDebugPayload read = FatigueDebugPayload.STREAM_CODEC.decode(buffer);
        assertEquals(FatigueDebugPayload.HIDDEN, read);
        assertFalse(read.visible());
        assertEquals(-1, read.graceLeft());
    }

    @Test
    void theDisplayModesCycleAndComeBackToTheStart() {
        ClientFatigue.Display mode = ClientFatigue.Display.AUTO;
        mode = mode.next();
        assertEquals(ClientFatigue.Display.ALWAYS, mode);
        mode = mode.next();
        assertEquals(ClientFatigue.Display.HIDDEN, mode);
        assertEquals(ClientFatigue.Display.AUTO, mode.next());
    }
}
