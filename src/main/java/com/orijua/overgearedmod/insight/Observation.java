package com.orijua.overgearedmod.insight;

import com.orijua.overgearedmod.notification.NotificationEvents;
import com.orijua.overgearedmod.notification.SystemNotification;
import com.orijua.overgearedmod.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observing to grow Insight: while a player sneaks looking at an NPC or a monster, a bar fills on the action bar; after
 * {@link InsightRules#OBSERVE_TICKS} of steady looking he gains a point. Each target teaches once per Minecraft day.
 * Crossing a detail threshold opens a system window saying what can be seen now.
 * <p>
 * The progress is kept in memory only: logging out or looking away starts it again.
 */
public final class Observation {
    private static final int BAR_SEGMENTS = 10;

    /** Who a player is studying, and for how long. */
    private record Study(UUID target, int ticks) {
    }

    private static final Map<UUID, Study> STUDYING = new HashMap<>();
    /** Targets each player already learned from, and on which day. */
    private static final Map<UUID, Map<UUID, Long>> LEARNED = new HashMap<>();

    private Observation() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Observation::onTick);
        gameBus.addListener(Observation::onLogout);
    }

    public static int insight(Player player) {
        return player.getData(ModAttachments.INSIGHT);
    }

    /** What NPC windows reveal by: Insight plus what a sword of observation in the main hand adds. */
    public static int effectiveInsight(Player player) {
        return insight(player) + (player.getMainHandItem().getItem() instanceof InsightSwordItem sword ? sword.insightBonus() : 0);
    }

    /** Ticks of steady looking for a point: shorter with a sword of observation in the main hand, and with more Insight. */
    public static int observeTicks(Player player) {
        int base = player.getMainHandItem().getItem() instanceof InsightSwordItem sword ? sword.observeTicks() : InsightRules.OBSERVE_TICKS;
        return InsightRules.observeTicks(base, insight(player));
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            observe(player, player.isShiftKeyDown() ? lookedAt(player) : null);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STUDYING.remove(event.getEntity().getUUID());
        LEARNED.remove(event.getEntity().getUUID());
    }

    /** The living creature (not a player) the player looks at, within reach and in sight, or null. */
    static LivingEntity lookedAt(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0f).scale(InsightRules.REACH);
        AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach), area,
                entity -> entity instanceof LivingEntity && !(entity instanceof Player) && entity.isAlive(), InsightRules.REACH * InsightRules.REACH);
        if (hit == null || !player.hasLineOfSight(hit.getEntity())) {
            return null;
        }
        return (LivingEntity) hit.getEntity();
    }

    /**
     * One tick of observing {@code target} (null: not observing). Returns whether this tick gave a point. Public so tests
     * can drive it without a real client.
     */
    public static boolean observe(ServerPlayer player, Entity target) {
        if (target == null) {
            STUDYING.remove(player.getUUID());
            return false;
        }
        long today = InsightRules.day(player.level().getGameTime());
        Map<UUID, Long> learned = LEARNED.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        learned.values().removeIf(day -> day != today);
        Study study = STUDYING.get(player.getUUID());
        int ticks = study != null && study.target().equals(target.getUUID()) ? study.ticks() + 1 : 1;
        STUDYING.put(player.getUUID(), new Study(target.getUUID(), ticks));
        if (learned.containsKey(target.getUUID())) {
            if (ticks == 1) {
                player.displayClientMessage(Component.translatable("message.overgearedmod.observed_today", target.getDisplayName()), true);
            }
            return false;
        }
        int needed = observeTicks(player);
        if (ticks < needed) {
            if (ticks % 10 == 1) {
                double fraction = ticks / (double) needed;
                player.displayClientMessage(Component.translatable("message.overgearedmod.observing", target.getDisplayName(),
                        bar(fraction)), true);
                // A soft tick that rises as the bar fills.
                player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.4f, 0.8f + (float) fraction);
            }
            return false;
        }
        learned.put(target.getUUID(), today);
        // Still studying the same target: looking on doesn't replace the "gained" message with "already observed today".
        STUDYING.put(player.getUUID(), new Study(target.getUUID(), ticks));
        gain(player, target);
        return true;
    }

    private static void gain(ServerPlayer player, Entity target) {
        int before = insight(player);
        int after = before + 1;
        player.setData(ModAttachments.INSIGHT, after);
        player.displayClientMessage(Component.translatable("message.overgearedmod.insight_gained", target.getDisplayName(), after), true);
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        InsightRules.Detail detail = InsightRules.detail(after);
        if (detail != InsightRules.detail(before)) {
            NotificationEvents.send(player, SystemNotification.Kind.INSIGHT, List.of(
                    Component.translatable("notification.overgearedmod.insight.body", after),
                    Component.translatable("notification.overgearedmod.insight." + detail.name().toLowerCase())));
        }
    }

    private static String bar(double fraction) {
        int filled = (int) Math.round(fraction * BAR_SEGMENTS);
        return "▮".repeat(filled) + "▯".repeat(BAR_SEGMENTS - filled);
    }

    /** For tests: forget what the player learned today. */
    public static void forget(Player player) {
        STUDYING.remove(player.getUUID());
        LEARNED.remove(player.getUUID());
    }
}
