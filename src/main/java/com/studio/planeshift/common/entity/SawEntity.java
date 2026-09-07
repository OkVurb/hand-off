package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A buzzsaw that runs a straight track and shows the track before it gets there.
 *
 * <p>The route is readable because the track is really there: the segment lays a line of rail
 * blocks the saw runs along, so the player sees where it goes for the same reason they can see a
 * wall. An earlier version drew the path in particles instead, which said the same thing in a
 * visual language nothing else in the game uses -- the fix was to build the track, not to draw it.
 *
 * <p>It is the third hazard here and the first that threatens a <em>line</em> rather than a point or
 * a radius. A Thwomp owns the column under it and a firebar owns a disc around it; a saw owns a
 * corridor the player has to cross, which is why the telegraph matters more here than anywhere
 * else -- without it the only way to learn the route is to be standing on it.
 *
 * <p>Drawn as a real toothed disc. An earlier version emitted a ring of sparks instead and drew no
 * model at all, which is the right call for a fire bar -- whose flames genuinely are the thing --
 * and the wrong one here. A grinder is a solid object, and a scatter of particles where one should
 * be does not read as a hazard so much as an effect.
 */
public class SawEntity extends Entity {

    /** Track half-length in blocks, synced so the client draws the same rail. */
    private static final EntityDataAccessor<Float> RANGE =
            SynchedEntityData.defineId(SawEntity.class, EntityDataSerializers.FLOAT);

    /** 0 travels along X, 1 along Y. Vertical saws exist in the reference and read very differently. */
    private static final EntityDataAccessor<Integer> AXIS =
            SynchedEntityData.defineId(SawEntity.class, EntityDataSerializers.INT);

    private static final float DEFAULT_RANGE = 5.0F;

    /**
     * Blocks per tick.
     *
     * <p>Faster than a moving platform and slower than a projectile. A saw the player can outrun is
     * scenery; one they cannot react to is a trap, and the whole point of drawing the rail is that
     * this is meant to be a timing problem rather than a surprise.
     */
    private static final double SPEED = 0.14D;

    /** How close counts as a hit. Slightly under a block, so a near miss reads as a near miss. */
    private static final double HIT_RADIUS = 0.85D;

    private static final float CONTACT_DAMAGE = 4.0F;

    private double originX = Double.NaN;
    private double originY = Double.NaN;
    private int tickOffset;

    public SawEntity(EntityType<? extends SawEntity> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(200);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RANGE, DEFAULT_RANGE);
        builder.define(AXIS, 0);
    }

    public float range() {
        return entityData.get(RANGE);
    }

    public void setRange(float range) {
        entityData.set(RANGE, Math.max(1.0F, range));
    }

    public int axis() {
        return entityData.get(AXIS);
    }

    public void setAxis(int axis) {
        entityData.set(AXIS, axis == 1 ? 1 : 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (Double.isNaN(originX)) {
            originX = getX();
            originY = getY();
        }

        // A cosine sweep rather than a bounce between two ends. It slows at the extremes, which is
        // what a real carriage on a track does and, more usefully, gives the player a moment at
        // each end where the saw is briefly easy to pass.
        double phase = (tickCount + tickOffset) * SPEED / range();
        double offset = Math.cos(phase) * range();
        if (axis() == 1) {
            setPos(originX, originY + offset, getZ());
        } else {
            setPos(originX + offset, originY, getZ());
        }

        if (level().isClientSide()) {
            return;
        }
        hurtTouching();
    }

    /** The two ends of the track, in world space. */
    private Vec3[] trackEnds() {
        double r = range();
        return axis() == 1
                ? new Vec3[] {new Vec3(originX, originY - r, getZ()),
                              new Vec3(originX, originY + r, getZ())}
                : new Vec3[] {new Vec3(originX - r, originY, getZ()),
                              new Vec3(originX + r, originY, getZ())};
    }

    private void hurtTouching() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB box = new AABB(getX() - HIT_RADIUS, getY() - HIT_RADIUS, getZ() - HIT_RADIUS,
                getX() + HIT_RADIUS, getY() + HIT_RADIUS, getZ() + HIT_RADIUS);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            player.hurtServer(serverLevel, damageSources().generic(), CONTACT_DAMAGE);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setRange(input.getFloatOr("Range", DEFAULT_RANGE));
        setAxis(input.getIntOr("Axis", 0));
        originX = input.getDoubleOr("OriginX", Double.NaN);
        originY = input.getDoubleOr("OriginY", Double.NaN);
        tickOffset = input.getIntOr("TickOffset", this.random.nextInt(200));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Range", range());
        output.putInt("Axis", axis());
        output.putDouble("OriginX", originX);
        output.putDouble("OriginY", originY);
        output.putInt("TickOffset", tickOffset);
    }

    /**
     * Immune to everything.
     *
     * <p>A hazard the player can destroy is a puzzle with an answer the level did not intend. The
     * saw is meant to be timed, not fought, and the drawn rail is the game keeping its side of
     * that bargain.
     */
    @Override
    public boolean hurtServer(ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    /** A saw is scenery with teeth: nothing the player does should move or break it. */
    @Override
    public boolean isPickable() {
        return false;
    }

    public static EntityType<SawEntity> type() {
        return ModEntities.SAW.get();
    }
}
