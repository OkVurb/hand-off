package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ice Flower's bouncing ice ball. Freezes targets on impact.
 */
public class IceballProjectile extends ThrowableItemProjectile {

    private static final int MAX_LIFETIME_TICKS = 80;

    public IceballProjectile(EntityType<? extends IceballProjectile> type, Level level) {
        super(type, level);
    }

    public IceballProjectile(Level level, LivingEntity owner) {
        super(ModEntities.ICEBALL.get(), owner, level, new ItemStack(ModItems.ICE_FLOWER.get()));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        Entity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            ProjectileTracker.add(this, owner.getUUID(), IceballProjectile.class);
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
        return ModItems.ICE_FLOWER.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            Entity target = hit.getEntity();
            target.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 3.0F);
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModEffects.FROZEN, 60, 0, false, false, true));
            }
            spawnHitEffects();
            discard();
        }
    }

    private void spawnHitEffects() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.ICESHOT.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide() && hit.getType() == HitResult.Type.BLOCK) {
            spawnHitEffects();
            discard();
        }
    }
}
