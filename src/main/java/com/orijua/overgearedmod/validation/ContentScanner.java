package com.orijua.overgearedmod.validation;

import com.orijua.overgearedmod.npc.BlacksmithNpc;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.validation.ContentIndex.Kind;
import com.orijua.overgearedmod.validation.ContentIndex.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the loaded registries into a {@link ContentIndex}: the Minecraft side of the content validator. */
public final class ContentScanner {
    private ContentScanner() {
    }

    public static ContentIndex index(RegistryAccess access) {
        Set<ResourceLocation> advancedClasses = new HashSet<>();
        Set<ResourceLocation> rewardedClasses = new HashSet<>();
        List<Reference> references = new ArrayList<>();

        for (Map.Entry<ResourceLocation, PlayerClassDefinition> entry : entries(access, ModDatapackRegistries.PLAYER_CLASS)) {
            String from = "class " + entry.getKey();
            if (!entry.getValue().selectable()) {
                advancedClasses.add(entry.getKey());
            }
            entry.getValue().skills().forEach(skill -> references.add(new Reference(from, Kind.SKILL, skill)));
        }
        for (Map.Entry<ResourceLocation, QuestDefinition> entry : entries(access, ModDatapackRegistries.QUEST)) {
            String from = "quest " + entry.getKey();
            QuestDefinition quest = entry.getValue();
            quest.requiredClass().ifPresent(id -> references.add(new Reference(from, Kind.CLASS, id)));
            quest.classReward().ifPresent(id -> {
                references.add(new Reference(from, Kind.CLASS, id));
                rewardedClasses.add(id);
            });
            for (ItemStack reward : quest.itemRewards()) {
                ResourceLocation skill = reward.get(ModDataComponents.SKILL_BOOK);
                if (skill != null) {
                    references.add(new Reference(from, Kind.SKILL, skill));
                }
            }
        }
        BlacksmithNpc.SOLD_SKILLS.forEach(skill -> references.add(new Reference("blacksmith trades", Kind.SKILL, skill)));

        return new ContentIndex(ids(access, ModDatapackRegistries.SKILL), ids(access, ModDatapackRegistries.PLAYER_CLASS),
                advancedClasses, rewardedClasses, references);
    }

    private static <T> Set<ResourceLocation> ids(RegistryAccess access, ResourceKey<Registry<T>> key) {
        return access.registry(key).map(registry -> Set.copyOf(registry.keySet())).orElse(Set.of());
    }

    private static <T> List<Map.Entry<ResourceLocation, T>> entries(RegistryAccess access, ResourceKey<Registry<T>> key) {
        return access.registry(key)
                .map(registry -> registry.keySet().stream().map(id -> Map.entry(id, registry.get(id))).toList())
                .orElse(List.of());
    }
}
