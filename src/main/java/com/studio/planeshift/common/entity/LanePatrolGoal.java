package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private Direction lane;

    public LanePatrolGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
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
        lane = mob.getDirection();
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

    private boolean shouldTurn() {
        if (mob.horizontalCollision) {
            return true;
        }
        // Ledge check: is there ground one block ahead (at foot level or one below)?
        BlockPos footAhead = mob.blockPosition().relative(lane);
        return mob.level().isEmptyBlock(footAhead.below())
                && mob.level().isEmptyBlock(footAhead.below(2));
    }
}
