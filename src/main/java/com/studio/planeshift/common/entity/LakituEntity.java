package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Lakitu - hovers above the player and drops Spinies.
 */
public class LakituEntity extends CourseEnemyEntity {

    public LakituEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public boolean isStompable() {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new LakituGoal(this));
    }
}
