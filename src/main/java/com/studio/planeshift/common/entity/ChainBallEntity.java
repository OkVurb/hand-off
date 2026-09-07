package com.studio.planeshift.common.entity;

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
 * A spiked ball swinging on a chain, and the arc it swings through.
 *
 * <p>The last of the four telegraph forms the reference uses, and the reason {@code Telegraph.arc}
 * was written before anything needed it. A firebar shows reach as a circle and a saw shows path as
 * a line; a pendulum shows neither, because its reach is a circle it only travels part of. Drawing
 * the full disc would claim ground the ball never visits, and drawing the chord would put the
 * warning where the ball is not. Only the arc is true.
 *
 * <p>The entity sits at the pivot and the ball hangs below it. That is worth stating because it is
 * the opposite of how it reads: the dangerous part is not where the entity is, and anything
 * reasoning about position -- damage, particles, the telegraph -- has to go through
 * {@link #ballPos}.
 *
 * <p>Swings on a sine, so it is slowest at the extremes and fastest through the bottom. That is
 * what a real pendulum does and it is also the whole game of the hazard: the safe moment is at the
 * ends, where it lingers, and the bottom is the part that must be crossed between passes.
 */
public class ChainBallEntity extends Entity {

    /** Chain length in blocks, synced so the client draws the ball in the same place. */
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(ChainBallEntity.class, EntityDataSerializers.FLOAT);

    /** Half the swing, in degrees either side of straight down. */
    private static final EntityDataAccessor<Float> SWEEP =
            SynchedEntityData.defineId(ChainBallEntity.class, EntityDataSerializers.FLOAT);

    private static final float DEFAULT_LENGTH = 4.0F;

    /**
     * Default half-sweep.
     *
     * <p>Sixty degrees either side. Much less and the ball barely leaves the bottom, which reads as
     * a stuck decoration; much more and it spends most of its time overhead where the player cannot
     * see it coming down at them.
     */
    private static final float DEFAULT_SWEEP = 60.0F;

    /** Radians per tick of the driving sine. One full swing takes roughly five seconds. */
    private static final double RATE = 0.06D;

    private static final double HIT_RADIUS = 0.9D;
    private static final float CONTACT_DAMAGE = 4.0F;

    private int tickOffset;

    public ChainBallEntity(EntityType<? extends ChainBallEntity> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(200);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LENGTH, DEFAULT_LENGTH);
        builder.define(SWEEP, DEFAULT_SWEEP);
    }

    public float chainLength() {
        return entityData.get(LENGTH);
    }

    public void setChainLength(float length) {
        entityData.set(LENGTH, Math.max(1.0F, length));
    }

    public float sweep() {
        return entityData.get(SWEEP);
    }

    public void setSweep(float degrees) {
        entityData.set(SWEEP, Math.clamp(degrees, 5.0F, 170.0F));
    }

    /**
     * The current angle of the chain, in degrees, measured from straight down.
     *
     * <p>Zero hangs vertically; positive swings one way. Straight down is 270 degrees in the
     * standard-position convention the arc drawing uses, which is why the two differ by that
     * quarter turn wherever they meet.
     */
    private double angleDegrees() {
        return Math.sin((tickCount + tickOffset) * RATE) * sweep();
    }

    /**
     * Offset from pivot to ball for a chain angle, measured from straight down.
     *
     * <p>Static and pure so the one genuinely error-prone part of this entity can be checked. The
     * chain measures from straight down and {@link Telegraph#arc} measures anticlockwise from east;
     * getting that quarter turn wrong draws the warning somewhere the ball never goes, which is
     * worse than drawing nothing, because the player would learn to trust it.
     */
    static Vec3 ballOffset(double chainAngleDegrees, double length) {
        double a = Math.toRadians(chainAngleDegrees);
        return new Vec3(Math.sin(a) * length, -Math.cos(a) * length, 0.0D);
    }

    /** The standard-position angle, in degrees, for a chain angle measured from straight down. */
    static double standardAngle(double chainAngleDegrees) {
        return ARC_CENTRE + chainAngleDegrees;
    }

    /** Straight down, in the anticlockwise-from-east convention {@link Telegraph#arc} uses. */
    private static final double ARC_CENTRE = 270.0D;

    /** Where the ball actually is. Not where the entity is -- that is the pivot. */
    public Vec3 ballPos() {
        return position().add(ballOffset(angleDegrees(), chainLength()));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            spawnChain();
            return;
        }
        hurtTouching();
    }

    /** The chain itself, drawn as beads from pivot to ball. */
    private void spawnChain() {
        Vec3 pivot = position();
        Vec3 ball = ballPos();
        int beads = Math.max(2, (int) chainLength() * 2);
        for (int i = 1; i <= beads; i++) {
            Vec3 at = pivot.lerp(ball, (double) i / beads);
            level().addParticle(net.minecraft.core.particles.ParticleTypes.CRIT,
                    at.x, at.y, at.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private void hurtTouching() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 ball = ballPos();
        AABB box = new AABB(ball.x - HIT_RADIUS, ball.y - HIT_RADIUS, ball.z - HIT_RADIUS,
                ball.x + HIT_RADIUS, ball.y + HIT_RADIUS, ball.z + HIT_RADIUS);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            player.hurtServer(serverLevel, damageSources().generic(), CONTACT_DAMAGE);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setChainLength(input.getFloatOr("ChainLength", DEFAULT_LENGTH));
        setSweep(input.getFloatOr("Sweep", DEFAULT_SWEEP));
        tickOffset = input.getIntOr("TickOffset", this.random.nextInt(200));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("ChainLength", chainLength());
        output.putFloat("Sweep", sweep());
        output.putInt("TickOffset", tickOffset);
    }

    /** Timed, not fought. Same bargain as the saw: the drawn arc is the game keeping its side. */
    @Override
    public boolean hurtServer(ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
