package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.economy.DebtService;
import com.orijua.overgearedmod.feature.economy.LicenseShop;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalShop;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * The messages of the terminal's services write every amount of 1000 or more with the thousands separator ({@code 12.345}).
 * Money.format has its own unit test; these check the services actually use it in the argument of each message
 * (the sale notification is checked in {@link TradeTests}).
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class AmountFormatTests {
    private AmountFormatTests() {
    }

    private static TranslatableContents translatable(GameTestHelper helper, Optional<Component> message, String key) {
        helper.assertTrue(message.isPresent() && message.get().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "the message is " + key + ": " + message);
        return (TranslatableContents) message.get().getContents();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theTerminalShopMessagesWriteTheSeparator(GameTestHelper helper) {
        TranslatableContents emeralds = translatable(helper, TerminalShop.message(
                new TerminalShop.Outcome(TerminalShop.Result.OK, 12_345L, 10, ItemStack.EMPTY)), "message.overgearedmod.terminal.bought_emeralds");
        helper.assertTrue("12.345".equals(emeralds.getArgs()[1]) && "10".equals(emeralds.getArgs()[0]), "emeralds: charged 12.345, 10 pieces: " + java.util.Arrays.toString(emeralds.getArgs()));
        TranslatableContents bigAmount = translatable(helper, TerminalShop.message(
                new TerminalShop.Outcome(TerminalShop.Result.OK, 12_345L, 1_234, ItemStack.EMPTY)), "message.overgearedmod.terminal.bought_emeralds");
        helper.assertTrue("1.234".equals(bigAmount.getArgs()[0]), "an amount of emeralds is an amount, not a count: " + java.util.Arrays.toString(bigAmount.getArgs()));
        TranslatableContents piece = translatable(helper, TerminalShop.message(
                new TerminalShop.Outcome(TerminalShop.Result.OK, 12_345L, 0, new ItemStack(Items.DIAMOND_SWORD))), "message.overgearedmod.terminal.bought_piece");
        helper.assertTrue("12.345".equals(piece.getArgs()[1]), "piece: charged 12.345: " + java.util.Arrays.toString(piece.getArgs()));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theLicenseMessageWritesTheSeparator(GameTestHelper helper) {
        TranslatableContents bought = translatable(helper, LicenseShop.message(
                new LicenseShop.Outcome(LicenseShop.Result.OK, CapsuleTier.DIAMOND, 12_345L)), "message.overgearedmod.terminal.license.bought");
        helper.assertTrue("12.345".equals(bought.getArgs()[1]), "the license says 12.345: " + java.util.Arrays.toString(bought.getArgs()));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theShiftMessagesWriteTheSeparator(GameTestHelper helper) {
        TranslatableContents plain = translatable(helper, TerminalService.message(
                new TerminalService.Outcome(TerminalService.Result.OK, 12_345L, 0L, false)), "message.overgearedmod.terminal.worked");
        helper.assertTrue("12.345".equals(plain.getArgs()[0]), "worked: 12.345: " + java.util.Arrays.toString(plain.getArgs()));
        TranslatableContents split = translatable(helper, TerminalService.message(
                new TerminalService.Outcome(TerminalService.Result.OK, 12_345L, 2_345L, false)), "message.overgearedmod.terminal.worked_debt");
        helper.assertTrue("12.345".equals(split.getArgs()[0]) && "2.345".equals(split.getArgs()[1]), "worked_debt: " + java.util.Arrays.toString(split.getArgs()));
        TranslatableContents settled = translatable(helper, TerminalService.message(
                new TerminalService.Outcome(TerminalService.Result.OK, 12_345L, 2_345L, true)), "message.overgearedmod.terminal.worked_settled");
        helper.assertTrue("12.345".equals(settled.getArgs()[0]), "worked_settled: " + java.util.Arrays.toString(settled.getArgs()));
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theDebtPaymentMessagesWriteTheSeparator(GameTestHelper helper) {
        TranslatableContents partial = translatable(helper, DebtService.message(
                new DebtService.Outcome(DebtService.Result.OK, 12_345L, 6_789L, false)), "message.overgearedmod.terminal.debt_paid");
        helper.assertTrue("12.345".equals(partial.getArgs()[0]) && "6.789".equals(partial.getArgs()[1]), "debt_paid: " + java.util.Arrays.toString(partial.getArgs()));
        TranslatableContents off = translatable(helper, DebtService.message(
                new DebtService.Outcome(DebtService.Result.OK, 12_345L, 0L, true)), "message.overgearedmod.terminal.debt_paid_off");
        helper.assertTrue("12.345".equals(off.getArgs()[0]), "debt_paid_off: " + java.util.Arrays.toString(off.getArgs()));
        helper.succeed();
    }
}
