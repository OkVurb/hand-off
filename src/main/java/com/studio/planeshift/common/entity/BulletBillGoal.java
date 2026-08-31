package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Bullet Bill behavior: keep the rotation set on spawn and fly forward.
 * It explodes on wall contact or after a maximum travel time.
 */
public class BulletBillGoal extends Goal {

    private static final double FLIGHT_SPEED = 0.45D;
    private static final int MAX_LIFETIME = 200;

    private final BulletBillEntity bullet;
    private float initialYaw;

    public BulletBillGoal(BulletBillEntity bullet) {
        this.bullet = bullet;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !bullet.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return !bullet.isDeadOrDying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        bullet.setNoGravity(true);
        initialYaw = bullet.getYRot();
        bullet.setYRot(initialYaw);
        bullet.setYHeadRot(initialYaw);
    }

    @Override
    public void tick() {
        if (bullet.level().isClientSide()) {
            return;
        }

        if (bullet.tickCount > MAX_LIFETIME || bullet.horizontalCollision || bullet.verticalCollision) {
            bullet.discard();
            return;
        }

        bullet.setYRot(initialYaw);
        bullet.setYHeadRot(initialYaw);
        Vec3 forward = bullet.getLookAngle().scale(FLIGHT_SPEED);
        bullet.setDeltaMovement(forward);
        bullet.hurtMarked = true;
    }
}
