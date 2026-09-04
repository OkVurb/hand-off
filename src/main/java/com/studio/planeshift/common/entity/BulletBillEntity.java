package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Bullet Bill - a flying cannon projectile that zooms in a straight line.
 */
public class BulletBillEntity extends CourseEnemyEntity {

    public BulletBillEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BulletBillGoal(this));
    }

    /**
     * A Bullet Bill is a shell in flight, so it parts whatever it flies through.
     *
     * <p>The scatter is not scored. The player did not do it, and paying them for it would make
     * standing next to a blaster and waiting a strategy.
     */
    @Override
    public void tick() {
        super.tick();
        if (level() instanceof net.minecraft.server.level.ServerLevel level && isAlive()) {
            com.studio.planeshift.server.EnemyHazardService.plough(level, this, PLOUGH_DAMAGE);
        }
    }

    /** Enough to finish the light infantry it is aimed at, not enough to delete a Hammer Bro. */
    private static final float PLOUGH_DAMAGE = 6.0F;

    /** Airborne, and ordnance rather than something with footing. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

}
