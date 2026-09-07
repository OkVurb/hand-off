package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The squid: it lunges, then sinks, and the sinking is the opening.
 *
 * <p>The third movement idea in the water, after patrol and pursuit. A Blooper does not travel
 * toward the player continuously — it gathers, darts, and then drifts helplessly downward while it
 * recovers. That rhythm is the encounter: the player is not dodging a thing, they are waiting for
 * the beat where it cannot steer and swimming through it.
 *
 * <p>Written as a two-state clock rather than as pathfinding for that reason. A navigator would
 * close the distance smoothly and produce a squid that is simply slow, which is the version of
 * this enemy with no timing in it at all.
 */
public class BlooperEntity extends CourseEnemyEntity {

    /** Ticks of drifting between lunges. The window the player is actually looking for. */
    private static final int DRIFT_TICKS = 34;

    /** Ticks a lunge lasts. */
    private static final int LUNGE_TICKS = 12;

    /** Lunge speed, in blocks per tick. Fast enough that it cannot be outrun once committed. */
    private static final double LUNGE_SPEED = 0.30D;

    /** How fast it sinks while recovering. */
    private static final double SINK_SPEED = 0.035D;

    /** How far away it will bother. */
    private static final double NOTICE = 14.0D;

    private int clock;
    private Vec3 lunge = Vec3.ZERO;

    public BlooperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        // None: the whole behaviour is the clock below, and a navigator would smooth out the one
        // thing that makes it worth having.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        Player target = level().getNearestPlayer(this, NOTICE);
        clock++;

        if (clock <= LUNGE_TICKS) {
            setDeltaMovement(lunge);
        } else {
            // Sinking, and unable to steer. This is the half the player swims through.
            setDeltaMovement(0.0D, -SINK_SPEED, 0.0D);
            if (clock >= LUNGE_TICKS + DRIFT_TICKS) {
                clock = 0;
                lunge = target == null ? Vec3.ZERO
                        : target.position().add(0.0D, 0.4D, 0.0D)
                                .subtract(position()).normalize().scale(LUNGE_SPEED);
            }
        }
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }

    /** Never on the floor. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }
}
