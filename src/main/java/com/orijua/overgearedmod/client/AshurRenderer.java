package com.orijua.overgearedmod.client;

import com.orijua.overgearedmod.pagma.AshurRetinue;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Earl Ashur and his knights: the player's model with what they wear (Ashur's crown and robe, the knights' iron).
 * Provisional: vanilla's player skins until they have their own art.
 */
final class AshurRenderer<T extends AshurRetinue> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    static final ResourceLocation ASHUR = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/kai.png");
    static final ResourceLocation KNIGHT = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final ResourceLocation texture;

    AshurRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.texture = texture;
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
