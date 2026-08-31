package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Bowser's straight-line fire breath. Heavy, ignores gravity, ignites targets.
 */
public class BowserFire extends ThrowableItemProjectile {

    private static final int MAX_LIFETIME_TICKS = 60;

    public BowserFire(EntityType<? extends BowserFire> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BowserFire(Level level, LivingEntity owner) {
        super(ModEntities.BOWSER_FIRE.get(), owner, level, new ItemStack(ModItems.FIRE_FLOWER.get()));
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIRE_FLOWER.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.FLAME,
                        getX(), getY() + 0.3D, getZ(),
                        (random.nextDouble() - 0.5D) * 0.1D,
                        (random.nextDouble() - 0.5D) * 0.1D,
                        (random.nextDouble() - 0.5D) * 0.1D);
            }
        } else if (tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            Entity target = hit.getEntity();
            target.igniteForSeconds(4.0F);
            target.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 6.0F);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            if (level() instanceof ServerLevel level) {
                level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.6F, 1.0F);
            }
            discard();
        }
    }
}
