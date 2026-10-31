package com.orijua.overgearedmod.validation;

import com.orijua.overgearedmod.equipment.option.ItemOptionDefinition;
import com.orijua.overgearedmod.goal.DeliverGoal;
import com.orijua.overgearedmod.npc.BlacksmithNpc;
import com.orijua.overgearedmod.playerclass.PlayerClassDefinition;
import com.orijua.overgearedmod.quest.QuestDefinition;
import com.orijua.overgearedmod.registry.ModDataComponents;
import com.orijua.overgearedmod.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.registry.ModEntities;
import com.orijua.overgearedmod.registry.ModRecipes;
import com.orijua.overgearedmod.smithing.ForgingExtras;
import com.orijua.overgearedmod.smithing.ForgingRecipe;
import com.orijua.overgearedmod.title.TitleDefinition;
import com.orijua.overgearedmod.validation.ContentIndex.Kind;
import com.orijua.overgearedmod.validation.ContentIndex.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the loaded registries into a {@link ContentIndex}: the Minecraft side of the content validator. */
public final class ContentScanner {
    private ContentScanner() {
    }

    public static ContentIndex index(RegistryAccess access, RecipeManager recipes) {
        Set<ResourceLocation> advancedClasses = new HashSet<>();
        Set<ResourceLocation> rewardedClasses = new HashSet<>();
        Map<ResourceLocation, ResourceLocation> questRequirements = new HashMap<>();
        Set<ResourceLocation> visibleClassQuests = new HashSet<>();
        List<Reference> references = new ArrayList<>();
        Map<ResourceLocation, ResourceLocation> questGivers = new HashMap<>();
        Set<String> deliveriesWithoutGiver = new HashSet<>();

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
            quest.requiresQuest().ifPresent(id -> {
                references.add(new Reference(from, Kind.QUEST, id));
                questRequirements.put(entry.getKey(), id);
            });
            quest.classReward().ifPresent(id -> {
                references.add(new Reference(from, Kind.CLASS, id));
                rewardedClasses.add(id);
                if (!quest.isHidden()) {
                    visibleClassQuests.add(entry.getKey());
                }
            });
            for (ItemStack reward : quest.itemRewards()) {
                ResourceLocation skill = reward.get(ModDataComponents.SKILL_BOOK);
                if (skill != null) {
                    references.add(new Reference(from, Kind.SKILL, skill));
                }
            }
            quest.giver().ifPresent(giver -> questGivers.put(entry.getKey(), BuiltInRegistries.ENTITY_TYPE.getKey(giver)));
            if (quest.goal() instanceof DeliverGoal && quest.giver().isEmpty()) {
                deliveriesWithoutGiver.add(from);
            }
            if (quest.discoverWhen().filter(DeliverGoal.class::isInstance).isPresent()) {
                deliveriesWithoutGiver.add(from + " (discover_when)");
            }
        }
        for (Map.Entry<ResourceLocation, TitleDefinition> entry : entries(access, ModDatapackRegistries.TITLE)) {
            if (entry.getValue().goal() instanceof DeliverGoal) {
                deliveriesWithoutGiver.add("title " + entry.getKey());
            }
        }
        for (Map.Entry<ResourceLocation, ItemOptionDefinition> entry : entries(access, ModDatapackRegistries.ITEM_OPTION)) {
            String from = "item option " + entry.getKey();
            entry.getValue().effect().grantedSkill().ifPresent(skill -> references.add(new Reference(from, Kind.SKILL, skill)));
        }
        for (RecipeHolder<ForgingRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FORGING.get())) {
            String from = "forging recipe " + holder.id();
            ForgingExtras extras = holder.value().extras();
            extras.requiredClass().ifPresent(id -> references.add(new Reference(from, Kind.CLASS, id)));
            extras.fixedOptions().forEach(option -> references.add(new Reference(from, Kind.OPTION, option.option())));
        }
        BlacksmithNpc.SOLD_SKILLS.forEach(skill -> references.add(new Reference("blacksmith trades", Kind.SKILL, skill)));
        Set<ResourceLocation> questGiverNpcs = new HashSet<>();
        ModEntities.questGivers().forEach(type -> questGiverNpcs.add(BuiltInRegistries.ENTITY_TYPE.getKey(type)));

        return new ContentIndex(ids(access, ModDatapackRegistries.SKILL), ids(access, ModDatapackRegistries.PLAYER_CLASS),
                ids(access, ModDatapackRegistries.QUEST), advancedClasses, rewardedClasses, questRequirements, visibleClassQuests, references,
                questGivers, questGiverNpcs, deliveriesWithoutGiver, ids(access, ModDatapackRegistries.ITEM_OPTION));
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
