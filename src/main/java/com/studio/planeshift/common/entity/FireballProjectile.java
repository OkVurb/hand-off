package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

/**
 * Fire Flower's bouncing fireball. Ignites targets on impact.
 */
public class FireballProjectile extends ThrowableItemProjectile {

    private static final int MAX_LIFETIME_TICKS = 80;
    /** How many ground bounces before the fireball burns out. */
    private static final int MAX_BOUNCES = 4;
    /** Fraction of vertical speed kept per bounce, so the arc decays. */
    private static final double BOUNCE_RESTITUTION = 0.72D;
    /** Fraction of horizontal speed kept per bounce. */
    private static final double HORIZONTAL_KEEP = 0.94D;

    private int bounces;

    public FireballProjectile(EntityType<? extends FireballProjectile> type, Level level) {
        super(type, level);
    }

    public FireballProjectile(Level level, LivingEntity owner) {
        super(ModEntities.FIREBALL.get(), owner, level, new ItemStack(ModItems.FIRE_FLOWER.get()));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        Entity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            ProjectileTracker.add(this, owner.getUUID(), FireballProjectile.class);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        if (!level().isClientSide()) {
            ProjectileTracker.remove(this);
        }
        super.onRemovedFromLevel();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIRE_FLOWER.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            Entity target = hit.getEntity();
            target.igniteForSeconds(3.0F);
            target.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 4.0F);
            spawnHitEffects();
            discard();
        }
    }

    private void spawnHitEffects() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.FIREBALL.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            level.sendParticles(ModParticles.HIT_BURST.get(), getX(), getY(), getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
        }
    }

    /**
     * Block contact. A fireball bounces along the ground rather than dying on the first surface,
     * which is what lets it travel ahead of the player and clear a lane. It only expires when it
     * runs out of bounces or hits a wall head on.
     */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide()) {
            return;
        }

        Vec3 velocity = getDeltaMovement();
        Direction face = hit.getDirection();

        if (face.getAxis().isVertical()) {
            if (bounces >= MAX_BOUNCES) {
                spawnHitEffects();
                discard();
                return;
            }
            bounces++;
            // Reflect vertically and keep most of the horizontal speed, so the arc shortens each
            // time instead of bouncing forever at the same height.
            setDeltaMovement(velocity.x * HORIZONTAL_KEEP,
                    Math.abs(velocity.y) * BOUNCE_RESTITUTION,
                    velocity.z * HORIZONTAL_KEEP);
            hurtMarked = true;
            spawnHitEffects();
            return;
        }

        // A wall stops it: bouncing off vertical faces would send fireballs back at the player.
        spawnHitEffects();
        discard();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
    }
}
