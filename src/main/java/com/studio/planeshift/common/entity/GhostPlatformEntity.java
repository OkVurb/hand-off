package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A platform carried by a ghost.
 *
 * <p>The sheets show ghost-house platforms held up by Boos, and the note taken from them was that
 * a moving platform does not have to be a block. Checking that against the code turned the finding
 * inside out: {@link MovingPlatformEntity} already extends {@code Mob}, so structurally the mod had
 * platforms-as-creatures from the start. What was missing was that any of them <em>looked</em> like
 * one. The gap was presentation, not architecture.
 *
 * <p>So this changes exactly that and nothing else. Movement is inherited untouched, which is not
 * laziness but the safety property: {@code CourseCanvas.movingSurface} declares the band a platform
 * sweeps so the reachability proof knows a pit is crossable, and a subclass that moved differently
 * would make every one of those declarations a lie. Identical motion means the existing proof still
 * applies word for word.
 *
 * <p>The ghost is drawn as a drift of particles below the deck rather than as a model. It reads at
 * this camera distance, costs no rig, and cannot fall out of step with the platform it is under.
 */
public class GhostPlatformEntity extends MovingPlatformEntity {

    public GhostPlatformEntity(EntityType<? extends MovingPlatformEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        // A sag of pale motes under the deck, wider than the platform so it reads as something
        // holding the platform up rather than as an effect painted on it.
        for (int i = 0; i < 3; i++) {
            double dx = (random.nextDouble() - 0.5D) * 2.6D;
            double dy = -0.35D - random.nextDouble() * 0.45D;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + dx, getY() + dy, getZ(), 0.0D, -0.01D, 0.0D);
        }
    }
}
