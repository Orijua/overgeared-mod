package com.orijua.overgearedmod.feature.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/** Why a capsule license cannot be bought right now; {@code OPEN} means it can. */
public enum LicenseLock {
    OPEN,
    /** The advanced license is closed while the starter debt is being paid. */
    DEBT_ACTIVE,
    /** The diamond license needs an advanced capsule (or better) first. */
    NEEDS_ADVANCED,
    /** The player already carries this license. */
    ALREADY_HAVE;

    public static final StreamCodec<ByteBuf, LicenseLock> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(LicenseLock::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), LicenseLock::ordinal);
}
