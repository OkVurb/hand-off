package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Boo behavior: fly toward the player, but freeze and look away while being watched.
 */
public class BooGoal extends Goal {

    private static final double DETECT_RANGE = 24.0D;
    private static final double CHASE_SPEED = 0.18D;
    private static final double LOOK_DOT_THRESHOLD = 0.7D;

    private final BooEntity boo;

    public BooGoal(BooEntity boo) {
        this.boo = boo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !boo.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return !boo.isDeadOrDying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (boo.level().isClientSide()) {
            return;
        }

        Player target = boo.level().getNearestPlayer(boo, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            boo.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 toBoo = boo.position().subtract(target.position()).normalize();
        double lookDot = target.getLookAngle().dot(toBoo);
        boolean beingWatched = lookDot > LOOK_DOT_THRESHOLD;

        if (beingWatched) {
            boo.setDeltaMovement(Vec3.ZERO);
            // Face away from the player (covering its face).
            float awayYaw = (float) Math.toDegrees(Math.atan2(-toBoo.x, -toBoo.z));
            boo.setYRot(awayYaw);
            boo.setYHeadRot(awayYaw);
        } else {
            // Target the player's upper body so the ghost actually flies and floats up.
            Vec3 targetPos = target.position().add(0, 1.2D, 0);
            Vec3 toPlayer = targetPos.subtract(boo.position());
            if (toPlayer.lengthSqr() > 1.0E-6D) {
                Vec3 move = toPlayer.normalize().scale(CHASE_SPEED);
                boo.setDeltaMovement(move);
                boo.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
        boo.hurtMarked = true;
    }
}
