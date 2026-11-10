package com.orijua.overgearedmod.feature.economy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LicenseLockTest {

    @Test
    void theStreamCodecRoundTripsEveryLock() {
        for (LicenseLock lock : LicenseLock.values()) {
            ByteBuf buf = Unpooled.buffer();
            LicenseLock.STREAM_CODEC.encode(buf, lock);
            assertEquals(lock, LicenseLock.STREAM_CODEC.decode(buf), lock.toString());
        }
    }

    @Test
    void openIsTheFirstLockBecauseItIsWhatAnOutOfRangeIdFallsBackTo() {
        assertEquals(0, LicenseLock.OPEN.ordinal());
    }

    @Test
    void anIdOutOfRangeDecodesAsOpenInsteadOfThrowing() {
        for (int id : new int[]{LicenseLock.values().length, LicenseLock.values().length + 1, 100, -1, Integer.MAX_VALUE}) {
            ByteBuf buf = Unpooled.buffer();
            net.minecraft.network.VarInt.write(buf, id);
            assertEquals(LicenseLock.OPEN, LicenseLock.STREAM_CODEC.decode(buf), "id " + id);
        }
    }
}
