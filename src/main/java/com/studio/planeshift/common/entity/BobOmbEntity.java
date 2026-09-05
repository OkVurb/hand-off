package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.block.BrickBlock;
import com.studio.planeshift.common.block.HitFromBelowBlock;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Walks, gets lit, panics, and takes the wall down with it.
 *
 * <p>The first enemy in the roster that changes the level rather than only itself. A Goomba is a
 * thing in your way; a Bob-omb is a tool you have to place, because the explosion breaks bricks and
 * kills everything nearby including other enemies. That turns a stretch of course into a small
 * puzzle — where do I want this to go off — without adding a single control.
 *
 * <p>Stomping lights the fuse rather than defeating it, which is the same shape as the Paratroopa's
 * wings and Dry Bones' collapse. Three enemies now share that pattern and none of them share code,
 * deliberately: each is one creature's identity, and a general "first hit does something else" hook
 * on {@code CourseEnemyEntity} would put a field on the eleven enemies that do not want one.
 */
public class BobOmbEntity extends CourseEnemyEntity {

    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(BobOmbEntity.class, EntityDataSerializers.INT);

    /**
     * How long the fuse burns.
     *
     * <p>Two and a half seconds. Long enough to run away from or to herd toward something, short
     * enough that lighting one is a commitment — if the player can comfortably light it and then
     * do something else entirely, it stops being a decision.
     */
    public static final int FUSE_TICKS = 50;

    /** Blast radius. Deliberately small: this is a tool, not an area denial weapon. */
    private static final double BLAST_RADIUS = 3.5D;

    private static final float BLAST_DAMAGE = 12.0F;

    public BobOmbEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LanePatrolGoal(this, 1.0D, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE, 0);
    }

    /** Lit and counting down. */
    public boolean lit() {
        return entityData.get(FUSE) > 0;
    }

    /**
     * How fast it should be flashing, 0 to 1.
     *
     * <p>Accelerating rather than constant. A steady blink tells the player it is dangerous; a
     * blink that speeds up tells them how long they have, which is the only information that
     * actually changes their decision.
     */
    public float urgency() {
        int fuse = entityData.get(FUSE);
        return fuse <= 0 ? 0.0F : 1.0F - fuse / (float) FUSE_TICKS;
    }

    /** Lights the fuse. Safe to call on an already-lit bomb; it does not restart the countdown. */
    public void light() {
        if (lit() || level().isClientSide()) {
            return;
        }
        entityData.set(FUSE, FUSE_TICKS);
        level().playSound(null, blockPosition(), ModSounds.FIREBALL.get(),
                SoundSource.HOSTILE, 0.8F, 1.6F);
    }

    /**
     * A stomp lights it instead of killing it.
     *
     * <p>Returning false leaves it alive so the player still gets a bounce — and, more to the
     * point, so they are standing next to a lit bomb, which is the moment the enemy exists to
     * create.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!lit() && source.getEntity() instanceof Player) {
            light();
            startSquish();
            return false;
        }
        // Anything else - a shell, a fireball, another Bob-omb - sets it off immediately.
        if (!lit()) {
            light();
            entityData.set(FUSE, 1);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (lit() && random.nextFloat() < 0.3F + urgency() * 0.5F) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.7D, getZ(),
                        0.0D, 0.03D, 0.0D);
            }
            return;
        }

        int fuse = entityData.get(FUSE);
        if (fuse <= 0) {
            return;
        }
        entityData.set(FUSE, fuse - 1);
        if (fuse == 1) {
            explode();
        }
    }

    /**
     * The blast.
     *
     * <p>Does three things, in the order that matters: kills nearby enemies, breaks bricks, and
     * only then hurts the player. The ordering is not cosmetic — if the player check came first, a
     * Bob-omb that killed the player would never get round to opening the wall they died trying to
     * open, and the run would end with no evidence of what it was for.
     */
    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5D), getZ(),
                8, 0.6D, 0.4D, 0.6D, 0.0D);
        server.playSound(null, blockPosition(), ModSounds.BRICK_BREAK.get(),
                SoundSource.HOSTILE, 1.4F, 0.6F);

        for (CourseEnemyEntity other : server.getEntitiesOfClass(CourseEnemyEntity.class,
                new AABB(position(), position()).inflate(BLAST_RADIUS),
                e -> e != this && e.isAlive())) {
            other.invulnerableTime = 0;
            other.hurtServer(server, damageSources().mobAttack(this), BLAST_DAMAGE);
        }

        // Bricks in reach come down. This is the reason a Bob-omb is worth herding somewhere.
        BlockPos centre = blockPosition();
        int reach = (int) Math.ceil(BLAST_RADIUS) - 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos at = centre.offset(dx, dy, dz);
                    if (server.getBlockState(at).getBlock() instanceof BrickBlock) {
                        HitFromBelowBlock.impact(server, at);
                    }
                }
            }
        }

        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class,
                new AABB(position(), position()).inflate(BLAST_RADIUS * 0.6D))) {
            player.hurtServer(server, damageSources().mobAttack(this), 2.0F);
        }

        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", entityData.get(FUSE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(FUSE, input.getIntOr("Fuse", 0));
    }
}
