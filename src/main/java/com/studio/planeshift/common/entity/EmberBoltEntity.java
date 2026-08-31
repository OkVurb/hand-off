package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Ember Core's projectile (Design Bible, "Offensive power Forms").
 *
 * <p>"Launch arcing ember; ignites tagged targets. Lights braziers; burns thorn seals.
 * Limits: two active shots; heat recovery." The active-shot cap and cooldown live in
 * {@code FormService}; this entity owns flight, impact and the brazier interaction
 * (vanilla campfires stand in for braziers until the original art pass).
 */
public class EmberBoltEntity extends ThrowableItemProjectile {

    /** Bounded lifetime so a lost bolt frees the player's shot budget quickly. */
    private static final int MAX_LIFETIME_TICKS = 60;

    public EmberBoltEntity(EntityType<? extends EmberBoltEntity> type, Level level) {
        super(type, level);
    }

    public EmberBoltEntity(Level level, LivingEntity owner) {
        super(ModEntities.EMBER_BOLT.get(), owner, level, new ItemStack(ModItems.EMBER_CHARM.get()));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        Entity owner = getOwner();
        if (!level().isClientSide() && owner != null) {
            ProjectileTracker.add(this, owner.getUUID(), EmberBoltEntity.class);
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
        return ModItems.EMBER_CHARM.get();
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
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level().getBlockState(pos);
        // Brazier interaction: light an unlit campfire.
        if (state.getBlock() instanceof CampfireBlock
                && !state.getValue(CampfireBlock.LIT)
                && CampfireBlock.canLight(state)) {
            level().setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
            level().playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            level().playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F, 1.4F);
            discard();
        }
    }
}
