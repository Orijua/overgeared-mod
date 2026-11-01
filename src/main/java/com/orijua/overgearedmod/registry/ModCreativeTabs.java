package com.orijua.overgearedmod.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.skill.SkillBookItem;
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
                    .icon(() -> ModItems.SMITHING_ANVIL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SMITHING_ANVIL);
                        output.accept(ModItems.SMELTER);
                        output.accept(ModItems.JAFFA_ORE);
                        output.accept(ModItems.DEEPSLATE_JAFFA_ORE);
                        output.accept(ModItems.RAW_JAFFA);
                        output.accept(ModItems.JAFFA_INGOT);
                        output.accept(ModItems.BLUE_ORICHALCUM_ORE);
                        output.accept(ModItems.DEEPSLATE_BLUE_ORICHALCUM_ORE);
                        output.accept(ModItems.RAW_BLUE_ORICHALCUM);
                        output.accept(ModItems.BLUE_ORICHALCUM_INGOT);
                        output.accept(ModItems.MITHRIL_ORE);
                        output.accept(ModItems.DEEPSLATE_MITHRIL_ORE);
                        output.accept(ModItems.RAW_MITHRIL);
                        output.accept(ModItems.MITHRIL_INGOT);
                        output.accept(ModItems.JAFFA_SWORD);
                        output.accept(ModItems.GRID_LEGENDARY_SWORD);
                        output.accept(ModItems.FRACASO_SWORD);
                        output.accept(ModItems.FRACASO_BLUEPRINT);
                        output.accept(ModItems.UNAPPRAISED_BOOK);
                        output.accept(ModItems.EYE_OF_PALLALIAN);
                        output.accept(ModItems.PAGMA_RARE_BOOK);
                        output.accept(ModItems.APPRENTICE_CONTRACT);
                        output.accept(ModItems.ENHANCEMENT_STONE);
                        output.accept(ModItems.RESPEC_SCROLL);
                        output.accept(ModItems.BLACKSMITH_SPAWN_EGG);
                        output.accept(ModItems.VILLAGE_CHIEF_SPAWN_EGG);
                        output.accept(ModItems.BONE_LORD_SPAWN_EGG);
                        // One book per skill defined in the loaded datapacks.
                        parameters.holders().lookup(ModDatapackRegistries.SKILL).ifPresent(skills -> skills.listElementIds()
                                .forEach(key -> output.accept(SkillBookItem.forSkill(ModItems.SKILL_BOOK.get(), key.location()))));
                    })
                    .build());

    /** Also list our items in the matching vanilla tabs. */
    public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.JAFFA_SWORD);
            event.accept(ModItems.GRID_LEGENDARY_SWORD);
            event.accept(ModItems.FRACASO_SWORD);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RAW_JAFFA);
            event.accept(ModItems.JAFFA_INGOT);
            event.accept(ModItems.RAW_BLUE_ORICHALCUM);
            event.accept(ModItems.BLUE_ORICHALCUM_INGOT);
            event.accept(ModItems.RAW_MITHRIL);
            event.accept(ModItems.MITHRIL_INGOT);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.JAFFA_ORE);
            event.accept(ModItems.DEEPSLATE_JAFFA_ORE);
            event.accept(ModItems.BLUE_ORICHALCUM_ORE);
            event.accept(ModItems.DEEPSLATE_BLUE_ORICHALCUM_ORE);
            event.accept(ModItems.MITHRIL_ORE);
            event.accept(ModItems.DEEPSLATE_MITHRIL_ORE);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.BLACKSMITH_SPAWN_EGG);
            event.accept(ModItems.VILLAGE_CHIEF_SPAWN_EGG);
            event.accept(ModItems.BONE_LORD_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.SMITHING_ANVIL);
            event.accept(ModItems.SMELTER);
            event.accept(ModItems.GOLDEN_TABLE);
        }
    }

    private ModCreativeTabs() {
    }
}
