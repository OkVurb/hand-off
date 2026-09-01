package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Bowser boss behavior: chase the player and breathe straight-line fire.
 */
public class BowserGoal extends Goal {

    private static final double DETECT_RANGE = 48.0D;
    private static final double MELEE_RANGE = 3.5D;
    private static final double FIRE_RANGE = 24.0D;
    private static final int FIRE_COOLDOWN = 45;
    private static final double JUMP_CHANCE = 0.02D;
    private static final double JUMP_FORCE = 0.4D;

    private final BowserEntity bowser;
    private int fireTimer = FIRE_COOLDOWN;

    public BowserGoal(BowserEntity bowser) {
        this.bowser = bowser;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !bowser.isDeadOrDying() && bowser.fallDistance < 1.5F;
    }

    @Override
    public boolean canContinueToUse() {
        return !bowser.isDeadOrDying() && bowser.fallDistance < 1.5F;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (bowser.level().isClientSide()) {
            return;
        }

        Player target = bowser.level().getNearestPlayer(bowser, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            return;
        }

        bowser.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distance = bowser.distanceTo(target);

        if (distance > MELEE_RANGE) {
            bowser.getNavigation().moveTo(target, 0.3D);
        } else {
            bowser.getNavigation().stop();
            if (bowser.getRandom().nextInt(20) == 0) {
                target.hurtServer((ServerLevel) bowser.level(), bowser.damageSources().mobAttack(bowser),
                        (float) bowser.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            }
        }

        if (bowser.onGround() && bowser.getRandom().nextDouble() < JUMP_CHANCE) {
            Vec3 move = bowser.getDeltaMovement();
            bowser.setDeltaMovement(move.x, JUMP_FORCE, move.z);
            bowser.hurtMarked = true;
        }

        fireTimer--;
        if (fireTimer <= 0 && distance <= FIRE_RANGE && bowser.hasLineOfSight(target)) {
            fireTimer = FIRE_COOLDOWN;
            breatheFire(target);
        }
    }

    private void breatheFire(Player target) {
        Vec3 launch = target.position().add(0.0D, 1.0D, 0.0D).subtract(bowser.position()).normalize();
        BowserFire fire = new BowserFire(bowser.level(), bowser);
        fire.setPos(bowser.getX(), bowser.getEyeY() - 0.2D, bowser.getZ());
        fire.shoot((float) launch.x, (float) launch.y, (float) launch.z, 1.2F, 1.0F);
        bowser.level().addFreshEntity(fire);
    }
}
