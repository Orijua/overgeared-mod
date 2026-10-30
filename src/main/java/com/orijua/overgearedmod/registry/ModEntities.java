package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.boss.BoneLord;
import com.orijua.overgearedmod.npc.BlacksmithNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom entities. Each living one also needs its attributes (below) and a renderer (OvergearedmodClient). */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Overgearedmod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlacksmithNpc>> BLACKSMITH = ENTITIES.register("blacksmith",
            () -> EntityType.Builder.of(BlacksmithNpc::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("blacksmith"));

    public static final DeferredHolder<EntityType<?>, EntityType<BoneLord>> BONE_LORD = ENTITIES.register("bone_lord",
            () -> EntityType.Builder.of(BoneLord::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.4f) // the wither skeleton's hitbox; the scale attribute grows it to boss size
                    .clientTrackingRange(10)
                    .build("bone_lord"));

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BLACKSMITH.get(), BlacksmithNpc.createAttributes().build());
        event.put(BONE_LORD.get(), BoneLord.createAttributes().build());
    }
}
