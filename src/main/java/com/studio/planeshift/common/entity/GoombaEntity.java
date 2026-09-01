package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Goomba - the classic stompable foot soldier.
 */
public class GoombaEntity extends CourseEnemyEntity {

    public GoombaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // No targeting and no melee goal on purpose. A Goomba does not hunt: it walks its lane,
        // turns at a wall or a ledge, and hurts whatever it happens to walk into. Chasing made it
        // read as a zombie in a costume, and in a side-on course a pursuer that leaves its lane is
        // also the thing most likely to end up somewhere the player cannot see it.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LanePatrolGoal(this, 1.0D));
    }
}

