package com.orijua.overgearedmod.feature.economy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.VarInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeModeTest {
    @Test
    void bothModesRoundTrip() {
        for (TradeMode mode : TradeMode.values()) {
            ByteBuf buffer = Unpooled.buffer();
            TradeMode.STREAM_CODEC.encode(buffer, mode);
            assertEquals(mode, TradeMode.STREAM_CODEC.decode(buffer));
        }
    }

    /** CLAMP (TradeMode.java): a bogus id never indexes outside the enum; the price fingerprint of the sale catches the rest. */
    @Test
    void bogusIdsClamp() {
        ByteBuf high = Unpooled.buffer();
        VarInt.write(high, 99);
        assertEquals(TradeMode.EXPORT, TradeMode.STREAM_CODEC.decode(high));
        ByteBuf low = Unpooled.buffer();
        VarInt.write(low, -1);
        assertEquals(TradeMode.SELL, TradeMode.STREAM_CODEC.decode(low));
    }
}
