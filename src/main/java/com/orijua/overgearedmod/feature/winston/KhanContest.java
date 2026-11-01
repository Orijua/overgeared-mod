package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.config.Balance;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.notification.SystemNotification.Kind;
import com.orijua.overgearedmod.feature.pagma.AshurBetrayal;
import com.orijua.overgearedmod.feature.quest.QuestAcceptedEvent;
import com.orijua.overgearedmod.feature.quest.QuestCompletedEvent;
import com.orijua.overgearedmod.feature.quest.QuestDeadlines;
import com.orijua.overgearedmod.feature.quest.QuestDefinition;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.quest.goal.MilestoneGoal;
import com.orijua.overgearedmod.feature.smithing.ItemForgedEvent;
import com.orijua.overgearedmod.feature.world.Winston;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Optional;

/**
 * The item production contest in Winston's square (canon, caps. 36-40): after "The Blacksmith's Rage", the Mero Company
 * hires Euphemina to beat Khan's side, and Pagma's successor forges for Khan. Khan gives the quest
 * ({@link #KHANS_CONTEST}); accepting it brings Euphemina to her anvil on the stage. Talking to her starts the contest:
 * {@link ContestRules#TIME_LIMIT_TICKS} to forge one piece by hand at the stage, with materials from Khan. That piece is
 * scored against her hidden roll (see {@link ContestRules}) and a system window shows both.
 * <p>
 * Winning counts {@link #CONTEST_WON}; claiming the quest at Khan makes the player his disciple (the
 * {@link #KHANS_DISCIPLE} milestone, which earns the title) and rebuilds his smithy for the whole world. Losing, or
 * forging nothing in time, either fails the quest for good (the Mero keeps the smithy and Khan stops teaching) or allows
 * a rematch the next game day, as the server config's {@code contestLoss} says. In the canon Valmont has Grid arrested
 * before the result and the residents rise up (caps. 41-42); that comes in phases 10-11.
 */
public final class KhanContest {
    public static final ResourceLocation KHANS_CONTEST = Overgearedmod.id("khans_contest");
    /** The player beat Euphemina. */
    public static final ResourceLocation CONTEST_WON = Overgearedmod.id("contest_won");
    /** Khan took the player as his disciple (claimed the contest quest). */
    public static final ResourceLocation KHANS_DISCIPLE = Overgearedmod.id("khans_disciple");
    /** Forging this close to Euphemina is forging on the stage (the two anvils are 4 blocks apart). */
    public static final double STAGE_REACH = 6.0;
    /** Khan's materials for the piece: a Jaffa sword (2 ingots and a handle). */
    static final int CONTEST_INGOTS = 2;
    /** How far around a spot an Euphemina already there counts. */
    private static final double SPOT_RANGE = 4.0;
    private static final int TICK_EVERY = 20;
    /** Her hammer rings this often while someone competes. */
    private static final int HAMMER_EVERY = 40;

    private KhanContest() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(KhanContest::onAccepted);
        gameBus.addListener(KhanContest::onCompleted);
        gameBus.addListener(KhanContest::onForged);
        gameBus.addListener(KhanContest::onTick);
    }

    public static ContestState state(Player player) {
        return player.getData(ModAttachments.KHAN_CONTEST);
    }

    private static void setState(ServerPlayer player, ContestState state) {
        player.setData(ModAttachments.KHAN_CONTEST, state);
    }

    /** The quest clock's day (sleeping counts). Both sides. */
    static long today(Player player) {
        return QuestDeadlines.now(player) / QuestDefinition.DAY_TICKS;
    }

    /** Won, not yet claimed. From the synced counters: both sides. */
    static boolean hasWon(Player player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS).get(MilestoneGoal.counterKey(CONTEST_WON)) > 0;
    }

    /** On the quest, not competing now, not won yet, and not waiting for tomorrow's rematch. From synced data: both sides. */
    public static boolean canStart(Player player) {
        ContestState state = state(player);
        return QuestService.get(player).isActive(KHANS_CONTEST) && !state.running() && !hasWon(player)
                && ContestRules.canRematch(state.lostDay(), today(player));
    }

    /** Whether any player online has the contest quest: Euphemina waits on the stage only for them. */
    static boolean anyoneInTheContest(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().anyMatch(player -> QuestService.get(player).isActive(KHANS_CONTEST));
    }

    private static void onAccepted(QuestAcceptedEvent event) {
        if (event.quest().equals(KHANS_CONTEST)) {
            event.giver().ifPresent(khan -> {
                summonEuphemina(event.player().serverLevel(), khan);
                event.player().displayClientMessage(Component.translatable("message.overgearedmod.khans_contest_accepted"), false);
            });
        }
    }

    /**
     * Euphemina at her anvil on the stage of the Winston that {@code khan} lives in (next to him where there is no
     * Winston, as in a test), unless she is already there.
     */
    static EupheminaNpc summonEuphemina(ServerLevel level, Entity khan) {
        BlockPos spot = stageSpot(level, khan.blockPosition())
                .orElseGet(() -> AshurBetrayal.spotsAround(level, khan.blockPosition(), 1).stream().findFirst().orElse(khan.blockPosition()));
        List<EupheminaNpc> there = level.getEntitiesOfClass(EupheminaNpc.class, new AABB(spot).inflate(SPOT_RANGE), Entity::isAlive);
        if (!there.isEmpty()) {
            return there.getFirst();
        }
        EupheminaNpc euphemina = ModEntities.EUPHEMINA.get().create(level);
        if (euphemina != null) {
            euphemina.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 180, 0);
            euphemina.setCustomName(Component.translatable("entity.overgearedmod.euphemina"));
            level.addFreshEntity(euphemina);
        }
        return euphemina;
    }

    /** Where Euphemina stands on the stage of the Winston around {@code near}. */
    private static Optional<BlockPos> stageSpot(ServerLevel level, BlockPos near) {
        StructureStart winston = Winston.at(level, near);
        if (!winston.isValid()) {
            return Optional.empty();
        }
        return Winston.piece(winston, Winston.SQUARE)
                .map(square -> square.at(Winston.RIVAL_SPOT.getX(), Winston.RIVAL_SPOT.getY(), Winston.RIVAL_SPOT.getZ()));
    }

    /** What Euphemina says when it isn't time to start: the confirmation window comes from the client otherwise. */
    public static void talkToEuphemina(ServerPlayer player) {
        ContestState state = state(player);
        String line;
        if (!QuestService.get(player).isActive(KHANS_CONTEST)) {
            line = QuestService.get(player).isCompleted(KHANS_CONTEST) ? "euphemina_beaten" : "euphemina_ignores";
        } else if (state.running()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_running", clock(player, state)), true);
            return;
        } else if (hasWon(player)) {
            line = "contest_go_to_khan";
        } else if (!ContestRules.canRematch(state.lostDay(), today(player))) {
            line = "euphemina_tomorrow";
        } else {
            return;
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod." + line), true);
    }

    /** The player confirmed: the clock starts, Euphemina rolls her score and Khan hands over the materials. */
    public static boolean start(ServerPlayer player) {
        return start(player, ContestRules.rivalScore(player.getRandom().nextDouble()));
    }

    /** As {@link #start(ServerPlayer)}, with Euphemina's score fixed (for tests). */
    public static boolean start(ServerPlayer player, int rivalScore) {
        if (!canStart(player)) {
            return false;
        }
        setState(player, state(player).start(player.level().getGameTime(), rivalScore));
        give(player, new ItemStack(ModItems.JAFFA_INGOT.get(), CONTEST_INGOTS));
        give(player, new ItemStack(Items.STICK));
        player.displayClientMessage(Component.translatable("message.overgearedmod.contest_started", ContestRules.TIME_LIMIT_TICKS / 20 / 60), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.0f, 1.0f);
        return true;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** A piece taken out of an anvil on the stage during the contest is the one judged; only a manual forge counts. */
    private static void onForged(ItemForgedEvent event) {
        ServerPlayer player = event.player();
        if (!state(player).running() || !onStage(player)) {
            return;
        }
        if (!event.manual()) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_by_hand"), false);
            return;
        }
        int score = ContestRules.score(event.rating(), event.precision());
        Component you = Component.translatable("notification.overgearedmod.contest_result.you", event.rating().displayName(),
                Math.round(Math.clamp(event.precision(), 0, 1) * 100), score);
        judge(player, score, you);
    }

    private static boolean onStage(ServerPlayer player) {
        return !player.serverLevel().getEntitiesOfClass(EupheminaNpc.class, player.getBoundingBox().inflate(STAGE_REACH), Entity::isAlive).isEmpty();
    }

    /** Both scores side by side in a system window, then the verdict. */
    private static void judge(ServerPlayer player, int score, Component you) {
        int rival = state(player).rivalScore();
        boolean won = ContestRules.wins(score, rival);
        NotificationEvents.send(player, Kind.CONTEST_RESULT, List.of(you,
                Component.translatable("notification.overgearedmod.contest_result.rival", rival),
                Component.translatable(won ? "notification.overgearedmod.contest_result.won" : "notification.overgearedmod.contest_result.lost")));
        if (won) {
            setState(player, ContestState.NONE);
            GoalTracker.milestone(player, CONTEST_WON);
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_won"), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            lose(player);
        }
    }

    /** Lost: for good or until tomorrow, as the config and the kind of world say. */
    private static void lose(ServerPlayer player) {
        boolean forGood = ContestRules.failsForGood(Balance.current().contestLoss(), WinstonStory.isWholeWorld(player.server));
        if (forGood) {
            setState(player, ContestState.NONE);
            QuestDeadlines.fail(player, KHANS_CONTEST);
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_lost_for_good"), false);
        } else {
            setState(player, state(player).lost(today(player)));
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_lost_rematch"), false);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % TICK_EVERY == 0) {
            tick(player);
        }
    }

    /** Once a second while competing: the clock on the action bar, Euphemina's hammer, and the end when time is up. */
    public static void tick(ServerPlayer player) {
        ContestState state = state(player);
        if (!state.running()) {
            return;
        }
        if (!QuestService.get(player).isActive(KHANS_CONTEST)) {
            setState(player, ContestState.NONE); // abandoned
            return;
        }
        if (ContestRules.ticksLeft(state.startedAt(), player.level().getGameTime()) <= 0) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.contest_time_up"), false);
            judge(player, 0, Component.translatable("notification.overgearedmod.contest_result.no_piece"));
            return;
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.contest_clock", clock(player, state)), true);
        if (player.tickCount % HAMMER_EVERY == 0) {
            player.serverLevel().getEntitiesOfClass(EupheminaNpc.class, player.getBoundingBox().inflate(16), Entity::isAlive)
                    .forEach(euphemina -> player.level().playSound(null, euphemina.blockPosition(), SoundEvents.ANVIL_USE,
                            SoundSource.NEUTRAL, 0.6f, 1.1f));
        }
    }

    /** "4:05": the time left, in minutes and seconds. */
    private static String clock(Player player, ContestState state) {
        long seconds = ContestRules.ticksLeft(state.startedAt(), player.level().getGameTime()) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /** Claimed at Khan: the player becomes his disciple, and his smithy is rebuilt. */
    private static void onCompleted(QuestCompletedEvent event) {
        if (event.quest().equals(KHANS_CONTEST)) {
            ServerPlayer player = event.player();
            GoalTracker.milestone(player, KHANS_DISCIPLE);
            restoreSmithy(player.serverLevel(), player.blockPosition());
            player.displayClientMessage(Component.translatable("message.overgearedmod.khans_contest_done"), false);
        }
    }

    /** Rebuilds Khan's smithy in the Winston around {@code near}, once for the whole world. */
    static void restoreSmithy(ServerLevel level, BlockPos near) {
        WinstonData data = WinstonData.get(level.getServer().overworld());
        if (data.smithyRestored()) {
            return;
        }
        StructureStart winston = Winston.at(level, near);
        if (!winston.isValid()) {
            return;
        }
        Winston.piece(winston, Winston.KHAN_SMITHY).ifPresent(smithy -> {
            if (smithy.place(level, Winston.KHAN_SMITHY_RESTORED)) {
                data.setSmithyRestored();
                level.playSound(null, near, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        });
    }
}
