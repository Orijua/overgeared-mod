package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.smelting.SmelterMenu;
import com.orijua.overgearedmod.feature.smithing.SmithingAnvilMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Overgearedmod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SmithingAnvilMenu>> SMITHING_ANVIL =
            MENUS.register("smithing_anvil", () -> new MenuType<>(SmithingAnvilMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<SmelterMenu>> SMELTER =
            MENUS.register("smelter", () -> new MenuType<>(SmelterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
