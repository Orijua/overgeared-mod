package com.orijua.overgearedmod;

import com.mojang.logging.LogUtils;
import com.orijua.overgearedmod.core.config.Config;
import com.orijua.overgearedmod.core.debug.DebugCommands;
import com.orijua.overgearedmod.core.network.ModNetwork;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModBlockEntities;
import com.orijua.overgearedmod.core.registry.ModBlocks;
import com.orijua.overgearedmod.core.registry.ModCreativeTabs;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.core.registry.ModEffects;
import com.orijua.overgearedmod.core.registry.ModEntities;
import com.orijua.overgearedmod.core.registry.ModItems;
import com.orijua.overgearedmod.core.registry.ModLoot;
import com.orijua.overgearedmod.core.registry.ModMenus;
import com.orijua.overgearedmod.core.registry.ModRecipes;
import com.orijua.overgearedmod.core.registry.ModWorldgen;
import com.orijua.overgearedmod.core.validation.ContentValidationEvents;
import com.orijua.overgearedmod.datagen.ModDataGenerators;
import com.orijua.overgearedmod.feature.equipment.EquipmentEvents;
import com.orijua.overgearedmod.feature.equipment.LootEvents;
import com.orijua.overgearedmod.feature.insight.Observation;
import com.orijua.overgearedmod.feature.mob.MobLevelEvents;
import com.orijua.overgearedmod.feature.notification.NotificationEvents;
import com.orijua.overgearedmod.feature.npc.ApprenticeAssignments;
import com.orijua.overgearedmod.feature.npc.Illumination;
import com.orijua.overgearedmod.feature.pagma.AshurBetrayal;
import com.orijua.overgearedmod.feature.pagma.LegendTitle;
import com.orijua.overgearedmod.feature.progression.ProgressionEvents;
import com.orijua.overgearedmod.feature.quest.goal.GoalTracker;
import com.orijua.overgearedmod.feature.rumor.MaterialFinds;
import com.orijua.overgearedmod.feature.skill.SkillEvents;
import com.orijua.overgearedmod.feature.world.NorthEndCaveProtection;
import com.orijua.overgearedmod.feature.world.StartingVillage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Common entry point (runs on both client and dedicated server).
 * Every registry and event listener is wired here explicitly, so this class is the
 * single place to look to see what the mod plugs into.
 */
@Mod(Overgearedmod.MODID)
public class Overgearedmod {
    public static final String MODID = "overgearedmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Overgearedmod(IEventBus modEventBus, ModContainer modContainer) {
        // Registries (mod bus): content must be registered before the game freezes registries.
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModLoot.LOOT_FUNCTIONS.register(modEventBus);
        ModLoot.LOOT_MODIFIERS.register(modEventBus);
        ModWorldgen.STRUCTURE_PLACEMENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Mod bus events (JSON registries, networking, creative tabs, entity attributes, datagen).
        modEventBus.addListener(ModDatapackRegistries::register);
        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(ModCreativeTabs::addToVanillaTabs);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModDataGenerators::gatherData);

        // Game bus events (gameplay: kills, ticks, item attributes...).
        EquipmentEvents.register(NeoForge.EVENT_BUS);
        ProgressionEvents.register(NeoForge.EVENT_BUS);
        SkillEvents.register(NeoForge.EVENT_BUS);
        MobLevelEvents.register(NeoForge.EVENT_BUS);
        LootEvents.register(NeoForge.EVENT_BUS);
        // Our own domain events (event/) have their subscribers on the same game bus.
        GoalTracker.register(NeoForge.EVENT_BUS);
        ContentValidationEvents.register(NeoForge.EVENT_BUS);
        NotificationEvents.register(NeoForge.EVENT_BUS);
        DebugCommands.register(NeoForge.EVENT_BUS);
        StartingVillage.register(NeoForge.EVENT_BUS);
        NorthEndCaveProtection.register(NeoForge.EVENT_BUS);
        ApprenticeAssignments.register(NeoForge.EVENT_BUS);
        Illumination.register(NeoForge.EVENT_BUS);
        Observation.register(NeoForge.EVENT_BUS);
        AshurBetrayal.register(NeoForge.EVENT_BUS);
        LegendTitle.register(NeoForge.EVENT_BUS);
        MaterialFinds.register(NeoForge.EVENT_BUS);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    /** Shorthand for {@code overgearedmod:<path>}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
