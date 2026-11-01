package com.orijua.overgearedmod.feature.winston.client;

import com.orijua.overgearedmod.feature.winston.EupheminaNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Euphemina: the slim player model with her smithing hammer. Provisional: a vanilla player skin until she has her own art. */
public final class EupheminaRenderer extends HumanoidMobRenderer<EupheminaNpc, HumanoidModel<EupheminaNpc>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    public EupheminaRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(EupheminaNpc entity) {
        return TEXTURE;
    }
}
