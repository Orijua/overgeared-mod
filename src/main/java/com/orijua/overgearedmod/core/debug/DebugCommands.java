package com.orijua.overgearedmod.core.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.config.TradeBalance;
import com.orijua.overgearedmod.feature.economy.TerminalService;
import com.orijua.overgearedmod.feature.economy.TerminalState;
import com.orijua.overgearedmod.feature.economy.TradeKey;
import com.orijua.overgearedmod.feature.economy.TradeRules;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.economy.TradeService;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.character.Characters;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.AccountService;
import com.orijua.overgearedmod.feature.economy.Money;
import com.orijua.overgearedmod.feature.fatigue.FatigueService;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.capsule.CapsuleService;
import com.orijua.overgearedmod.feature.capsule.CapsuleText;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.economy.LicenseOffer;
import com.orijua.overgearedmod.feature.economy.LicenseShop;
import com.orijua.overgearedmod.core.config.CapsuleBalance;
import com.orijua.overgearedmod.feature.capsule.CapsuleRules;
import com.orijua.overgearedmod.core.config.FatigueBalance;
import com.orijua.overgearedmod.feature.capsule.WorldSeparation;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.mob.MobKilledEvent;
import com.orijua.overgearedmod.feature.npc.ApprenticeContractItem;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.NpcGrade;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.progression.ProgressionRules;
import com.orijua.overgearedmod.feature.progression.ProgressionService;
import com.orijua.overgearedmod.feature.progression.StatType;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestCompletedEvent;
import com.orijua.overgearedmod.feature.quest.QuestDefinition;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.smithing.ItemForgedEvent;
import com.orijua.overgearedmod.feature.world.Satisfy;
import com.orijua.overgearedmod.feature.world.SatisfyOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Test helpers under /satisfy debug (op only; the rest of /satisfy, like logout, is open to everyone), so titles and quests can be tried without grinding:
 * <ul>
 *   <li>{@code /satisfy debug simulate kill <entity> [amount]}: counts kills as if they happened.</li>
 *   <li>{@code /satisfy debug simulate forge <rating> [amount]}: counts forges of that rating.</li>
 *   <li>{@code /satisfy debug reset quests}: forgets accepted, completed and discovered quests.</li>
 *   <li>{@code /satisfy debug give xp <amount>}: awards XP; {@code /satisfy debug reset all} resets every system.</li>
 *   <li>{@code /satisfy debug set smithing <level>}: sets the smithing level (its experience).</li>
 *   <li>{@code /satisfy debug set level <level>}: sets the level; levels gained bring their stat points.</li>
 *   <li>{@code /satisfy debug set stat <stat> <value>}: sets the points spent in a stat.</li>
 *   <li>{@code /satisfy debug set insight <value>}: sets Insight.</li>
 *   <li>{@code /satisfy debug set won <amount>}: sets the Won balance of the account.</li>
 *   <li>{@code /satisfy debug set debt <amount>}: sets the debt (0 over an active debt settles it, without the announcement).</li>
 *   <li>{@code /satisfy debug account}: shows the Won and the debt of the account.</li>
 *   <li>{@code /satisfy debug debt}: shows the debt and its state; {@code clear} leaves it unassigned again, keeping the Won.</li>
 *   <li>{@code /satisfy debug trade}: lists the pressure and the price percentage of each trade key; {@code reset} clears them.</li>
 *   <li>{@code /satisfy debug spawn apprentice <grade> [level]}: hires an apprentice of that grade where the player stands.</li>
 *   <li>{@code /satisfy debug goto <place>}: to the surface in front of a structure (bairan, north_end_cave, ashur_manor,
 *   winston) or on dry ground at a canon place of the map (reidan, titan, behen_archipelago...); refused in the sea.</li>
 *   <li>{@code /satisfy debug quest complete <quest>}: marks it and the quests before it in its chain done; only the named
 *   quest, and only if it was not done yet, gives its item and XP rewards (not its class reward nor its loans; the ones before it give nothing).</li>
 * </ul>
 * Simulations post the same domain events as real play, so every listener (counters, titles,
 * hidden quests, notifications) reacts the same way. No mob dies and no item is created, except the quest rewards of {@code quest complete}.
 */
public final class DebugCommands {
    private static final SimpleCommandExceptionType NOT_A_MOB =
            new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.simulate_not_a_mob"));

    private DebugCommands() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DebugCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("satisfy")
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.literal("smithing")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000))
                                                .executes(ctx -> setSmithing(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                                .then(Commands.literal("level")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000))
                                                .executes(ctx -> setLevel(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                                .then(Commands.literal("stat")
                                        .then(Commands.argument("stat", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(StatType.values()).map(StatType::getSerializedName), builder))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100_000))
                                                        .executes(ctx -> setStat(ctx, IntegerArgumentType.getInteger(ctx, "value"))))))
                                .then(Commands.literal("debt")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                                    AccountService.setDebt(player, amount);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_set_debt", Money.format(amount)), false);
                                                    return 1;
                                                })))
                                .then(Commands.literal("fatigue")
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int percent = IntegerArgumentType.getInteger(ctx, "percent");
                                                    FatigueService.setEnergy(player, percent / 100.0);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_fatigue", percent), false);
                                                    return 1;
                                                })))
                                .then(Commands.literal("won")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                                    AccountService.setWon(player, amount);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_won", Money.format(amount)), false);
                                                    return 1;
                                                })))
                                .then(Commands.literal("insight")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10_000))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    player.setData(ModAttachments.INSIGHT, value);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_insight", value), false);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("terminal")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    TerminalState state = TerminalService.state(player, java.util.Optional.empty());
                                    Component stored = state.characterAvailable() ? Component.literal(Money.format(state.storedEmeralds()))
                                            : Component.translatable("message.overgearedmod.debug_terminal_no_character");
                                    Component workDay = state.workDay() == Long.MIN_VALUE
                                            ? Component.translatable("message.overgearedmod.debug_terminal_never") : Component.literal(Long.toString(state.workDay()));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_terminal",
                                            stored, state.order().id() + " x" + state.order().count(), workDay, state.today()), false);
                                    return 1;
                                })
                                .then(Commands.literal("work")
                                        .then(Commands.literal("reset")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    player.removeData(ModAttachments.WORK_DAY.get());
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_terminal_work_reset"), false);
                                                    return 1;
                                                })))
                                .then(Commands.literal("offer")
                                        .executes(DebugCommands::terminalOffer)
                                        .then(Commands.literal("reset")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    player.removeData(ModAttachments.OFFER_BOUGHT.get());
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_terminal_offer_reset"), false);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("account")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Account account = AccountService.get(player);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "message.overgearedmod.debug_account", Money.format(account.won()), Money.format(account.debt()),
                                            Component.translatable("message.overgearedmod.debug_debt_state." + account.debtState().getSerializedName())), false);
                                    return 1;
                                }))
                        .then(Commands.literal("debt")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Account account = AccountService.get(player);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_debt", Money.format(account.debt()),
                                            Component.translatable("message.overgearedmod.debug_debt_state." + account.debtState().getSerializedName())), false);
                                    return 1;
                                })
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            AccountService.clearDebt(player);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_debt_clear"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("trade")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    TradeBalance balance = TradeBalance.current();
                                    long today = TradeRules.day(player.serverLevel().getGameTime());
                                    TradeSaturation saturation = TradeService.saturation(player);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_trade"), false);
                                    saturation.entries().keySet().stream().sorted(Comparator.comparing(TradeKey::asString)).forEach(key -> {
                                        int pressure = saturation.pressure(key, today, balance);
                                        long percent = TradeRules.saturationPercent(pressure, balance);
                                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                                "message.overgearedmod.debug_trade_entry", key.asString(), pressure, percent), false);
                                    });
                                    return 1;
                                })
                                .then(Commands.literal("reset")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            player.removeData(ModAttachments.TRADE_SATURATION.get());
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_trade_reset"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("fatigue")
                                .then(Commands.literal("overlay")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            boolean on = FatigueService.toggleDebug(player);
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    on ? "message.overgearedmod.debug_fatigue_overlay_on" : "message.overgearedmod.debug_fatigue_overlay_off"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("give")
                                .then(Commands.literal("xp")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    if (!Satisfy.is(player.level())) {
                                                        ctx.getSource().sendFailure(Component.translatable(SatisfyOnly.WARNING_KEY));
                                                        return 0;
                                                    }
                                                    ProgressionService.awardXp(player, IntegerArgumentType.getInteger(ctx, "amount"));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("reset")
                                .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ModAttachments.resetAll(player);
                                            ProgressionService.set(player, PlayerStats.DEFAULT);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.reset"), false);
                                            return 1;
                                        }))
                                .then(Commands.literal("quests")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            player.setData(ModAttachments.PLAYER_QUESTS, PlayerQuests.EMPTY);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.quests_reset"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("quest")
                                .then(Commands.literal("complete")
                                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        Definitions.all(ctx.getSource().getLevel(), ModDatapackRegistries.QUEST).stream()
                                                                .map(Definitions.Entry::id), builder))
                                                .executes(DebugCommands::completeQuest))))
                        .then(Commands.literal("spawn")
                                .then(Commands.literal("apprentice")
                                        .then(Commands.argument("grade", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(NpcGrade.values()).map(grade -> grade.name().toLowerCase()), builder))
                                                .executes(ctx -> spawnApprentice(ctx, -1))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000))
                                                        .executes(ctx -> spawnApprentice(ctx, IntegerArgumentType.getInteger(ctx, "level")))))))
                        .then(Commands.literal("goto")
                                .then(Commands.argument("site", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SiteLanding.sites(), builder))
                                        .executes(DebugCommands::gotoSite)))
                        .then(Commands.literal("capsule")
                                .executes(ctx -> capsuleInfo(ctx.getSource()))
                                .then(Commands.literal("disconnect")
                                        .executes(ctx -> disconnectFromSatisfy(ctx.getSource().getPlayerOrException())))
                                .then(Commands.literal("tier")
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(CapsuleTier.values()).map(CapsuleTier::getSerializedName), builder))
                                                .executes(DebugCommands::setCapsuleTier)))
                                .then(Commands.literal("rested")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                    player.setData(ModAttachments.RESTED_SECONDS.get(), seconds);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_capsule_rested", CapsuleText.clock(seconds)), false);
                                                    return seconds;
                                                })))
                                .then(Commands.literal("cooldown")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                    player.setData(ModAttachments.RESTED_COOLDOWN.get(), seconds);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_capsule_cooldown", CapsuleText.clock(seconds)), false);
                                                    return seconds;
                                                }))))
                        .then(Commands.literal("simulate")
                                .then(Commands.literal("kill")
                                        .then(Commands.argument("entity", ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                                                .executes(ctx -> simulateKills(ctx, 1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                        .executes(ctx -> simulateKills(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))
                                .then(Commands.literal("forge")
                                        .then(Commands.argument("rating", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(ItemRating.values()).map(ItemRating::getSerializedName), builder))
                                                .executes(ctx -> simulateForges(ctx, 1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                        .executes(ctx -> simulateForges(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))));
    }

    /** The capsule tier of the player, what it gives (rested, session, recovery) and the lock of each license. */
    private static int capsuleInfo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CapsuleTier tier = player.getData(ModAttachments.CAPSULE_TIER.get());
        FatigueBalance fatigue = FatigueBalance.current();
        CapsuleBalance capsule = CapsuleBalance.current();
        StringBuilder locks = new StringBuilder();
        for (LicenseOffer offer : LicenseShop.offers(player, capsule)) {
            locks.append(locks.length() == 0 ? "" : ", ").append(offer.tier().getSerializedName()).append("=").append(offer.lock().name().toLowerCase());
        }
        source.sendSuccess(() -> Component.translatable("message.overgearedmod.debug_capsule", tier.tierName(),
                CapsuleText.clock(FatigueService.restedSeconds(player)),
                CapsuleText.clock(CapsuleRules.cooldownLeft(FatigueService.restedCooldown(player), capsule)),
                CapsuleText.minutes(capsule.sessionMinutes(tier, fatigue)),
                CapsuleText.minutes(capsule.recoveryMinutes(tier, fatigue)), locks.toString()), false);
        return 1;
    }

    private static int setCapsuleTier(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "tier");
        Optional<CapsuleTier> tier = Arrays.stream(CapsuleTier.values()).filter(t -> t.getSerializedName().equals(name)).findFirst();
        if (tier.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("message.overgearedmod.debug_capsule_bad_tier", name));
            return 0;
        }
        player.setData(ModAttachments.CAPSULE_TIER.get(), tier.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_capsule_tier", tier.get().tierName()), false);
        return 1;
    }

    private static int simulateKills(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EntityType<?> type = ResourceArgument.getEntityType(ctx, "entity").value();
        if (!(type.create(player.serverLevel()) instanceof LivingEntity)) {
            throw NOT_A_MOB.create();
        }
        for (int i = 0; i < amount; i++) {
            // A fresh, never-spawned entity per kill, as listeners may keep or inspect the victim.
            NeoForge.EVENT_BUS.post(new MobKilledEvent(player, (LivingEntity) type.create(player.serverLevel())));
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.simulated_kills", amount, type.getDescription()), false);
        return amount;
    }

    private static int simulateForges(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "rating");
        ItemRating rating = Arrays.stream(ItemRating.values()).filter(r -> r.getSerializedName().equals(name)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.simulate_bad_rating", name)).create());
        for (int i = 0; i < amount; i++) {
            NeoForge.EVENT_BUS.post(new ItemForgedEvent(player, new ItemStack(Items.IRON_SWORD), rating));
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.simulated_forges", amount, rating.displayName()), false);
        return amount;
    }

    /** Lists today's offer of the terminal: position, item, rating, price and whether this player bought it. */
    private static int terminalOffer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TerminalState state = TerminalService.state(player, java.util.Optional.empty());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_terminal_offer", state.today(), state.offer().size()), false);
        for (com.orijua.overgearedmod.feature.economy.OfferEntry entry : state.offer()) {
            ItemRating rating = entry.stack().get(com.orijua.overgearedmod.core.registry.ModDataComponents.ITEM_RATING);
            Component bought = Component.translatable(state.boughtToday().contains(entry.position())
                    ? "message.overgearedmod.debug_terminal_offer_yes" : "message.overgearedmod.debug_terminal_offer_no");
            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_terminal_offer_entry", entry.position(),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString(),
                    rating == null ? "?" : rating.getSerializedName(), Money.format(entry.price()), bought), false);
        }
        return 1;
    }

    private static int setSmithing(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ProgressionService.set(player, ProgressionService.get(player).withSmithingXp(ProgressionRules.smithingXpFor(level)));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_smithing", level,
                ProgressionService.effectiveSmithingLevel(player)), false);
        return level;
    }

    /** Raising the level also brings the stat points those levels give, as levelling up would. */
    private static int setLevel(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerStats stats = ProgressionService.get(player);
        int gained = Math.max(0, level - stats.level()) * Balance.current().statPointsPerLevel();
        ProgressionService.set(player, stats.withProgress(level, 0, stats.freePoints() + gained));
        player.setHealth(player.getMaxHealth());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_level", level, gained), false);
        return level;
    }

    private static int setStat(CommandContext<CommandSourceStack> ctx, int value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "stat");
        StatType stat = Arrays.stream(StatType.values()).filter(s -> s.getSerializedName().equals(name)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_bad_stat", name)).create());
        ProgressionService.set(player, ProgressionService.get(player).with(stat, value));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_stat", stat.displayName(), value), false);
        return value;
    }

    /**
     * Hires an apprentice of the given grade where the player stands, as a contract would (the smithing level still
     * limits how many), optionally with the smithing level he has learned ({@code level} &lt; 0: a new one's).
     */
    private static int spawnApprentice(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "grade");
        NpcGrade grade = Arrays.stream(NpcGrade.values()).filter(g -> g.name().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_bad_grade", name)).create());
        ApprenticeSmith apprentice = ApprenticeContractItem.hire(player, player.blockPosition())
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_apprentice_refused")).create());
        apprentice.setGrade(grade);
        if (level >= 0) {
            apprentice.setSmithingXp(ProgressionRules.smithingXpFor(level));
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_apprentice", apprentice.getDisplayName(),
                grade.name().toLowerCase(), apprentice.smithingLevel()), false);
        return 1;
    }

    /** Out of Satisfy and back beside the capsule, as the logout will (until it exists, the only way back). */
    private static int disconnectFromSatisfy(ServerPlayer player) {
        boolean was = CapsuleService.disconnect(player, CapsuleService.Reason.DEBUG);
        if (!was) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.capsule.in_real_world"), false);
        }
        return was ? 1 : 0;
    }

    /** To the surface in front of one of the world's places, in the Satisfy dimension, facing it. */
    private static int gotoSite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String site = StringArgumentType.getString(ctx, "site");
        ServerLevel satisfy = Satisfy.of(ctx.getSource().getServer())
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_goto_none", site)).create());
        SiteLanding.Landing found = SiteLanding.locate(satisfy, site);
        BlockPos landing = found.pos().orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable(
                switch (found.reason()) {
                    case UNKNOWN -> "message.overgearedmod.debug_goto_unknown";
                    case SEA -> "message.overgearedmod.debug_goto_sea";
                    default -> "message.overgearedmod.debug_goto_none";
                }, site)).create());
        if (!Characters.canCross(player, WorldSide.SATISFY)) {
            throw new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.capsule.character_unreadable")).create();
        }
        // Facing the place: the landing is in front of it.
        // A debug crossing between the real world and Satisfy is allowed (operators only).
        WorldSeparation.cross(player, () ->
                player.teleportTo(satisfy, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, found.yaw(), 0.0f));
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_goto", site,
                landing.getX(), landing.getY(), landing.getZ()), false);
        return 1;
    }

    /**
     * Marks the quest done along with every quest before it in its chain ({@code requires_quest}), and tells the
     * listeners as if each had been claimed. Hidden ones count as discovered. Gives the item and XP rewards ({@link QuestService#giveRewards})
     * of the named quest only, and only if it was not done yet (so repeating the command does not duplicate them); the earlier quests of the
     * chain give nothing. It does not grant the class reward (it changes the class without the confirmation the window asks for) nor touch loans.
     */
    private static int completeQuest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        List<ResourceLocation> chain = new ArrayList<>();
        Optional<ResourceLocation> next = Optional.of(id);
        while (next.isPresent() && !chain.contains(next.get())) {
            ResourceLocation current = next.get();
            QuestDefinition quest = Definitions.get(player.level(), ModDatapackRegistries.QUEST, current)
                    .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_bad_quest", current)).create());
            chain.addFirst(current);
            next = quest.requiresQuest();
        }
        PlayerQuests quests = QuestService.get(player);
        List<ResourceLocation> done = chain.stream().filter(quest -> !quests.isCompleted(quest)).toList();
        PlayerQuests updated = quests;
        for (ResourceLocation quest : done) {
            updated = updated.discover(quest).complete(quest);
        }
        player.setData(ModAttachments.PLAYER_QUESTS, updated);
        if (done.contains(id)) {
            QuestDefinition requested = Definitions.get(player.level(), ModDatapackRegistries.QUEST, id).orElseThrow();
            QuestService.giveRewards(player, requested);
        }
        done.forEach(quest -> NeoForge.EVENT_BUS.post(new QuestCompletedEvent(player, quest)));
        QuestService.checkDiscoveries(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_quest_complete", id.toString(), done.size()), false);
        return done.size();
    }
}
