package com.orijua.overgearedmod.feature.insight;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.quest.QuestDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A sword that sharpens its bearer's eye (canon, cap. 157, medium certainty: the Commander's Sword and the Ruler's
 * Sword both carry <i>Character Observation</i>, which raises Insight; the Ruler's also has <i>Talent Search</i>). The
 * speeds, the Insight bonuses and "only for recognised lords" are own design. Held in the main hand, observing starts from {@code observeTicks} instead of ten seconds (and Insight
 * shortens it further, see {@link InsightRules#observeTicks}), and NPC windows show
 * as much as {@code insightBonus} more Insight would. With Talent Search, using it reveals the talents around.
 */
public class InsightSwordItem extends SwordItem {
    private final int observeTicks;
    private final int insightBonus;
    private final boolean talentSearch;

    public InsightSwordItem(Tier tier, int observeTicks, int insightBonus, boolean talentSearch, Properties properties) {
        super(tier, properties);
        this.observeTicks = observeTicks;
        this.insightBonus = insightBonus;
        this.talentSearch = talentSearch;
    }

    public int observeTicks() {
        return observeTicks;
    }

    /** How much faster it makes observing than bare-handed, in percent: the same at any Insight (both shrink alike) until the 2 s floor. */
    public int observeSpeedPercent() {
        return (int) Math.round((1 - observeTicks / (double) InsightRules.OBSERVE_TICKS) * 100);
    }

    public int insightBonus() {
        return insightBonus;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sword = player.getItemInHand(hand);
        if (!talentSearch || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(sword);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TalentSearch.search(serverPlayer, sword);
            player.getCooldowns().addCooldown(this, TalentSearch.COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(sword, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overgearedmod.insight_sword.observation", observeSpeedPercent(), insightBonus)
                .withStyle(ChatFormatting.AQUA));
        if (talentSearch) {
            tooltip.add(Component.translatable("item.overgearedmod.insight_sword.talent_search").withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        ResourceLocation loan = stack.get(ModDataComponents.LOANED_FOR);
        if (loan != null && context.registries() != null) {
            Component quest = Definitions.get(context.registries(), ModDatapackRegistries.QUEST, loan).map(QuestDefinition::name)
                    .orElse(Component.literal(loan.toString()));
            tooltip.add(Component.translatable("item.overgearedmod.loaned", quest).withStyle(ChatFormatting.GOLD));
        }
    }
}
