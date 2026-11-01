package com.orijua.overgearedmod.network;

import com.orijua.overgearedmod.Overgearedmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: read Pagma's rare book (confirmed in its window); the server checks the player carries it. */
public record ReadPagmaBookPayload() implements CustomPacketPayload {
    public static final ReadPagmaBookPayload INSTANCE = new ReadPagmaBookPayload();
    public static final Type<ReadPagmaBookPayload> TYPE = new Type<>(Overgearedmod.id("read_pagma_book"));
    public static final StreamCodec<ByteBuf, ReadPagmaBookPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ReadPagmaBookPayload> type() {
        return TYPE;
    }
}
