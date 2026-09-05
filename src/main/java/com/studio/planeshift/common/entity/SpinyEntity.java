package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Spiny - covered in spikes; jumping on it hurts the player.
 */
public class SpinyEntity extends CourseEnemyEntity {

    public SpinyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }


    @Override
    protected void registerGoals() {
        // No targeting and no melee goal on purpose. A Spiny does not hunt: it walks its lane,
        // turns at a wall or a ledge, and hurts whatever it happens to walk into. Chasing made it
        // read as a zombie in a costume, and in a side-on course a pursuer that leaves its lane is
        // also the thing most likely to end up somewhere the player cannot see it.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LanePatrolGoal(this, 1.0D));
    }

    /**
     * Flippable: the shell is the entire reason this enemy cannot simply be stomped, so taking it
     * away is exactly what a ground pound is for.
     */
    @Override
    public boolean canBeFlipped() {
        return true;
    }


    /**
     * The spikes are the point: landing on it is the one thing that does not work. Everything else
     * does, and the ground pound is the specific answer - it flips the Spiny onto its back, after
     * which an ordinary stomp finishes it.
     */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.GROUND_POUND, DefeatVector.SHELL,
                DefeatVector.FIRE, DefeatVector.ICE, DefeatVector.STAR);
    }

}

