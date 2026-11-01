package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.feature.npc.NpcRules;
import com.orijua.overgearedmod.feature.quest.client.NpcQuestScreen;
import com.orijua.overgearedmod.feature.rumor.Rumors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Khan, Winston's old blacksmith (canon, cap. 24): once excellent, he took to drink when the Mero Company took his
 * customers, and his smithy fell into ruin. He sends everyone away except Pagma's successor, who gets his quest
 * window (unless they let his time run out: then he no longer speaks to them). Like the other NPCs, he stays where he is placed, never despawns and only takes damage from creative players
 * or /kill.
 */
public class KhanNpc extends PathfinderMob {
    public KhanNpc(EntityType<? extends KhanNpc> type, Level level) {
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
            if (WinstonStory.heardByKhan(player)) {
                // Only runs on the client, so the client-only class is never loaded on a server.
                NpcQuestScreen.open(this, false);
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            WinstonStory.talkToKhan(serverPlayer, this);
            if (WinstonStory.heardByKhan(serverPlayer)) {
                Rumors.hear(serverPlayer, getType());
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Once he has died for the whole world (see {@link WinstonStory#killKhan}), he is buried as soon as his chunk loads. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 == 0 && WinstonStory.isKhanDead((ServerLevel) level())) {
            WinstonStory.bury(this);
        }
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
