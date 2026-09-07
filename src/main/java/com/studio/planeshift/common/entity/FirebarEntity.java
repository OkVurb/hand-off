package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModParticles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A castle firebar: a line of fireballs rotating about a fixed anchor.
 *
 * <p>Not an AI mob. It is a moving hazard with a completely predictable path, which is the point
 * — the player is meant to read the rotation and time a run through it, so any randomness would
 * make it unfair rather than difficult.
 *
 * <p>Implemented as one entity that owns the whole bar rather than one entity per flame. The bar
 * is defined by an anchor, a length and an angular speed; contact is tested against the line
 * segment each tick and the flames are drawn along it. That keeps a six-segment firebar at one
 * entity instead of six, which matters when a castle room has several.
 */
public class FirebarEntity extends Entity {

    /** Segments in the bar, i.e. its reach in blocks from the anchor. */
    private static final EntityDataAccessor<Integer> LENGTH =
            SynchedEntityData.defineId(FirebarEntity.class, EntityDataSerializers.INT);
    /** Current rotation in degrees, synced so the client draws the same bar. */
    private static final EntityDataAccessor<Float> ANGLE =
            SynchedEntityData.defineId(FirebarEntity.class, EntityDataSerializers.FLOAT);

    /** Degrees per tick. Two full turns a minute: readable, not frantic. */
    private static final float DEGREES_PER_TICK = 3.0F;
    /** How close to the bar counts as a hit. */
    private static final double HIT_RADIUS = 0.6D;
    /** Damage per contact. The pip model turns this into one pip. */
    private static final float CONTACT_DAMAGE = 4.0F;

    private static final int DEFAULT_LENGTH = 5;

    private float spinDirection = 1.0F;

    public FirebarEntity(EntityType<? extends FirebarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LENGTH, DEFAULT_LENGTH);
        builder.define(ANGLE, 0.0F);
    }

    public int barLength() {
        return entityData.get(LENGTH);
    }

    public void setBarLength(int length) {
        entityData.set(LENGTH, Math.max(1, length));
    }

    public float angle() {
        return entityData.get(ANGLE);
    }

    /** Reverses the spin, so a castle can alternate bars and force a rhythm change. */
    public void setSpinDirection(float direction) {
        this.spinDirection = Math.signum(direction) == 0 ? 1.0F : Math.signum(direction);
    }

    @Override
    public void tick() {
        super.tick();
        float next = (angle() + DEGREES_PER_TICK * spinDirection) % 360.0F;
        entityData.set(ANGLE, next);

        if (level().isClientSide()) {
            spawnFlames(next);
            return;
        }
        hurtAlongBar(next);
    }

    /** Positions along the bar, one per segment, in world space. */
    private Vec3 segmentPos(float degrees, int segment) {
        double radians = Math.toRadians(degrees);
        // The bar sweeps the vertical plane the 2.5D lane lies in, so it reads as a wheel from
        // the side camera rather than sweeping toward and away from the player.
        double dx = Math.cos(radians) * segment;
        double dy = Math.sin(radians) * segment;
        return new Vec3(getX() + dx, getY() + dy, getZ());
    }

    private void spawnFlames(float degrees) {
        for (int segment = 1; segment <= barLength(); segment++) {
            Vec3 pos = segmentPos(degrees, segment);
            level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private void hurtAlongBar(float degrees) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int segment = 1; segment <= barLength(); segment++) {
            Vec3 pos = segmentPos(degrees, segment);
            for (Player player : level().players()) {
                if (!player.isAlive() || player.isSpectator()) {
                    continue;
                }
                if (player.position().add(0.0D, player.getBbHeight() / 2.0D, 0.0D)
                        .distanceToSqr(pos) <= HIT_RADIUS * HIT_RADIUS) {
                    player.hurtServer(serverLevel, damageSources().inFire(), CONTACT_DAMAGE);
                }
            }
            if (tickCount % 4 == 0) {
                serverLevel.sendParticles(ModParticles.HIT_BURST.get(),
                        pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(LENGTH, input.getIntOr("BarLength", DEFAULT_LENGTH));
        entityData.set(ANGLE, input.getFloatOr("Angle", 0.0F));
        spinDirection = input.getFloatOr("Spin", 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("BarLength", barLength());
        output.putFloat("Angle", angle());
        output.putFloat("Spin", spinDirection);
    }

    /**
     * Indestructible. A firebar is scenery with a hitbox, not a target; letting it be destroyed
     * would let a player delete the obstacle instead of solving it.
     */
    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    /** Hazards are not attackable; the player is meant to dodge, not fight. */
    @Override
    public boolean isPickable() {
        return false;
    }
}
