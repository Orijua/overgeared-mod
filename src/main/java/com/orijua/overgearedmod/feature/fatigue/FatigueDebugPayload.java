package com.orijua.overgearedmod.feature.fatigue;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, once per second and only to an operator who asked for it (`/satisfy debug fatigue overlay`): what the
 * fatigue is doing, for the debug panel. {@code visible} false hides the panel.
 *
 * @param activity          0 rest, 1 normal, 2 combat
 * @param drainPerSecond    energy lost per second at the current activity (0 in the real world)
 * @param secondsToEmpty    seconds until empty at this activity, or -1 if nothing drains
 * @param graceLeft         seconds left of the grace period, or -1
 * @param secondsSinceCombat seconds since the last damage given or taken, or -1 if none this session
 * @param protectedNow      whether nothing can hurt the player right now
 */
public record FatigueDebugPayload(boolean visible, float energy, int activity, float drainPerSecond, int secondsToEmpty,
                                  int graceLeft, int secondsSinceCombat, boolean protectedNow) implements CustomPacketPayload {
    public static final Type<FatigueDebugPayload> TYPE = new Type<>(Overgearedmod.id("fatigue_debug"));
    public static final StreamCodec<ByteBuf, FatigueDebugPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.visible);
                buf.writeFloat(p.energy);
                buf.writeByte(p.activity);
                buf.writeFloat(p.drainPerSecond);
                buf.writeInt(p.secondsToEmpty);
                buf.writeInt(p.graceLeft);
                buf.writeInt(p.secondsSinceCombat);
                buf.writeBoolean(p.protectedNow);
            },
            buf -> new FatigueDebugPayload(buf.readBoolean(), buf.readFloat(), buf.readByte(), buf.readFloat(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readBoolean()));

    public static final FatigueDebugPayload HIDDEN = new FatigueDebugPayload(false, 0, 1, 0, -1, -1, -1, false);

    @Override
    public Type<FatigueDebugPayload> type() {
        return TYPE;
    }
}
