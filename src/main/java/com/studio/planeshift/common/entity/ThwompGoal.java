package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Thwomp behavior: hover in place, slam downward when a player is directly below,
 * then slowly rise back to its home position.
 */
public class ThwompGoal extends Goal {

    private static final double DETECT_RANGE_XZ = 1.0D; // Drops only when player is directly underneath
    private static final double DETECT_RANGE_Y_DOWN = 24.0D;
    private static final double FALL_SPEED = -1.4D;
    private static final double RISE_SPEED = 0.15D;
    private static final int GROUND_COOLDOWN = 20;

    private final ThwompEntity thwomp;
    private double homeY;
    private int groundTimer = 0;
    private boolean grounded = false;

    public ThwompGoal(ThwompEntity thwomp) {
        this.thwomp = thwomp;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !thwomp.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return !thwomp.isDeadOrDying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        thwomp.setNoGravity(true);
        double saved = thwomp.getHomeY();
        this.homeY = Double.isNaN(saved) ? thwomp.getY() : saved;
        thwomp.setHomeY(this.homeY);
    }

    @Override
    public void tick() {
        if (thwomp.level().isClientSide()) {
            return;
        }

        if (groundTimer > 0) {
            groundTimer--;
            if (groundTimer == 0) {
                grounded = false;
            }
            return;
        }

        boolean onGroundNow = thwomp.onGround();
        if (onGroundNow && !grounded) {
            grounded = true;
            groundTimer = GROUND_COOLDOWN;
            thwomp.setDeltaMovement(Vec3.ZERO);
            thwomp.hurtMarked = true;
            return;
        }

        boolean playerBelow = hasPlayerBelow();

        if (playerBelow && thwomp.getY() > homeY - DETECT_RANGE_Y_DOWN) {
            thwomp.setDeltaMovement(new Vec3(0.0D, FALL_SPEED, 0.0D));
            thwomp.hurtMarked = true;
        } else if (!playerBelow && thwomp.getY() < homeY) {
            thwomp.setDeltaMovement(new Vec3(0.0D, RISE_SPEED, 0.0D));
            thwomp.hurtMarked = true;
        } else {
            thwomp.setDeltaMovement(Vec3.ZERO);
        }
    }

    private boolean hasPlayerBelow() {
        AABB range = thwomp.getBoundingBox()
                .inflate(DETECT_RANGE_XZ, 0.0D, DETECT_RANGE_XZ)
                .move(0.0D, -DETECT_RANGE_Y_DOWN, 0.0D)
                .expandTowards(0.0D, -DETECT_RANGE_Y_DOWN, 0.0D);
        return thwomp.level().getEntitiesOfClass(ServerPlayer.class, range, p -> p.isAlive()).stream()
                .anyMatch(p -> p.getX() >= thwomp.getX() - DETECT_RANGE_XZ
                        && p.getX() <= thwomp.getX() + DETECT_RANGE_XZ
                        && p.getZ() >= thwomp.getZ() - DETECT_RANGE_XZ
                        && p.getZ() <= thwomp.getZ() + DETECT_RANGE_XZ
                        && p.getY() < thwomp.getY());
    }
}
