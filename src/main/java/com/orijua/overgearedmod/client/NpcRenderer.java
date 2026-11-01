package com.orijua.overgearedmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.npc.ApprenticeSmith;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** The mod's NPCs, drawn with vanilla's villager model and one texture each. */
final class NpcRenderer<T extends Mob> extends MobRenderer<T, VillagerModel<T>> {
    static final ResourceLocation BLACKSMITH = Overgearedmod.id("textures/entity/blacksmith.png");
    /** Provisional: vanilla's plain villager until the chief has its own art. */
    static final ResourceLocation VILLAGE_CHIEF = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    private static final Component ALERT = Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    /** How far above the name tag the "!" floats. */
    private static final double ALERT_RAISE = 0.3;

    private final ResourceLocation texture;

    NpcRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    /** An apprentice that stopped on his own (no materials, no room, unpaid...) shows a red "!" over his name. */
    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        if (entity instanceof ApprenticeSmith apprentice && apprentice.status().stopped()) {
            poseStack.pushPose();
            poseStack.translate(0.0, ALERT_RAISE, 0.0);
            renderNameTag(entity, ALERT, poseStack, buffer, packedLight, partialTick);
            poseStack.popPose();
        }
    }

    /** Same size as vanilla villagers. */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
