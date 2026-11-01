package com.orijua.overgearedmod.feature.winston.client;

import com.orijua.overgearedmod.core.client.SatisfyConfirmScreen;
import com.orijua.overgearedmod.feature.winston.ContestRules;
import com.orijua.overgearedmod.feature.winston.EupheminaNpc;
import com.orijua.overgearedmod.feature.winston.StartContestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Starting the contest against Euphemina: one piece, against the clock, so it asks first. */
public final class ContestScreen {
    private ContestScreen() {
    }

    public static void confirm(EupheminaNpc euphemina) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SatisfyConfirmScreen(minecraft.screen, Component.translatable("screen.overgearedmod.khans_contest"),
                Component.translatable("screen.overgearedmod.khans_contest.message", ContestRules.TIME_LIMIT_TICKS / 20 / 60),
                () -> PacketDistributor.sendToServer(new StartContestPayload(euphemina.getId()))));
    }
}
