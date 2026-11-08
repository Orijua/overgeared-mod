package com.orijua.overgearedmod.feature.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orijua.overgearedmod.core.config.TradeBalance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How saturated each market of the player is: the pressure (pieces sold or exported of a category and rating) and the
 * game day of the last sale. The pressure goes away with the days, applied when it is read, so nothing ticks.
 * A future shared market (MMO) would key by item and rating instead; not implemented.
 */
public record TradeSaturation(Map<TradeKey, Entry> entries) {
    public static final TradeSaturation EMPTY = new TradeSaturation(Map.of());

    public record Entry(int pressure, long lastDay) {
    }

    /** One saved line. The key stays text here so an unknown one (a removed rating) is skipped instead of failing the player's data. */
    private record Saved(String key, int pressure, long day) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(Saved::key),
                Codec.INT.optionalFieldOf("pressure", 0).forGetter(Saved::pressure),
                Codec.LONG.optionalFieldOf("day", 0L).forGetter(Saved::day)
        ).apply(instance, Saved::new));
    }

    public static final Codec<TradeSaturation> CODEC = Saved.CODEC.listOf().xmap(
            saved -> {
                Map<TradeKey, Entry> map = new HashMap<>();
                for (Saved line : saved) {
                    TradeKey.parse(line.key()).ifPresent(key -> map.put(key, new Entry(Math.max(0, line.pressure()), line.day())));
                }
                return new TradeSaturation(map);
            },
            saturation -> {
                List<Saved> saved = new ArrayList<>();
                saturation.entries().forEach((key, entry) -> saved.add(new Saved(key.asString(), entry.pressure(), entry.lastDay())));
                saved.sort(Comparator.comparing(Saved::key));
                return saved;
            });

    public static final StreamCodec<ByteBuf, TradeSaturation> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public TradeSaturation {
        entries = Map.copyOf(entries);
    }

    /** The pressure of {@code key} on game day {@code today}, with the recovery of the days gone by. */
    public int pressure(TradeKey key, long today, TradeBalance balance) {
        Entry entry = entries.get(key);
        return entry == null ? 0 : TradeRules.recovered(entry.pressure(), entry.lastDay(), today, balance);
    }

    /** One more piece sold or exported: recovers first, then adds 1 (up to the cap) and stamps the day. */
    public TradeSaturation recordSale(TradeKey key, long today, TradeBalance balance) {
        int next = Math.min(balance.pressureCap(), pressure(key, today, balance) + 1);
        Map<TradeKey, Entry> copy = new HashMap<>(entries);
        copy.put(key, new Entry(next, today));
        return new TradeSaturation(copy);
    }
}
