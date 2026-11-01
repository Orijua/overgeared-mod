package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.mob.BoneLord;
import com.orijua.overgearedmod.feature.npc.ApprenticeSmith;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.npc.VillageChiefNpc;
import com.orijua.overgearedmod.feature.pagma.AshurKnight;
import com.orijua.overgearedmod.feature.pagma.EarlAshur;
import com.orijua.overgearedmod.feature.winston.KhanNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/** Custom entities. Each living one also needs its attributes (below) and a renderer (OvergearedmodClient). */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Overgearedmod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlacksmithNpc>> BLACKSMITH = ENTITIES.register("blacksmith",
            () -> EntityType.Builder.of(BlacksmithNpc::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("blacksmith"));

    public static final DeferredHolder<EntityType<?>, EntityType<VillageChiefNpc>> VILLAGE_CHIEF = ENTITIES.register("village_chief",
            () -> EntityType.Builder.of(VillageChiefNpc::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("village_chief"));

    public static final DeferredHolder<EntityType<?>, EntityType<BoneLord>> BONE_LORD = ENTITIES.register("bone_lord",
            () -> EntityType.Builder.of(BoneLord::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.4f) // the wither skeleton's hitbox; the scale attribute grows it to boss size
                    .clientTrackingRange(10)
                    .build("bone_lord"));

    // An apprentice blacksmith hired with a contract from Smith: forges alone at a smithing anvil (chest in, barrel out).
    public static final DeferredHolder<EntityType<?>, EntityType<ApprenticeSmith>> APPRENTICE_SMITH = ENTITIES.register("apprentice_smith",
            () -> EntityType.Builder.of(ApprenticeSmith::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("apprentice_smith"));

    // Earl Ashur and his knights (canon, caps. 1-2): peaceful in his manor near Bairan, hunters once Pagma's book is appraised.
    public static final DeferredHolder<EntityType<?>, EntityType<EarlAshur>> EARL_ASHUR = ENTITIES.register("earl_ashur",
            () -> EntityType.Builder.of(EarlAshur::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("earl_ashur"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshurKnight>> ASHUR_KNIGHT = ENTITIES.register("ashur_knight",
            () -> EntityType.Builder.of(AshurKnight::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("ashur_knight"));

    // Khan, Winston's old blacksmith (canon): drinks in his ruined smithy until Pagma's successor comes to help.
    public static final DeferredHolder<EntityType<?>, EntityType<KhanNpc>> KHAN = ENTITIES.register("khan",
            () -> EntityType.Builder.of(KhanNpc::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("khan"));

    private ModEntities() {
    }

    /** The NPCs with a quest window: a quest's "giver" must be one of them. */
    public static Set<EntityType<?>> questGivers() {
        return Set.of(BLACKSMITH.get(), VILLAGE_CHIEF.get(), EARL_ASHUR.get(), KHAN.get());
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BLACKSMITH.get(), BlacksmithNpc.createAttributes().build());
        event.put(VILLAGE_CHIEF.get(), VillageChiefNpc.createAttributes().build());
        event.put(BONE_LORD.get(), BoneLord.createAttributes().build());
        event.put(APPRENTICE_SMITH.get(), ApprenticeSmith.createAttributes().build());
        event.put(EARL_ASHUR.get(), EarlAshur.createAttributes().build());
        event.put(ASHUR_KNIGHT.get(), AshurKnight.createAttributes().build());
        event.put(KHAN.get(), KhanNpc.createAttributes().build());
    }
}
