package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Hammer Bro - a ground trooper that jumps and throws hammers.
 */
public class HammerBroEntity extends CourseEnemyEntity {

    public HammerBroEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.17D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HammerBroGoal(this));
    }

    /**
     * The thing this Bro throws.
     *
     * <p>The one hook the whole family hangs off. Fire Bros and Boomerang Bros differ from a Hammer
     * Bro in exactly this and nothing else — same perch clamp, same range gating, same jump, same
     * arc — so they are subclasses overriding one method rather than three copies of a goal that
     * would drift apart the first time the perch logic changed.
     */
    protected net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile createProjectile() {
        return new HammerProjectile(level(), this);
    }

    /**
     * Upward bias on the throw.
     *
     * <p>Exposed because it is the other half of what makes a projectile feel different. A hammer
     * lobs; a fireball goes flat and fast. Same code path, two very different things to dodge.
     */
    protected float throwArc() {
        return 0.15F;
    }

    /** Launch speed. */
    protected float throwPower() {
        return 0.9F;
    }

}

