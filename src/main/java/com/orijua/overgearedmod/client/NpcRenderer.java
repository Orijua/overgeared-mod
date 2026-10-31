package com.orijua.overgearedmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** The mod's NPCs, drawn with vanilla's villager model and one texture each. */
final class NpcRenderer<T extends Mob> extends MobRenderer<T, VillagerModel<T>> {
    static final ResourceLocation BLACKSMITH = Overgearedmod.id("textures/entity/blacksmith.png");
    /** Provisional: vanilla's plain villager until the chief has its own art. */
    static final ResourceLocation VILLAGE_CHIEF = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    private final ResourceLocation texture;

    NpcRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    /** Same size as vanilla villagers. */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
