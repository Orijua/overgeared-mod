package com.orijua.overgearedmod;

import com.mojang.logging.LogUtils;
import com.orijua.overgearedmod.rating.RatingEvents;
import com.orijua.overgearedmod.registry.ModCreativeTabs;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModItems;
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
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Mod bus events (lifecycle, networking setup, creative tabs).
        modEventBus.addListener(ModCreativeTabs::addToVanillaTabs);

        // Game bus events (gameplay: kills, ticks, item attributes...).
        RatingEvents.register(NeoForge.EVENT_BUS);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Shorthand for {@code overgearedmod:<path>}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
