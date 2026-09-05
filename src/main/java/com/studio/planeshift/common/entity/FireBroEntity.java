package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

/**
 * A Hammer Bro that throws fire.

 * <p>Flat and fast rather than lobbed, which is the entire difference in how you deal with one: a
 * hammer arcs over a crouch and a fireball does not, so the answer stops being "wait" and becomes
 * "get out of the lane".
 */
public class FireBroEntity extends HammerBroEntity {

    public FireBroEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HammerBroEntity.createAttributes();
    }

    @Override
    protected ThrowableItemProjectile createProjectile() {
        return new FireballProjectile(level(), this);
    }

    @Override
    protected float throwArc() {
        return 0.02F;
    }

    @Override
    protected float throwPower() {
        return 1.25F;
    }
}
