package com.studio.planeshift.common.entity;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Hammer Bro behaviour: hold a perch, hop, and throw hammers at a player who is close enough to
 * do something about it.
 *
 * <p>Three rules, each fixing a way the original read badly:
 *
 * <ul>
 *   <li><b>Range.</b> It used to throw at anything within 24 blocks, which in a side-on course is
 *       most of the visible world — hammers arrived from off-screen with no visible source. It now
 *       only throws inside {@link #THROW_RANGE}, close enough that the thrower is on screen.</li>
 *   <li><b>Jump cadence.</b> A flat 3% chance every tick meant it jumped roughly twice a second
 *       forever, including with nobody near. Jumping is now on a cooldown and only while a player
 *       is in throwing range, so the hop reads as a reaction rather than as an idle animation.</li>
 *   <li><b>Perch.</b> It walked off its platform and wandered. It now stays within
 *       {@link #PERCH_HALF_WIDTH} of where it spawned, so the fight stays where the level
 *       designer put it and the player can approach on their own terms.</li>
 * </ul>
 *
 * <p>Throwing is deliberately independent of being airborne — a hammer released mid-hop is the
 * whole character.
 */
public class HammerBroGoal extends Goal {

    /** How far away a player is still tracked and looked at. */
    private static final double DETECT_RANGE = 24.0D;
    /** How close a player must be before hammers start flying. */
    private static final double THROW_RANGE = 11.0D;
    private static final int THROW_COOLDOWN = 60;

    /** Minimum ticks between hops, so it cannot chain-jump. */
    private static final int JUMP_COOLDOWN = 45;
    /** Chance per eligible tick once the cooldown has expired. */
    private static final double JUMP_CHANCE = 0.06D;
    private static final double JUMP_FORCE = 0.42D;

    /** How far either side of its spawn the Bro may travel along the rail. */
    private static final double PERCH_HALF_WIDTH = 2.5D;

    private final HammerBroEntity bro;
    private int throwTimer = THROW_COOLDOWN;
    private int jumpTimer = JUMP_COOLDOWN;

    /** Where it started. Captured on the first tick, when the entity is finally positioned. */
    private double anchorX = Double.NaN;
    private double anchorZ;

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
        if (Double.isNaN(anchorX)) {
            anchorX = bro.getX();
            anchorZ = bro.getZ();
        }

        holdPerch();

        Player target = bro.level().getNearestPlayer(bro, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            return;
        }

        bro.getLookControl().setLookAt(target, 30.0F, 30.0F);

        boolean inRange = bro.distanceToSqr(target) <= THROW_RANGE * THROW_RANGE;

        if (jumpTimer > 0) {
            jumpTimer--;
        }
        if (inRange && bro.onGround() && jumpTimer <= 0
                && bro.getRandom().nextDouble() < JUMP_CHANCE) {
            Vec3 move = bro.getDeltaMovement();
            bro.setDeltaMovement(move.x, JUMP_FORCE, move.z);
            bro.hurtMarked = true;
            jumpTimer = JUMP_COOLDOWN;
        }

        if (throwTimer > 0) {
            throwTimer--;
        }
        // Airborne throws are allowed on purpose; the arc from the top of a hop is the threat.
        if (inRange && throwTimer <= 0) {
            throwTimer = THROW_COOLDOWN;
            throwHammer(target);
        }
    }

    /**
     * Keeps the Bro on its platform.
     *
     * <p>Corrects position rather than pathfinding, because the perch is a few blocks wide and a
     * navigation-based leash at that scale oscillates. Velocity along the axis is cleared at the
     * same time, so it settles at the edge instead of grinding against an invisible wall.
     */
    private void holdPerch() {
        Vec3 pos = bro.position();
        double clampedX = clamp(pos.x, anchorX);
        double clampedZ = clamp(pos.z, anchorZ);
        if (clampedX == pos.x && clampedZ == pos.z) {
            return;
        }

        bro.setPos(clampedX, pos.y, clampedZ);
        Vec3 move = bro.getDeltaMovement();
        bro.setDeltaMovement(
                clampedX != pos.x ? 0.0D : move.x,
                move.y,
                clampedZ != pos.z ? 0.0D : move.z);
        bro.hurtMarked = true;
    }

    private static double clamp(double value, double anchor) {
        if (value < anchor - PERCH_HALF_WIDTH) {
            return anchor - PERCH_HALF_WIDTH;
        }
        if (value > anchor + PERCH_HALF_WIDTH) {
            return anchor + PERCH_HALF_WIDTH;
        }
        return value;
    }

    private void throwHammer(Player target) {
        Vec3 launch = target.position().add(0.0D, 1.0D, 0.0D).subtract(bro.position()).normalize();
        HammerProjectile hammer = new HammerProjectile(bro.level(), bro);
        hammer.setPos(bro.getX(), bro.getEyeY() - 0.1D, bro.getZ());
        // Arcing throw: slight upward bias, then gravity takes over.
        hammer.shoot((float) launch.x, (float) (launch.y + 0.15D), (float) launch.z, 0.9F, 0.5F);
        bro.level().addFreshEntity(hammer);
    }

    /** The axis the perch is measured along, for callers that place one. */
    public static Direction.Axis perchAxis() {
        return Direction.Axis.X;
    }
}
