package com.orijua.overgearedmod.feature.insight;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.NpcGrade;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Talent Search, the Ruler's Sword's skill (canon, cap. 157, medium certainty: it reveals hidden potential; the reach,
 * the cooldown and the named/semi-named apprentice grades are own design). Around the bearer, villagers
 * with a talent ({@link Talents}) and named apprentices (semi-named ones too with high Insight) glow for a while and are
 * listed in the chat. Each villager counts once per player (kept on the player, not on the sword) for the {@link #MILESTONE}
 * (the chief's quest asks for three).
 */
public final class TalentSearch {
    public static final ResourceLocation MILESTONE = Overgearedmod.id("talent_found");
    public static final int COOLDOWN_TICKS = 400;
    /** Insight for each new talent found (own design; canon, medium certainty: it raises Insight faster than observing). */
    public static final int INSIGHT_PER_TALENT = 2;
    /** Search radius around the bearer, in blocks (a sphere). */
    static final double RANGE = 24.0;
    private static final int GLOW_TICKS = 200;

    private TalentSearch() {
    }

    /** Searches around {@code player}; returns how many new talents this sword found. */
    public static int search(ServerPlayer player, ItemStack sword) {
        List<Component> lines = new ArrayList<>();
        List<UUID> known = new ArrayList<>(player.getData(ModAttachments.TALENTS_FOUND));
        int found = 0;
        for (Villager villager : nearby(player, Villager.class)) {
            Optional<String> talent = Talents.of(villager);
            if (talent.isEmpty()) {
                continue;
            }
            reveal(villager);
            lines.add(Component.translatable("message.overgearedmod.talent_villager", villager.getDisplayName(),
                    Component.translatable("talent.overgearedmod." + talent.get())));
            if (!known.contains(villager.getUUID())) {
                known.add(villager.getUUID());
                GoalTracker.milestone(player, MILESTONE);
                found++;
            }
        }
        boolean seesSemiNamed = InsightRules.detail(Observation.effectiveInsight(player)) == InsightRules.Detail.HIGH;
        for (ApprenticeSmith apprentice : nearby(player, ApprenticeSmith.class)) {
            NpcGrade grade = apprentice.grade();
            if (grade == NpcGrade.NAMED || (grade == NpcGrade.SEMI_NAMED && seesSemiNamed)) {
                reveal(apprentice);
                lines.add(Component.translatable("message.overgearedmod.talent_npc", apprentice.getDisplayName(),
                        Component.translatable("npc.overgearedmod.grade." + grade.name().toLowerCase())));
            }
        }
        player.setData(ModAttachments.TALENTS_FOUND, List.copyOf(known));
        if (found > 0) {
            Observation.raise(player, found * INSIGHT_PER_TALENT);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (lines.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.talent_none"), true);
            return 0;
        }
        player.sendSystemMessage(Component.translatable("message.overgearedmod.talent_search", lines.size()).withStyle(ChatFormatting.AQUA));
        lines.forEach(player::sendSystemMessage);
        return found;
    }

    /** The entities of {@code type} within {@link #RANGE} blocks of the player. */
    private static <T extends LivingEntity> List<T> nearby(ServerPlayer player, Class<T> type) {
        return player.serverLevel().getEntitiesOfClass(type, player.getBoundingBox().inflate(RANGE),
                entity -> player.distanceToSqr(entity) <= RANGE * RANGE);
    }

    private static void reveal(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
    }
}
