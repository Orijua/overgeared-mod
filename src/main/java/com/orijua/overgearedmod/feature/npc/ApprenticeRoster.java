package com.orijua.overgearedmod.feature.npc;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The apprentices who died (or were removed) while their master was away. The master's list of apprentices
 * ({@code APPRENTICES}) lives on the player, who may be offline when it happens: the loss waits here, saved with the
 * overworld (data/overgearedmod_apprentice_roster.dat), and is applied when the master logs in or next hires.
 */
public final class ApprenticeRoster extends SavedData {
    private static final String NAME = Overgearedmod.MODID + "_apprentice_roster";
    private static final String LOST = "lost";
    private static final String APPRENTICE = "apprentice";
    private static final String MASTER = "master";

    /** Apprentice -> his master. */
    private final Map<UUID, UUID> lost = new LinkedHashMap<>();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ApprenticeRoster::onLoggedIn);
    }

    static ApprenticeRoster get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ApprenticeRoster::new, ApprenticeRoster::load), NAME);
    }

    static ApprenticeRoster load(CompoundTag tag, HolderLookup.Provider registries) {
        ApprenticeRoster roster = new ApprenticeRoster();
        for (Tag saved : tag.getList(LOST, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) saved;
            if (entry.hasUUID(APPRENTICE) && entry.hasUUID(MASTER)) {
                roster.lost.put(entry.getUUID(APPRENTICE), entry.getUUID(MASTER));
            }
        }
        return roster;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        lost.forEach((apprentice, master) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(APPRENTICE, apprentice);
            entry.putUUID(MASTER, master);
            list.add(entry);
        });
        tag.put(LOST, list);
        return tag;
    }

    /** Notes that {@code apprentice} is gone and his master has not heard of it yet. */
    void markLost(UUID apprentice, UUID master) {
        lost.put(apprentice, master);
        setDirty();
    }

    /** The apprentices of {@code master} that are gone and not yet taken off his list. */
    List<UUID> lostOf(UUID master) {
        List<UUID> found = new ArrayList<>();
        lost.forEach((apprentice, owner) -> {
            if (owner.equals(master)) {
                found.add(apprentice);
            }
        });
        return found;
    }

    void forget(UUID apprentice) {
        if (lost.remove(apprentice) != null) {
            setDirty();
        }
    }

    int size() {
        return lost.size();
    }

    /** Takes the lost apprentices off {@code master}'s list and Workshop. Returns how many. */
    public static int apply(ServerPlayer master) {
        ApprenticeRoster roster = get(master.server);
        int released = 0;
        for (UUID apprentice : roster.lostOf(master.getUUID())) {
            Workshop.release(master, apprentice);
            roster.forget(apprentice);
            released++;
        }
        return released;
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
        }
    }
}
