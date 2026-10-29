package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Overgearedmod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OVERGEARED_TAB = CREATIVE_MODE_TABS.register("overgeared",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.overgearedmod"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    // Lambda: the stack is built lazily, never during class init.
                    .icon(() -> ModItems.TEST_INGOT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEST_INGOT);
                        output.accept(ModItems.JAFFA_SWORD);
                    })
                    .build());

    /** Also list our items in the matching vanilla tabs. */
    public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.JAFFA_SWORD);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.TEST_INGOT);
        }
    }

    private ModCreativeTabs() {
    }
}
