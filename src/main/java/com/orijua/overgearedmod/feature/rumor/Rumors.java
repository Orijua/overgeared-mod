package com.orijua.overgearedmod.feature.rumor;

import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.QuestService;
import com.orijua.overgearedmod.feature.quest.goal.MilestoneGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The rumors Smith, the village chief and Earl Ashur drop in their window. Works on both sides with synced data (the
 * rumor registry, the player's quests and counters), so the window shows the same rumor the server writes down in the
 * player's Journal ({@link ModAttachments#HEARD_RUMORS}).
 */
public final class Rumors {
    private Rumors() {
    }

    /** What {@code giver} could tell {@code player}: its rumors that are unlocked and not yet solved, sorted by id. */
    public static List<Definitions.Entry<RumorDefinition>> open(Player player, EntityType<?> giver) {
        PlayerQuests quests = QuestService.get(player);
        return Definitions.all(player.level(), ModDatapackRegistries.RUMOR).stream()
                .filter(entry -> entry.value().isToldBy(giver))
                .filter(entry -> entry.value().requiresQuest().map(quests::isCompleted).orElse(true))
                .filter(entry -> !solved(player, entry.value()))
                .toList();
    }

    /** Today's rumor from {@code giver}, if it has any left for this player. */
    public static Optional<Definitions.Entry<RumorDefinition>> today(Player player, EntityType<?> giver) {
        List<Definitions.Entry<RumorDefinition>> open = open(player, giver);
        int index = RumorRules.pick(open.size(), RumorRules.day(player.level().getDayTime()), player.getUUID(),
                BuiltInRegistries.ENTITY_TYPE.getKey(giver).toString());
        return index < 0 ? Optional.empty() : Optional.of(open.get(index));
    }

    /** Its secret is found: one of its quests discovered, accepted or completed, or one of its milestones reached. */
    public static boolean solved(Player player, RumorDefinition rumor) {
        PlayerQuests quests = QuestService.get(player);
        boolean byQuest = rumor.solvedBy().quests().stream()
                .anyMatch(quest -> quests.isDiscovered(quest) || quests.isActive(quest) || quests.isCompleted(quest));
        return byQuest || rumor.solvedBy().milestones().stream()
                .anyMatch(milestone -> player.getData(ModAttachments.PLAYER_COUNTERS).get(MilestoneGoal.counterKey(milestone)) > 0);
    }

    /** The rumors {@code player} has heard, oldest first. */
    public static List<ResourceLocation> heard(Player player) {
        return player.getData(ModAttachments.HEARD_RUMORS);
    }

    /** Opening {@code giver}'s window: today's rumor goes into the Journal, once. */
    public static void hear(ServerPlayer player, EntityType<?> giver) {
        today(player, giver).ifPresent(rumor -> {
            List<ResourceLocation> heard = heard(player);
            if (!heard.contains(rumor.id())) {
                List<ResourceLocation> updated = new ArrayList<>(heard);
                updated.add(rumor.id());
                player.setData(ModAttachments.HEARD_RUMORS, List.copyOf(updated));
            }
        });
    }
}
