package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

/**
 * A Hammer Bro that throws boomerangs.

 * <p>The dangerous one, because the projectile comes back. A hammer is a thing to dodge once; a
 * boomerang is a thing to dodge, move away from, and then dodge again from the other side, which
 * makes standing still after a successful dodge the wrong instinct.
 */
public class BoomerangBroEntity extends HammerBroEntity {

    public BoomerangBroEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HammerBroEntity.createAttributes();
    }

    @Override
    protected ThrowableItemProjectile createProjectile() {
        return new BoomerangProjectile(level(), this);
    }

    @Override
    protected float throwArc() {
        return 0.10F;
    }

    @Override
    protected float throwPower() {
        return 1.05F;
    }
}
