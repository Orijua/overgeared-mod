package com.orijua.overgearedmod.feature.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.orijua.overgearedmod.feature.world.Satisfy;

import java.util.Optional;

/**
 * Paying the starter debt by hand from the terminal. Everything is decided here: the client says what it saw and every
 * check comes before anything moves. Paying through work is {@link TerminalService#work}.
 */
public final class DebtService {
    private DebtService() {
    }

    public enum Result { OK, NO_TERMINAL, IN_SATISFY, NO_DEBT, STALE, INVALID_AMOUNT, NOT_ENOUGH_WON }

    /**
     * The result of a payment.
     *
     * @param paid    the Won paid now
     * @param left    the debt left
     * @param settled whether this payment paid the debt off
     */
    public record Outcome(Result result, long paid, long left, boolean settled) {
        static Outcome of(Result result) {
            return new Outcome(result, 0L, 0L, false);
        }
    }

    /** The message for the player after an attempt; empty when there is nothing to say. */
    public static Optional<Component> message(Outcome outcome) {
        return switch (outcome.result()) {
            case OK -> Optional.of(outcome.settled() ? settledMessage()
                    : Component.translatable("message.overgearedmod.terminal.debt_paid", outcome.paid(), outcome.left()));
            case NO_TERMINAL -> Optional.empty();
            case IN_SATISFY -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_signal"));
            case NO_DEBT -> Optional.of(Component.translatable("message.overgearedmod.terminal.no_debt"));
            case STALE -> Optional.of(Component.translatable("message.overgearedmod.terminal.stale"));
            case INVALID_AMOUNT -> Optional.of(Component.translatable("message.overgearedmod.terminal.invalid_amount"));
            case NOT_ENOUGH_WON -> Optional.of(Component.translatable("message.overgearedmod.terminal.not_enough_won"));
        };
    }

    static Component settledMessage() {
        return Component.translatable("message.overgearedmod.debt.settled");
    }

    /** The order of the terminal's window: it must be the open menu of the sender. */
    public static Outcome payDebt(ServerPlayer player, TerminalPayDebtPayload payload) {
        if (Satisfy.is(player.level())) {
            return Outcome.of(Result.IN_SATISFY);
        }
        if (!TerminalService.menuOpen(player, payload.containerId())) {
            return Outcome.of(Result.NO_TERMINAL);
        }
        return payDebt(player, payload.amount(), payload.seenDebt());
    }

    /** Pays {@code amount} of the debt, having seen it at {@code seenDebt}. Nothing changes unless every check passes. */
    public static Outcome payDebt(ServerPlayer player, long amount, long seenDebt) {
        if (!TerminalService.carriesRealWorld(player)) {
            return Outcome.of(Result.IN_SATISFY);
        }
        Account account = AccountService.get(player);
        if (account.debtState() != DebtState.ACTIVE) {
            return Outcome.of(Result.NO_DEBT);
        }
        if (seenDebt != account.debt()) {
            return Outcome.of(Result.STALE);
        }
        if (amount < 1L || amount > account.debt()) {
            return Outcome.of(Result.INVALID_AMOUNT);
        }
        if (amount > account.won()) {
            return Outcome.of(Result.NOT_ENOUGH_WON);
        }
        boolean settled = AccountService.payDebt(player, amount);
        if (settled) {
            announceSettled(player);
        }
        return new Outcome(Result.OK, amount, account.debt() - amount, settled);
    }

    /**
     * The moment the debt is paid off: a chat line, a title and its subtitle, and the sound of an achievement. Not a
     * {@code SystemNotification}: that layer only exists in Satisfy and the debt is paid outside it.
     */
    public static void announceSettled(ServerPlayer player) {
        player.sendSystemMessage(settledMessage());
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.overgearedmod.debt.settled")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("subtitle.overgearedmod.debt.settled")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
