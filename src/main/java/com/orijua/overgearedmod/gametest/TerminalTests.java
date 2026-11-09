package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.TerminalBalance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.capsule.StarterCapsule;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.Characters;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.TerminalConvertPayload;
import com.orijua.overgearedmod.feature.economy.TerminalMenu;
import com.orijua.overgearedmod.feature.economy.TerminalRules;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalWorkPayload;
import com.orijua.overgearedmod.feature.economy.WorkOrder;
import com.orijua.overgearedmod.feature.world.SatisfyOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * The account terminal and the phone (economy, step 3): converting the Satisfy emeralds of the stored character into Won,
 * the daily work, when the window stays open, and who gets the phone and when. The services are called directly with the
 * balance of the test; the window is built by hand because a fake player has no real connection to open one.
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class TerminalTests {
    private static final BlockPos LANDING = new BlockPos(1, 1, 1);
    private static final WorkOrder COAL_32 = new WorkOrder(ResourceLocation.withDefaultNamespace("coals"), true, 32);
    private static final TerminalBalance COAL_ONLY = new TerminalBalance(100, 1000L, List.of(COAL_32));

    private TerminalTests() {
    }

    private static Item emerald() {
        return ModItems.SATISFY_EMERALD.get();
    }

    private static void real(GameTestHelper helper, Runnable body) {
        CapsuleTests.outsideSatisfy(helper, body);
    }

    private static void satisfy(GameTestHelper helper, Runnable body) {
        CapsuleTests.inSatisfy(helper, helper.absolutePos(LANDING), body);
    }

    /** Takes the inventory as a snapshot (the stored Satisfy character) and leaves the player empty-handed. */
    private static CompoundTag storeInventory(ServerPlayer player) {
        CompoundTag snapshot = CharacterSnapshot.capture(player);
        player.getInventory().clearContent();
        return snapshot;
    }

    /** The real world's player with a stored Satisfy character holding 10 + 5 emeralds and a sword. Returns the snapshot. */
    private static CompoundTag withStoredCharacter(ServerPlayer player) {
        player.getInventory().setItem(0, new ItemStack(emerald(), 10));
        player.getInventory().setItem(1, new ItemStack(emerald(), 5));
        player.getInventory().setItem(2, new ItemStack(Items.DIAMOND_SWORD));
        CompoundTag snapshot = storeInventory(player);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, snapshot));
        return snapshot;
    }

    /** The player carries the real world's character (live REAL), with nothing stored. */
    private static void realCharacter(ServerPlayer player) {
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
    }

    private static int storedEmeralds(ServerPlayer player) {
        CharacterVault vault = player.getData(ModAttachments.CHARACTER_VAULT.get());
        return TerminalRules.countEmeralds(CharacterSnapshot.inventory(vault.stored().orElseThrow()));
    }

    private static String key(Optional<Component> message) {
        return message.map(component -> ((TranslatableContents) component.getContents()).getKey()).orElse("");
    }

    // ---- Converting ----

    @GameTest(template = TestSupport.EMPTY)
    public static void convertingTakesTheEmeraldsOfTheStoredCharacterAndPaysTheNet(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CompoundTag snapshot = withStoredCharacter(player);
        ListTag before = CharacterSnapshot.inventory(snapshot).copy();
        player.getInventory().setItem(0, new ItemStack(emerald(), 4));
        real(helper, () -> {
            TerminalService.Outcome outcome = TerminalService.convert(player, 12, 15, TerminalBalance.DEFAULT);
            helper.assertTrue(outcome.result() == TerminalService.Result.OK, "converted: " + outcome.result());
            helper.assertTrue(outcome.won() == 1140L && outcome.amount() == 12, "12 emeralds are worth 1140 Won: " + outcome);
            helper.assertTrue(AccountService.won(player) == 1140L, "the account got them: " + AccountService.won(player));
            helper.assertTrue(storedEmeralds(player) == 3, "3 are left in the stored character: " + storedEmeralds(player));
            CharacterVault vault = player.getData(ModAttachments.CHARACTER_VAULT.get());
            helper.assertTrue(vault.live().equals(Optional.of(WorldSide.REAL)), "the live world did not change");
            ListTag after = CharacterSnapshot.inventory(vault.stored().orElseThrow());
            helper.assertTrue(after.equals(TerminalRules.removeEmeralds(before, 12)), "the rest of the stored character is identical, only 12 emeralds fewer");
            helper.assertTrue(player.getInventory().countItem(emerald()) == 4, "the live inventory (4 emeralds of Satisfy) was not touched");
            helper.assertTrue(key(TerminalService.message(outcome)).equals("message.overgearedmod.terminal.converted"), "and says so");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void withoutAReadableSatisfyCharacterNothingIsConverted(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CompoundTag snapshot = withStoredCharacter(player);
        real(helper, () -> {
            CharacterVault live = CharacterVault.of(WorldSide.SATISFY, snapshot);
            player.setData(ModAttachments.CHARACTER_VAULT.get(), live);
            helper.assertTrue(TerminalService.convert(player, 1, 15, TerminalBalance.DEFAULT).result() == TerminalService.Result.NO_CHARACTER,
                    "the player carries Satisfy's things: the stored one is the real world's");
            helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(live), "the vault is the same");

            CharacterVault empty = new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty());
            player.setData(ModAttachments.CHARACTER_VAULT.get(), empty);
            helper.assertTrue(TerminalService.convert(player, 1, 0, TerminalBalance.DEFAULT).result() == TerminalService.Result.NO_CHARACTER, "nothing stored");

            CharacterVault unreadable = CharacterVault.of(WorldSide.REAL, new CompoundTag());
            player.setData(ModAttachments.CHARACTER_VAULT.get(), unreadable);
            helper.assertTrue(TerminalService.convert(player, 1, 0, TerminalBalance.DEFAULT).result() == TerminalService.Result.NO_CHARACTER, "an unreadable snapshot");
            helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(unreadable), "and it is left as it was");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aStaleOrInvalidAmountChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CompoundTag snapshot = withStoredCharacter(player);
        real(helper, () -> {
            CharacterVault vault = player.getData(ModAttachments.CHARACTER_VAULT.get());
            helper.assertTrue(TerminalService.convert(player, 5, 14, TerminalBalance.DEFAULT).result() == TerminalService.Result.STALE, "the player saw 14, there are 15");
            helper.assertTrue(TerminalService.convert(player, 0, 15, TerminalBalance.DEFAULT).result() == TerminalService.Result.INVALID_AMOUNT, "0");
            helper.assertTrue(TerminalService.convert(player, 16, 15, TerminalBalance.DEFAULT).result() == TerminalService.Result.INVALID_AMOUNT, "16 of 15");
            helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "the vault did not change");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
            helper.assertTrue(CharacterSnapshot.inventory(snapshot).size() == 3, "sanity: the snapshot had 3 entries");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void convertingAllLeavesNoEmeraldEntry(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStoredCharacter(player);
        real(helper, () -> {
            TerminalService.Outcome outcome = TerminalService.convert(player, 15, 15, TerminalBalance.DEFAULT);
            helper.assertTrue(outcome.result() == TerminalService.Result.OK, "converted: " + outcome.result());
            helper.assertTrue(AccountService.won(player) == 1425L, "15 emeralds: " + AccountService.won(player));
            ListTag after = CharacterSnapshot.inventory(player.getData(ModAttachments.CHARACTER_VAULT.get()).stored().orElseThrow());
            helper.assertTrue(after.size() == 1 && after.getCompound(0).getString("id").equals("minecraft:diamond_sword"),
                    "only the sword is left in the list: " + after);
        });
        helper.succeed();
    }

    /** BUG-063: an amount that pays no whole Won is refused before the vault is touched. */
    @GameTest(template = TestSupport.EMPTY)
    public static void anAmountThatPaysNothingIsRefusedAndChangesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStoredCharacter(player);
        real(helper, () -> {
            CharacterVault vault = player.getData(ModAttachments.CHARACTER_VAULT.get());
            TerminalBalance cheap = new TerminalBalance(1, 1000L, List.of(COAL_32));
            TerminalService.Outcome low = TerminalService.convert(player, 1, 15, cheap);
            helper.assertTrue(low.result() == TerminalService.Result.NOTHING_TO_PAY, "1 Won gross, 0 net: " + low.result());
            helper.assertTrue(key(TerminalService.message(low)).equals("message.overgearedmod.terminal.nothing_to_pay"), "says so");
            TerminalBalance free = new TerminalBalance(100, 1000L, List.of(COAL_32));
            helper.assertTrue(TerminalService.convert(player, 15, 15, free, 1.0).result() == TerminalService.Result.NOTHING_TO_PAY, "commission 100 %");
            helper.assertTrue(player.getData(ModAttachments.CHARACTER_VAULT.get()).equals(vault), "the vault is the same");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
            TerminalService.Outcome ok = TerminalService.convert(player, 15, 15, cheap);
            helper.assertTrue(ok.result() == TerminalService.Result.OK && ok.won() == 14L, "15 emeralds at 1 Won pay 14: " + ok);
        });
        helper.succeed();
    }

    /** BUG-065: the server pushes the new state when the game day changes while the window is open. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theOpenWindowGetsANewStateWhenTheDayChanges(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        realCharacter(player);
        real(helper, () -> {
            TerminalMenu menu = new TerminalMenu(5, player.getInventory(), Optional.empty());
            long today = TerminalService.today(player);
            menu.markSent(today);
            menu.broadcastChanges();
            helper.assertTrue(menu.sentDay() == today, "same day: nothing to push");
            menu.markSent(today - 1);
            menu.broadcastChanges();
            helper.assertTrue(menu.sentDay() == today, "the day changed: a new state was sent for day " + today + ", sent " + menu.sentDay());
        });
        helper.succeed();
    }

    // ---- In Satisfy ----

    @GameTest(template = TestSupport.EMPTY)
    public static void inSatisfyEverythingSaysNoSignal(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStoredCharacter(player);
        satisfy(helper, () -> {
            TerminalService.Outcome convert = TerminalService.convert(player, 1, 15, TerminalBalance.DEFAULT);
            helper.assertTrue(convert.result() == TerminalService.Result.IN_SATISFY, "convert: " + convert.result());
            helper.assertTrue(TerminalService.work(player, COAL_32, 5L, COAL_ONLY).result() == TerminalService.Result.IN_SATISFY, "work");
            helper.assertTrue(TerminalService.open(player, Optional.empty()) == TerminalService.Result.IN_SATISFY, "open");
            helper.assertTrue(key(TerminalService.message(convert)).equals("message.overgearedmod.terminal.no_signal"), "the message is No signal");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
        });
        helper.succeed();
    }

    /** BUG-062: in the real world but still carrying Satisfy's things (a failed settle), the terminal does nothing. */
    @GameTest(template = TestSupport.EMPTY)
    public static void carryingSatisfysThingsInTheRealWorldTheTerminalDoesNothing(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CompoundTag snapshot = CharacterSnapshot.capture(player);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.SATISFY, snapshot));
        real(helper, () -> {
            player.getInventory().setItem(0, new ItemStack(Items.COAL, 40));
            TerminalService.Outcome outcome = TerminalService.work(player, COAL_32, 5L, COAL_ONLY);
            helper.assertTrue(outcome.result() == TerminalService.Result.IN_SATISFY, "work: " + outcome.result());
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 40, "the coal is intact");
            helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "the day is not marked");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
            helper.assertTrue(TerminalService.open(player, Optional.empty()) == TerminalService.Result.IN_SATISFY, "open");
            helper.assertFalse(player.containerMenu instanceof TerminalMenu, "no window opened");
        });
        helper.succeed();
    }

    // ---- Work ----

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftPaysOncePerDay(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        realCharacter(player);
        real(helper, () -> {
            player.getInventory().setItem(0, new ItemStack(Items.COAL, 40));
            TerminalService.Outcome first = TerminalService.work(player, COAL_32, 5L, COAL_ONLY);
            helper.assertTrue(first.result() == TerminalService.Result.OK && first.won() == 1000L, "first shift: " + first);
            helper.assertTrue(AccountService.won(player) == 1000L, "paid 1000");
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 8, "32 handed over, 8 left");
            helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == 5L, "today is marked");
            helper.assertTrue(key(TerminalService.message(first)).equals("message.overgearedmod.terminal.worked"), "and says so");

            helper.assertTrue(TerminalService.work(player, COAL_32, 5L, COAL_ONLY).result() == TerminalService.Result.ALREADY_WORKED, "not twice the same day");
            helper.assertTrue(AccountService.won(player) == 1000L, "nothing more paid");

            player.getInventory().add(new ItemStack(Items.COAL, 32));
            helper.assertTrue(TerminalService.work(player, COAL_32, 6L, COAL_ONLY).result() == TerminalService.Result.OK, "the next day works again");
            helper.assertTrue(AccountService.won(player) == 2000L, "paid again");
            helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == 6L, "and marked");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftNeedsEveryItemAndCountsBothHandsButNotArmor(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        realCharacter(player);
        real(helper, () -> {
            player.getInventory().setItem(0, new ItemStack(Items.COAL, 31));
            helper.assertTrue(TerminalService.work(player, COAL_32, 5L, COAL_ONLY).result() == TerminalService.Result.NOT_ENOUGH, "31 of 32");
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 31, "nothing was taken");
            helper.assertTrue(AccountService.won(player) == 0L, "nothing was paid");
            helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "and the day is not marked");

            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.COAL, 16));
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COAL, 16));
            helper.assertTrue(TerminalService.work(player, COAL_32, 5L, COAL_ONLY).result() == TerminalService.Result.OK, "16 in the main hand and 16 in the offhand");
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 0, "all handed over");

            TerminalBalance pumpkin = new TerminalBalance(100, 1000L,
                    List.of(new WorkOrder(ResourceLocation.withDefaultNamespace("carved_pumpkin"), false, 1)));
            player.getInventory().clearContent();
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
            helper.assertTrue(TerminalService.work(player, pumpkin.workOrders().get(0), 7L, pumpkin).result() == TerminalService.Result.NOT_ENOUGH,
                    "what is worn does not count");
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN), "and is not taken");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aShiftForAnOrderThatIsNotTodaysIsStale(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        realCharacter(player);
        real(helper, () -> {
            player.getInventory().setItem(0, new ItemStack(Items.COAL, 40));
            WorkOrder wheat = new WorkOrder(ResourceLocation.withDefaultNamespace("wheat"), false, 64);
            helper.assertTrue(TerminalService.work(player, wheat, 5L, COAL_ONLY).result() == TerminalService.Result.STALE, "the player saw another order");
            helper.assertTrue(player.getInventory().countItem(Items.COAL) == 40 && AccountService.won(player) == 0L, "nothing changed");
            helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "and the day is not marked");
        });
        helper.succeed();
    }

    // ---- The window stays open ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theWindowStaysOpenNearTheTerminalOrWithAPhone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        BlockPos relative = new BlockPos(0, 1, 3);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, ModBlocks.ACCOUNT_TERMINAL.get());
        TerminalMenu menu = new TerminalMenu(1, player.getInventory(), Optional.of(pos));
        real(helper, () -> {
            Vec3 c = Vec3.atCenterOf(pos);
            helper.assertTrue(menu.stillValid(player), "valid at 3 blocks");
            player.moveTo(c.x, c.y, c.z - 8.0);
            helper.assertTrue(menu.stillValid(player), "valid at exactly 8 blocks: " + player.distanceToSqr(c));
            player.moveTo(c.x, c.y, c.z - 8.1);
            helper.assertFalse(menu.stillValid(player), "not valid at 8.1 blocks: " + player.distanceToSqr(c));
            player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
            helper.assertTrue(menu.stillValid(player), "valid at 8.1 blocks with a phone");
            player.getInventory().clearContent();
            helper.assertFalse(menu.stillValid(player), "not valid once the phone is gone");
            player.moveTo(c.x, c.y, c.z - 3.0);
            helper.assertTrue(menu.stillValid(player), "near again");
            player.setHealth(0.0f);
            helper.assertFalse(menu.stillValid(player), "not valid for a dead player");
            player.setHealth(player.getMaxHealth());
            helper.setBlock(relative, Blocks.AIR);
            helper.assertFalse(menu.stillValid(player), "not valid if the terminal is gone");
            helper.assertTrue(new TerminalMenu(2, player.getInventory(), Optional.empty()).stillValid(player) == false, "a phone window without the phone");
        });
        satisfy(helper, () -> {
            helper.setBlock(relative, ModBlocks.ACCOUNT_TERMINAL.get());
            player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
            helper.assertFalse(menu.stillValid(player), "never valid in Satisfy");
            player.getInventory().clearContent();
            helper.setBlock(relative, Blocks.AIR);
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPacketOnlyActsOnTheOpenWindowOfTheSender(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        withStoredCharacter(player);
        player.getInventory().setItem(0, new ItemStack(ModItems.PHONE.get()));
        TerminalBalance.withOverride(COAL_ONLY, () -> real(helper, () -> {
            TerminalConvertPayload convert = new TerminalConvertPayload(7, 1, 15);
            helper.assertTrue(TerminalService.convert(player, convert).result() == TerminalService.Result.NO_TERMINAL, "no window open");
            helper.assertTrue(TerminalService.work(player, new TerminalWorkPayload(7, COAL_32)).result() == TerminalService.Result.NO_TERMINAL, "nor for work");

            player.containerMenu = new TerminalMenu(7, player.getInventory(), Optional.empty());
            try {
                helper.assertTrue(TerminalService.convert(player, new TerminalConvertPayload(8, 1, 15)).result() == TerminalService.Result.NO_TERMINAL,
                        "another container id");
                helper.assertTrue(storedEmeralds(player) == 15, "nothing was taken");
                TerminalService.Outcome ok = TerminalService.convert(player, convert);
                helper.assertTrue(ok.result() == TerminalService.Result.OK && ok.won() == 95L, "the right id works: " + ok);
                player.getInventory().setItem(1, new ItemStack(Items.COAL, 40));
                TerminalService.Outcome worked = TerminalService.work(player, new TerminalWorkPayload(7, COAL_32));
                helper.assertTrue(worked.result() == TerminalService.Result.OK && worked.won() == 1000L, "work by payload: " + worked);
                helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == TerminalService.today(player), "today is marked");
                helper.assertTrue(TerminalService.today(player) == com.orijua.overgearedmod.feature.economy.TradeRules.day(helper.getLevel().getGameTime()), "today is the day of the game time");
            } finally {
                player.containerMenu = player.inventoryMenu;
            }
        }));
        helper.succeed();
    }

    // ---- The phone ----
    /** BUG-068 T1: in Satisfy the phone is never given, even carrying the real character. */
    @GameTest(template = TestSupport.EMPTY)
    public static void thePhoneIsNotGivenInSatisfyEvenCarryingTheRealCharacter(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.STARTER_CAPSULE_GIVEN.get(), true);
        CompoundTag snapshot = storeInventory(player);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, snapshot));
        satisfy(helper, () -> {
            helper.assertFalse(TerminalService.giveIfMissing(player), "not in Satisfy");
            helper.assertFalse(player.getData(ModAttachments.PHONE_GIVEN.get()), "and not marked");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 0, "no phone in the inventory");
        });
        helper.succeed();
    }

    // BUG-068 T6 (N4, not automated): the login and change-of-dimension listeners skip fake players, and the mock server player of
    // GameTestHelper cannot receive attachment sync packets (the server crashes ticking it). Check by hand: join with the capsule
    // given and the real character, and come back from Satisfy: one phone each time.

    @GameTest(template = TestSupport.EMPTY)
    public static void aWorkOrderMatchesItsItemsAndEveryDefaultOrderHasAnIcon(GameTestHelper helper) {
        helper.assertTrue(COAL_32.matches(new ItemStack(Items.COAL)), "coal is in #coals");
        helper.assertFalse(COAL_32.matches(new ItemStack(Items.STICK)), "a stick is not");
        helper.assertFalse(COAL_32.matches(ItemStack.EMPTY), "nor is nothing");
        for (WorkOrder order : TerminalBalance.DEFAULT.workOrders()) {
            helper.assertFalse(order.icon().is(Items.BARRIER), "the order " + order.asString() + " has a known item or tag");
        }
        helper.succeed();
    }


    @GameTest(template = TestSupport.EMPTY)
    public static void aNewPlayerGetsThePhoneOnlyAfterTheCapsuleAndOnlyOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.REAL), Optional.empty()));
        real(helper, () -> {
            helper.assertFalse(TerminalService.giveIfMissing(player), "no capsule given yet: no phone");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 0, "none in the inventory");
            helper.assertTrue(StarterCapsule.giveIfFirstTime(player), "the starter capsule is given");
            helper.assertTrue(TerminalService.giveIfMissing(player), "now the phone");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 1, "one phone");
            helper.assertTrue(player.getData(ModAttachments.PHONE_GIVEN.get()), "marked");
            helper.assertFalse(TerminalService.giveIfMissing(player), "not a second time");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 1, "still one");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void anExistingPlayerGetsThePhoneWhenTheyAreInTheRealWorld(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.STARTER_CAPSULE_GIVEN.get(), true);
        player.getInventory().setItem(0, new ItemStack(emerald(), 3));
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        CompoundTag satisfyCharacter = storeInventory(player);
        ListTag satisfyBefore = CharacterSnapshot.inventory(satisfyCharacter).copy();
        player.setData(ModAttachments.CHARACTER_VAULT.get(), new CharacterVault(Optional.of(WorldSide.SATISFY), Optional.empty()));
        satisfy(helper, () -> {
            helper.assertFalse(TerminalService.giveIfMissing(player), "not in Satisfy");
            helper.assertFalse(player.getData(ModAttachments.PHONE_GIVEN.get()), "and not marked");
        });
        real(helper, () -> {
            helper.assertFalse(TerminalService.giveIfMissing(player), "in the real world but carrying Satisfy's things (live SATISFY)");
            player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.EMPTY);
            helper.assertFalse(TerminalService.giveIfMissing(player), "an empty vault says nothing about the world");
            helper.assertFalse(player.getData(ModAttachments.PHONE_GIVEN.get()), "still not marked");
            player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, satisfyCharacter));
            helper.assertTrue(TerminalService.giveIfMissing(player), "carrying the real world's things: given");
            helper.assertTrue(CharacterSnapshot.inventory(player.getData(ModAttachments.CHARACTER_VAULT.get()).stored().orElseThrow()).equals(satisfyBefore),
                    "the stored Satisfy character is untouched");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 1, "one phone");
        });
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void thePhoneStaysWithTheRealCharacter(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.STARTER_CAPSULE_GIVEN.get(), true);
        player.getInventory().setItem(0, new ItemStack(emerald(), 3));
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        CompoundTag satisfyCharacter = storeInventory(player);
        ListTag satisfyBefore = CharacterSnapshot.inventory(satisfyCharacter).copy();
        player.setData(ModAttachments.CHARACTER_VAULT.get(), CharacterVault.of(WorldSide.REAL, satisfyCharacter));
        real(helper, () -> {
            helper.assertTrue(TerminalService.giveIfMissing(player), "given in the real world");
            helper.assertTrue(CharacterSnapshot.inventory(player.getData(ModAttachments.CHARACTER_VAULT.get()).stored().orElseThrow()).equals(satisfyBefore),
                    "the stored Satisfy character is the same as before");
            helper.assertFalse(inventoryHas(CharacterSnapshot.inventory(player.getData(ModAttachments.CHARACTER_VAULT.get()).stored().orElseThrow()), "overgearedmod:phone"),
                    "not in the stored Satisfy character");
        });
        satisfy(helper, () -> {
            helper.assertTrue(Characters.settle(player) == Characters.Result.SWAPPED, "entering Satisfy swaps");
            helper.assertTrue(player.getInventory().countItem(ModItems.PHONE.get()) == 0, "the Satisfy character has no phone");
            helper.assertTrue(inventoryHas(CharacterSnapshot.inventory(player.getData(ModAttachments.CHARACTER_VAULT.get()).stored().orElseThrow()), "overgearedmod:phone"),
                    "it waits with the real character");
        });
        helper.succeed();
    }

    private static boolean inventoryHas(ListTag inventory, String id) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getCompound(i).getString("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    // ---- Attachments and registry ----

    @GameTest(template = TestSupport.EMPTY)
    public static void theTerminalAttachmentsAreClearedByResetAll(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "never worked by default");
        player.setData(ModAttachments.WORK_DAY.get(), 5L);
        player.setData(ModAttachments.PHONE_GIVEN.get(), true);
        player.removeData(ModAttachments.WORK_DAY.get());
        helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "removing the data goes back to never");
        player.setData(ModAttachments.WORK_DAY.get(), 5L);
        ModAttachments.resetAll(player);
        helper.assertTrue(player.getData(ModAttachments.WORK_DAY.get()) == Long.MIN_VALUE, "reset all clears the work day");
        helper.assertFalse(player.getData(ModAttachments.PHONE_GIVEN.get()), "and the phone flag");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theEmeraldIdOfTheRulesIsTheRegisteredOne(GameTestHelper helper) {
        helper.assertTrue(TerminalRules.EMERALD_ID.equals(BuiltInRegistries.ITEM.getKey(ModItems.SATISFY_EMERALD.get()).toString()),
                "the id used to count emeralds in a snapshot is the item's");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theTerminalAndThePhoneAreNotInert(GameTestHelper helper) {
        helper.assertFalse(SatisfyOnly.isSatisfyItem(BuiltInRegistries.ITEM.getKey(ModItems.PHONE.get())), "the phone works in the real world");
        helper.assertFalse(SatisfyOnly.isSatisfyItem(BuiltInRegistries.ITEM.getKey(ModItems.ACCOUNT_TERMINAL.get())), "the terminal item too");
        helper.assertFalse(SatisfyOnly.blocksPlacement(BuiltInRegistries.BLOCK.getKey(ModBlocks.ACCOUNT_TERMINAL.get())), "and it can be placed");
        helper.succeed();
    }
}
