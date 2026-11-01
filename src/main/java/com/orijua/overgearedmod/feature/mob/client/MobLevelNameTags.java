package com.orijua.overgearedmod.feature.mob.client;

import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Shows "[Lv 5] Zombie" above leveled mobs near the player (gold "★ Elite" for elites). */
public final class MobLevelNameTags {
    private static final double SHOW_DISTANCE = 16.0;

    private MobLevelNameTags() {
    }

    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || Minecraft.getInstance().player == null
                || mob.distanceToSqr(Minecraft.getInstance().player) > SHOW_DISTANCE * SHOW_DISTANCE) {
            return;
        }
        MobLevel level = mob.getData(ModAttachments.MOB_LEVEL);
        if (!level.isLeveled()) {
            return;
        }
        event.setContent(level.elite()
                ? Component.translatable("entity.overgearedmod.elite_level", level.level(), event.getContent()).withStyle(ChatFormatting.GOLD)
                : Component.translatable("entity.overgearedmod.level", level.level(), event.getContent()));
        event.setCanRender(TriState.TRUE);
    }
}
