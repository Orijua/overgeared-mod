package com.orijua.overgearedmod.npc;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.client.NpcQuestScreen;
import com.orijua.overgearedmod.rumor.Rumors;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.skill.SkillBookItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * A blacksmith NPC: right-click opens his quest window (with a Trade button for the shop).
 * Reuses vanilla's merchant logic (AbstractVillager) and the villager model. It stays where it is
 * placed (no wandering), never despawns and only takes damage from creative players or /kill.
 */
public class BlacksmithNpc extends AbstractVillager {
    /** Skill books on sale; the content validator checks these skills exist. */
    public static final List<ResourceLocation> SOLD_SKILLS = List.of(Overgearedmod.id("rending_slash"), Overgearedmod.id("iron_guard"));

    public BlacksmithNpc(EntityType<? extends BlacksmithNpc> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** The shop. Edit this list to change what the blacksmith sells or buys. */
    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(ModItems.ENHANCEMENT_STONE.get()), 16, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), new ItemStack(ModItems.RESPEC_SCROLL.get()), 4, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(ModItems.MITHRIL_INGOT.get(), 4), new ItemStack(Items.EMERALD, 3), 16, 0, 0.05f));
        for (ResourceLocation skill : SOLD_SKILLS) {
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 16),
                    SkillBookItem.forSkill(ModItems.SKILL_BOOK.get(), skill), 3, 0, 0.05f));
        }
        offers.add(eyeOfPallalian());
        offers.add(apprenticeContract());
    }

    /** More apprentice blacksmiths, once the smith's level allows them (the first contract he gives for free). */
    private static MerchantOffer apprenticeContract() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, ApprenticeRules.CONTRACT_PRICE), new ItemStack(ModItems.APPRENTICE_CONTRACT.get()),
                5, 0, 0.05f);
    }

    /** The most expensive appraisal item (canon, cap. 1), to know what Pagma's old book is. */
    private static MerchantOffer eyeOfPallalian() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 32), Optional.of(new ItemCost(Items.DIAMOND, 2)),
                new ItemStack(ModItems.EYE_OF_PALLALIAN.get()), 2, 0, 0.05f);
    }

    /** Smiths whose shop was stocked before these offers existed get them added. */
    private void stockNewOffers() {
        MerchantOffers offers = getOffers();
        if (offers.stream().noneMatch(offer -> offer.getResult().is(ModItems.EYE_OF_PALLALIAN.get()))) {
            offers.add(eyeOfPallalian());
        }
        if (offers.stream().noneMatch(offer -> offer.getResult().is(ModItems.APPRENTICE_CONTRACT.get()))) {
            offers.add(apprenticeContract());
        }
    }

    /** Right-click: his quest window, whose Trade button opens the shop ({@link #startTrading}). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            // Only runs on the client, so the client-only class is never loaded on a server.
            NpcQuestScreen.open(this, true);
        } else if (player instanceof ServerPlayer serverPlayer) {
            ApprenticeContractItem.introduce(serverPlayer);
            Rumors.hear(serverPlayer, getType());
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Opens the shop for {@code player}, unless someone is already trading with him. */
    public void startTrading(Player player) {
        if (isAlive() && !isTrading()) {
            stockNewOffers();
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
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
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
}
