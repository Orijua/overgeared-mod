package com.orijua.overgearedmod.core.registry;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.feature.mob.BleedingEffect;
import com.orijua.overgearedmod.feature.skill.BlacksmithRageEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom status effects. Apply them from JSON like any vanilla effect ("overgearedmod:bleeding"),
 * e.g. in an {@code apply_effect} item option or a skill's {@code target_effects}.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Overgearedmod.MODID);

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);
    /** The Blacksmith's Rage (canon, cap. 71): attack power +10%. */
    public static final DeferredHolder<MobEffect, BlacksmithRageEffect> BLACKSMITH_RAGE = EFFECTS.register("blacksmith_rage", BlacksmithRageEffect::new);

    /** data/overgearedmod/damage_type/bleeding.json; tagged minecraft:bypasses_armor. */
    public static final ResourceKey<DamageType> BLEEDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Overgearedmod.id("bleeding"));

    private ModEffects() {
    }
}
