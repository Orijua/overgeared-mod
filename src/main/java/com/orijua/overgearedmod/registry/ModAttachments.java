package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.progression.PlayerStats;
import com.orijua.overgearedmod.skill.SkillState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Data attachments = custom data stored on an entity (here, the player) and saved with it.
 * {@code sync(...)} pushes it to the owning client automatically on every setData.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Overgearedmod.MODID);

    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS = ATTACHMENT_TYPES.register("player_stats",
            () -> AttachmentType.builder(() -> PlayerStats.DEFAULT)
                    .serialize(PlayerStats.CODEC)
                    .copyOnDeath()                                              // progression survives death
                    .sync((holder, to) -> holder == to, PlayerStats.STREAM_CODEC) // only the owner sees it
                    .build());

    public static final Supplier<AttachmentType<SkillState>> SKILL_STATE = ATTACHMENT_TYPES.register("skill_state",
            () -> AttachmentType.builder(() -> SkillState.DEFAULT)
                    .serialize(SkillState.CODEC)
                    .sync((holder, to) -> holder == to, SkillState.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }
}
