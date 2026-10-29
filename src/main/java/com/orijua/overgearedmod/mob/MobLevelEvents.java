package com.orijua.overgearedmod.mob;

import com.orijua.overgearedmod.Balance;
import com.orijua.overgearedmod.progression.ProgressionService;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Levels every hostile mob when it spawns, based on the nearest player. */
public final class MobLevelEvents {
    /** How far to look for the player a new mob scales with. */
    private static final double PLAYER_RANGE = 128.0;

    private MobLevelEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MobLevelEvents::onMobSpawn);
    }

    private static void onMobSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || MobLevelService.get(mob).isLeveled()) {
            return;
        }
        Player nearest = event.getLevel().getNearestPlayer(event.getX(), event.getY(), event.getZ(), PLAYER_RANGE, false);
        int playerLevel = nearest != null ? ProgressionService.get(nearest).level() : 0;
        Balance balance = Balance.current();
        MobLevelService.assign(mob, MobLevelRules.roll(balance, playerLevel, mob.getRandom()), balance);
    }
}
