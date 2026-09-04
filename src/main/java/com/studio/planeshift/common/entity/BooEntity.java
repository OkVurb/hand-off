package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Boo - a shy ghost that chases when the player isn't looking.
 */
public class BooEntity extends CourseEnemyEntity {

    public BooEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public boolean isStompable() {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BooGoal(this));
    }

    /** Airborne, and never touching the floor the wave travels along. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

}
