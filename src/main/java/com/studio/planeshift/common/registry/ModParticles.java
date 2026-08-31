package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types for pickups, hits, respawn, and course themes.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, PlaneShift.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COIN_SPARKLE =
            PARTICLES.register("coin_sparkle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PICKUP_GLOW =
            PARTICLES.register("pickup_glow", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HIT_BURST =
            PARTICLES.register("hit_burst", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESPAWN_WARP =
            PARTICLES.register("respawn_warp", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THEME_DUST =
            PARTICLES.register("theme_dust", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
