package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Boomerang projectile: flies forward then returns to the thrower, damaging enemies.
 */
public class BoomerangProjectile extends ThrowableItemProjectile {

    private static final int MAX_FORWARD_TICKS = 20;
    private static final double SPEED = 1.2;

    private boolean returning = false;
    private Entity lastHit;
    private int lastHitTick = -100;

    public BoomerangProjectile(EntityType<? extends BoomerangProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BoomerangProjectile(Level level, LivingEntity owner) {
        super(ModEntities.BOOMERANG.get(), owner, level, new ItemStack(ModItems.BOOMERANG.get()));
        setNoGravity(true);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        Entity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            ProjectileTracker.add(this, owner.getUUID(), BoomerangProjectile.class);
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
        return ModItems.BOOMERANG.get();
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (returning) {
                Entity owner = getOwner();
                if (owner == null || !owner.isAlive()) {
                    discard();
                    return;
                }
                Vec3 target = owner.position().add(0.0D, owner.getEyeHeight() / 2.0D, 0.0D);
                Vec3 toOwner = target.subtract(position());
                if (toOwner.lengthSqr() < 1.0D) {
                    discard();
                    return;
                }
                setDeltaMovement(toOwner.normalize().scale(SPEED));
            } else {
                if (tickCount > MAX_FORWARD_TICKS) {
                    returning = true;
                } else {
                    Vec3 forward = getDeltaMovement();
                    if (forward.lengthSqr() < 1.0E-4D) {
                        discard();
                        return;
                    }
                    setDeltaMovement(forward.normalize().scale(SPEED));
                }
            }
        }
        super.tick();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (owner != null && target == owner && !returning) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide()) {
            return;
        }
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            discard();
            return;
        }
        if (target == lastHit && tickCount - lastHitTick < 10) {
            return;
        }
        if (target instanceof LivingEntity living) {
            living.hurtServer((ServerLevel) level(), damageSources().thrown(this, owner), 4.0F);
        } else {
            target.hurtServer((ServerLevel) level(), damageSources().thrown(this, owner), 4.0F);
        }
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.BOOMERANG_THROW.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getY(), getZ(), 3, 0.1D, 0.1D, 0.1D, 0.05D);
        }
        lastHit = target;
        lastHitTick = tickCount;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide()) {
            returning = true;
            setDeltaMovement(getDeltaMovement().scale(-0.5D));
        }
    }
}
