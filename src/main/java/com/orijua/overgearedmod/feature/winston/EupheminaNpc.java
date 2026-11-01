package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.npc.NpcRules;
import com.orijua.overgearedmod.feature.winston.client.ContestScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Euphemina, the smith the Mero Company hires for the production contest against Khan's side (canon, caps. 36-40). She
 * waits at her anvil on the stage of Winston's square while anyone has the contest quest; talking to her (with a
 * confirmation) starts it. She stays where she is placed, only takes damage from creative players or /kill, and leaves
 * once no one online is in the contest.
 */
public class EupheminaNpc extends PathfinderMob {
    public EupheminaNpc(EntityType<? extends EupheminaNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SMITHING_HAMMER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.0);
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
            if (KhanContest.canStart(player)) {
                // Only runs on the client, so the client-only class is never loaded on a server.
                ContestScreen.confirm(this);
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            KhanContest.talkToEuphemina(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 100 == 0 && !KhanContest.anyoneInTheContest(((ServerLevel) level()).getServer())) {
            discard(); // she comes back when someone takes the contest up again
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return NpcRules.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return false;
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
