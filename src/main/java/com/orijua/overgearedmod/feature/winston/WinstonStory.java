package com.orijua.overgearedmod.feature.winston;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.feature.pagma.AshurBetrayal;
import com.orijua.overgearedmod.feature.pagma.PagmaStory;
import com.orijua.overgearedmod.feature.quest.QuestAcceptedEvent;
import com.orijua.overgearedmod.feature.quest.QuestCompletedEvent;
import com.orijua.overgearedmod.feature.quest.QuestFailedEvent;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.quest.goal.MilestoneGoal;
import com.orijua.overgearedmod.feature.world.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Khan's story in Winston (canon, caps. 24 and 36-42). Only Pagma's successor can help him: Grid gets Khan's quest
 * because he inherited Pagma's humanitarian ideals. Smith points the successor to Winston with a map; talking to Khan
 * then counts the {@link #MET_KHAN} milestone, which reveals his quest. Before that, Khan is just a drunk who sends
 * everyone away.
 * <p>
 * "The Blacksmith's Rage" [B] ({@link #KHANS_RAGE}, cap. 24): accepting it creates the skill of the same name and
 * brings {@link #THUGS} Mero thugs into the smithy; the last one drops the transfer contract, and tearing it up
 * completes the quest. It has 7 days. If the time runs out Khan dies, as in the canon: in a single-player world for
 * good (a grave is left in his smithy), in a shared world only for the player who failed him (he no longer speaks to
 * them), so nobody else loses him. The production contest that follows is {@link KhanContest}.
 */
public final class WinstonStory {
    /** Smith told Pagma's successor about Khan and gave them the map to Winston. */
    public static final ResourceLocation WINSTON_MAP = Overgearedmod.id("winston_map");
    /** Pagma's successor talked to Khan in his smithy. */
    public static final ResourceLocation MET_KHAN = Overgearedmod.id("met_khan");
    /** The smithy transfer contract was torn up. */
    public static final ResourceLocation CONTRACT_DESTROYED = Overgearedmod.id("contract_destroyed");
    public static final ResourceLocation KHANS_RAGE = Overgearedmod.id("khans_rage");
    /** Canon (cap. 24): the Mero Company's minions in the smithy. */
    public static final int THUGS = 5;
    /** How many lines Khan has for those he sends away (message.overgearedmod.khan_drunk.N). */
    static final int DRUNK_LINES = 3;

    private WinstonStory() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(WinstonStory::onAccepted);
        gameBus.addListener(WinstonStory::onFailed);
        gameBus.addListener(WinstonStory::onCompleted);
    }

    /**
     * Whether the player inherited Pagma's legacy, the condition of Khan's quests in the canon: they read the book or
     * completed "Pagma's Legacy" (which reading completes; a debug command can complete it alone). Not the class, so a
     * later class change doesn't undo it. Read from the synced counters and quests: both sides.
     */
    public static boolean isPagmasSuccessor(Player player) {
        return player.getData(ModAttachments.PLAYER_COUNTERS).get(MilestoneGoal.counterKey(PagmaStory.READ)) > 0
                || QuestService.get(player).isCompleted(PagmaStory.LEGACY);
    }

    /**
     * The player let Khan's time run out (he is dead for them), or lost his contest for good (the Mero keeps his smithy
     * and he no longer teaches them). From the synced quests: both sides.
     */
    public static boolean hasLostKhan(Player player) {
        return QuestService.get(player).isFailed(KHANS_RAGE) || QuestService.get(player).isFailed(KhanContest.KHANS_CONTEST);
    }

    /** Whether Khan opens his quest window to this player. */
    public static boolean heardByKhan(Player player) {
        return isPagmasSuccessor(player) && !hasLostKhan(player);
    }

    /** Talking to Smith: the first time as Pagma's successor, he tells of his old friend in Winston and gives the map. */
    public static void talkToSmith(ServerPlayer player) {
        if (!isPagmasSuccessor(player) || GoalTracker.milestones(player, WINSTON_MAP) > 0) {
            return;
        }
        Optional<ItemStack> map = winstonMap(player);
        if (map.isEmpty()) {
            return; // a world without Winston
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.smith_tells_of_khan"), false);
        GoalTracker.milestone(player, WINSTON_MAP);
        if (!player.getInventory().add(map.get())) {
            player.drop(map.get(), false);
        }
    }

    /**
     * Talking to Khan: the successor is heard (the milestone, every time), and thanked once his smithy is saved; anyone
     * else is sent away, and whoever failed him gets silence. If the thugs are gone while the quest still runs (and
     * the contract is nowhere), they come back; so does Euphemina while the contest quest runs. Once the contest is
     * claimed, his smithy is rebuilt if it wasn't yet (e.g. after a debug command completed the quest elsewhere).
     */
    public static void talkToKhan(ServerPlayer player, Entity khan) {
        if (hasLostKhan(player)) {
            String line = QuestService.get(player).isFailed(KHANS_RAGE) ? "khan_lost" : "khan_contest_lost";
            player.displayClientMessage(Component.translatable("message.overgearedmod." + line), true);
        } else if (isPagmasSuccessor(player)) {
            GoalTracker.milestone(player, MET_KHAN);
            if (QuestService.get(player).isCompleted(KhanContest.KHANS_CONTEST)) {
                KhanContest.restoreSmithy(player.serverLevel(), khan.blockPosition());
                player.displayClientMessage(Component.translatable("message.overgearedmod.khan_master"), true);
            } else if (QuestService.get(player).isActive(KhanContest.KHANS_CONTEST)) {
                KhanContest.summonEuphemina(player.serverLevel(), khan);
            } else if (QuestService.get(player).isCompleted(KHANS_RAGE)) {
                player.displayClientMessage(Component.translatable("message.overgearedmod.khan_sober"), true);
            } else if (thugsLost(player, khan)) {
                summonThugs(player, khan);
            }
        } else {
            int line = player.getRandom().nextInt(DRUNK_LINES) + 1;
            player.displayClientMessage(Component.translatable("message.overgearedmod.khan_drunk." + line), true);
        }
    }

    /** The quest runs, the contract isn't torn, and neither the gang nor the contract is anywhere to be found. */
    private static boolean thugsLost(ServerPlayer player, Entity khan) {
        if (!QuestService.get(player).isActive(KHANS_RAGE) || GoalTracker.milestones(player, CONTRACT_DESTROYED) > 0
                || player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.TRANSFER_CONTRACT.get()))) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        AABB around = khan.getBoundingBox().inflate(MeroThug.GANG_RANGE);
        boolean contractOnTheGround = !level.getEntitiesOfClass(ItemEntity.class, around,
                item -> item.getItem().is(ModItems.TRANSFER_CONTRACT.get())).isEmpty();
        return !contractOnTheGround && gang(level, player.getUUID(), khan.position(), MeroThug.GANG_RANGE).isEmpty();
    }

    /** The living thugs sent for {@code owner} within {@code range} of {@code center}. */
    static List<MeroThug> gang(ServerLevel level, UUID owner, Vec3 center, double range) {
        return level.getEntitiesOfClass(MeroThug.class, AABB.ofSize(center, range * 2, range * 2, range * 2),
                thug -> thug.isAlive() && thug.owner().filter(owner::equals).isPresent());
    }

    private static void onAccepted(QuestAcceptedEvent event) {
        if (event.quest().equals(KHANS_RAGE)) {
            event.giver().ifPresent(khan -> summonThugs(event.player(), khan));
        }
    }

    /** The Mero's thugs burst into the smithy around Khan, after this player. */
    static void summonThugs(ServerPlayer player, Entity khan) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> spots = AshurBetrayal.spotsAround(level, khan.blockPosition(), THUGS);
        for (BlockPos spot : spots) {
            MeroThug thug = ModEntities.MERO_THUG.get().create(level);
            if (thug == null) {
                continue;
            }
            thug.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            thug.sendFor(player, khan.blockPosition());
            level.addFreshEntity(thug);
        }
        level.playSound(null, khan.blockPosition(), SoundEvents.VINDICATOR_CELEBRATE, SoundSource.HOSTILE, 1.5f, 0.8f);
        player.displayClientMessage(Component.translatable("message.overgearedmod.mero_thugs_arrive"), false);
    }

    private static void onCompleted(QuestCompletedEvent event) {
        if (event.quest().equals(KHANS_RAGE)) {
            event.player().displayClientMessage(Component.translatable("message.overgearedmod.khans_rage_done"), false);
        }
    }

    /** Khan's time ran out: he dies for the whole world when it is a single-player one, else only for this player. */
    private static void onFailed(QuestFailedEvent event) {
        if (!event.quest().equals(KHANS_RAGE)) {
            return;
        }
        MinecraftServer server = event.player().server;
        if (isWholeWorld(server)) {
            killKhan(server.overworld());
            event.player().displayClientMessage(Component.translatable("message.overgearedmod.khan_died"), false);
        } else {
            event.player().displayClientMessage(Component.translatable("message.overgearedmod.khan_died_for_you"), false);
        }
    }

    /** One player alone in a world nobody else can join: what happens to Khan happens to the world. */
    static boolean isWholeWorld(MinecraftServer server) {
        return server.isSingleplayer() && !server.isPublished();
    }

    /** Khan dies for the whole world: those loaded now are buried at once, the rest when their chunk loads. */
    public static void killKhan(ServerLevel overworld) {
        WinstonData.get(overworld).setKhanDead();
        for (ServerLevel level : overworld.getServer().getAllLevels()) {
            for (KhanNpc khan : level.getEntities(ModEntities.KHAN.get(), Entity::isAlive)) {
                bury(khan);
            }
        }
    }

    public static boolean isKhanDead(ServerLevel level) {
        return WinstonData.get(level.getServer().overworld()).khanDead();
    }

    /** Khan is gone; where he stood, a grave sign. */
    public static void bury(KhanNpc khan) {
        ServerLevel level = (ServerLevel) khan.level();
        BlockPos at = khan.blockPosition();
        if (level.getBlockState(at).canBeReplaced()) {
            int rotation = Math.floorMod(Math.round(khan.getYRot() * 16 / 360f), 16);
            level.setBlockAndUpdate(at, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
            if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
                SignText text = new SignText()
                        .setMessage(1, Component.translatable("sign.overgearedmod.khan_grave.1"))
                        .setMessage(2, Component.translatable("sign.overgearedmod.khan_grave.2"));
                sign.setText(text, true);
                sign.setWaxed(true);
                sign.setChanged();
                level.sendBlockUpdated(at, sign.getBlockState(), sign.getBlockState(), 3);
            }
        }
        level.playSound(null, at, SoundEvents.BELL_RESONATE, SoundSource.NEUTRAL, 1.0f, 0.5f);
        khan.discard();
    }

    /** An explorer map centered on the world's Winston, with the city marked; empty if the world has none. */
    static Optional<ItemStack> winstonMap(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return Optional.empty();
        }
        return WorldSites.siteFor(level.getSeed(), WorldSites.WINSTON).map(chunk -> {
            BlockPos winston = chunk.getMiddleBlockPosition(64);
            ItemStack map = MapItem.create(level, winston.getX(), winston.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, winston, "+", MapDecorationTypes.RED_X);
            map.set(DataComponents.ITEM_NAME, Component.translatable("item.overgearedmod.winston_map"));
            return map;
        });
    }
}
