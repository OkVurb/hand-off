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
import net.minecraft.world.phys.EntityHitResult;

/**
 * A thrown hammer from a Hammer Bro. Follows a gravity arc and hurts the player.
 */
public class HammerProjectile extends ThrowableItemProjectile {

    public HammerProjectile(EntityType<? extends HammerProjectile> type, Level level) {
        super(type, level);
    }

    public HammerProjectile(Level level, LivingEntity owner) {
        super(ModEntities.HAMMER.get(), owner, level, new ItemStack(ModItems.HAMMER.get()));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        Entity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            ProjectileTracker.add(this, owner.getUUID(), HammerProjectile.class);
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
        return ModItems.HAMMER.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            Entity target = hit.getEntity();
            if (target instanceof LivingEntity living) {
                living.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 4.0F);
            } else {
                target.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 4.0F);
            }
            spawnHitEffects();
            discard();
        }
    }

    private void spawnHitEffects() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.HAMMER_THROW.get(), SoundSource.HOSTILE, 0.7F, 1.0F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.15D, 0.15D, 0.15D, 0.05D);
        }
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide() && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            spawnHitEffects();
            discard();
        }
    }
}
