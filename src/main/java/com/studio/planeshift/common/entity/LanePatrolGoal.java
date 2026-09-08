package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * "Patrols; turns at ledge" (Design Bible, "Ground enemy archetypes" — Goomba, Koopa, Spiny, Buzzy Beetle).
 *
 * <p>2.5D-aware navigation rule: "enemies remain on authored plane lanes unless their
 * archetype explicitly changes depth." The goal walks a straight horizontal lane and
 * flips direction on walls and ledges, using only bounded local block queries.
 */
public class LanePatrolGoal extends Goal {

    private final PathfinderMob mob;
    private final double speedModifier;
    private final boolean turnsAtLedge;
    private Direction lane;

    public LanePatrolGoal(PathfinderMob mob, double speedModifier) {
        this(mob, speedModifier, true);
    }

    public LanePatrolGoal(PathfinderMob mob, double speedModifier, boolean turnsAtLedge) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.turnsAtLedge = turnsAtLedge;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.onGround() && lane != null;
    }

    @Override
    public void start() {
        lane = alongTheLane(mob.getDirection());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (lane == null) {
            lane = mob.getDirection();
        }
        if (shouldTurn()) {
            lane = lane.getOpposite();
        }
        Vec3 ahead = mob.position().add(Vec3.atLowerCornerOf(lane.getUnitVec3i()).scale(2.0D));
        mob.getMoveControl().setWantedPosition(ahead.x, mob.getY(), ahead.z, speedModifier);
    }

    /**
     * Forces a patrol direction onto the axis the course actually runs along.
     *
     * <p>The lane runs east-west and is three blocks deep; this goal used to take its heading
     * straight from {@code mob.getDirection()}, which is whatever yaw the thing was spawned with.
     * Seven spawns in the segment library pass a yaw of zero, which is *south* — so those enemies
     * set off across the corridor, hit its side after one block, turned, hit the other side, and
     * spent their whole lives oscillating in a one-block space. They never patrolled, and a group
     * of them ends up in a heap, which is exactly what a playtest showed.
     *
     * <p>Fixed here rather than at the seven call sites, because the call sites are not wrong about
     * anything except a number: a goal that patrols a lane should decide what "along" means, and
     * then no future spawn can get it wrong either.
     *
     * <p>Which way it faces when it has to choose is taken from its block position rather than a
     * random, so a course generates identically every time — the same rule the rest of generation
     * follows.
     */
    private Direction alongTheLane(Direction spawned) {
        if (spawned.getAxis() == Direction.Axis.X) {
            return spawned;
        }
        return (mob.blockPosition().getX() & 1) == 0 ? Direction.EAST : Direction.WEST;
    }

    private boolean shouldTurn() {
        if (mob.horizontalCollision) {
            return true;
        }
        if (blockedByAnother()) {
            return true;
        }
        if (!turnsAtLedge) {
            return false;
        }
        // Ledge check: is there ground one block ahead (at foot level or one below)?
        BlockPos footAhead = mob.blockPosition().relative(lane);
        return mob.level().isEmptyBlock(footAhead.below())
                && mob.level().isEmptyBlock(footAhead.below(2));
    }

    /**
     * Whether another patroller is directly ahead.
     *
     * <p>Without this they pile up, and they pile up for a structural reason rather than a random
     * one: the lane is three blocks wide and everything in it walks along the same axis, so a
     * faster enemy behind a slower one has nowhere to go. Minecraft's own push-apart then spreads
     * them sideways into the walls and upward onto each other's heads, and a course that was
     * seeded with six enemies across a hundred blocks ends up with six enemies in one square metre.
     *
     * <p>Turning is the reference's answer and it is also the only one that keeps them spread: two
     * that meet head-on both reverse and walk away from each other, and one that catches up with a
     * slower one turns and patrols the other way. Nothing needs to know how fast anything else is.
     *
     * <p>Only the lane axis is searched, and only two blocks of it. A radius check would turn an
     * enemy away from something on a ledge above it that it was never going to touch.
     */
    private boolean blockedByAnother() {
        Vec3 step = Vec3.atLowerCornerOf(lane.getUnitVec3i()).scale(1.1D);
        net.minecraft.world.phys.AABB ahead = mob.getBoundingBox().move(step).inflate(0.05D);
        for (Entity other : mob.level().getEntities(mob, ahead,
                e -> e instanceof PathfinderMob && e.isAlive())) {
            // Only things that walk the lane. A shell sliding through is supposed to hit them.
            if (other instanceof CourseEnemyEntity enemy && !enemy.isPassenger()) {
                return true;
            }
        }
        return false;
    }
}
