package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.client.NpcQuestScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The village chief of Bairan (own design, not canon): gives the quests that aren't smithing. Right-click opens the
 * chief's quest window. Like the blacksmith, it stays where it is placed, never despawns and only takes damage from
 * creative players or /kill.
 */
public class VillageChiefNpc extends PathfinderMob {
    public VillageChiefNpc(EntityType<? extends VillageChiefNpc> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            // Only runs on the client, so the client-only class is never loaded on a server.
            NpcQuestScreen.open(this, false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return NpcRules.isInvulnerableTo(source);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
