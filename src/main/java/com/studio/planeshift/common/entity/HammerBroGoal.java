package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Hammer Bro behavior: hop in place and throw hammers at the player.
 */
public class HammerBroGoal extends Goal {

    private static final double DETECT_RANGE = 24.0D;
    private static final int THROW_COOLDOWN = 60;
    private static final double JUMP_CHANCE = 0.03D;
    private static final double JUMP_FORCE = 0.35D;

    private final HammerBroEntity bro;
    private int throwTimer = THROW_COOLDOWN;

    public HammerBroGoal(HammerBroEntity bro) {
        this.bro = bro;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !bro.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return !bro.isDeadOrDying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (bro.level().isClientSide()) {
            return;
        }

        Player target = bro.level().getNearestPlayer(bro, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            return;
        }

        bro.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (bro.onGround() && bro.getRandom().nextDouble() < JUMP_CHANCE) {
            Vec3 move = bro.getDeltaMovement();
            bro.setDeltaMovement(move.x, JUMP_FORCE, move.z);
            bro.hurtMarked = true;
        }

        throwTimer--;
        if (throwTimer <= 0) {
            throwTimer = THROW_COOLDOWN;
            throwHammer(target);
        }
    }

    private void throwHammer(Player target) {
        Vec3 launch = target.position().add(0.0D, 1.0D, 0.0D).subtract(bro.position()).normalize();
        HammerProjectile hammer = new HammerProjectile(bro.level(), bro);
        hammer.setPos(bro.getX(), bro.getEyeY() - 0.1D, bro.getZ());
        // Arcing throw: slight upward bias, then gravity takes over.
        hammer.shoot((float) launch.x, (float) (launch.y + 0.15D), (float) launch.z, 0.9F, 0.5F);
        bro.level().addFreshEntity(hammer);
    }
}
