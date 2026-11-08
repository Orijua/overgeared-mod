package com.orijua.overgearedmod.feature.capsule;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When each player last dealt or took effective damage (server tick). The logout refuses to start in the 3 seconds
 * after it, and the fatigue doubles its drain in the 10 seconds after it. Only in memory.
 */
public final class CombatClock {
    private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();

    private CombatClock() {
    }

    public static void mark(ServerPlayer player, long tick) {
        LAST_COMBAT.put(player.getUUID(), tick);
    }

    public static OptionalLong last(ServerPlayer player) {
        Long last = LAST_COMBAT.get(player.getUUID());
        return last == null ? OptionalLong.empty() : OptionalLong.of(last);
    }

    public static void forget(ServerPlayer player) {
        LAST_COMBAT.remove(player.getUUID());
    }

    public static void forgetAll() {
        LAST_COMBAT.clear();
    }
}
