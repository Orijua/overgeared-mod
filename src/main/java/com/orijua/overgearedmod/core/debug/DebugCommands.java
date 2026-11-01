package com.orijua.overgearedmod.core.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
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
import java.util.List;
import java.util.Optional;

/**
 * Test helpers under /satisfy (op only), so titles and quests can be tried without grinding:
 * <ul>
 *   <li>{@code /satisfy simulate kill <entity> [amount]}: counts kills as if they happened.</li>
 *   <li>{@code /satisfy simulate forge <rating> [amount]}: counts forges of that rating.</li>
 *   <li>{@code /satisfy quest reset}: forgets accepted, completed and discovered quests.</li>
 *   <li>{@code /satisfy debug smithing <level>}: sets the smithing level (its experience).</li>
 *   <li>{@code /satisfy debug level <level>}: sets the level; levels gained bring their stat points.</li>
 *   <li>{@code /satisfy debug stat <stat> <value>}: sets the points spent in a stat.</li>
 *   <li>{@code /satisfy debug insight <value>}: sets Insight.</li>
 *   <li>{@code /satisfy debug apprentice <grade> [level]}: hires an apprentice of that grade where the player stands.</li>
 *   <li>{@code /satisfy debug goto <bairan|north_end_cave|ashur_manor|winston>}: to the surface in front of that place.</li>
 *   <li>{@code /satisfy debug quest complete <quest>}: marks it and the quests before it in its chain done, without rewards.</li>
 * </ul>
 * Simulations post the same domain events as real play, so every listener (counters, titles,
 * hidden quests, notifications) reacts the same way. No mob dies and no item is created.
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
                .requires(source -> source.hasPermission(2))
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
                                                .executes(ctx -> simulateForges(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("debug")
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
                        .then(Commands.literal("insight")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 10_000))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            player.setData(ModAttachments.INSIGHT, value);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_insight", value), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("apprentice")
                                .then(Commands.argument("grade", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(NpcGrade.values()).map(grade -> grade.name().toLowerCase()), builder))
                                        .executes(ctx -> spawnApprentice(ctx, -1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000))
                                                .executes(ctx -> spawnApprentice(ctx, IntegerArgumentType.getInteger(ctx, "level"))))))
                        .then(Commands.literal("goto")
                                .then(Commands.argument("site", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SiteLanding.SITES, builder))
                                        .executes(DebugCommands::gotoSite)))
                        .then(Commands.literal("quest")
                                .then(Commands.literal("complete")
                                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        Definitions.all(ctx.getSource().getLevel(), ModDatapackRegistries.QUEST).stream()
                                                                .map(Definitions.Entry::id), builder))
                                                .executes(DebugCommands::completeQuest)))))
                .then(Commands.literal("quest")
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.setData(ModAttachments.PLAYER_QUESTS, PlayerQuests.EMPTY);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.quests_reset"), false);
                                    return 1;
                                }))));
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

    /** To the surface in front of one of the world's places, in the overworld, facing it. */
    private static int gotoSite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String site = StringArgumentType.getString(ctx, "site");
        ServerLevel overworld = ctx.getSource().getServer().overworld();
        BlockPos landing = SiteLanding.find(overworld, site)
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("message.overgearedmod.debug_goto_none", site)).create());
        // Facing north, towards the place: the landing is south of it.
        player.teleportTo(overworld, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, 180.0f, 0.0f);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_goto", site,
                landing.getX(), landing.getY(), landing.getZ()), false);
        return 1;
    }

    /**
     * Marks the quest done along with every quest before it in its chain ({@code requires_quest}), with no rewards, and
     * tells the listeners as if each had been claimed. Hidden ones count as discovered.
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
        done.forEach(quest -> NeoForge.EVENT_BUS.post(new QuestCompletedEvent(player, quest)));
        QuestService.checkDiscoveries(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.overgearedmod.debug_quest_complete", id.toString(), done.size()), false);
        return done.size();
    }
}
