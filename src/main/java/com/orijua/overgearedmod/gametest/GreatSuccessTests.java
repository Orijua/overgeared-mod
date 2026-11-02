package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.feature.equipment.EquipmentService;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.smithing.ForgingExtras;
import com.orijua.overgearedmod.feature.smithing.GreatSuccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/** A great success at the anvil: the piece comes out one rating higher, within the recipe's range, and is celebrated. */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class GreatSuccessTests {
    /** Only Normal or Rare can come out, so a sure great success always ends at Rare. */
    private static final ForgingExtras NORMAL_TO_RARE = new ForgingExtras(Optional.empty(), ItemRating.NORMAL, ItemRating.RARE, List.of());

    private GreatSuccessTests() {
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aGreatSuccessRaisesTheRatingWithinItsRange(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        int greats = 0;
        for (int i = 0; i < 30; i++) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            if (EquipmentService.stampForged(player, sword, Optional.empty(), NORMAL_TO_RARE, 0, 0, 1.0)) {
                greats++;
            }
            helper.assertTrue(sword.get(ModDataComponents.ITEM_RATING) == ItemRating.RARE, "always Rare: rolled Rare, or raised from Normal");
        }
        helper.assertTrue(greats > 0, "Normal rolls were raised");
        boolean sawNormal = false;
        for (int i = 0; i < 30 && !sawNormal; i++) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            helper.assertFalse(EquipmentService.stampForged(player, sword, Optional.empty(), NORMAL_TO_RARE, 0, 0, 0), "no chance, no great success");
            sawNormal = sword.get(ModDataComponents.ITEM_RATING) == ItemRating.NORMAL;
        }
        helper.assertTrue(sawNormal, "without it, Normal still comes out");
        helper.succeed();
    }

    /** A roll already at the recipe's top has nowhere to go up: a sure great success still doesn't happen. */
    @GameTest(template = TestSupport.EMPTY)
    public static void noGreatSuccessWhenTheRollIsAlreadyTheTop(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ForgingExtras onlyNormal = new ForgingExtras(Optional.empty(), ItemRating.NORMAL, ItemRating.NORMAL, List.of());
        for (int i = 0; i < 30; i++) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            helper.assertFalse(EquipmentService.stampForged(player, sword, Optional.empty(), onlyNormal, 0, 0, 1.0), "nothing above Normal to raise to");
            helper.assertTrue(sword.get(ModDataComponents.ITEM_RATING) == ItemRating.NORMAL, "still Normal");
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aGreatSuccessIsCounted(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        int before = GoalTracker.milestones(player, GreatSuccess.MILESTONE);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ITEM_RATING, ItemRating.EPIC);
        GreatSuccess.celebrate(player, sword, helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(GoalTracker.milestones(player, GreatSuccess.MILESTONE) == before + 1, "counted for future titles");
        helper.succeed();
    }
}
