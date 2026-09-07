package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Chomp on a chain: it lunges, and it cannot follow.
 *
 * <p>The last of the hazards the plan lists as missing. It is worth having for a reason none of the
 * others cover: every threat in the mod so far is either a place ({@code Thwomp} owns a column,
 * a firebar owns a disc) or a thing that travels (a projectile, a patrolling enemy). A Chomp is a
 * <em>radius</em> — a piece of ground that belongs to it, which the player can see the edge of and
 * walk right up to. That is a different question from anything else here.
 *
 * <p>The chain is the entire design. It means the answer is never "run" and never "wait" but
 * "stand where it cannot reach", which is a decision made once and then committed to, and it means
 * the player can be given a corridor that is genuinely safe two blocks from something enormous.
 * Take the tether away and it becomes an ordinary chasing enemy with a bigger hitbox.
 */
public class ChainChompEntity extends CourseEnemyEntity {

    /** How far the chain lets it travel from its post. */
    private static final double TETHER = 4.5D;

    /** How close the player has to be before it lunges. */
    private static final double NOTICE = 9.0D;

    /** Ticks between lunges. Slow: the gap is the part the player is reading. */
    private static final int LUNGE_INTERVAL = 45;

    /** Speed of a lunge, in blocks per tick. */
    private static final double LUNGE_SPEED = 0.55D;

    /** How hard it pulls back to the post between lunges. */
    private static final double RECOIL = 0.12D;

    private double postX = Double.NaN;
    private double postY;
    private int cooldown;

    public ChainChompEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        // None. Everything it does is tethered motion, and a navigator that did not know about the
        // chain would drag it off its post the first time a player stood behind a wall.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (Double.isNaN(postX)) {
            postX = getX();
            postY = getY();
        }

        Player target = level().getNearestPlayer(this, NOTICE);
        Vec3 home = new Vec3(postX, postY, getZ());
        if (target != null && cooldown <= 0) {
            cooldown = LUNGE_INTERVAL;
            Vec3 at = target.position().subtract(position()).normalize().scale(LUNGE_SPEED);
            setDeltaMovement(at.x, at.y * 0.5D, 0.0D);
        } else {
            cooldown--;
            // Drawn back toward the post rather than snapped: the return is as readable as the
            // lunge, and a Chomp that teleported home would make its reach impossible to learn.
            setDeltaMovement(getDeltaMovement().scale(0.86D)
                    .add(home.subtract(position()).scale(RECOIL)));
        }

        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        // The chain, enforced. Clamped rather than repelled, so the reach the player measured by
        // eye is exactly the reach they get -- a soft limit would let a lunge overshoot it and the
        // safe ground would stop being safe at the worst moment.
        Vec3 out = position().subtract(home);
        if (out.horizontalDistance() > TETHER) {
            Vec3 clamped = home.add(out.normalize().scale(TETHER));
            setPos(clamped.x, clamped.y, getZ());
            setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Bolted down. Nothing the player does moves the post. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** Never on the floor, so the shockwave that staggers grounded enemies does not apply. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!Double.isNaN(postX)) {
            output.putDouble("PostX", postX);
            output.putDouble("PostY", postY);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        postX = input.getDoubleOr("PostX", Double.NaN);
        postY = input.getDoubleOr("PostY", 0.0D);
    }
}
