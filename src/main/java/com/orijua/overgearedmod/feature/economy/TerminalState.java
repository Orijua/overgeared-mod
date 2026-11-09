package com.orijua.overgearedmod.feature.economy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Everything the terminal's screen needs, decided by the server and sent whenever it changes: whether there is a
 * character to take emeralds from, how many it holds, the numbers of the balance, today's work and the last message.
 *
 * @param characterAvailable whether the Satisfy character is stored and readable (the real world's player carries the other one)
 * @param storedEmeralds     Satisfy emeralds in the stored character's inventory
 * @param wonPerEmerald      Won for one emerald
 * @param commission         share the site keeps (0.05 = 5 %)
 * @param workPay            Won for one shift
 * @param today              the server's game day
 * @param order              today's work order
 * @param icon               an item to draw for the order
 * @param workDay            the day of the last shift ({@link Long#MIN_VALUE}: never)
 * @param message            the result of the last action, if any
 */
public record TerminalState(boolean characterAvailable, int storedEmeralds, int wonPerEmerald, double commission, long workPay,
                            long today, WorkOrder order, ItemStack icon, long workDay, Optional<Component> message) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalState> STREAM_CODEC = StreamCodec.of(TerminalState::write, TerminalState::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> MESSAGE = ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC);

    private static void write(RegistryFriendlyByteBuf buf, TerminalState state) {
        buf.writeBoolean(state.characterAvailable);
        buf.writeVarInt(state.storedEmeralds);
        buf.writeVarInt(state.wonPerEmerald);
        buf.writeDouble(state.commission);
        buf.writeVarLong(state.workPay);
        buf.writeLong(state.today);
        WorkOrder.STREAM_CODEC.encode(buf, state.order);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, state.icon);
        buf.writeLong(state.workDay);
        MESSAGE.encode(buf, state.message);
    }

    private static TerminalState read(RegistryFriendlyByteBuf buf) {
        boolean available = buf.readBoolean();
        int stored = buf.readVarInt();
        int wonPerEmerald = buf.readVarInt();
        double commission = buf.readDouble();
        long workPay = buf.readVarLong();
        long today = buf.readLong();
        WorkOrder order = WorkOrder.STREAM_CODEC.decode(buf);
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        long workDay = buf.readLong();
        Optional<Component> message = MESSAGE.decode(buf);
        return new TerminalState(available, stored, wonPerEmerald, commission, workPay, today, order, icon, workDay, message);
    }

    public TerminalState withMessage(Optional<Component> message) {
        return new TerminalState(characterAvailable, storedEmeralds, wonPerEmerald, commission, workPay, today, order, icon, workDay, message);
    }
}
