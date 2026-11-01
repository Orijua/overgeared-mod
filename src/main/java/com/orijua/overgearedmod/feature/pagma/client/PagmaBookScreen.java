package com.orijua.overgearedmod.feature.pagma.client;

import com.orijua.overgearedmod.core.client.SatisfyConfirmScreen;
import com.orijua.overgearedmod.feature.pagma.ReadPagmaBookPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Reading Pagma's book can't be undone (the level and stats go back to the start), so it asks first. */
public final class PagmaBookScreen {
    private PagmaBookScreen() {
    }

    public static void confirm() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SatisfyConfirmScreen(minecraft.screen, Component.translatable("screen.overgearedmod.read_pagma_book"),
                Component.translatable("screen.overgearedmod.read_pagma_book.message"),
                () -> PacketDistributor.sendToServer(ReadPagmaBookPayload.INSTANCE)));
    }
}
