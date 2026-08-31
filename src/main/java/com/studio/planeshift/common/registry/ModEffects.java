package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Power-up auras used for the visual player hue and for gameplay effects.
 *
 * <p>The visual auras are handled as mob effects; Star Power also grants
 * invincibility and a massive attack boost.
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT, PlaneShift.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FIRE_AURA =
            EFFECTS.register("fire_aura", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFF6633) {});

    public static final DeferredHolder<MobEffect, MobEffect> ICE_AURA =
            EFFECTS.register("ice_aura", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x88CCFF) {});

    public static final DeferredHolder<MobEffect, MobEffect> FROZEN =
            EFFECTS.register("frozen", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.HARMFUL, 0xCCFFFF) {};
                effect.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        PlaneShift.id("frozen_move"), -0.09D, AttributeModifier.Operation.ADD_VALUE);
                effect.addAttributeModifier(Attributes.JUMP_STRENGTH,
                        PlaneShift.id("frozen_jump"), -0.4D, AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> LEAF_AURA =
            EFFECTS.register("leaf_aura", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x55AA00) {});

    public static final DeferredHolder<MobEffect, MobEffect> STAR_POWER =
            EFFECTS.register("star_power", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFDD00) {};
                effect.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                        PlaneShift.id("star_power"), 10_000_000.0D, AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> MEGA_AURA =
            EFFECTS.register("mega_aura", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0xFF66AA) {};
                effect.addAttributeModifier(Attributes.SCALE,
                        PlaneShift.id("mega_scale"), 1.0D, AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> MINI_AURA =
            EFFECTS.register("mini_aura", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0x9AF0A8) {};
                effect.addAttributeModifier(Attributes.SCALE,
                        PlaneShift.id("mini_scale"), -0.6D, AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> PROPELLER_AURA =
            EFFECTS.register("propeller_aura", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFD700) {};
                effect.addAttributeModifier(Attributes.JUMP_STRENGTH,
                        PlaneShift.id("propeller_jump"), 0.2D, AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> ACORN_AURA =
            EFFECTS.register("acorn_aura", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0xCC8833) {};
                effect.addAttributeModifier(Attributes.JUMP_STRENGTH,
                        PlaneShift.id("acorn_jump"), 0.15D, AttributeModifier.Operation.ADD_VALUE);
                effect.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        PlaneShift.id("acorn_speed"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                return effect;
            });

    public static final DeferredHolder<MobEffect, MobEffect> CLOUD_AURA =
            EFFECTS.register("cloud_aura", () -> {
                MobEffect effect = new MobEffect(MobEffectCategory.BENEFICIAL, 0xAADDFF) {};
                effect.addAttributeModifier(Attributes.JUMP_STRENGTH,
                        PlaneShift.id("cloud_jump"), 0.15D, AttributeModifier.Operation.ADD_VALUE);
                effect.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        PlaneShift.id("cloud_speed"), 0.08D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                return effect;
            });

    private ModEffects() {
    }
}
