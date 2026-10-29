package com.orijua.overgearedmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.npc.BlacksmithNpc;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The blacksmith NPC drawn with vanilla's villager model and its own texture (textures/entity/blacksmith.png). */
final class BlacksmithRenderer extends MobRenderer<BlacksmithNpc, VillagerModel<BlacksmithNpc>> {
    private static final ResourceLocation TEXTURE = Overgearedmod.id("textures/entity/blacksmith.png");

    BlacksmithRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(BlacksmithNpc entity) {
        return TEXTURE;
    }

    /** Same size as vanilla villagers. */
    @Override
    protected void scale(BlacksmithNpc entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
