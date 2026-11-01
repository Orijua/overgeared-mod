package com.orijua.overgearedmod.feature.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orijua.overgearedmod.feature.mob.BoneLord;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;

/** Placeholder look: the vanilla wither skeleton (model, armor layers and texture), drawn 1.2x like it. */
public final class BoneLordRenderer extends SkeletonRenderer<BoneLord> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

    public BoneLordRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.WITHER_SKELETON, ModelLayers.WITHER_SKELETON_INNER_ARMOR, ModelLayers.WITHER_SKELETON_OUTER_ARMOR);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneLord entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(BoneLord entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2f, 1.2f, 1.2f);
    }
}
