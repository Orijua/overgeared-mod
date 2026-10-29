package com.orijua.overgearedmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.network.UseSkillPayload;
import com.orijua.overgearedmod.registry.ModMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entry point: never loaded on a dedicated server, so it may use client classes
 * (Minecraft, screens, key mappings) freely.
 */
@Mod(value = Overgearedmod.MODID, dist = Dist.CLIENT)
public class OvergearedmodClient {
    private static final String KEY_CATEGORY = "key.categories.overgearedmod";

    public static final KeyMapping OPEN_STATUS = new KeyMapping("key.overgearedmod.status",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY);
    public static final KeyMapping USE_SKILL = new KeyMapping("key.overgearedmod.use_skill",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);

    public OvergearedmodClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(OvergearedmodClient::onRegisterKeys);
        modEventBus.addListener(OvergearedmodClient::onRegisterScreens);
        NeoForge.EVENT_BUS.addListener(OvergearedmodClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATUS);
        event.register(USE_SKILL);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SMITHING_ANVIL.get(), SmithingAnvilScreen::new);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        while (OPEN_STATUS.consumeClick()) {
            minecraft.setScreen(new StatusScreen());
        }
        while (USE_SKILL.consumeClick()) {
            PacketDistributor.sendToServer(UseSkillPayload.INSTANCE);
        }
    }
}
