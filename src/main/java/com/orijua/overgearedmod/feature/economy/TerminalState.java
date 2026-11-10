package com.orijua.overgearedmod.feature.economy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Everything the terminal window needs, decided by the server and sent whenever it changes: whether there is a
 * character to buy into, how many emeralds it holds, the numbers of the balance, today's work, what can be bought and the
 * last message.
 *
 * @param characterAvailable whether the Satisfy character is stored and readable (the real world's player carries the other one)
 * @param storedEmeralds     Satisfy emeralds in the stored character's inventory
 * @param workPay            Won for one shift
 * @param today              the server's game day
 * @param order              today's work order
 * @param icon               an item to draw for the order
 * @param workDay            the day of the last shift ({@link Long#MIN_VALUE}: never)
 * @param buyWonPerEmerald   Won that buying one emerald costs
 * @param emeraldRoom        emeralds that fit in the stored character's inventory (0 without character)
 * @param freeSlots          free main slots of the stored character (0 without character)
 * @param offer              today's offer, in position order (empty: no offer)
 * @param boughtToday        the positions of the offer this player already bought today
 * @param debtQuotaPercent   percent of a shift that goes to the debt while it is active (the debt itself is read from the synced account)
 * @param message            the result of the last action, if any
 */
public record TerminalState(boolean characterAvailable, int storedEmeralds, long workPay,
                            long today, WorkOrder order, ItemStack icon, long workDay, int buyWonPerEmerald, int emeraldRoom,
                            int freeSlots, List<OfferEntry> offer, Set<Integer> boughtToday, int debtQuotaPercent, Optional<Component> message) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalState> STREAM_CODEC = StreamCodec.of(TerminalState::write, TerminalState::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> MESSAGE = ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC);

    private static void write(RegistryFriendlyByteBuf buf, TerminalState state) {
        buf.writeBoolean(state.characterAvailable);
        buf.writeVarInt(state.storedEmeralds);
        buf.writeVarLong(state.workPay);
        buf.writeLong(state.today);
        WorkOrder.STREAM_CODEC.encode(buf, state.order);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, state.icon);
        buf.writeLong(state.workDay);
        buf.writeVarInt(state.buyWonPerEmerald);
        buf.writeVarInt(state.emeraldRoom);
        buf.writeVarInt(state.freeSlots);
        buf.writeVarInt(state.offer.size());
        for (OfferEntry entry : state.offer) {
            OfferEntry.STREAM_CODEC.encode(buf, entry);
        }
        buf.writeVarInt(state.boughtToday.size());
        for (int position : state.boughtToday) {
            buf.writeVarInt(position);
        }
        buf.writeVarInt(state.debtQuotaPercent);
        MESSAGE.encode(buf, state.message);
    }

    private static TerminalState read(RegistryFriendlyByteBuf buf) {
        boolean available = buf.readBoolean();
        int stored = buf.readVarInt();
        long workPay = buf.readVarLong();
        long today = buf.readLong();
        WorkOrder order = WorkOrder.STREAM_CODEC.decode(buf);
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        long workDay = buf.readLong();
        int buyWon = buf.readVarInt();
        int room = buf.readVarInt();
        int free = buf.readVarInt();
        int offerSize = buf.readVarInt();
        if (offerSize < 0 || offerSize > OfferBought.MAX_POSITIONS) {
            throw new IllegalArgumentException("the offer has " + offerSize + " pieces");
        }
        List<OfferEntry> offer = new ArrayList<>(offerSize);
        for (int i = 0; i < offerSize; i++) {
            offer.add(OfferEntry.STREAM_CODEC.decode(buf));
        }
        int boughtSize = buf.readVarInt();
        if (boughtSize < 0 || boughtSize > OfferBought.MAX_POSITIONS) {
            throw new IllegalArgumentException("the bought set has " + boughtSize + " positions");
        }
        Set<Integer> bought = new TreeSet<>();
        for (int i = 0; i < boughtSize; i++) {
            bought.add(buf.readVarInt());
        }
        int quotaPercent = buf.readVarInt();
        Optional<Component> message = MESSAGE.decode(buf);
        return new TerminalState(available, stored, workPay, today, order, icon, workDay, buyWon, room, free,
                List.copyOf(offer), Set.copyOf(bought), quotaPercent, message);
    }

    public TerminalState withMessage(Optional<Component> message) {
        return new TerminalState(characterAvailable, storedEmeralds, workPay, today, order, icon, workDay,
                buyWonPerEmerald, emeraldRoom, freeSlots, offer, boughtToday, debtQuotaPercent, message);
    }
}
