package com.orijua.overgearedmod.pagma;

import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.quest.PlayerQuests;
import com.orijua.overgearedmod.quest.QuestService;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.structure.WorldSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Earl Ashur's side of the story (canon, caps. 1-2). Once the player has finished Smith's chain, Ashur tells them of his
 * secret S-rank request and gives them a map to the Northern End Cave. Bringing him the book unappraised completes it
 * (and Ashur keeps the book). Appraising it instead reveals what it is, and Ashur comes for it with his knights: the
 * request becomes "Earl Ashur's Anger" (SS). Whoever dies at his hands with the book loses it to him.
 */
public final class AshurBetrayal {
    /** Canon (cap. 1): Ashur arrives with dozens of knights; here four, which is already too many. */
    public static final int KNIGHTS = 4;

    private AshurBetrayal() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOW, AshurBetrayal::onDeath);
        gameBus.addListener(AshurBetrayal::onDrops);
    }

    /**
     * Talking to Ashur in his manor: before Smith's chain is done he has no time for a nobody; after it, the first time,
     * he tells of his secret request (the hidden quest is discovered) and hands over a map to the cave.
     */
    public static void talk(ServerPlayer player) {
        if (!QuestService.get(player).isCompleted(PagmaStory.SMITH_CHAIN_END)) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.ashur_nobody"), true);
            return;
        }
        if (GoalTracker.milestones(player, PagmaStory.ASHUR_SECRET) > 0) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.overgearedmod.ashur_secret"), false);
        GoalTracker.milestone(player, PagmaStory.ASHUR_SECRET);
        caveMap(player).ifPresent(map -> {
            if (!player.getInventory().add(map)) {
                player.drop(map, false);
            }
        });
    }

    /** An explorer map centered on the world's Northern End Cave, with the cave marked; empty if the world has none. */
    static Optional<ItemStack> caveMap(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return Optional.empty();
        }
        return WorldSites.siteFor(level.getSeed(), WorldSites.NORTH_END_CAVE).map(chunk -> {
            BlockPos cave = chunk.getMiddleBlockPosition(64);
            ItemStack map = MapItem.create(level, cave.getX(), cave.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, cave, "+", MapDecorationTypes.RED_X);
            map.set(DataComponents.ITEM_NAME, Component.translatable("item.overgearedmod.ashur_map"));
            return map;
        });
    }

    /**
     * The book was appraised: Pagma's legacy is revealed, and Ashur arrives with {@link #KNIGHTS} knights around the
     * player. His secret request is dropped and "Earl Ashur's Anger" (kill him) starts.
     */
    public static void betray(ServerPlayer player) {
        GoalTracker.milestone(player, PagmaStory.APPRAISED);
        if (PagmaStory.killedAshur(player)) {
            return; // he is already dead for this player: nobody comes
        }
        PlayerQuests quests = QuestService.get(player);
        if (quests.isActive(PagmaStory.SECRET_REQUEST)) {
            player.setData(ModAttachments.PLAYER_QUESTS, quests.abandon(PagmaStory.SECRET_REQUEST));
        }
        boolean first = GoalTracker.milestones(player, PagmaStory.BETRAYAL) == 0;
        GoalTracker.milestone(player, PagmaStory.BETRAYAL);
        if (first || !QuestService.get(player).isActive(PagmaStory.ANGER)) {
            QuestService.accept(player, PagmaStory.ANGER);
        }
        ServerLevel level = player.serverLevel();
        List<BlockPos> spots = spotsAround(level, player.blockPosition(), KNIGHTS + 1);
        summon(level, ModEntities.EARL_ASHUR.get(), spots.getFirst(), player);
        for (int i = 1; i < spots.size(); i++) {
            summon(level, ModEntities.ASHUR_KNIGHT.get(), spots.get(i), player);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.overgearedmod.ashur_arrives"), false);
    }

    private static void summon(ServerLevel level, EntityType<? extends AshurRetinue> type, BlockPos at, ServerPlayer target) {
        AshurRetinue member = type.create(level);
        if (member == null) {
            return;
        }
        member.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        member.setSummoned(true);
        member.setTarget(target);
        level.addFreshEntity(member);
    }

    /**
     * {@code count} places to stand 3 to 7 blocks around {@code center} (free for two blocks, on solid ground), nearest
     * first; where there aren't enough, the center itself fills the rest.
     */
    static List<BlockPos> spotsAround(ServerLevel level, BlockPos center, int count) {
        List<BlockPos> spots = new ArrayList<>();
        for (int radius = 3; radius <= 7 && spots.size() < count; radius++) {
            for (int step = 0; step < 16 && spots.size() < count; step++) {
                double angle = Math.PI * 2 * step / 16 + radius;
                BlockPos column = center.offset((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos feet = column.above(dy);
                    if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                            && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                            && !spots.contains(feet)) {
                        spots.add(feet);
                        break;
                    }
                }
            }
        }
        while (spots.size() < count) {
            spots.add(center);
        }
        return spots;
    }

    /** Canon (cap. 2): dying to Ashur with the book means he takes it. The chance to read it is lost. */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PagmaStory.isAshursEnemy(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean taken = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.PAGMA_RARE_BOOK.get())) {
                inventory.setItem(slot, ItemStack.EMPTY);
                taken = true;
            }
        }
        if (taken) {
            player.displayClientMessage(Component.translatable("message.overgearedmod.ashur_took_book"), false);
        }
    }

    /** In case the book was already among the drops (another mod dropping the inventory first). */
    private static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PagmaStory.isAshursEnemy(player)) {
            event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.PAGMA_RARE_BOOK.get()));
        }
    }

}
