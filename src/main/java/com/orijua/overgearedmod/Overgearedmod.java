package com.orijua.overgearedmod;

import com.mojang.logging.LogUtils;
import com.orijua.overgearedmod.command.DebugCommands;
import com.orijua.overgearedmod.datagen.ModDataGenerators;
import com.orijua.overgearedmod.equipment.EquipmentEvents;
import com.orijua.overgearedmod.goal.GoalTracker;
import com.orijua.overgearedmod.loot.LootEvents;
import com.orijua.overgearedmod.mob.MobLevelEvents;
import com.orijua.overgearedmod.network.ModNetwork;
import com.orijua.overgearedmod.notification.NotificationEvents;
import com.orijua.overgearedmod.progression.ProgressionEvents;
import com.orijua.overgearedmod.registry.ModAttachments;
import com.orijua.overgearedmod.registry.ModBlockEntities;
import com.orijua.overgearedmod.registry.ModBlocks;
import com.orijua.overgearedmod.registry.ModCreativeTabs;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.registry.ModEffects;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModItems;
import com.orijua.overgearedmod.registry.ModLoot;
import com.orijua.overgearedmod.registry.ModMenus;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.registry.ModWorldgen;
import com.orijua.overgearedmod.skill.SkillEvents;
import com.orijua.overgearedmod.structure.NorthEndCaveProtection;
import com.orijua.overgearedmod.pagma.AshurBetrayal;
import com.orijua.overgearedmod.pagma.LegendTitle;
import com.orijua.overgearedmod.structure.StartingVillage;
import com.orijua.overgearedmod.validation.ContentValidationEvents;
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
        AshurBetrayal.register(NeoForge.EVENT_BUS);
        LegendTitle.register(NeoForge.EVENT_BUS);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    /** Shorthand for {@code overgearedmod:<path>}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
