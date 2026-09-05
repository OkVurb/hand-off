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
    protected void registerGoals() {
        goalSelector.addGoal(0, new LakituGoal(this));
    }

    /** Rides a cloud. There are no feet to sweep. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }


    /**
     * Out of reach of a stomp on its cloud, but not of anything thrown. The spin is the answer a
     * player always has, which is what keeps it an enemy rather than a weather condition.
     */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.SPIN, DefeatVector.SHELL,
                DefeatVector.FIRE, DefeatVector.ICE, DefeatVector.STAR);
    }

}
