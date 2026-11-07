package com.orijua.overgearedmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.orijua.overgearedmod.core.client.HudVisibility;
import com.orijua.overgearedmod.core.client.SatisfyTabs;
import com.orijua.overgearedmod.core.client.SatisfyTooltips;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModMenus;
import com.orijua.overgearedmod.feature.capsule.ClientCapsule;
import com.orijua.overgearedmod.feature.capsule.client.CapsuleConnectingLayer;
import com.orijua.overgearedmod.feature.mob.client.BoneLordRenderer;
import com.orijua.overgearedmod.feature.mob.client.MobLevelNameTags;
import com.orijua.overgearedmod.feature.notification.ClientNotifications;
import com.orijua.overgearedmod.feature.notification.client.SystemNotificationHudLayer;
import com.orijua.overgearedmod.feature.npc.client.NpcRenderer;
import com.orijua.overgearedmod.feature.pagma.client.AshurRenderer;
import com.orijua.overgearedmod.feature.progression.client.XpHudLayer;
import com.orijua.overgearedmod.feature.quest.client.QuestTrackerHudLayer;
import com.orijua.overgearedmod.feature.skill.PlayerSkills;
import com.orijua.overgearedmod.feature.skill.SkillService;
import com.orijua.overgearedmod.feature.skill.UseSkillPayload;
import com.orijua.overgearedmod.feature.skill.client.ManaHudLayer;
import com.orijua.overgearedmod.feature.skill.client.SkillSlotsHudLayer;
import com.orijua.overgearedmod.feature.smelting.client.SmelterScreen;
import com.orijua.overgearedmod.feature.smithing.client.SmithingAnvilScreen;
import com.orijua.overgearedmod.feature.winston.client.EupheminaRenderer;
import com.orijua.overgearedmod.feature.winston.client.MeroThugRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Client-only entry point: never loaded on a dedicated server, so it may use client classes freely. */
@Mod(value = Overgearedmod.MODID, dist = Dist.CLIENT)
public class OvergearedmodClient {
    private static final String KEY_CATEGORY = "key.categories.overgearedmod";

    public static final KeyMapping OPEN_STATUS = key("status", GLFW.GLFW_KEY_K);
    /** One key per skill slot; the HUD shows their bound letters. */
    public static final List<KeyMapping> SKILL_KEYS = List.of(
            key("skill_1", GLFW.GLFW_KEY_Z),
            key("skill_2", GLFW.GLFW_KEY_X),
            key("skill_3", GLFW.GLFW_KEY_C));
    /** Casts the held weapon's own skill (Bisection on a Legendary Fracaso); its HUD slot only shows while one is held. */
    public static final KeyMapping WEAPON_SKILL = key("weapon_skill", GLFW.GLFW_KEY_V);

    public OvergearedmodClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(OvergearedmodClient::onRegisterKeys);
        modEventBus.addListener(OvergearedmodClient::onRegisterScreens);
        modEventBus.addListener(OvergearedmodClient::onRegisterGuiLayers);
        modEventBus.addListener(OvergearedmodClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(OvergearedmodClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MobLevelNameTags::onRenderNameTag);
        NeoForge.EVENT_BUS.addListener(SatisfyTooltips::onTooltipColor);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientNotifications.QUEUE.clear());
        // A "Connecting…" wait must not outlive the world it started in.
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientCapsule.connecting(0, 0));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ClientCapsule.connecting(0, 0));
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.overgearedmod." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, defaultKey, KEY_CATEGORY);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATUS);
        SKILL_KEYS.forEach(event::register);
        event.register(WEAPON_SKILL);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SMITHING_ANVIL.get(), SmithingAnvilScreen::new);
        event.register(ModMenus.SMELTER.get(), SmelterScreen::new);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLACKSMITH.get(), context -> new NpcRenderer<>(context, NpcRenderer.BLACKSMITH));
        event.registerEntityRenderer(ModEntities.VILLAGE_CHIEF.get(), context -> new NpcRenderer<>(context, NpcRenderer.VILLAGE_CHIEF));
        event.registerEntityRenderer(ModEntities.BONE_LORD.get(), BoneLordRenderer::new);
        event.registerEntityRenderer(ModEntities.APPRENTICE_SMITH.get(), context -> new NpcRenderer<>(context, NpcRenderer.BLACKSMITH));
        event.registerEntityRenderer(ModEntities.EARL_ASHUR.get(), context -> new AshurRenderer<>(context, AshurRenderer.ASHUR));
        event.registerEntityRenderer(ModEntities.ASHUR_KNIGHT.get(), context -> new AshurRenderer<>(context, AshurRenderer.KNIGHT));
        event.registerEntityRenderer(ModEntities.KHAN.get(), context -> new NpcRenderer<>(context, NpcRenderer.KHAN));
        event.registerEntityRenderer(ModEntities.MERO_THUG.get(), MeroThugRenderer::new);
        event.registerEntityRenderer(ModEntities.EUPHEMINA.get(), EupheminaRenderer::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, Overgearedmod.id("mana"), satisfyOnly(new ManaHudLayer()));
        event.registerAbove(Overgearedmod.id("mana"), Overgearedmod.id("xp"), satisfyOnly(new XpHudLayer()));
        event.registerAbove(VanillaGuiLayers.HOTBAR, Overgearedmod.id("skill_slots"), satisfyOnly(new SkillSlotsHudLayer()));
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, Overgearedmod.id("quest_tracker"), satisfyOnly(new QuestTrackerHudLayer()));
        event.registerAbove(Overgearedmod.id("quest_tracker"), Overgearedmod.id("system_notification"), satisfyOnly(new SystemNotificationHudLayer()));
        event.registerAboveAll(Overgearedmod.id("capsule_connecting"), new CapsuleConnectingLayer());
    }

    /** Draws the layer only while the player is in Satisfy. */
    private static LayeredDraw.Layer satisfyOnly(LayeredDraw.Layer layer) {
        return (graphics, deltaTracker) -> {
            if (HudVisibility.inSatisfy(Minecraft.getInstance())) {
                layer.render(graphics, deltaTracker);
            }
        };
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (!HudVisibility.inSatisfy(minecraft)) {
            // In the real world the keys do nothing and say nothing: empty their clicks.
            while (OPEN_STATUS.consumeClick()) {
                // nothing
            }
            for (KeyMapping skillKey : SKILL_KEYS) {
                while (skillKey.consumeClick()) {
                    // nothing
                }
            }
            while (WEAPON_SKILL.consumeClick()) {
                // nothing
            }
            return;
        }
        while (OPEN_STATUS.consumeClick()) {
            minecraft.setScreen(SatisfyTabs.createScreen());
        }
        for (int slot = 0; slot < PlayerSkills.SLOT_COUNT; slot++) {
            while (SKILL_KEYS.get(slot).consumeClick()) {
                PacketDistributor.sendToServer(new UseSkillPayload(slot));
            }
        }
        while (WEAPON_SKILL.consumeClick()) {
            PacketDistributor.sendToServer(new UseSkillPayload(SkillService.WEAPON_SLOT));
        }
    }
}
