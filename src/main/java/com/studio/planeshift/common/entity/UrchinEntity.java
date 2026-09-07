package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A drifting spine ball: the water's answer to a Muncher.
 *
 * <p>The cast needed one thing that cannot be answered at all. Fish are avoided by timing and the
 * pursuing one by moving; both are questions with an answer the player performs. An urchin is a
 * piece of geometry that hurts, and the only thing to do with it is not be there -- which is what
 * makes a corridor with two of them in it a route rather than a fight.
 *
 * <p>It rises and sinks on a slow fixed cycle rather than patrolling sideways, so what it closes
 * is the <em>gap</em> between floor and ceiling rather than a stretch of corridor. That is a
 * different shape of threat from anything else in the water and it is why the drift is vertical.
 */
public class UrchinEntity extends CourseEnemyEntity {

    /** How far it travels above and below where it was placed. */
    private static final double DRIFT = 1.8D;

    /** Ticks for a full rise and fall. Slow enough to swim around, never fast enough to dodge. */
    private static final double PERIOD = 110.0D;

    private double originY = Double.NaN;
    private final int tickOffset;

    public UrchinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(200);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        // None. It has nowhere to go.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (Double.isNaN(originY)) {
            originY = getY();
        }
        double phase = (tickCount + tickOffset) / PERIOD * Math.PI * 2.0D;
        setPos(getX(), originY + Math.sin(phase) * DRIFT, getZ());
        setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Nothing the player has works on it.
     *
     * <p>Not difficulty: it is the point. An urchin that could be stomped would be a slow enemy,
     * and the reason it is worth building is that it is not an enemy at all -- it is terrain that
     * moves.
     */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.STAR);
    }

    /** Never on the floor, so a ground-pound shockwave has nothing to travel to it along. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
