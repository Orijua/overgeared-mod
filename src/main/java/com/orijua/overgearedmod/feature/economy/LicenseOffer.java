package com.orijua.overgearedmod.feature.economy;

import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A capsule license on sale at the terminal: its tier, its price in Won and what stops it from being bought. */
public record LicenseOffer(CapsuleTier tier, long price, LicenseLock lock) {
    public static final StreamCodec<ByteBuf, LicenseOffer> STREAM_CODEC = StreamCodec.composite(
            CapsuleTier.STREAM_CODEC, LicenseOffer::tier,
            ByteBufCodecs.VAR_LONG, LicenseOffer::price,
            LicenseLock.STREAM_CODEC, LicenseOffer::lock,
            LicenseOffer::new);
}
