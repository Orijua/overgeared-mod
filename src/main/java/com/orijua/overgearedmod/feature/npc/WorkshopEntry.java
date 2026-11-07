package com.orijua.overgearedmod.feature.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/**
 * What the Workshop tab knows of one apprentice: a snapshot he sends his master while he works (his master can't see
 * him from afar). {@code status} is an {@link ApprenticeSmith.Status} name.
 */
public record WorkshopEntry(UUID apprentice, String name, boolean named, boolean craftsman, int level, String status,
                            Optional<ResourceLocation> order, int daysPaid, int progress, Optional<ResourceLocation> missing,
                            int missingCount, long reportedAt) {
    /** The piece's progress is reported in steps of this many percent, so the entry changes (and syncs) rarely. */
    public static final int PROGRESS_STEP = 5;
    /** An unchanged entry is still re-stamped this often (ticks) while the apprentice is around, so it never looks stale. */
    public static final long REFRESH_TICKS = 100;
    /** After this long (ticks) without a report the Workshop stops trusting the entry: he is out of reach. */
    public static final long STALE_TICKS = 300;

    public static final Codec<WorkshopEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("apprentice").forGetter(WorkshopEntry::apprentice),
            Codec.STRING.fieldOf("name").forGetter(WorkshopEntry::name),
            Codec.BOOL.optionalFieldOf("named", false).forGetter(WorkshopEntry::named),
            Codec.BOOL.optionalFieldOf("craftsman", false).forGetter(WorkshopEntry::craftsman),
            Codec.INT.fieldOf("level").forGetter(WorkshopEntry::level),
            Codec.STRING.fieldOf("status").forGetter(WorkshopEntry::status),
            ResourceLocation.CODEC.optionalFieldOf("order").forGetter(WorkshopEntry::order),
            Codec.INT.fieldOf("days_paid").forGetter(WorkshopEntry::daysPaid),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(WorkshopEntry::progress),
            ResourceLocation.CODEC.optionalFieldOf("missing").forGetter(WorkshopEntry::missing),
            Codec.INT.optionalFieldOf("missing_count", 0).forGetter(WorkshopEntry::missingCount),
            Codec.LONG.optionalFieldOf("reported_at", 0L).forGetter(WorkshopEntry::reportedAt)
    ).apply(instance, WorkshopEntry::new));

    /** Whether he has not reported for too long at game time {@code now}: what the entry says may be out of date. */
    public boolean isStale(long now) {
        return now - reportedAt > STALE_TICKS;
    }

    /** The same news, ignoring when it was reported. */
    boolean sameNewsAs(WorkshopEntry other) {
        return withReportedAt(0).equals(other.withReportedAt(0));
    }

    private WorkshopEntry withReportedAt(long time) {
        return new WorkshopEntry(apprentice, name, named, craftsman, level, status, order, daysPaid, progress, missing, missingCount, time);
    }

    /** Progress rounded down to a {@link #PROGRESS_STEP}, 0-100. */
    public static int quantize(int progress) {
        return Math.clamp(progress, 0, 100) / PROGRESS_STEP * PROGRESS_STEP;
    }

    public ApprenticeSmith.Status statusValue() {
        for (ApprenticeSmith.Status value : ApprenticeSmith.Status.values()) {
            if (value.name().equals(status)) {
                return value;
            }
        }
        return ApprenticeSmith.Status.NO_ORDER;
    }

    public MasteryStage stage() {
        return MasteryStage.of(level);
    }
}
