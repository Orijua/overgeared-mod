package com.orijua.overgearedmod.progression;

import com.orijua.overgearedmod.Overgearedmod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Maps stats onto vanilla attributes (project design). Intelligence has no vanilla attribute:
 * it drives max mana instead, see {@link ProgressionRules#maxMana}.
 */
public final class StatEffects {
    private record Mapping(StatType stat, Holder<Attribute> attribute, double perPoint, AttributeModifier.Operation operation) {
        ResourceLocation id() {
            return Overgearedmod.id("stat_" + stat.getSerializedName());
        }
    }

    private static final Mapping[] MAPPINGS = {
            new Mapping(StatType.STRENGTH, Attributes.ATTACK_DAMAGE, 0.25, AttributeModifier.Operation.ADD_VALUE),
            new Mapping(StatType.STAMINA, Attributes.MAX_HEALTH, 0.5, AttributeModifier.Operation.ADD_VALUE),
            new Mapping(StatType.AGILITY, Attributes.MOVEMENT_SPEED, 0.004, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Mapping(StatType.DEXTERITY, Attributes.ATTACK_SPEED, 0.01, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    };

    private StatEffects() {
    }

    /** Idempotent: safe to call on login, respawn and every stat change. */
    public static void apply(Player player, PlayerStats stats) {
        for (Mapping mapping : MAPPINGS) {
            AttributeInstance instance = player.getAttribute(mapping.attribute());
            if (instance == null) {
                continue;
            }
            int points = stats.get(mapping.stat());
            if (points <= 0) {
                instance.removeModifier(mapping.id());
            } else {
                instance.addOrReplacePermanentModifier(
                        new AttributeModifier(mapping.id(), points * mapping.perPoint(), mapping.operation()));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
