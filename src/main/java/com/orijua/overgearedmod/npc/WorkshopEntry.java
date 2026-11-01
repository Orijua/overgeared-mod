package com.orijua.overgearedmod.npc;

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
                            Optional<ResourceLocation> order, int daysPaid) {
    public static final Codec<WorkshopEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("apprentice").forGetter(WorkshopEntry::apprentice),
            Codec.STRING.fieldOf("name").forGetter(WorkshopEntry::name),
            Codec.BOOL.optionalFieldOf("named", false).forGetter(WorkshopEntry::named),
            Codec.BOOL.optionalFieldOf("craftsman", false).forGetter(WorkshopEntry::craftsman),
            Codec.INT.fieldOf("level").forGetter(WorkshopEntry::level),
            Codec.STRING.fieldOf("status").forGetter(WorkshopEntry::status),
            ResourceLocation.CODEC.optionalFieldOf("order").forGetter(WorkshopEntry::order),
            Codec.INT.fieldOf("days_paid").forGetter(WorkshopEntry::daysPaid)
    ).apply(instance, WorkshopEntry::new));

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
